package io.semla.persistence;

import io.semla.relation.Relation;

import java.util.LinkedHashSet;
import java.util.Set;

public class RelationContext {

    private final Set<String> relations = new LinkedHashSet<>();

    public <T, R> boolean shouldFetch(Relation<T, R> relation, T parent) {
        return isNot("fetch", relation, parent);
    }

    public <T, R> boolean shouldCreateOrUpdate(Relation<T, R> relation, T parent) {
        return isNot("createOrUpdate", relation, parent);
    }

    public <T, R> boolean shouldDelete(Relation<T, R> relation, T parent) {
        return isNot("delete", relation, parent);
    }

    private <T, R> boolean isNot(String operation, Relation<T, R> relation, T parent) {
        return relations.add("%s->%s::%s->%s".formatted(
            operation,
            parent.getClass().getCanonicalName(),
            relation.parentModel().key().member().getOn(parent),
            relation.member().getName())
        );
    }
}
