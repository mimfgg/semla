package io.semla.relation;

import io.semla.model.EntityModel;
import io.semla.persistence.PersistenceContext;
import io.semla.query.Include;
import io.semla.query.Includes;
import io.semla.reflect.Member;

import javax.persistence.OneToMany;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.semla.util.Lists.toMap;

public class EmbeddedToManyRelation<ParentType, ChildType> extends AbstractRelation<ParentType, ChildType> implements NToManyRelation<ParentType, ChildType> {

    private final Member<ChildType> mappedBy;

    public EmbeddedToManyRelation(Member<ParentType> member, OneToMany oneToMany, EntityModel<ParentType> parentModel, Class<ChildType> childClass) {
        super(member, parentModel, childClass, new IncludeTypes(oneToMany.fetch(), oneToMany.cascade(), oneToMany.orphanRemoval()));
        mappedBy = oneToMany.mappedBy().equals("") ? null : getMappedBy(member, oneToMany, childClass);
    }

    @Override
    public Collection<ChildType> getFor(ParentType parent, PersistenceContext context, Include<ParentType, ChildType> include) {
        Collection<ChildType> list = member().getOn(parent);
        if (list != null) {
            // refreshing the list
            return context.get(list.stream().map(childModel().key().member()::getOn).collect(Collectors.toList()), include.includes())
                .values().stream()
                .map(context.entityContext()::remapOrCache)
                .collect(toCollection());
        }
        return collectionSupplier().get();
    }

    @Override
    public Map<ParentType, Collection<ChildType>> getFor(Collection<ParentType> parents, PersistenceContext context, Include<ParentType, ChildType> include) {
        Map<Object, ParentType> parentsByKey = toMap(parents, parentModel().key().member()::getOn);
        Map<ParentType, Collection<ChildType>> result = new LinkedHashMap<>();
        Set<Object> keysToFetch = parentsByKey.values().stream().map(parent -> {
                Collection<ChildType> list = member().<Collection<ChildType>>getOn(parent)
                    .stream()
                    .map(context.entityContext()::remapOrCache)
                    .collect(toCollection());
                result.put(parent, list);
                member().<Collection<ChildType>>getOn(parent).clear();
                return list.stream().filter(EntityModel::isReference).map(childModel().key().member()::getOn);
            }
        ).flatMap(Function.identity()).collect(Collectors.toSet());
        context.get(keysToFetch, include.includes());
        return result;
    }

    @Override
    public void createOrUpdateOn(ParentType parent, PersistenceContext context, Include<ParentType, ChildType> include) {
        Collection<ChildType> list = member().getOn(parent);
        if (list != null) {
            list.forEach(child -> {
                if (mappedBy != null) {
                    mappedBy.setOn(child, parent);
                }
                context.createOrUpdate(child, include);
            });
            context.update(parent, Includes.of(parentModel()));
        }
    }

    @Override
    public void deleteOn(ParentType parent, PersistenceContext context, Include<ParentType, ChildType> include) {
        Collection<ChildType> list = member().getOn(parent);
        if (list != null && !list.isEmpty()) {
            context.delete(list.stream().map(childModel().key().member()::getOn).collect(Collectors.toList()), include.includes());
        }
    }

    @Override
    public void deleteOn(Collection<ParentType> parents, PersistenceContext context, Include<ParentType, ChildType> include) {
        Set<Object> keysToDelete = parents.stream()
            .map(parent ->
                member().<Collection<ChildType>>getOn(parent).stream().map(childModel().key().member()::getOn)
            )
            .flatMap(Function.identity()).collect(Collectors.toSet());
        if (!keysToDelete.isEmpty()) {
            context.delete(keysToDelete, include.includes());
        }
    }

    @Override
    protected StringBuilder getDetails() {
        return super.getDetails().append(", mappedBy: ").append(mappedBy);
    }
}
