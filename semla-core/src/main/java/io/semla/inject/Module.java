package io.semla.inject;

@FunctionalInterface
public interface Module {

    void configure(Binder binder);
}
