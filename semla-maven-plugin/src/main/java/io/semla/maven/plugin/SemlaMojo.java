package io.semla.maven.plugin;

import io.semla.persistence.TypedEntityManager;
import io.semla.reflect.Types;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;

@Mojo(
    name = "generate",
    threadSafe = true,
    aggregator = true,
    requiresDependencyResolution = TEST,
    requiresDependencyCollection = TEST
)
public class SemlaMojo extends AbstractMojo {

    @Parameter(readonly = true, defaultValue = "${project}")
    private MavenProject project;

    @Parameter
    private List<String> additionalClasspathElements = new ArrayList<>();

    @Parameter
    private String additionalClasspath;

    @Parameter
    private List<String> sources = new ArrayList<>();

    @Parameter(defaultValue = "${basedir}/target/generated-sources/java/")
    private String outputPath;

    @Parameter(defaultValue = "RUNTIME")
    private String resolutionScope;

    @Override
    public void execute() {
        try {
            additionalClasspathElements = additionalClasspathElements.stream().map(this::withBasedir).collect(Collectors.toList());
            sources = sources.stream().map(this::withBasedir).toList();
            outputPath = withBasedir(outputPath);
            List<String> classpathElements = getClasspathElements();
            List<File> jarList = classpathElements.stream().map(File::new).toList();
            for (File file : jarList) {
                Types.addToClassLoader(file.toURI().toURL());
            }

            List<File> processedSources = TypedEntityManager.preProcessSources(classpathElements, outputPath, sources);
            if (processedSources.isEmpty()) {
                getLog().warn("No entity found in " + sources + "!");
            } else {
                getLog().info("Generated:");
                processedSources.forEach(file -> {
                    getLog().info("\t" + file.getAbsolutePath());
                });
                project.addCompileSourceRoot(new File(outputPath).getAbsolutePath());
                getLog().info("Added source directory: " + outputPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private String withBasedir(String path) {
        if (path.startsWith(project.getBasedir().getPath())) {
            return path;
        }
        return project.getBasedir().getPath() + path;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public List<String> getClasspathElements() throws DependencyResolutionRequiredException, IOException {
        List<String> classpathElements = new ArrayList<>();

        // collect provided dependencies, they will not be collected automatically
        Set<String> providedClasspathElements = project.getDependencies().stream()
            .filter(dependency -> dependency.getScope().equals("provided"))
            .map(dependency -> dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getVersion())
            .collect(Collectors.toSet());
        project.getArtifacts().stream()
            .filter(artifact -> providedClasspathElements.contains(artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion()))
            .forEach(artifact -> classpathElements.add(artifact.getFile().getAbsolutePath()));

        //
        switch (ResolutionScope.valueOf(resolutionScope.toUpperCase())) {
            case TEST -> classpathElements.addAll(project.getTestClasspathElements());
            case COMPILE -> classpathElements.addAll(project.getCompileClasspathElements());
            case RUNTIME -> classpathElements.addAll(project.getRuntimeClasspathElements());
            default -> throw new UnsupportedOperationException(
                "resolution scope " + resolutionScope + " is not supported. Supported values are [TEST, COMPILE, RUNTIME]"
            );
        }
        classpathElements.addAll(additionalClasspathElements);
        if (additionalClasspath != null) {
            Files.list(new File(withBasedir(additionalClasspath)).toPath()).forEach(path -> classpathElements.add(path.toAbsolutePath().toString()));
        }
        return classpathElements;
    }

}
