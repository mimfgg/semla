package io.semla;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(strict = true, glue = {"io.semla.serialization", "io.semla.cucumber.steps"})
public class CoreTest {}
