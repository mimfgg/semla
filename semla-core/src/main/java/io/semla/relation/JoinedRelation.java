package io.semla.relation;

import io.semla.model.EntityModel;
import io.semla.persistence.PersistenceContext;
import io.semla.query.Include;
import io.semla.query.Includes;

import java.util.Collection;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public interface JoinedRelation<ParentType, JoinType, ChildType> extends Relation<ParentType, ChildType> {

    default EntityModel<JoinType> relationModel() {
        return EntityModel.of(relationClass());
    }

    Class<JoinType> relationClass();

    String parentFieldName();

    String childFieldName();

    default void deleteOn(ParentType parent, PersistenceContext context, Include<ParentType, ChildType> include) {
        context.select(relationClass()).where(parentFieldName()).is(parentModel().key().member().getOn(parent)).delete(with(include));
    }

    default void deleteOn(Collection<ParentType> parents, PersistenceContext context, Include<ParentType, ChildType> include) {
        context.select(relationClass())
            .where(parentFieldName()).in(parents.stream().map(parentModel().key().member()::getOn).collect(Collectors.toList())).delete(with(include));
    }

    default UnaryOperator<Includes<JoinType>> with(Include<ParentType, ChildType> include) {
        return includes -> includes.include(relationModel().relationByFieldName(childFieldName()), include.type(), include.includes());
    }
}
