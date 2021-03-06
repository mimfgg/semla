package io.semla.relation;

import io.semla.model.EntityModel;
import io.semla.persistence.PersistenceContext;
import io.semla.query.Include;
import io.semla.reflect.Member;
import io.semla.util.Maps;
import io.semla.util.Singleton;
import io.semla.util.Strings;

import javax.persistence.OneToOne;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class JoinedOneToOneRelation<ParentType, JoinType, ChildType> extends AbstractRelation<ParentType, ChildType>
    implements NToOneRelation<ParentType, ChildType>, JoinedRelation<ParentType, JoinType, ChildType> {

    private final Singleton<Class<JoinType>> relationClass;
    private final String parentFieldName;
    private final String childFieldName;

    public JoinedOneToOneRelation(Member<ParentType> member, OneToOne oneToOne, EntityModel<ParentType> parentModel, Class<ChildType> childClass) {
        super(member, parentModel, childClass, new IncludeTypes(oneToOne.fetch(), oneToOne.cascade(), oneToOne.orphanRemoval()));
        parentFieldName = Strings.decapitalize(parentModel.getType().getSimpleName());
        childFieldName = Strings.decapitalize(childClass.getSimpleName());
        if (!oneToOne.mappedBy().equals("")) {
            relationClass = Singleton.lazy(((JoinedOneToOneRelation<ParentType, JoinType, ChildType>) EntityModel.of(childClass).getRelation(oneToOne.mappedBy()))::relationClass);
        } else {
            relationClass = JoinTables.create(parentFieldName, childFieldName, member, parentModel, childClass);
        }
    }

    @Override
    public Class<JoinType> relationClass() {
        return relationClass.get();
    }

    @Override
    public String parentFieldName() {
        return parentFieldName;
    }

    @Override
    public String childFieldName() {
        return childFieldName;
    }

    @Override
    public Optional<ChildType> getFor(ParentType parent, PersistenceContext context, Include<ParentType, ChildType> include) {
        return context.select(relationClass())
            .where(parentFieldName).is(parentModel().key().member().getOn(parent))
            .first(with(include))
            .map(row -> (ChildType) relationModel().member(childFieldName).getOn(row))
            .map(context.entityContext()::remapOrCache);
    }

    @Override
    public Map<ParentType, ChildType> getFor(Collection<ParentType> parents, PersistenceContext context, Include<ParentType, ChildType> include) {
        Map<ParentType, ChildType> result = new LinkedHashMap<>();
        Map<Object, ParentType> parentsByKey = parents.stream()
            .collect(Collectors.toMap(parent -> parentModel().key().member().getOn(parent), Function.identity()));
        if (!parentsByKey.isEmpty()) {
            result = context.select(relationClass())
                .where(parentFieldName).in(parentsByKey.keySet())
                .list(with(include))
                .stream()
                .collect(Maps.collect(
                    row -> parentsByKey.get(parentModel().key().member().getOn(relationModel().member(parentFieldName).getOn(row))),
                    row -> context.entityContext().remapOrCache((ChildType) relationModel().member(childFieldName).getOn(row))
                ));
        }
        return result;
    }

    @Override
    public void createOrUpdateOn(ParentType parent, PersistenceContext context, Include<ParentType, ChildType> include) {
        ChildType child = member().getOn(parent);
        if (child != null) {
            context.createOrUpdate(child, include);
            if (context.select(relationClass())
                .where(parentFieldName).is(EntityModel.referenceTo(parent))
                .and(childFieldName).is(EntityModel.referenceTo(child))
                .count() == 0) {
                context.factory().of(relationClass()).newInstance()
                    .with(parentFieldName, parent)
                    .with(childFieldName, child)
                    .create();
            }
        }
    }
}
