package io.semla.relation;

import io.semla.model.EntityModel;
import io.semla.persistence.PersistenceContext;
import io.semla.query.Include;
import io.semla.reflect.Member;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Map;

public interface Relation<ParentType, ChildType> {

    IncludeTypes defaultIncludeType();

    void fetchOn(ParentType parent, PersistenceContext context, Include<ParentType, ChildType> include);

    Object getFor(ParentType parent, PersistenceContext context, Include<ParentType, ChildType> include);

    void fetchOn(Collection<ParentType> parents, PersistenceContext context, Include<ParentType, ChildType> include);

    Map<ParentType, ?> getFor(Collection<ParentType> parents, PersistenceContext context, Include<ParentType, ChildType> include);

    void createOrUpdateOn(ParentType parent, PersistenceContext context, Include<ParentType, ChildType> include);

//    void createOrUpdateOn(Collection<ParentType> parents, PersistenceContext context, Include<ParentType, ChildType> include);

    default void createOrUpdateOn(Collection<ParentType> parents, PersistenceContext context, Include<ParentType, ChildType> include) {
        parents.forEach(parent -> createOrUpdateOn(parent, context, include)); // FIXME: batch it
    }

    void deleteOn(ParentType parent, PersistenceContext context, Include<ParentType, ChildType> include);

    void deleteOn(Collection<ParentType> parents, PersistenceContext context, Include<ParentType, ChildType> include);

    EntityModel<ParentType> parentModel();

    EntityModel<ChildType> childModel();

    Member<ParentType> member();

    Logger logger();
}
