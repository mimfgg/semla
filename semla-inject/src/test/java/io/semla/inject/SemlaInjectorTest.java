package io.semla.inject;

import io.semla.exception.InjectionException;
import io.semla.reflect.TypeReference;
import io.semla.reflect.Types;
import io.semla.util.ImmutableSet;
import io.semla.util.Javassist;
import io.semla.util.Lists;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nullable;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Qualifier;
import javax.inject.Singleton;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SemlaInjectorTest {

    @Test(expected = InjectionException.class)
    public void fail_if_no_zero_arg_constructor() {
        SemlaInjector.create().getInstance(SomeNonInstanciableObject.class);
    }

    @Test(expected = InjectionException.class)
    public void fail_if_plain_java_parameters_in_constructor() {
        SemlaInjector.create().getInstance(SomeNonInstanciableObjectWithAString.class);
    }

    @Test(expected = InjectionException.class)
    public void fail_if_a_new_unknown_instance_is_created_while_explicit_binding_is_required() {
        SemlaInjector.create(Binder::requireExplicitBinding).getInstance(SomeObject.class);
    }

    @Test
    public void get_a_known_new_instance_while_explicit_binding_is_required() {
        SomeObject someObject = SemlaInjector.create(binder ->
            binder.requireExplicitBinding().bind(SomeObject.class).to(SomeObject.class)
        ).getInstance(SomeObject.class);
        Assert.assertNotNull(someObject);
    }

    @Test
    public void get_a_new_instance() {
        SomeObject someObject = SemlaInjector.create().getInstance(SomeObject.class);
        Assert.assertNotNull(someObject);
    }

    @Test
    public void get_a_singleton() {
        Injector injector = SemlaInjector.create();
        SomeSingleton first = injector.getInstance(SomeSingleton.class);
        SomeSingleton second = injector.getInstance(SomeSingleton.class);
        Assert.assertEquals(first, second);
    }

    @Test
    public void inject_a_member() {
        Injector injector = SemlaInjector.create();
        SomeSingleton someSingleton = injector.getInstance(SomeSingleton.class);
        SomeHost someHost = injector.getInstance(SomeHost.class);
        Assert.assertEquals(someSingleton, someHost.someSingleton);
    }

    @Test
    public void inject_a_constructor() {
        Injector injector = SemlaInjector.create();
        SomeSingleton someSingleton = injector.getInstance(SomeSingleton.class);
        SomeInjectedConstructor someInjectedConstructor = injector.getInstance(SomeInjectedConstructor.class);
        Assert.assertEquals(someSingleton, someInjectedConstructor.someSingleton);
    }

    @Test
    public void inject_a_method() {
        Injector injector = SemlaInjector.create();
        SomeSingleton someSingleton = injector.getInstance(SomeSingleton.class);
        SomeInjectedMethod someInjectedMethod = injector.getInstance(SomeInjectedMethod.class);
        Assert.assertEquals(someSingleton, someInjectedMethod.someSingleton);
    }

    @Test
    public void bind_a_value() {
        SomeSingleton someSingleton = new SomeSingleton();
        Injector injector = SemlaInjector.create(binder -> binder.bind(SomeSingleton.class).to(someSingleton));
        SomeInjectedMethod someInjectedMethod = injector.getInstance(SomeInjectedMethod.class);
        Assert.assertEquals(someSingleton, someInjectedMethod.someSingleton);
    }

    @Test
    public void named_field_binding() {
        Injector injector = SemlaInjector.create(binder -> binder
            .bind(String.class).named("myName").to("Heisenberg")
        );
        SomeHostWithANamedMember someHostWithANamedMember = injector.getInstance(SomeHostWithANamedMember.class);
        Assert.assertEquals(someHostWithANamedMember.name, "Heisenberg");
    }

    @Test
    public void get_value_by_annotation() {
        Injector injector = SemlaInjector.create(binder -> binder
            .bind(String.class).named("myName").to("Heisenberg")
        );
        Assert.assertEquals(injector.getInstance(String.class, Value.named("myName")), "Heisenberg");
    }

    @Test
    public void named_method_binding() {
        Injector injector = SemlaInjector.create(binder -> binder
            .bind(String.class).named("myName").to("Heisenberg")
        );
        SomeHostWithANamedMethod someHostWithANamedMethod = injector.getInstance(SomeHostWithANamedMethod.class);
        Assert.assertEquals(someHostWithANamedMethod.name, "Heisenberg");
    }

    @Test
    public void annotated_field_binding() {
        Injector injector = SemlaInjector.create(binder -> binder
            .bind(String.class).annotatedWith(UserName.class).to("Heisenberg")
        );
        SomeHostWithAnAnnotatedMember someHostWithAnAnnotatedMember = injector.getInstance(SomeHostWithAnAnnotatedMember.class);
        Assert.assertEquals(someHostWithAnAnnotatedMember.name, "Heisenberg");
    }

    @Test
    public void annotated_method_binding() {
        Injector injector = SemlaInjector.create(binder -> binder
            .bind(String.class).annotatedWith(UserName.class).to("Heisenberg")
        );
        SomeHostWithAnAnnotatedMethod someHostWithAnAnnotatedMethod = injector.getInstance(SomeHostWithAnAnnotatedMethod.class);
        Assert.assertEquals(someHostWithAnAnnotatedMethod.name, "Heisenberg");
    }

    @Test
    public void filtered_binding() {
        Injector injector = SemlaInjector.create(binder -> binder
            .bind(String.class)
            .annotatedWith(Value.named("myName"))
            .to("Heisenberg")
        );
        SomeHostWithANamedMember someHostWithANamedMember = injector.getInstance(SomeHostWithANamedMember.class);
        Assert.assertEquals(someHostWithANamedMember.name, "Heisenberg");
    }

    @Test
    public void single_annotation_filtered_binding() {
        Injector injector = SemlaInjector.create(binder -> binder
            .bind(String.class)
            .annotatedWith(Value.named("myName"))
            .to("Heisenberg")
        );
        SomeHostWithANamedMember someHostWithANamedMember = injector.getInstance(SomeHostWithANamedMember.class);
        Assert.assertEquals(someHostWithANamedMember.name, "Heisenberg");
    }

    @Test
    public void supplier() {
        Injector injector = SemlaInjector.create(binder -> binder
            .bind(String.class).named("myName").toSupplier(() -> "Heisenberg")
        );
        SomeHostWithANamedMember someHostWithANamedMember = injector.getInstance(SomeHostWithANamedMember.class);
        Assert.assertEquals(someHostWithANamedMember.name, "Heisenberg");
    }

    @Test
    public void supplier_class() {
        Injector injector = SemlaInjector.create(binder -> binder
            .bind(String.class).named("myName").toSupplier(NameSupplier.class)
        );
        SomeHostWithANamedMember someHostWithANamedMember = injector.getInstance(SomeHostWithANamedMember.class);
        Assert.assertEquals(someHostWithANamedMember.name, "Heisenberg");
    }

    @Test
    public void injected_supplier_class() {
        Injector injector = SemlaInjector.create(binder -> binder
            .bind(String.class).named("theName").to("Heisenberg")
            .bind(String.class).named("myName").toSupplier(InjectedSupplier.class)
        );
        SomeHostWithANamedMember someHostWithANamedMember = injector.getInstance(SomeHostWithANamedMember.class);
        Assert.assertEquals(someHostWithANamedMember.name, "Heisenberg");
    }

    @Test(expected = InjectionException.class)
    public void fail_if_multiple_constructor_are_annotated_with_inject_and_suppliers_are_used() {
        SemlaInjector.create().getInstance(SomeObjectWith2InjectedConstructors.class);
    }

    @Test
    public void multiple_constructor_are_annotated_with_inject_and_a_constructor_is_binded() {
        SemlaInjector.create(binder ->
            binder.bind(SomeObjectWith2InjectedConstructors.class)
                .toConstructor(SomeObjectWith2InjectedConstructors.class.getConstructor(SomeSingleton.class, SomeObject.class))
        ).getInstance(SomeObjectWith2InjectedConstructors.class);
    }

    @Test
    public void bind_type_literals() {
        Map<String, String> configFile = new LinkedHashMap<>();
        SomeHostWithAConfig someHostWithAConfig = SemlaInjector.create(binder -> binder
            .bind(new TypeReference<Map<String, String>>() {}).named("Configuration").to(configFile)
        ).getInstance(SomeHostWithAConfig.class);
        Assert.assertEquals(someHostWithAConfig.config, configFile);
    }

    @Test
    public void get_instance_of_a_type_literal() {
        Map<String, String> configFile = new LinkedHashMap<>();
        Map<String, String> injectedConfigFile = SemlaInjector.create(binder -> binder
            .bind(new TypeReference<Map<String, String>>() {}).to(configFile)
        ).getInstance(new TypeReference<Map<String, String>>() {});
        Assert.assertEquals(injectedConfigFile, configFile);
    }

    @Test
    public void singleton_scope() {
        Injector injector = SemlaInjector.create(binder -> binder
            .bind(SomeObject.class).in(Singleton.class).to(SomeObject.class)
        );
        Assert.assertEquals(injector.getInstance(SomeObject.class), injector.getInstance(SomeObject.class));
    }

    @Test
    public void non_scope_annotation_given_as_scope() {
        assertThatThrownBy(() -> SemlaInjector.create(binder -> binder
            .bind(SomeObject.class).in(Named.class).to(SomeObject.class)
        )).hasMessage("javax.inject.Named must be annotated with javax.inject.Scope or javax.enterprise.context.NormalScope");
    }

    @Test
    public void defining_a_new_scope_factory() {
        Injector injector = SemlaInjector.create(binder -> binder
            .register(RequestScoped.class, (type, supplier, predicates) ->
                new TypedSupplierFactory<>(type,
                    () -> {
                        // doSomething to check the current request, like using a ThreadLocal
                        return supplier.get();
                    },
                    predicates))
            .bind(SomeObject.class).in(RequestScoped.class).to(SomeObject.class)
        );
        assertThat(injector.getInstance(SomeObject.class)).isNotNull();
    }

    @Test
    public void unsupported_scope() {
        assertThatThrownBy(() -> SemlaInjector.create(binder -> binder
            .bind(SomeObject.class).in(RequestScoped.class).to(SomeObject.class)
        )).hasMessage("no scopedFactory for scope: javax.enterprise.context.RequestScoped");
    }

    @Test
    public void nullable_arguments() {
        Injector injector = SemlaInjector.create(binder -> binder
            .bind(String.class).named("myName").toSupplier(() -> "Heisenberg")
        );
        ObjectWithNullable objectWithNullable = injector.getInstance(ObjectWithNullable.class);
        Assert.assertEquals(objectWithNullable.name, "Heisenberg");
    }

    @Test
    public void a_factory_can_be_used_to_build_a_subType() {
        Injector injector = SemlaInjector.create(binder ->
            binder.register(new TypedFactory<SuperType>() {
                @Override
                public SuperType create(Type type, Annotation[] annotations) {
                    return Types.newInstance(Types.rawTypeOf(type), Instant.now());
                }
            })
        );
        SubType subType = injector.getInstance(SubType.class);
        Assert.assertNotNull(subType);
    }

    @Test
    public void having_2_factories_for_the_same_type_throws_an_understable_error() {
        Injector injector = SemlaInjector.create(binder -> binder
            .bind(String.class).named("myName").toSupplier(() -> "Heisenberg")
            .bind(String.class).to("SomethingElse")
        );
        assertThatThrownBy(() -> injector.getInstance(ObjectWithNullable.class))
            .isInstanceOf(InjectionException.class)
            .hasMessageContainingAll(
                "defined at io.semla.inject.SemlaInjectorTest.lambda$having_2_factories_for_the_same_type_throws_an_understable_error$28(SemlaInjectorTest.java:",
                "defined at io.semla.inject.SemlaInjectorTest.lambda$having_2_factories_for_the_same_type_throws_an_understable_error$28(SemlaInjectorTest.java:"
            );

        assertThatThrownBy(() -> injector.inject(new SomeHostWithANamedMember()))
            .isInstanceOf(InjectionException.class)
            .hasMessageContainingAll(
                "while injecting field 'public java.lang.String io.semla.inject.SemlaInjectorTest$SomeHostWithANamedMember.name'",
                "defined at io.semla.inject.SemlaInjectorTest.lambda$having_2_factories_for_the_same_type_throws_an_understable_error$28(SemlaInjectorTest.java:",
                "defined at io.semla.inject.SemlaInjectorTest.lambda$having_2_factories_for_the_same_type_throws_an_understable_error$28(SemlaInjectorTest.java:"
            );

        assertThatThrownBy(() -> injector.inject(new SomeHostWithANamedMethod()))
            .isInstanceOf(InjectionException.class)
            .hasMessageContainingAll(
                "while injecting method 'public void io.semla.inject.SemlaInjectorTest$SomeHostWithANamedMethod.setName(java.lang.String)'",
                "defined at io.semla.inject.SemlaInjectorTest.lambda$having_2_factories_for_the_same_type_throws_an_understable_error$28(SemlaInjectorTest.java:",
                "defined at io.semla.inject.SemlaInjectorTest.lambda$having_2_factories_for_the_same_type_throws_an_understable_error$28(SemlaInjectorTest.java:"
            );
    }

    @Test
    public void get_instance_of_a_non_static_member_class_is_handled() {
        Injector injector = SemlaInjector.create();
        assertThatThrownBy(() -> injector.getInstance(Javassist.ClassBuilder.MemberBuilder.class))
            .hasMessage("member class io.semla.util.Javassist$ClassBuilder$MemberBuilder is not static, it cannot be instanciated without a context.");
    }

    @Test
    public void injection_decoration() {
        AtomicBoolean intercepted = new AtomicBoolean(false);
        Injector injector = SemlaInjector.create(binder -> binder
            .intercept(SomeObject.class).with(someObject -> {
                intercepted.set(true);
                return someObject;
            })
            .bind(List.class).named("non-empty").to(new ArrayList<>())
            .intercept(List.class).named("non-empty").with(value -> Lists.of("never empty ohohoh"))
        );
        injector.getInstance(SomeObject.class);
        assertThat(intercepted).isTrue();
        assertThat(injector.<List<String>>getInstance(List.class, Value.named("non-empty")))
            .isEqualTo(Lists.of("never empty ohohoh"));
    }

    @Test
    public void multibinding() {
        Injector injector = SemlaInjector.create(binder -> binder
            .multiBind(Action.class).named("actions").add(ActionA.class)
            .multiBind(Action.class).named("actions").add(Lists.of(ActionB.class, ActionC.class))
        );
        Set<Action> actions = injector.getInstance(Types.parameterized(Set.class).of(Action.class), Value.named("actions"));
        assertThat(actions).isNotNull().isNotEmpty();
        assertThat(actions.stream().map(Action::getClass).collect(Collectors.toSet()))
            .isEqualTo(ImmutableSet.of(ActionA.class, ActionB.class, ActionC.class));
        Action actionC = actions.stream().filter(action -> action instanceof ActionC).findFirst().get();
        Set<Action> actions2 = injector.getInstance(Types.parameterized(Set.class).of(Action.class), Value.named("actions"));
        Action actionsC2 = actions2
            .stream().filter(action -> action instanceof ActionC).findFirst().get();
        assertThat(actionC).isEqualTo(actionsC2);
        Action actionB = actions.stream().filter(action -> action instanceof ActionB).findFirst().get();
        Action actionsB2 = actions2
            .stream().filter(action -> action instanceof ActionC).findFirst().get();
        assertThat(actionB).isNotEqualTo(actionsB2);
    }

    @Test
    public void multibindingOfInstances() {
        ActionA actionA = new ActionA();
        ActionB actionB = new ActionB();
        Injector injector = SemlaInjector.create(binder -> {
            Binder.MultiBinding<Action> actions = binder.multiBind(Action.class).named("actions");
            actions.add(actionA);
            actions.add(actionB);
            return binder;
        });
        Set<Action> actions = injector.getInstance(Types.parameterized(Set.class, Action.class), Value.named("actions"));
        assertThat(actions).isNotNull().isNotEmpty();
        assertThat(actions.stream().map(Action::getClass).collect(Collectors.toSet()))
            .isEqualTo(ImmutableSet.of(ActionA.class, ActionB.class));
        assertThat(actions.stream().filter(action -> action instanceof ActionA).findFirst().get()).isEqualTo(actionA);
        assertThat(actions.stream().filter(action -> action instanceof ActionB).findFirst().get()).isEqualTo(actionB);
    }
    ////

    public static class SomeNonInstanciableObject {

        public final String name;

        public SomeNonInstanciableObject(String name) {
            this.name = name;
        }
    }

    public static class SomeNonInstanciableObjectWithAString {

        public final String name;

        @Inject
        public SomeNonInstanciableObjectWithAString(String name) {
            this.name = name;
        }
    }

    public static class SomeObject {

    }

    @Singleton
    public static class SomeSingleton {

    }

    public static class SomeHost {

        @Inject
        public SomeSingleton someSingleton;

    }

    public static class SomeInjectedMethod {

        public SomeSingleton someSingleton;

        @Inject
        public void setSomeSingleton(SomeSingleton someSingleton) {
            this.someSingleton = someSingleton;
        }
    }

    public static class SomeInjectedConstructor {

        public final SomeSingleton someSingleton;

        @Inject
        public SomeInjectedConstructor(SomeSingleton someSingleton) {
            this.someSingleton = someSingleton;
        }
    }

    public static class SomeObjectWith2InjectedConstructors {

        public final SomeSingleton someSingleton;
        public final SomeObject someObject;

        @Inject
        public SomeObjectWith2InjectedConstructors(SomeSingleton someSingleton) {
            this(someSingleton, null);
        }

        @Inject
        public SomeObjectWith2InjectedConstructors(SomeSingleton someSingleton, SomeObject someObject) {
            this.someSingleton = someSingleton;
            this.someObject = someObject;
        }
    }

    public static class SomeHostWithANamedMember {

        @Inject
        @Named("myName")
        public String name;
    }

    public static class SomeHostWithAnAnnotatedMember {

        @Inject
        @UserName
        public String name;
    }

    public static class SomeHostWithANamedMethod {

        public String name;

        @Inject
        public void setName(@Named("myName") String name) {
            this.name = name;
        }
    }

    public static class SomeHostWithAnAnnotatedMethod {

        public String name;

        @Inject
        public void setName(@UserName String name) {
            this.name = name;
        }
    }

    public static class SomeHostWithAConfig {

        @Inject
        @Named("Configuration")
        public Map<String, String> config;
    }

    @Qualifier
    @Retention(RUNTIME)
    public @interface UserName {

    }

    public static class NameSupplier implements Supplier<String> {
        @Override
        public String get() {
            return "Heisenberg";
        }
    }

    public static class InjectedSupplier implements Supplier<String> {

        @Inject
        @Named("theName")
        private String name;

        @Override
        public String get() {
            return name;
        }
    }

    public static class ObjectWithNullable {

        public String name;
        public String value;

        @Inject
        public ObjectWithNullable(@Named("myName") String name, @Nullable String value) {
            this.name = name;
            this.value = value;
        }
    }

    public static abstract class SuperType {

        private final Instant birth;

        public SuperType(Instant birth) {
            this.birth = birth;
        }

    }

    public static class SubType extends SuperType {

        public SubType(Instant birth) {
            super(birth);
        }
    }

    public interface Action {

        String doSomething();

    }

    public static class ActionA implements Action {

        @Override
        public String doSomething() {
            return "A";
        }
    }

    public static class ActionB implements Action {

        @Override
        public String doSomething() {
            return "B";
        }
    }

    @Singleton
    public static class ActionC implements Action {

        @Override
        public String doSomething() {
            return "B";
        }
    }

}
