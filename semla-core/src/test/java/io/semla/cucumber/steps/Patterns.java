package io.semla.cucumber.steps;


public final class Patterns {

    public static final String A = "(?:(?:a|an|this|that|the|those|these) ?)";
    public static final String QUOTED_CONTENT = "\"([^\"]*)\"";
    public static final String THAT = "(?:that )?";
    public static final String A_USER = "(?:[\\w ]+)?";
    public static final String VARIABLE = "([_a-zA-Z][_\\-.\\w\\[\\]]+)";
    public static final String CLASS_NAME = "((?:\\{\\{\\S+\\}\\}.)?(?:[\\p{L}_$][\\p{L}\\p{N}_$]*\\.)*[\\p{Upper}_$][\\p{L}\\p{N}_$]*) ?";

    private Patterns() {
    }
}
