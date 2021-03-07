package io.semla;

import io.semla.datasource.Datasource;
import io.semla.datasource.DatasourceFactory;
import io.semla.inject.Binder;
import io.semla.inject.Injector;
import io.semla.inject.Module;
import io.semla.inject.SemlaInjector;
import io.semla.model.EntityModel;
import io.semla.persistence.EntityManagerFactory;
import io.semla.persistence.TypedEntityManagerFactory;
import io.semla.reflect.TypeReference;
import io.semla.util.Lists;
import io.semla.util.Throwables;
import io.semla.validation.ValidatorProvider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

public class Semla implements Injector {

    private Injector injector;

    private Semla() {}

    @Override
    public <E> E getInstance(Type type, Annotation... annotations) {
        return injector.getInstance(type, annotations);
    }

    @Override
    public <E> E inject(E instance) {
        return injector.inject(instance);
    }

    // configuration

    public static Configurator configure() {
        return new Configurator();
    }

    public static Semla create() {
        return new Configurator().create();
    }

    public static class Configurator {

        private final Semla semla = new Semla();
        private final List<Datasource<?>> datasources = new ArrayList<>();
        private final List<Module> modules = new ArrayList<>();
        private final List<Throwables.UnaryOperator<Binder>> bindings = Lists.of(
            binder -> binder.bind(Semla.class).to(semla),
            binder -> binder.register(DatasourceFactory.class),
            binder -> binder.register(EntityManagerFactory.class),
            binder -> binder.register(TypedEntityManagerFactory.class),
            binder -> binder.register(ValidatorProvider.class),
            binder -> binder.bind(new TypeReference<Supplier<UUID>>() {}).to(uuidSupplier())
        );
        private Supplier<UUID> uuidSupplier = UUID::randomUUID;
        private Function<Throwables.UnaryOperator<Binder>[], Injector> injectorFunction = SemlaInjector::create;
        private Datasource.Configuration defaultDatasourceConfiguration;

        public Configurator withDefaultDatasource(Datasource.Configuration defaultDatasource) {
            this.defaultDatasourceConfiguration = defaultDatasource;
            return this;
        }

        public Configurator withInjector(Function<Throwables.UnaryOperator<Binder>[], Injector> injectorFunction) {
            this.injectorFunction = injectorFunction;
            return this;
        }

        @SafeVarargs
        public final Configurator withBindings(Throwables.UnaryOperator<Binder>... bindings) {
            this.bindings.addAll(Arrays.asList(bindings));
            return this;
        }

        public Configurator.BulkDatasourceConfigurator withDatasourceOf(Class<?> clazz, Class<?>... other) {
            return new Configurator.BulkDatasourceConfigurator(Lists.of(clazz, other));
        }

        public Configurator withDatasource(Datasource<?> datasource) {
            datasources.add(datasource);
            return this;
        }

        public Configurator withModules(Module module, Module... modules) {
            return withModules(Lists.of(module, modules));
        }

        public Configurator withModules(List<Module> modules) {
            this.modules.addAll(modules);
            return this;
        }

        private Supplier<UUID> uuidSupplier() {
            return uuidSupplier;
        }

        public Configurator withUUIDGenerator(Supplier<UUID> uuidSupplier) {
            this.uuidSupplier = uuidSupplier;
            return this;
        }

        public class BulkDatasourceConfigurator {

            private final List<Class<?>> classes;

            public BulkDatasourceConfigurator(List<Class<?>> classes) {
                this.classes = classes;
            }

            public Configurator as(Datasource.Configuration configuration) {
                classes.forEach(clazz -> datasources.add(configuration.create(EntityModel.of(clazz))));
                return Configurator.this;
            }
        }

        @SuppressWarnings("unchecked")
        public Semla create() {
            modules.forEach(module -> bindings.add(binder -> {
                module.configure(binder);
                return binder;
            }));

            Injector injector = injectorFunction.apply(bindings.toArray(new Throwables.UnaryOperator[0]));

            DatasourceFactory datasourceFactory = injector.getInstance(DatasourceFactory.class);
            if (defaultDatasourceConfiguration != null) {
                datasourceFactory.setDefaultDatasourceConfiguration(defaultDatasourceConfiguration);
            }
            datasources.forEach(datasourceFactory::registerDatasource);

            semla.injector = injector;
            return semla;
        }
    }

}
