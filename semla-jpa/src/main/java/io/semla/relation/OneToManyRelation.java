package io.semla.relation;

import io.semla.exception.InvalidPersitenceAnnotationException;
import io.semla.model.EntityModel;
import io.semla.persistence.PersistenceContext;
import io.semla.query.Include;
import io.semla.query.Select;
import io.semla.reflect.Member;

import javax.persistence.OneToMany;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static io.semla.util.Lists.toMap;

public class OneToManyRelation<ParentType, ChildType> extends AbstractRelation<ParentType, ChildType> implements NToManyRelation<ParentType, ChildType> {

    private final Member<ChildType> mappedBy;

    public OneToManyRelation(Member<ParentType> member, OneToMany oneToMany, EntityModel<ParentType> parentModel, Class<ChildType> childClass) {
        super(member, parentModel, childClass, new IncludeTypes(oneToMany.fetch(), oneToMany.cascade(), oneToMany.orphanRemoval()));
        if (oneToMany.mappedBy().equals("")) {
            throw new InvalidPersitenceAnnotationException("@OneToMany.mappedBy on " + member + " needs to be set");
        }
        mappedBy = getMappedBy(member, oneToMany, childClass);
    }

    @Override
    public Collection<ChildType> getFor(ParentType parent, PersistenceContext context, Include<ParentType, ChildType> include) {
        return context.select(childModel())
            .where(mappedBy.getName()).is(parentModel().key().member().getOn(parent))
            .orderedBy(childModel().key().member().getName())
            .list(include::addTo)
            .stream()
            .collect(toCollection());
    }

    @Override
    public Map<ParentType, Collection<ChildType>> getFor(Collection<ParentType> parents, PersistenceContext context, Include<ParentType, ChildType> include) {
        Map<Object, ParentType> parentsByKey = toMap(parents, parentModel().key().member()::getOn);
        Map<ParentType, Collection<ChildType>> result = parentsByKey.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getValue, e -> collectionSupplier().get()));
        context.select(childModel())
            .where(mappedBy.getName()).in(parentsByKey.keySet())
            .orderedBy(childModel().key().member().getName())
            .list(include::addTo)
            .stream()
            .collect(Collectors.groupingBy(
                child -> parentsByKey.get(parentModel().key().member().getOn(mappedBy.getOn(child))),
                LinkedHashMap::new,
                toCollection()
            ))
            .forEach((parent, children) -> result.get(parent).addAll(children));
        return result;
    }

    @Override
    public void createOrUpdateOn(ParentType parent, PersistenceContext context, Include<ParentType, ChildType> include) {
        Collection<ChildType> list = member().getOn(parent);
        if (list != null) {
            list.forEach(child -> {
                mappedBy.setOn(child, parent);
                context.createOrUpdate(child, include);
            });
        }
    }

    @Override
    public void deleteOn(ParentType parent, PersistenceContext context, Include<ParentType, ChildType> include) {
        deleteOn(context.select(childModel()).where(mappedBy.getName()).is(parentModel().key().member().getOn(parent)), include);
        removeOrphans(context, include);
    }

    @Override
    public void deleteOn(Collection<ParentType> parents, PersistenceContext context, Include<ParentType, ChildType> include) {
        deleteOn(context.select(childModel()).where(mappedBy.getName()).in(parents.stream().map(parentModel().key().member()::getOn).collect(Collectors.toList())), include);
        removeOrphans(context, include);
    }

    private void removeOrphans(PersistenceContext context, Include<ParentType, ChildType> include) {
        if (include.type().should(IncludeType.DELETE_ORPHANS)) {
            context.select(childModel()).where(mappedBy.getName()).is(null).delete(include::addTo);
        }
    }

    private void deleteOn(Select<ChildType> query, Include<ParentType, ChildType> include) {
        if (include.type().should(IncludeType.DELETE)) {
            query.delete(include::addTo);
        } else {
            query.set(mappedBy.getName(), null).patch();
        }
    }

    @Override
    protected StringBuilder getDetails() {
        return super.getDetails().append(", mappedBy: ").append(mappedBy);
    }
}
