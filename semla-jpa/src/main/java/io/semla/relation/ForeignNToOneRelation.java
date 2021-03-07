package io.semla.relation;

import io.semla.model.EntityModel;
import io.semla.persistence.PersistenceContext;
import io.semla.query.Include;
import io.semla.util.Maps;
import io.semla.util.Pair;
import io.semla.util.Strings;

import java.util.*;

public interface ForeignNToOneRelation<ParentType, ChildType> extends NToOneRelation<ParentType, ChildType> {

    @Override
    default Optional<ChildType> getFor(ParentType parent, PersistenceContext context, Include<ParentType, ChildType> include) {
        return Optional.ofNullable(member().<ChildType>getOn(parent))
            .map(child ->
                !EntityModel.isReference(child)
                    ? child
                    : context.entityContext().getCached(child).orElseGet(() ->
                    context.get(childModel().key().member().<Object>getOn(child), include.includes()).orElse(null)
                ));
    }

    @Override
    default Map<ParentType, ChildType> getFor(Collection<ParentType> parents, PersistenceContext context, Include<ParentType, ChildType> include) {
        Map<ParentType, ChildType> result = new LinkedHashMap<>();
        Map<Object, List<ParentType>> parentsByChildKey = new LinkedHashMap<>();
        parents.forEach(parent -> {
            ChildType child = member().getOn(parent);
            if (EntityModel.isReference(child)) {
                parentsByChildKey
                    .computeIfAbsent(childModel().key().member().getOn(child), k -> new ArrayList<>())
                    .add(parent);
            } else {
                result.put(parent, child);
            }
        });
        if (!parentsByChildKey.isEmpty()) {
            context.get(parentsByChildKey.keySet(), include.includes()).values().
                forEach(child -> parentsByChildKey.get(childModel().key().member().getOn(child))
                    .forEach(parent -> member().setOn(parent, child)));
        }
        return result;
    }

    @Override
    default void createOrUpdateOn(ParentType parent, PersistenceContext context, Include<ParentType, ChildType> include) {
        ChildType child = member().getOn(parent);
        if (EntityModel.isNotNullOrReference(child)) {
            context.createOrUpdate(child, include);
            long detached = context.select(parentModel())
                .set(member().getName(), null)
                .where(member().getName()).is(child)
                .and(parentModel().key().member().getName()).not(parentModel().key().member().getOn(parent))
                .patch();
            if (detached > 0 && logger().isTraceEnabled()) {
                logger().trace("detached {} entity not being {}", detached, Strings.toString(parent));
            }
            long attached = context.select(parentModel())
                .set(member().getName(), EntityModel.referenceTo(child))
                .where(parentModel().key().member().getName()).is(parentModel().key().member().getOn(parent))
                .patch();
            if (attached > 0 && logger().isTraceEnabled()) {
                logger().trace("attached {} entity to {}", attached, Strings.toString(parent));
            }
            member().setOn(parent, child);
        }
    }

    @Override
    default void createOrUpdateOn(Collection<ParentType> parents, PersistenceContext context, Include<ParentType, ChildType> include) {
        Map<ParentType, ChildType> childrenByParent = parents.stream()
            .map(parent -> Pair.of(parent, member().<ChildType>getOn(parent)))
            .filter(pair -> EntityModel.isNotNullOrReference(pair.right()))
            .collect(Maps.collect());
        context.createOrUpdate(childrenByParent.values(), include);
        // could be more optimized but should work with all vendors
        childrenByParent.forEach((parent, child) -> {
            long detached = context.select(parentModel())
                .set(member().getName(), null)
                .where(member().getName()).is(child)
                .and(parentModel().key().member().getName()).not(parentModel().key().member().getOn(parent))
                .patch();
            if (detached > 0 && logger().isTraceEnabled()) {
                logger().trace("detached {} entity not being {}", detached, Strings.toString(parent));
            }
            long attached = context.select(parentModel())
                .set(member().getName(), EntityModel.referenceTo(child))
                .where(parentModel().key().member().getName()).is(parentModel().key().member().getOn(parent))
                .patch();
            if (attached > 0 && logger().isTraceEnabled()) {
                logger().trace("attached {} entity to {}", attached, Strings.toString(parent));
            }
            member().setOn(parent, child);
        });
    }

    @Override
    default void deleteOn(ParentType parent, PersistenceContext context, Include<ParentType, ChildType> include) {
        ChildType child = member().getOn(parent);
        if (child != null) {
            if (include.type().should(IncludeType.DELETE)) {
                context.delete(childModel().key().member().<Object>getOn(child), include.includes());
            }
            if (include.type().should(IncludeType.DELETE_ORPHANS)) {
                // FIXME: find something that would work with all vendors;
                throw new UnsupportedOperationException();
            }
        }
    }

    @Override
    default void deleteOn(Collection<ParentType> parents, PersistenceContext context, Include<ParentType, ChildType> include) {
        parents.forEach(parent -> deleteOn(parent, context, include));
    }
}
