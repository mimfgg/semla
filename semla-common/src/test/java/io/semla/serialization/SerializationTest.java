package io.semla.serialization;

import io.semla.exception.DeserializationException;
import io.semla.exception.SerializationException;
import io.semla.model.Child;
import io.semla.model.Parent;
import io.semla.model.Score;
import io.semla.reflect.Annotations;
import io.semla.reflect.TypeReference;
import io.semla.reflect.Types;
import io.semla.serialization.io.OutputStreamWriter;
import io.semla.serialization.json.Json;
import io.semla.serialization.json.JsonSerializer;
import io.semla.serialization.yaml.Yaml;
import io.semla.util.Lists;
import io.semla.util.Maps;
import io.semla.util.Splitter;
import io.semla.util.Strings;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SerializationTest {

    @Test
    public void deserializerCanUseTypeReference() {
        String content = "[1,2,3,4,5]";
        Set<Integer> set = Yaml.getDeserializer().read(content, new TypeReference<Set<Integer>>() {
        });
        assertThat(Json.write(set)).isEqualTo(content);
    }

    @Test
    public void deserializerCanUseComponentTypes() {
        String content = "[1,2,3,4,5]";
        List<Integer> list = Json.read(content, Types.parameterized(List.class, Integer.class));
        assertThat(Json.write(list)).isEqualTo(content);
    }

    @Test
    public void deserializeFromAndToAStream() {
        String content = "[1,2,3,4,5]";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes());
        List<Integer> list = Json.getDeserializer().read(inputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Json.getSerializer().write(list, byteArrayOutputStream);
        assertThat(byteArrayOutputStream.toString()).isEqualTo(content);

        ByteArrayOutputStream yamlByteArrayOutputStream = new ByteArrayOutputStream();
        Yaml.getSerializer().write(list, yamlByteArrayOutputStream);
        assertThat(yamlByteArrayOutputStream.toString()).isEqualTo(
                "- 1\n" +
                        "- 2\n" +
                        "- 3\n" +
                        "- 4\n" +
                        "- 5");

        assertThat(Json.getDeserializer().<List<Integer>>read(new ByteArrayInputStream(content.getBytes()), Types.parameterized(List.class, Integer.class)))
                .isEqualTo(Lists.of(1, 2, 3, 4, 5));
        assertThat(Json.getDeserializer().read(new ByteArrayInputStream(content.getBytes()), new TypeReference<List<Integer>>() {}))
                .isEqualTo(Lists.of(1, 2, 3, 4, 5));
    }

    @Test
    public void failingStreams() {
        assertThatThrownBy(() -> Json.getDeserializer().read(new InputStream() {
            @Override
            public synchronized int read() throws IOException {
                throw new IOException("something went wrong");
            }
        }))
                .isInstanceOf(DeserializationException.class)
                .hasMessage("something went wrong");

        assertThatThrownBy(() -> Json.getDeserializer().read(new InputStream() {
            @Override
            public int read() throws IOException {
                return 0;
            }

            @Override
            public synchronized int available() throws IOException {
                throw new IOException("something went wrong");
            }
        }))
                .isInstanceOf(DeserializationException.class)
                .hasMessage("something went wrong");

        assertThatThrownBy(() -> Json.getSerializer().write(new LinkedHashMap<>(), new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new IOException("something went wrong");
            }
        }))
                .isInstanceOf(SerializationException.class)
                .hasMessage("something went wrong");
    }

    @Test
    public void outputStreamWriter() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(byteArrayOutputStream);
        assertThat(outputStreamWriter.isEmpty()).isTrue();
        outputStreamWriter.append(true);
        assertThat(outputStreamWriter.isEmpty()).isFalse();
        outputStreamWriter.append('c');
        outputStreamWriter.append(1);
        outputStreamWriter.append(1L);
        outputStreamWriter.append(1f);
        outputStreamWriter.append(1d);
        outputStreamWriter.append("TEST");
        assertThat(byteArrayOutputStream.toString()).isEqualTo("truec111.01.0TEST");
    }

    @Test
    public void is() {
        Strings.firstNonWhitespaceCharacterIs(" {}", '{');
        assertThat(Json.isJson(" {}")).isTrue();
        assertThat(Json.isJson("[]")).isTrue();
    }

    @Test
    public void deserializeFromAndToASlowStream() {
        String content = "[1,2,3,4,5]";
        InputStream inputStream = new InputStream() {
            int position;
            boolean blocking = true;

            @Override
            public int read() {
                if (position == content.length()) {
                    return -1;
                }
                blocking ^= true;
                if (blocking) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(2);
                    } catch (InterruptedException e) {
                        throw new AssertionError();
                    }
                }
                return content.charAt(position++);
            }

            @Override
            public int available() {
                return blocking ? 0 : 1;
            }
        };
        List<Integer> list = Json.getDeserializer().read(inputStream);
        StringBuilder output = new StringBuilder();
        OutputStream outputStream = new OutputStream() {
            boolean blocking = true;

            @Override
            public void write(int b) {
                blocking ^= true;
                if (blocking) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(2);
                    } catch (InterruptedException e) {
                        throw new AssertionError();
                    }
                }
                output.append((char) b);
            }
        };

        Json.getSerializer().write(list, outputStream);
        assertThat(output.toString()).isEqualTo(content);
    }

    @Test
    public void breakOnUnknownProperties() {
        try {
            Yaml.read("score: 1\nnick: test", Score.class);
            Assert.fail("should have failed");
        } catch (DeserializationException e) {
            assertThat(e.getMessage()).isEqualTo("unknown property 'nick' on class io.semla.model.Score");
        }
    }

    @Test
    public void ignoreUnknownProperties() {
        Yaml.read("id: 1\nnick: test", Score.class, Deserializer.IGNORE_UNKNOWN_PROPERTIES);
    }

    @Test
    public void deserializeAStringStartingWithANumber() {
        assertThat(Yaml.<Object>read("1 and some content")).isEqualTo("1 and some content");
        assertThat(Json.<Object>read("\"1 and some content\"")).isEqualTo("1 and some content");
        assertThatThrownBy(() -> Json.read("1 and some content"))
                .isInstanceOf(DeserializationException.class)
                .hasMessage("unexpected character 'a' at 2/18");
        assertThatThrownBy(() -> Json.read("1 \"and some content\""))
                .isInstanceOf(DeserializationException.class)
                .hasMessage("unexpected trailing content of type STRING in column: 2 line: 0 character: '\"' @2/20");
    }

    @Test
    public void polymorphic() {
        Types.registerSubType(Child.class);
        assertThat(Yaml.read("type: child", Parent.class)).isNotNull().isInstanceOf(Child.class);
        assertThatThrownBy(() -> Yaml.read("name: bob\ntype: child", Parent.class))
                .isInstanceOf(DeserializationException.class)
                .hasMessage("while using polymorphic deserialization on interface io.semla.model.Parent," +
                        " 'type' must be the first property, was 'name'");
    }

    @Test
    public void wrong_context() {
        assertThatThrownBy(() -> Yaml.read("true", Score.class))
                .isInstanceOf(DeserializationException.class)
                .hasMessage("cannot deserialize a class io.semla.model.Score out of a BOOLEAN");
        assertThatThrownBy(() -> Yaml.read("true", List.class))
                .isInstanceOf(DeserializationException.class)
                .hasMessage("cannot deserialize a class java.util.ArrayList out of a BOOLEAN");
        assertThatThrownBy(() -> Yaml.read("true", Map.class))
                .isInstanceOf(DeserializationException.class)
                .hasMessage("cannot deserialize a class java.util.LinkedHashMap out of a BOOLEAN");
    }

    @Test
    public void options() {
        Json.getSerializer().options().add(JsonSerializer.PRETTY);
        Json.getSerializer().options().remove(JsonSerializer.PRETTY);
    }

    @Test
    public void customReaderAndWriter() {
        Yaml.getDeserializer().read(SplittedContent.class).as(Token.STRING, value -> Splitter.on('.').split(value).map(SplittedContent::new));
        Yaml.getSerializer().write(SplittedContent.class).as(splittedContent -> String.join(".", splittedContent.getValues()));
        SplittedContent splittedContent = Yaml.read("a.b.c.d", SplittedContent.class);
        assertThat(splittedContent.getValues()).isEqualTo(Lists.of("a", "b", "c", "d"));
        assertThat(Yaml.write(splittedContent)).isEqualTo("a.b.c.d");
    }

    public static class SplittedContent {

        private final List<String> values;

        public SplittedContent(List<String> values) {
            this.values = values;
        }

        public List<String> getValues() {
            return values;
        }
    }

    @Test
    public void unexpectedCharacters() {
        assertThatThrownBy(() -> Json.read("{\"value\": nice}"))
                .hasMessage("expected the next non white space character to be 'u' but it was 'i' at index: 11/15");
    }

    @Test
    public void windowsLineBreaks() {
        assertThat(Yaml.<Map<String, String>>read("content:\r\n  value")).isEqualTo(Maps.of("content", "value"));
    }

    @Test
    public void invalidFlowScalar() {
        assertThatThrownBy(() -> Yaml.read("something:\ntest "))
                .hasMessage("Plain flow scalars cannot be placed at the same indentation level than their property @column: 0 line: 1 character: 't' @11/16");
    }

    @Test
    public void escapedContent() {
        assertThat(Json.<Map<String, String>>read("{\"content\":\"value with a \\\\ and a \\\"\"}"))
                .isEqualTo(Maps.of("content", "value with a \\ and a \""));
        assertThat(Json.write("\t\n\b\r\f\\\"")).isEqualTo("\"\\t\\n\\b\\r\\f\\\\\\\"\"");
    }

    @Test
    public void proxyAndAnnotationsSerialization() {
        assertThat(Json.write(Annotations.proxyOf(TestAnnotation.class, Maps.of("value", "test")))).isEqualTo("{\"value\":\"test\"}");
        assertThat(Json.read("{\"value\":\"test\"}", TestAnnotation.class)).isEqualTo(Annotations.proxyOf(TestAnnotation.class, Maps.of("value", "test")));
    }

    @Target(TYPE)
    @Retention(RUNTIME)
    public @interface TestAnnotation {

        String value();

    }
}
