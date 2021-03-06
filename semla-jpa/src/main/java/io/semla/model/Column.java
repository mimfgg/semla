package io.semla.model;

import io.semla.query.Predicates;
import io.semla.reflect.Annotations;
import io.semla.reflect.Member;
import io.semla.util.Strings;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public class Column<T> {

    private final Member<T> member;
    private final String name;
    private final boolean isGenerated;
    private final boolean unique;
    private final boolean nullable;
    private final boolean insertable;
    private final boolean updatable;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<String> columnDefinition;
    private final int length;
    private final int precision;
    private final int scale;

    public Column(Member<T> member) {
        boolean isId = member.annotation(Id.class).isPresent();
        this.member = member;
        this.isGenerated = member.annotation(GeneratedValue.class).isPresent();
        javax.persistence.Column jpaColumn = member.annotation(javax.persistence.Column.class)
            .orElseGet(() -> Annotations.defaultOf(javax.persistence.Column.class));
        this.name = !jpaColumn.name().equals("") ? jpaColumn.name() : Strings.toSnakeCase(member.getName());
        this.unique = jpaColumn.unique();
        this.nullable = jpaColumn.nullable() && !member.annotation(NotNull.class).isPresent() && (!isId || isGenerated);
        this.insertable = (jpaColumn.insertable() && !this.isGenerated && !member.annotation(Version.class).isPresent()) || member.getType().equals(UUID.class);
        this.updatable = jpaColumn.updatable() && !this.isGenerated && !member.annotation(Version.class).isPresent() && !isId;
        this.columnDefinition = !jpaColumn.columnDefinition().equals("") ? Optional.of(jpaColumn.columnDefinition()) : Optional.empty();
        this.length = jpaColumn.length();
        this.precision = jpaColumn.precision();
        this.scale = jpaColumn.scale();
    }

    public String name() {
        return name;
    }

    public Member<T> member() {
        return member;
    }

    public boolean isGenerated() {
        return isGenerated;
    }

    public boolean unique() {
        return unique;
    }

    public boolean nullable() {
        return nullable;
    }

    public boolean insertable() {
        return insertable;
    }

    public boolean updatable() {
        return updatable;
    }

    public Optional<String> columnDefinition() {
        return columnDefinition;
    }

    public int length() {
        return length;
    }

    public int precision() {
        return precision;
    }

    public int scale() {
        return scale;
    }

    public Predicates<T> is(Object value) {
        return Predicates.of(member.getDeclaringClass()).where(member.getName()).is(value);
    }

    public Predicates<T> in(Collection<?> values) {
        return Predicates.of(member.getDeclaringClass()).where(member.getName()).in(values);
    }

    @Override
    public String toString() {
        return "Column("
            + "name: " + name
            + ", member: " + member
            + ", isGenerated: " + isGenerated
            + ", unique: " + unique
            + ", nullable: " + nullable
            + ", insertable: " + insertable
            + ", updatable: " + updatable
            + ", columnDefinition: " + columnDefinition
            + ", length: " + length
            + ", precision: " + precision
            + ", scale: " + scale
            + ")";
    }
}
