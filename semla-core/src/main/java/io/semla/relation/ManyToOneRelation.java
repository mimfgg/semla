package io.semla.relation;

import io.semla.model.EntityModel;
import io.semla.reflect.Member;

import javax.persistence.ManyToOne;


public class ManyToOneRelation<ParentType, ChildType> extends AbstractRelation<ParentType, ChildType> implements ForeignNToOneRelation<ParentType, ChildType> {

    public ManyToOneRelation(Member<ParentType> member, ManyToOne manyToOne, EntityModel<ParentType> parentModel, Class<ChildType> childClass) {
        super(member, parentModel, childClass, new IncludeTypes(manyToOne.fetch(), manyToOne.cascade(), false));
    }

}
