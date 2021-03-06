package io.semla.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Splitter {

    private final char on;
    private boolean omitEmptyStrings;
    private boolean trim;

    public static Splitter on(char c) {
        return new Splitter(c);
    }

    private Splitter(char on) {
        this.on = on;
    }

    public Splitter trim() {
        this.trim = true;
        return this;
    }

    public Splitter omitEmptyStrings() {
        omitEmptyStrings = true;
        return this;
    }

    public Splitted split(String content) {
        List<String> splitted = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == on) {
                splitted.add(buffer.toString());
                buffer = new StringBuilder();
                if (i == content.length() - 1) {
                    splitted.add(buffer.toString());
                }
            } else {
                buffer.append(c);
            }
        }
        if (buffer.length() > 0) {
            splitted.add(buffer.toString());
        }
        if (trim || omitEmptyStrings) {
            splitted = splitted.stream()
                .map(token -> trim ? token.trim() : token)
                .filter(token -> !omitEmptyStrings || token.length() > 0)
                .collect(Collectors.toList());
        }
        return new Splitted(splitted);
    }

    public static class Splitted {

        private final List<String> splitted;

        public Splitted(List<String> splitted) {
            this.splitted = splitted;
        }

        public List<String> toList() {
            return splitted;
        }

        public <ListType extends List<String>> ListType toList(Supplier<ListType> listSupplier) {
            ListType list = listSupplier.get();
            list.addAll(splitted);
            return list;
        }

        public String[] toArray() {
            return splitted.toArray(new String[0]);
        }

        public Splitted forEach(Consumer<String> action) {
            splitted.forEach(action);
            return this;
        }

        public Stream<String> stream() {
            return splitted.stream();
        }

        public <E> E map(Function<List<String>, E> mapper) {
            return mapper.apply(splitted);
        }
    }
}
