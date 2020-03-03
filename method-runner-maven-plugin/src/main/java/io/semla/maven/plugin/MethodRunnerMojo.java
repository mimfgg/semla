package io.semla.maven.plugin;

import org.apache.commons.lang3.ClassUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;

@Mojo(
    name = "generate",
    requiresDependencyResolution = TEST,
    requiresDependencyCollection = TEST
)
public class MethodRunnerMojo extends AbstractMojo {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Parameter(readonly = true, defaultValue = "${project}")
    private MavenProject project;

    @Parameter(property = "classname")
    private String classname;

    @Parameter(property = "methodname")
    private String methodname;

    @Parameter(property = "parameters")
    private Map<String, Object> parameters;

    @Override
    public void execute() {
        try {
            logger.info("invoking: " + classname + "." + methodname + "(" + parameters + ")");
            Method addUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addUrl.setAccessible(true);
            Set<File> jarList = project.getCompileClasspathElements().stream().map(File::new).collect(Collectors.toSet());
            jarList.addAll(project.getTestClasspathElements().stream().map(File::new).collect(Collectors.toSet()));
            for (File file : jarList) {
                addUrl.invoke(ClassLoader.getSystemClassLoader(), file.toURI().toURL());
            }
            Class<?> clazz = ClassLoader.getSystemClassLoader().loadClass(classname);
            Method method = Stream.of(clazz.getDeclaredMethods())
                .filter(m -> m.getName().equals(methodname) && m.getParameterCount() == parameters.size())
                .filter(m -> Arrays.stream(m.getParameters(), 0, m.getParameterCount())
                    .allMatch(parameter -> ClassUtils.isAssignable(parameters.get(parameter.getName()).getClass(), parameter.getType())))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("didn't find any method matching parameters: " + parameters));
            Object[] args = Stream.of(method.getParameters())
                .map(parameter -> parameters.get(parameter.getName()))
                .toArray();
            method.invoke(null, args);
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
