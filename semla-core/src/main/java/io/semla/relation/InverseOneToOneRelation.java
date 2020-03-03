package io.semla.relation;

import io.semla.exception.InvalidPersitenceAnnotationException;
import io.semla.model.EntityModel;
import io.semla.persistence.PersistenceContext;
import io.semla.query.Include;
import io.semla.query.Select;
import io.semla.reflect.Member;
import io.semla.reflect.Properties;

import javax.persistence.OneToOne;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class InverseOneToOneRelation<ParentType, ChildType> extends AbstractRelation<ParentType, ChildType> implements NToOneRelation<ParentType, ChildType> {

    private final Member<ChildType> mappedBy;

    public InverseOneToOneRelation(Member<ParentType> member, OneToOne oneToOne, EntityModel<ParentType> parentModel, Class<ChildType> childClass) {
        super(member, parentModel, childClass, new IncludeTypes(oneToOne.fetch(), oneToOne.cascade(), oneToOne.orphanRemoval()));
        if (!Properties.hasMember(childClass, oneToOne.mappedBy())) {
            throw new InvalidPersitenceAnnotationException(
                "@OneToOne.mappedBy on " + member + " defines a non existent member '" + oneToOne.mappedBy() + "' on " + childClass);
        }
        this.mappedBy = Properties.membersOf(childClass).get(oneToOne.mappedBy());
    }

    @Override
    public Optional<ChildType> getFor(ParentType parent, PersistenceContext context, Include<ParentType, ChildType> include) {
        return context.select(childModel()).where(mappedBy.getName()).is(parentModel().key().member().getOn(parent)).first(include::addTo);
    }

    @Override
    public Map<ParentType, ChildType> getFor(Collection<ParentType> parents, PersistenceContext context, Include<ParentType, ChildType> include) {
        Map<ParentType, ChildType> result = new LinkedHashMap<>();
        Map<Object, ParentType> parentsByKey = parents.stream().collect(Collectors.toMap(parent -> parentModel().key().member().getOn(parent), Function.identity()));
        if (!parentsByKey.isEmpty()) {
            context.select(childModel()).where(mappedBy.getName()).in(parentsByKey.keySet()).list(include::addTo)
                .forEach(child -> result.put(parentsByKey.get(parentModel().key().member().getOn(mappedBy.getOn(child))), child));
        }
        return result;
    }

    @Override
    public void createOrUpdateOn(ParentType parent, PersistenceContext context, Include<ParentType, ChildType> include) {
        ChildType child = member().getOn(parent);
        if (EntityModel.isNotNullOrReference(child)) {
            mappedBy.setOn(child, parent);
            context.createOrUpdate(child, include);
        }
    }

    @Override
    public void createOrUpdateOn(Collection<ParentType> parents, PersistenceContext context, Include<ParentType, ChildType> include) {
        context.createOrUpdate(
            parents.stream()
                .map(parent -> {
                    ChildType child = member().getOn(parent);
                    if (EntityModel.isNotNullOrReference(child)) {
                        mappedBy.setOn(child, parent);
                        return child;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList()),
            include
        );
    }

    @Override
    public void deleteOn(ParentType parent, PersistenceContext context, Include<ParentType, ChildType> include) {
        deleteOn(context, context.select(childModel()).where(mappedBy.getName()).is(parentModel().key().member().getOn(parent)), include);
    }

    @Override
    public void deleteOn(Collection<ParentType> parents, PersistenceContext context, Include<ParentType, ChildType> include) {
        deleteOn(context, context.select(childModel()).where(mappedBy.getName()).in(parents.stream().map(parentModel().key().member()::getOn).collect(Collectors.toList())), include);
    }

    private void deleteOn(PersistenceContext context, Select<ChildType> query, Include<ParentType, ChildType> include) {
        if (include.type().should(IncludeType.DELETE)) {
            query.delete(include.includes()::addTo);
        } else {
            query.set(mappedBy.getName(), null).patch();
        }
        if (include.type().should(IncludeType.DELETE_ORPHANS)) {
            context.select(childModel()).where(mappedBy.getName()).is(null).delete(include.includes()::addTo);
        }
    }
}
