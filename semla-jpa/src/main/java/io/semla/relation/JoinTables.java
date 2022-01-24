package io.semla.relation;

import io.semla.model.EntityModel;
import io.semla.persistence.annotations.Indexed;
import io.semla.reflect.Member;
import io.semla.reflect.Types;
import io.semla.util.Javassist;
import io.semla.util.Singleton;
import io.semla.util.Strings;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JoinTables {

    public static <JoinType> Singleton<Class<JoinType>> create(String parentFieldName,
                                                               String childFieldName,
                                                               Member<?> member,
                                                               EntityModel<?> parentModel,
                                                               Class<?> childClass) {
        Optional<JoinTable> joinTable = member.annotation(JoinTable.class);

        // defaulted relation table will be parenttype_childtype
        String tableName = joinTable.map(JoinTable::name).filter(name -> !name.equals(""))
            .orElseGet(() -> Strings.toSnakeCase(parentModel.getType().getSimpleName() + childClass.getSimpleName()));

        // defaulted joinColumn will be parent_field_name
        String joinColumnName = joinTable.map(JoinTable::joinColumns).filter(joinColumns -> joinColumns.length > 0).map(joinColumns -> joinColumns[0].name())
            .orElseGet(() -> Strings.toSnakeCase(parentFieldName));

        // defaulted inverseJoinColumn will be child_field_name
        String inverseJoinColumnName = joinTable.map(JoinTable::inverseJoinColumns)
            .filter(joinColumns -> joinColumns.length > 0).map(joinColumns -> joinColumns[0].name())
            .orElseGet(() -> Strings.toSnakeCase(childFieldName));

        return Singleton.of(
            Javassist.getOrCreate(generateName(member, parentModel), parentModel.getType(), clazz -> clazz
                .addAnnotation(Entity.class)
                .addAnnotation(Table.class, annotation -> annotation.set("name", tableName))
                .addField("id", Integer.class, idField -> idField
                    .addAnnotation(Id.class)
                    .addAnnotation(GeneratedValue.class)
                )
                .addField(parentFieldName, parentModel.getType(), parentField -> parentField
                    .addAnnotation(Indexed.class)
                    .addAnnotation(Column.class, annotation -> annotation.set("name", joinColumnName))
                    .addAnnotation(ManyToOne.class, annotation -> annotation.set("fetch", FetchType.LAZY))
                )
                .addField(childFieldName, childClass, childField -> childField
                    .addAnnotation(Indexed.class)
                    .addAnnotation(Column.class, annotation -> annotation.set("name", inverseJoinColumnName))
                    .addAnnotation(ManyToOne.class, annotation -> annotation.set("fetch", FetchType.LAZY))
                )
            )
        );
    }

    public static String generateName(Member<?> member, EntityModel<?> parentModel) {
        return Stream.<Supplier<String>>of(
                () -> parentModel.getType().getCanonicalName() + Types.optionalRawTypeArgumentOf(member.getGenericType())
                    .map(Class::getSimpleName).orElse(member.getType().getSimpleName()),  // ParentChild
                () -> parentModel.getType().getCanonicalName() + "_" + Types.optionalRawTypeArgumentOf(member.getGenericType())
                    .map(Class::getSimpleName).orElse(member.getType().getSimpleName()), // Parent_Child
                () -> {
                    String name = Strings.capitalize(member.getName());
                    if (name.equals(member.getType().getSimpleName())) {
                        name = "_" + name;
                    }
                    return parentModel.getType().getCanonicalName() + "_" + name;
                } // Parent_Children
            )
            .map(Supplier::get)
            .filter(className -> {
                try {
                    Types.forName(className);
                } catch (ClassNotFoundException e) {
                    return true;
                }
                return false;
            })
            .findFirst()
            .orElseThrow(IllegalArgumentException::new);
    }
}
