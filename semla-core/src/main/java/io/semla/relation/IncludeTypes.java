package io.semla.relation;

import javax.persistence.CascadeType;
import javax.persistence.FetchType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static io.semla.relation.IncludeType.*;

public class IncludeTypes {

    private final int value;

    public IncludeTypes(FetchType fetchType, CascadeType[] cascadeTypes, boolean orphanRemoval) {
        this.value = Stream.concat(
            Stream.of(cascadeTypes).map(IncludeType::fromCascadeType),
            Stream.of(fromFetchType(fetchType), fromOrphanRemoval(orphanRemoval)))
            .map(IncludeType::value)
            .reduce(NONE.value(), (a, b) -> a | b);
    }

    public IncludeTypes(IncludeType... includeTypes) {
        this.value = Stream.of(includeTypes).map(IncludeType::value).reduce(NONE.value(), (a, b) -> a | b);
    }

    public int value() {
        return value;
    }

    public boolean isEager() {
        return FETCH.isContainedIn(value);
    }

    public boolean should(CascadeType cascade) {
        return fromCascadeType(cascade).isContainedIn(value);
    }

    public boolean matchesAnyOf(IncludeTypes includeType) {
        return (this.value & includeType.value) > 0;
    }

    public boolean should(IncludeType includeType) {
        return includeType.isContainedIn(this.value);
    }

    @Override
    public String toString() {
        List<String> includeTypes = new ArrayList<>();
        if (isEager()) {
            includeTypes.add("FETCH");
        }
        if (ALL.isContainedIn(value)) {
            includeTypes.add("ALL");
        } else {
            Stream.of(CREATE, UPDATE, DELETE)
                .filter(includeType -> includeType.isContainedIn(value))
                .forEach(includeType -> includeTypes.add(includeType.name()));
        }
        if (DELETE_ORPHANS.isContainedIn(value)) {
            includeTypes.add("DELETE_ORPHANS");
        }
        return includeTypes.toString();
    }

    public static IncludeTypes any() {
        return of(ALL, FETCH);
    }

    public static IncludeTypes none() {
        return of(NONE);
    }

    public static IncludeTypes of(IncludeType... includeTypes) {
        return new IncludeTypes(includeTypes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IncludeTypes that = (IncludeTypes) o;
        return value == that.value;
    }
}
