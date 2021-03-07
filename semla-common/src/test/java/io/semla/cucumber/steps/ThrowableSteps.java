package io.semla.cucumber.steps;

import io.cucumber.java.After;
import io.cucumber.java.en.Then;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;
import org.junit.Assert;

import static io.semla.cucumber.steps.Patterns.CLASS_NAME;

public class ThrowableSteps {

    private Throwable thrown = null;

    @Then("^an " + CLASS_NAME + " is thrown with a message matching:$")
    public void anExceptionIsThrownWithAMessageMatching(String exceptionType, String expectedMessage) {
        Assert.assertNotNull(thrown);
        try {
            if (!thrown.getClass().getCanonicalName().endsWith(exceptionType)) {
                thrown.printStackTrace();
                Assert.fail("was excepting an exception of type '" + exceptionType + "' but got a " + thrown.getClass().getCanonicalName());
            } else if (expectedMessage != null) {
                Assert.assertTrue(thrown.getMessage() + "\ndoesn't match:\n" + expectedMessage, thrown.getMessage().matches(expectedMessage));
            }
        } finally {
            thrown = null;
        }
    }

    public void catchThrowable(ThrowableAssert.ThrowingCallable shouldRaiseThrowable) {
        thrown = Assertions.catchThrowable(shouldRaiseThrowable);
    }

    @After
    public void assertNothingWasThrown() {
        if (thrown != null) {
            throw new AssertionError(thrown + " got caught be didn't get asserted", thrown);
        }
    }

}
