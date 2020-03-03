package io.semla.inject;

import io.semla.util.Strings;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AnnotationMatcher implements Predicate<Annotation[]> {

    private final List<String> annotations = new ArrayList<>();

    public AnnotationMatcher() {}

    public AnnotationMatcher add(Annotation annotation) {
        annotations.add(Strings.toString(annotation));
        return this;
    }

    @Override
    public boolean test(Annotation[] annotations) {
        if (this.annotations.isEmpty()) {
            return true;
        }
        return Stream.of(annotations).map(Strings::toString).collect(Collectors.toSet()).containsAll(this.annotations);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnnotationMatcher that = (AnnotationMatcher) o;
        return Strings.toString(annotations).equals(Strings.toString(that.annotations));
    }

    @Override
    public int hashCode() {
        return Strings.toString(annotations).hashCode();
    }


}
