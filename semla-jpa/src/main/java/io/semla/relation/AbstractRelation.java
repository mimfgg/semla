package io.semla.relation;

import io.semla.exception.InvalidPersitenceAnnotationException;
import io.semla.model.EntityModel;
import io.semla.reflect.Member;
import io.semla.util.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractRelation<ParentType, ChildType> implements Relation<ParentType, ChildType> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Member<ParentType> member;
    private final EntityModel<ParentType> parentModel;
    private final Singleton<EntityModel<ChildType>> childModel;
    private final IncludeTypes defaultIncludeTypes;

    public AbstractRelation(Member<ParentType> member, EntityModel<ParentType> parentModel, Class<ChildType> childClass, IncludeTypes defaultIncludeTypes) {
        if (!EntityModel.isEntity(childClass)) {
            throw new InvalidPersitenceAnnotationException(member + " does not refer to an entity!");
        }
        this.member = member;
        this.parentModel = parentModel;
        this.defaultIncludeTypes = defaultIncludeTypes;
        this.childModel = Singleton.lazy(() -> EntityModel.of(childClass));
    }

    @Override
    public IncludeTypes defaultIncludeType() {
        return defaultIncludeTypes;
    }

    @Override
    public EntityModel<ParentType> parentModel() {
        return parentModel;
    }

    @Override
    public EntityModel<ChildType> childModel() {
        return childModel.get();
    }

    @Override
    public Member<ParentType> member() {
        return member;
    }

    @Override
    public Logger logger() {
        return logger;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(" + getDetails().toString() + ")";
    }

    protected StringBuilder getDetails() {
        return new StringBuilder()
            .append("member: ").append(member)
            .append(", defaultIncludeType: ").append(defaultIncludeTypes);
    }
}
