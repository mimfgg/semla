package io.semla.query;

import io.semla.relation.IncludeTypes;
import io.semla.relation.Relation;

import java.util.Set;
import java.util.function.Function;

public class Include<T, R> {

    protected final Relation<T, R> relation;
    protected final Includes<R> includes;
    protected final IncludeTypes includeTypes;

    public Include(Relation<T, R> relation, IncludeTypes includeTypes) {
        this(relation, includeTypes, Includes.of(relation.childModel()));
    }

    public Include(Relation<T, R> relation, IncludeTypes includeTypes, Set<Relation<?, ?>> knownRelations) {
        this(relation, includeTypes, Includes.matching(relation.childModel(), includeTypes, knownRelations));
    }

    public Include(Relation<T, R> relation, IncludeTypes includeTypes, Includes<R> includes) {
        this.relation = relation;
        this.includeTypes = includeTypes;
        this.includes = includes;
    }

    public IncludeTypes type() {
        return includeTypes;
    }

    public Relation<T, R> relation() {
        return relation;
    }

    public Includes<R> includes() {
        return includes;
    }

    public Includes<R> addTo(Includes<R> includes) {
        includes.relations().putAll(this.includes.relations());
        return includes;
    }

    protected void appendTo(StringBuilder builder) {
        builder.append(relation.member().getName());
        if (includeTypes.value() != 0 && !IncludeTypes.any().equals(includeTypes)) {
            builder.append(includeTypes.toString());
        }
        if (!includes.isEmpty()) {
            includes.appendTo(builder.append('{')).append('}');
        }
        builder.append(',');
    }

    public Include<T, R> include(String include) {
        includes.include(include);
        return this;
    }

    public <N> Include<T, R> include(String relationName, Function<Include<R, N>, Include<R, N>> function) {
        includes.include(relationName, function);
        return this;
    }

    public static <T, R> Include<T, R> anyOf(Relation<T, R> relation) {
        return new Include<>(relation, IncludeTypes.any());
    }
}
