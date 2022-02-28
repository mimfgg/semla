package io.semla.maven.plugin;

import io.semla.maven.plugin.model.Child;
import io.semla.maven.plugin.model.Hobby;
import io.semla.maven.plugin.model.Parent;
import io.semla.reflect.Types;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;


public class SemlaMojoTest extends AbstractMojoTestCase {

    protected void setUp() throws Exception {
        // required for mojo lookups to work
        super.setUp();
    }

    public void testPomWithPath() throws Exception {
        MavenProject mavenProject = getMavenProject(getBasedir() + "/test-pom.xml");
        SemlaMojo mojo = (SemlaMojo) lookupConfiguredMojo(mavenProject, "generate");
        assertNotNull(mojo);
        mojo.execute();

        File[] files = Files.list(new File(mojo.getOutputPath()+"/io/semla/maven/plugin/model").toPath())
            .map(Path::toFile)
            .toArray(File[]::new);

        Types.compileFromFiles(mojo.getClasspathElements(), files);

        assertNotNull(Class.forName(Child.class.getCanonicalName() + "Manager"));
        assertNotNull(Class.forName(Hobby.class.getCanonicalName() + "Manager"));
        assertNotNull(Class.forName(Parent.class.getCanonicalName() + "Manager"));
    }

    private MavenProject getMavenProject(String pomPath) throws Exception {
        File pom = new File(pomPath);
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        getContainer().lookup(MavenExecutionRequestPopulator.class).populateDefaults(request);
        request.setPom(pom);
        ProjectBuildingRequest configuration = request.getProjectBuildingRequest();
        return lookup(ProjectBuilder.class).build(pom, configuration).getProject();
    }
}