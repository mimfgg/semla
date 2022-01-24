package io.semla.inject;

import io.semla.exception.InjectionException;
import io.semla.reflect.*;
import io.semla.serialization.yaml.Yaml;
import io.semla.serialization.yaml.YamlSerializer;
import io.semla.util.Strings;
import io.semla.util.Throwables;
import io.semla.util.Unchecked;
import io.semla.util.function.TriFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.enterprise.context.NormalScope;
import javax.inject.Inject;
import javax.inject.Scope;
import javax.inject.Singleton;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.semla.util.Singleton.lazy;
import static io.semla.util.Unchecked.unchecked;

@SuppressWarnings("unchecked")
public class SemlaInjector implements Injector {

    protected static final AnnotationMatcher ANY = new AnnotationMatcher();
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected final Map<Object, String> allocationTraces = new LinkedHashMap<>();
    protected final Map<Type, Map<String, Factory<?>>> factoryCache = new LinkedHashMap<>();
    protected final Map<Type, Map<AnnotationMatcher, UnaryOperator<Object>>> interceptors = new LinkedHashMap<>();
    protected final List<Factory<?>> factories = new ArrayList<>();
    protected final Factory<?> defaultFactory = new TypedFactory<Object>() {
        @Override
        public Object create(Type type, Annotation[] annotations) {
            return createNewInstance(Types.rawTypeOf(type));
        }
    };
    protected boolean requireExplicitBinding = false;

    protected void addFactory(Factory<?> factory) {
        try {
            throw new InjectionException("stack extraction");
        } catch (InjectionException e) {
            String trace = factory + "\n  defined at " +
                Stream.of(e.getStackTrace())
                    .filter(element -> !element.getClassName().contains("Binder$")
                        && !element.getMethodName().equals("addFactory")
                        && !element.getMethodName().equals("register"))
                    .findFirst()
                    .map(StackTraceElement::toString)
                    .orElse("unknown");
            logger.trace("adding factory: {}", trace);
            this.allocationTraces.put(factory, trace);
        }

        this.factories.add(factory);
    }

    @Override
    public <E> E getInstance(Type type, Annotation... rawAnnotations) {
        Annotation[] annotations = Stream.of(rawAnnotations).filter(annotation -> !(annotation instanceof Inject)).toArray(Annotation[]::new);
        String annotationsSignature = Strings.toString(annotations);
        if (!factoryCache.computeIfAbsent(type, t -> new LinkedHashMap<>()).containsKey(annotationsSignature)) {
            // let see if we have a candidate
            List<Factory<?>> candidates = factories.stream()
                .filter(factory -> factory.appliesTo(type, annotations))
                .collect(Collectors.toList());
            if (candidates.isEmpty()) {
                boolean isNullable = Stream.of(annotations).anyMatch(annotation -> annotation instanceof Nullable);
                if (isNullable) {
                    return null;
                }
                if (requireExplicitBinding) {
                    throw new InjectionException(
                        "explicit binding is required and there was candidate for injecting " + type + " with annotations: " + annotationsSignature
                    );
                }
                // no explicit binding required, let's try some default factories
                Class<?> clazz = Types.rawTypeOf(type);
                if (clazz.isAnnotationPresent(Singleton.class)) {
                    candidates.add(new TypedSupplierFactory<>(type, lazy(() -> createNewInstance(clazz)), ANY));
                } else {
                    candidates.add(defaultFactory);
                }
            } else if (candidates.size() > 1) {
                throw new InjectionException("multiple candidates for:" +
                    "\ntype: " + type +
                    "\nannotated with: " + annotationsSignature +
                    "\ncandidates:\n" + Yaml.write(candidates.stream().map(allocationTraces::get).collect(Collectors.toList()), YamlSerializer.NO_BREAK));
            }

            Factory<?> candidate = candidates.get(0);

            Optional<UnaryOperator<Object>> interceptorMatch = interceptors.containsKey(type)
                ? interceptors.get(type).entrySet().stream().filter(e -> e.getKey().test(annotations)).map(Map.Entry::getValue).findFirst()
                : Optional.empty();
            if (interceptorMatch.isPresent()) {
                UnaryOperator<Object> interceptor = interceptorMatch.get();
                factoryCache.get(type).put(annotationsSignature, new TypedFactory<Object>() {
                    @Override
                    public Object create(Type type, Annotation[] annotations) {
                        return interceptor.apply(candidate.create(type, annotations));
                    }
                });
            } else {
                factoryCache.get(type).put(annotationsSignature, candidate);
            }
        }
        return (E) factoryCache.get(type).get(annotationsSignature).create(type, annotations);
    }

    protected <E> E createNewInstance(Class<E> clazz) {
        if (clazz.getPackage().getName().startsWith("java.")) {
            throw new InjectionException("cannot create an instance of " + clazz);
        }

        if (clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers())) {
            throw new InjectionException("member " + clazz + " is not static, it cannot be instanciated without a context.");
        }

