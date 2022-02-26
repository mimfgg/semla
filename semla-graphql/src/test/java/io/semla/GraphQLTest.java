package io.semla;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(plugin = "pretty", glue = "com.decathlon.tzatziki.steps")
public class GraphQLTest {}
