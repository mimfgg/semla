package io.semla.relation;

import io.semla.exception.InvalidPersitenceAnnotationException;
import io.semla.persistence.PersistenceContext;
import io.semla.query.Include;
import io.semla.reflect.Fields;
import io.semla.reflect.Member;
import io.semla.reflect.Properties;
import io.semla.reflect.Types;

import javax.persistence.OneToMany;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public interface NToManyRelation<ParentType, ChildType> extends Relation<ParentType, ChildType> {

    default Member<ChildType> getMappedBy(Member<ParentType> member, OneToMany oneToMany, Class<ChildType> childClass) {
        if (!Fields.hasField(childClass, oneToMany.mappedBy())) {
            throw new InvalidPersitenceAnnotationException(
                "@OneToMany.mappedBy on " + member + " defines a non existent member '" + oneToMany.mappedBy() + "' on " + childClass);
        }
        return Properties.membersOf(childClass).get(oneToMany.mappedBy());
    }

    @Override
    default void fetchOn(ParentType parent, PersistenceContext context, Include<ParentType, ChildType> include) {
        member().setOn(parent, getFor(parent, context, include));
    }

    @Override
    Collection<ChildType> getFor(ParentType parent, PersistenceContext context, Include<ParentType, ChildType> include);

    @Override
    default void fetchOn(Collection<ParentType> parents, PersistenceContext context, Include<ParentType, ChildType> include) {
        if (!parents.isEmpty()) {
            getFor(parents, context, include).forEach((parent, list) ->
                member().setOn(parent, context.entityContext().remapOrCache(list))
            );
        }
    }

    @Override
    Map<ParentType, Collection<ChildType>> getFor(Collection<ParentType> parents, PersistenceContext context, Include<ParentType, ChildType> include);

    default Collector<ChildType, ?, Collection<ChildType>> toCollection() {
        return Collectors.toCollection(collectionSupplier());
    }

    default Supplier<Collection<ChildType>> collectionSupplier() {
        return Types.supplierOf(member().getGenericType());
    }

}
