package io.semla.relation;

import io.semla.exception.InvalidPersitenceAnnotationException;
import io.semla.model.EntityModel;
import io.semla.reflect.Fields;
import io.semla.reflect.Member;

import javax.persistence.JoinTable;
import javax.persistence.OneToOne;
import java.lang.reflect.Field;

public class OneToOneRelation<ParentType, ChildType> extends AbstractRelation<ParentType, ChildType> implements ForeignNToOneRelation<ParentType, ChildType> {

    private OneToOneRelation(Member<ParentType> member, OneToOne oneToOne, EntityModel<ParentType> parentModel, Class<ChildType> childClass) {
        super(member, parentModel, childClass, new IncludeTypes(oneToOne.fetch(), oneToOne.cascade(), oneToOne.orphanRemoval()));
    }

    public static <ParentType, ChildType> NToOneRelation<ParentType, ChildType> of(Member<ParentType> member,
                                                                                   OneToOne oneToOne,
                                                                                   EntityModel<ParentType> parentModel,
                                                                                   Class<ChildType> childClass) {
        if (oneToOne.mappedBy().equals("")) {
            // look for the reverse if any
            Fields.of(member.getType())
                .filter(entry -> entry.isAnnotationPresent(OneToOne.class))
                .filter(entry -> entry.getType().equals(parentModel.getType()))
                .findFirst()
                .ifPresent(reverseField -> {
                    String mappedBy = reverseField.getAnnotation(OneToOne.class).mappedBy();
                    if (mappedBy.equals("")) {
                        throw new InvalidPersitenceAnnotationException("@OneToOne.mappedBy on " + member + " or " + reverseField + " needs to be set");
                    }
                    if (!parentModel.hasMember(mappedBy)) {
                        throw new InvalidPersitenceAnnotationException(
                            "@OneToOne.mappedBy on " + reverseField + " defines a non existent member '" + mappedBy + "' on " + parentModel.getType());
                    }
                });
            if (member.annotation(JoinTable.class).isPresent()) {
                return new JoinedOneToOneRelation<>(member, oneToOne, parentModel, member.getType());
            } else {
                return new OneToOneRelation<>(member, oneToOne, parentModel, member.getType());
            }
        } else {
            Field mappedField = Fields.getField(member.getType(), oneToOne.mappedBy());
            if (mappedField == null) {
                throw new InvalidPersitenceAnnotationException(
                    "@OneToOne.mappedBy on " + member + " defines a non existent member '" + oneToOne.mappedBy() + "' on " + member.getType());
            }
            if (!mappedField.isAnnotationPresent(OneToOne.class)) {
                throw new InvalidPersitenceAnnotationException(
                    "@OneToOne.mappedBy on " + member + " defines a member '" + oneToOne.mappedBy() + "' on " + member.getType() + " " +
                        "that is not annotated with @OneToOne");
            }
            if (!mappedField.getAnnotation(OneToOne.class).mappedBy().equals("")) {
                throw new InvalidPersitenceAnnotationException(
                    "@OneToOne.mappedBy on " + member + " defines a member '" + oneToOne.mappedBy() + "' on " + member.getType() + " " +
                        "that also defines a member '" + mappedField.getAnnotation(OneToOne.class).mappedBy() + "'. " +
                        "Only one of the two class can own this relationship!");
            }
            if (mappedField.isAnnotationPresent(JoinTable.class)) {
                return new JoinedOneToOneRelation<>(member, oneToOne, parentModel, member.getType());
            } else {
                return new InverseOneToOneRelation<>(member, oneToOne, parentModel, member.getType());
            }
        }
    }
}