        List<Constructor<?>> injectedConstructors = Stream.of(clazz.getConstructors())
            .filter(constructor -> constructor.isAnnotationPresent(Inject.class)).collect(Collectors.toList());
        if (injectedConstructors.size() > 1) {
            throw new InjectionException("multiple candidates for constructing " + clazz + ": \n" + injectedConstructors);
        }

        E instance = Optional.ofNullable(injectedConstructors.isEmpty() ? null : injectedConstructors.get(0))
            .map(constructor -> unchecked(() -> (E) constructor.newInstance(getInjectedParametersFor(constructor))))
            .orElseGet(() -> {
                try {
                    return clazz.getConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new InjectionException("no zero parameter constructor available on " + clazz, e);
                }
            });
        return inject(instance);
    }

    @Override
    public <E> E inject(E instance) {
        Fields.of(instance).filter(field -> field.isAnnotationPresent(Inject.class)).forEach(field ->
            Unchecked.catchAndPrefixMessage(() -> "while injecting field '" + field + "':\n", () -> inject(instance, field))
        );

        Methods.of(instance).filter(method -> method.isAnnotationPresent(Inject.class)).forEach(method ->
            Unchecked.catchAndPrefixMessage(() -> "while injecting method '" + method + "':\n", () -> inject(instance, method))
        );
        return instance;
    }

    protected <E> void inject(E instance, Method method) {
        Methods.invoke(instance, method, getInjectedParametersFor(method));
    }

    protected <E> void inject(E instance, Field field) {
        Fields.setValue(instance, field, getInstance(field.getGenericType(), field.getAnnotations()));
    }

    protected Object[] getInjectedParametersFor(Constructor<?> constructor) {
        return IntStream.range(0, constructor.getParameterCount()).mapToObj(i ->
            getInstance(constructor.getGenericParameterTypes()[i], constructor.getParameters()[i].getAnnotations())
        ).toArray();
    }

    protected Object[] getInjectedParametersFor(Method method) {
        return IntStream.range(0, method.getParameterCount()).mapToObj(i ->
            getInstance(method.getGenericParameterTypes()[i], method.getParameters()[i].getAnnotations())
        ).toArray();
    }

    @SafeVarargs
    public static SemlaInjector create(Throwables.UnaryOperator<Binder>... bindings) {
        SemlaBinder binder = new SemlaBinder(new SemlaInjector());
        binder.bind(Injector.class).to(binder.injector);
        binder.bind(SemlaInjector.class).to(binder.injector);
        binder.scopedFactories.put(Singleton.class, (type, supplier, predicates) ->
            new TypedSupplierFactory<>(type, lazy(supplier), predicates)
        );
        Stream.of(bindings).forEach(binding -> unchecked(() -> binding.apply(binder)));
        binder.multiBindings.forEach((t, b) -> b.forEach(((filter, suppliers) -> {
            binder.injector.factories.add(new Factory<Object>() {
                @Override
                public boolean appliesTo(Type type, Annotation[] annotations) {
                    Optional<Type> argument = Types.optionalTypeArgumentOf(type);
                    return argument.isPresent() && Types.isAssignableTo(type, Collection.class) && t.equals(argument.get());
                }

                @Override
                public Object create(Type type, Annotation[] annotations) {
                    return suppliers.stream().map(Supplier::get).collect(Collectors.toCollection(Types.supplierOf(type)));
                }
            });
        })));
        binder.lateBindings.forEach(Runnable::run);
        return binder.injector;
    }

    protected static class SemlaBinder implements Binder {

        protected final SemlaInjector injector;
        protected Map<Class<? extends Annotation>, TriFunction<Type, Supplier<?>, AnnotationMatcher, Factory<?>>> scopedFactories = new LinkedHashMap<>();
        protected final Map<Type, Map<AnnotationMatcher, List<Supplier<?>>>> multiBindings = new LinkedHashMap<>();
        protected final List<Runnable> lateBindings = new ArrayList<>();

        public SemlaBinder(SemlaInjector injector) {
            this.injector = injector;
        }

        @Override
        public Binder requireExplicitBinding() {
            this.injector.requireExplicitBinding = true;
            return this;
        }

        @Override
        public <E> SemlaBinding<E> bind(Class<E> clazz) {
            return new SemlaBinding<>(clazz);
        }

        @Override
        public <E> SemlaBinding<E> bind(TypeReference<E> typeReference) {
            return new SemlaBinding<>(typeReference.getType());
        }

        @Override
        public Binder register(Factory<?> factory) {
            injector.addFactory(factory);
            return this;
        }

        @Override
        public Binder register(Class<? extends Factory<?>> factoryClass) {
            lateBindings.add(() -> injector.addFactory(injector.getInstance(factoryClass)));
            return this;
        }

        @Override
        public Binder register(Class<? extends Annotation> scope, TriFunction<Type, Supplier<?>, AnnotationMatcher, Factory<?>> factoryBuilder) {
            scopedFactories.put(scope, factoryBuilder);
            return this;
        }

        @Override
        public <E> SemlaBindingInterceptor<E> intercept(Class<E> clazz) {
            return new SemlaBindingInterceptor<>(clazz);
        }

        @Override
        public <E> SemlaBindingInterceptor<E> intercept(TypeReference<E> typeReference) {
            return new SemlaBindingInterceptor<>(typeReference.getType());
        }

        @Override
        public <E> MultiBinding<E> multiBind(Class<E> clazz) {
            return new SemlaMultiBinding<>(clazz);
        }

        protected static abstract class SemlaFilteredBinding<SelfType> implements FilteredBinding<SelfType> {

            protected final Type type;
            protected AnnotationMatcher annotationMatcher = new AnnotationMatcher();

            protected SemlaFilteredBinding(Type type) {
                this.type = type;
            }

            @Override
            public SelfType named(String name) {
                return annotatedWith(Value.named(name));
            }

            @Override
            public SelfType annotatedWith(Class<? extends Annotation> annotationClass) {
                return annotatedWith(Annotations.defaultOf(annotationClass));
            }

            @Override
            public SelfType annotatedWith(Annotation annotation) {
                this.annotationMatcher.add(annotation);
                return (SelfType) this;
            }
        }

        protected class SemlaBinding<E> extends SemlaFilteredBinding<Binding<E>> implements Binding<E> {

            protected Class<? extends Annotation> scope;

            protected SemlaBinding(Type type) {
                super(type);
            }

            @Override
            public SemlaBinding<E> in(Class<? extends Annotation> scope) {
                if (!scope.isAnnotationPresent(Scope.class) && !scope.isAnnotationPresent(NormalScope.class)) {
                    throw new IllegalArgumentException(scope.getCanonicalName() + " must be annotated with " + Scope.class.getCanonicalName() + " or " + NormalScope.class.getCanonicalName());
                }
                this.scope = scope;
                return this;
            }

            @Override
            public SemlaBinder to(Class<? extends E> clazz) {
                return toScopedSupplier(() -> injector.createNewInstance(clazz));
            }

            @Override
            public SemlaBinder to(E instance) {
                return toSupplier(() -> instance);
            }

            @Override
            public Binder toConstructor(Constructor<? extends E> constructor) {
                return toScopedSupplier(() -> unchecked(() -> constructor.newInstance(injector.getInjectedParametersFor(constructor))));
            }

            @Override
            public SemlaBinder toSupplier(Class<? extends Supplier<? extends E>> supplierClass) {
                return toScopedSupplier(injector.getInstance(supplierClass));
            }

            protected SemlaBinder toScopedSupplier(Supplier<? extends E> supplier) {
                if (scope != null) {
                    TriFunction<Type, Supplier<?>, AnnotationMatcher, Factory<?>> factoryBuilder = scopedFactories.get(scope);
                    if (factoryBuilder == null) {
                        throw new IllegalArgumentException("no scopedFactory for scope: " + scope.getCanonicalName());
                    }
                    injector.addFactory(factoryBuilder.apply(type, supplier, annotationMatcher));
                    return SemlaBinder.this;
                }
                return toSupplier(supplier);
            }

            public SemlaBinder toSupplier(Supplier<? extends E> supplier) {
                injector.addFactory(new TypedSupplierFactory<>(type, supplier, annotationMatcher));
                return SemlaBinder.this;
            }
        }

        protected class SemlaMultiBinding<E> extends SemlaFilteredBinding<MultiBinding<E>> implements MultiBinding<E> {

            protected SemlaMultiBinding(Type type) {
                super(type);
            }

            @Override
            public Binder add(Class<? extends E> clazz) {
                getMultiBindings().add((Supplier<E>) () -> injector.getInstance(clazz));
                return SemlaBinder.this;
            }

            @Override
            public Binder add(Collection<Class<? extends E>> classes) {
                getMultiBindings().addAll(classes.stream().map(clazz -> (Supplier<E>) () -> injector.getInstance(clazz)).toList());
                return SemlaBinder.this;
            }

            @Override
            public Binder add(E instance) {
                getMultiBindings().add((Supplier<E>) () -> instance);
                return SemlaBinder.this;
            }

            private List<Supplier<?>> getMultiBindings() {
                return multiBindings
                    .computeIfAbsent(type, t -> new LinkedHashMap<>())
                    .computeIfAbsent(annotationMatcher, f -> new ArrayList<>());
            }
        }

        protected class SemlaBindingInterceptor<E> extends SemlaFilteredBinding<BindingInterceptor<E>> implements BindingInterceptor<E> {

            protected SemlaBindingInterceptor(Type type) {
                super(type);
            }

            @Override
            public Binder with(UnaryOperator<E> unaryOperator) {
                injector.interceptors.computeIfAbsent(type, t -> new LinkedHashMap<>()).put(annotationMatcher, (UnaryOperator<Object>) unaryOperator);
                return SemlaBinder.this;
            }
        }
    }
}
