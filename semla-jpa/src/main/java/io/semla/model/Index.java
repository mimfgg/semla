package io.semla.model;

import io.semla.util.ImmutableList;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Index<T> {

    private final String name;
    private final boolean unique;
    private final boolean primary;
    private final List<Column<T>> columns;

    @SafeVarargs
    public Index(Column<T>... columns) {
        this("", false, false, columns);
    }

    @SafeVarargs
    public Index(String name, boolean unique, boolean primary, Column<T>... columns) {
        if (name == null || name.isEmpty()) {
            this.name = Stream.of(columns).map(Column::name).collect(Collectors.joining("_")) + "_idx";
        } else {
            this.name = name;
        }
        this.primary = primary;
        this.unique = unique;
        this.columns = ImmutableList.of(columns);
    }

    public String name() {
        return name;
    }

    public boolean isUnique() {
        return unique;
    }

    public boolean isPrimary() {
        return primary;
    }

    public List<Column<T>> columns() {
        return columns;
    }

    public String columnNames(Function<String, String> escape) {
        return columns().stream().map(Column::name).map(escape).collect(Collectors.joining(", "));
    }
}
