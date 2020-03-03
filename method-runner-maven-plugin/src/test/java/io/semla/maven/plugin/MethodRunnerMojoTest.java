package io.semla.maven.plugin;

import junit.framework.Assert;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;


public class MethodRunnerMojoTest extends AbstractMojoTestCase {

    protected void setUp() throws Exception {
        // required for mojo lookups to work
        super.setUp();
    }

    public void testMethodInvocation() throws Exception {
        MavenProject mavenProject = getMavenProject(getBasedir() + "/src/test/resources/test-pom.xml");
        MethodRunnerMojo mojo = (MethodRunnerMojo) lookupConfiguredMojo(mavenProject, "generate");
        assertNotNull(mojo);
        mojo.execute();
        assertEquals("test", new String(Files.readAllBytes(new File("target/test").toPath())));
    }

    public void testFailingMethodInvocation() throws Exception {
        MavenProject mavenProject = getMavenProject(getBasedir() + "/src/test/resources/invalid-test-pom.xml");
        MethodRunnerMojo mojo = (MethodRunnerMojo) lookupConfiguredMojo(mavenProject, "generate");
        assertNotNull(mojo);
        try {
            mojo.execute();
            Assert.fail("didn't thow an exception");
        } catch (Exception e) {
            Assert.assertEquals("didn't find any method matching parameters: {arg0=test, arg1=extra}", e.getCause().getMessage());
        }
    }

    public static void someMethod(String someParam) throws IOException {
        Files.write(new File("target/test").toPath(), someParam.getBytes());
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