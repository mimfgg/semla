package io.semla.relation;

import io.semla.exception.InvalidPersitenceAnnotationException;
import io.semla.model.EntityModel;
import io.semla.persistence.PersistenceContext;
import io.semla.query.Include;
import io.semla.reflect.Fields;
import io.semla.reflect.Member;
import io.semla.reflect.Types;
import io.semla.util.Singleton;
import io.semla.util.Strings;

import javax.persistence.ManyToMany;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static io.semla.util.Lists.toMap;

@SuppressWarnings("unchecked")
public class ManyToManyRelation<ParentType, JoinType, ChildType> extends AbstractRelation<ParentType, ChildType>
    implements NToManyRelation<ParentType, ChildType>, JoinedRelation<ParentType, JoinType, ChildType> {

    private final Singleton<Class<JoinType>> relationClass;
    protected String parentFieldName;
    protected String childFieldName;

    public ManyToManyRelation(Member<ParentType> member, ManyToMany manyToMany, EntityModel<ParentType> parentModel, Class<ChildType> childClass) {
        super(member, parentModel, childClass, new IncludeTypes(manyToMany.fetch(), manyToMany.cascade(), false));
        parentFieldName = Strings.decapitalize(parentModel.getType().getSimpleName());
        childFieldName = Strings.decapitalize(childClass.getSimpleName());
        if (parentFieldName.equals(childFieldName)) {
            parentFieldName += "A";
            childFieldName += "B";
        }
        if (!manyToMany.mappedBy().equals("")) {
            // this relation is owned by the other side
            Field childField = Fields.getField(childClass, manyToMany.mappedBy());
            if (childField == null) {
                throw new InvalidPersitenceAnnotationException(
                    "@ManyToMany.mappedBy on " + member + " defines a non existent member '" + manyToMany.mappedBy() + "' on " + childClass);
            }
            ManyToMany reverse = childField.getAnnotation(ManyToMany.class);
            if (reverse == null) {
                throw new InvalidPersitenceAnnotationException(
                    "@ManyToMany.mappedBy on " + member + " defines a member '" + manyToMany.mappedBy() + "' on " + childClass + " " +
                        "that is not annotated with @ManyToMany");
            }
            if (!reverse.mappedBy().equals("")) {
                throw new InvalidPersitenceAnnotationException(
                    "@ManyToMany.mappedBy on " + member + " defines a member '" + manyToMany.mappedBy() + "' on " + childClass + " " +
                        "that also defines a member '" + reverse.mappedBy() + "'. Only one of the two class can own this relationship!");
            }
            relationClass = Singleton.lazy(((ManyToManyRelation<?, JoinType, ?>) EntityModel.of(childClass).relationByFieldName(manyToMany.mappedBy()))::relationClass);
        } else {
            // look for the reverse if any
            Fields.of(childClass)
                .filter(entry -> entry.isAnnotationPresent(ManyToMany.class))
                .filter(entry -> Types.typeArgumentOf(entry.getGenericType()).equals(parentModel.getType()))
                .filter(entry -> !entry.getDeclaringClass().equals(member.getDeclaringClass()) && !entry.getName().equals(member.getName())) // recursive relation
                .findFirst()
                .ifPresent(reverseField -> {
                    String mappedBy = reverseField.getAnnotation(ManyToMany.class).mappedBy();
                    if (mappedBy.equals("")) {
                        throw new InvalidPersitenceAnnotationException("@ManyToMany.mappedBy on " + member + " or " + reverseField + " needs to be set");
                    }
                    if (!parentModel.hasMember(mappedBy)) {
                        throw new InvalidPersitenceAnnotationException(
                            "@ManyToMany.mappedBy on " + reverseField + " defines a non existent member '" + mappedBy + "' on " + parentModel.getType());
                    }
                });
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
    public Collection<ChildType> getFor(ParentType parent, PersistenceContext context, Include<ParentType, ChildType> include) {
        return context.select(relationClass())
            .where(parentFieldName).is(parentModel().key().member().getOn(parent)).orderedBy(parentFieldName)
            .list(with(include))
            .stream()
            .map(row -> relationModel().member(childFieldName).<ChildType>getOn(row))
            .collect(toCollection());
    }

    @Override
    public Map<ParentType, Collection<ChildType>> getFor(Collection<ParentType> parents, PersistenceContext context, Include<ParentType, ChildType> include) {
        Map<Object, ParentType> parentsByKey = toMap(parents, parentModel().key().member()::getOn);
        Map<ParentType, Collection<ChildType>> result = parentsByKey.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getValue, e -> collectionSupplier().get()));
        context.select(relationClass())
            .where(parentFieldName).in(parentsByKey.keySet()).orderedBy(parentFieldName)
            .list(with(include))
            .stream()
            .collect(Collectors.groupingBy(
                row -> parentsByKey.get(parentModel().key().member().getOn(relationModel().member(parentFieldName).getOn(row))),
                LinkedHashMap::new,
                Collectors.mapping(
                    row -> context.entityContext().remapOrCache((ChildType) relationModel().member(childFieldName).getOn(row)),
                    Collectors.toList()
                )
            ))
            .forEach((parent, children) -> result.get(parent).addAll(children));

        return result;
    }

    @Override
    public void createOrUpdateOn(ParentType parent, PersistenceContext context, Include<ParentType, ChildType> include) {
        Collection<ChildType> list = member().getOn(parent);
        if (list != null) {
            list.forEach(child -> {
                context.createOrUpdate(child, include);
                if (context.select(relationClass())
                    .where(parentFieldName).is(EntityModel.referenceTo(parent))
                    .and(childFieldName).is(EntityModel.referenceTo(child))
                    .count() == 0) {
                    context.newInstanceOf(relationClass())
                        .with(parentFieldName, parent)
                        .with(childFieldName, child)
                        .create();
                }
            });
        }
    }
}
