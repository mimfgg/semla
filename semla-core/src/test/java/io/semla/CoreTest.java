package io.semla;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(glue = {"io.semla.serialization", "io.semla.cucumber.steps"})
public class CoreTest {}
