package io.semla.relation;

import io.semla.persistence.PersistenceContext;
import io.semla.query.Include;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface NToOneRelation<ParentType, ChildType> extends Relation<ParentType, ChildType> {

    @Override
    default void fetchOn(ParentType parent, PersistenceContext context, Include<ParentType, ChildType> include) {
        getFor(parent, context, include).ifPresent(child -> member().setOn(parent, child));
    }

    @Override
    Optional<ChildType> getFor(ParentType parent, PersistenceContext context, Include<ParentType, ChildType> include);

    @Override
    default void fetchOn(Collection<ParentType> parents, PersistenceContext context, Include<ParentType, ChildType> include) {
        getFor(parents, context, include).forEach((parent, child) -> member().setOn(parent, child));
    }

    @Override
    Map<ParentType, ChildType> getFor(Collection<ParentType> parents, PersistenceContext context, Include<ParentType, ChildType> include);
}
