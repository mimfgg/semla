package io.semla;

import io.semla.datasource.InMemoryDatasource;
import io.semla.inject.Binder;
import io.semla.inject.Module;
import io.semla.inject.SemlaInjector;
import io.semla.inject.Value;
import io.semla.model.EntityModel;
import io.semla.model.Fruit;
import io.semla.model.Player;
import io.semla.persistence.EntityManager;
import io.semla.persistence.EntityManagerFactory;
import org.junit.Assert;
import org.junit.Test;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SemlaTest {

    @Test
    public void define_a_specific_datasource() {
        Semla semla = Semla.configure()
            .withDatasource(InMemoryDatasource.configure().create(EntityModel.of(Player.class)))
            .create();
        EntityManager<Player> players = semla.getInstance(EntityManagerFactory.class).of(Player.class);
        Player bob = players.create(Player.with(1, "bob", 100));
    }

    @Test
    public void define_a_lazy_datasource() {
        Semla semla = Semla.configure()
            .withDatasourceOf(Player.class).as(InMemoryDatasource.configure())
            .create();
        EntityManager<Player> players = semla.getInstance(EntityManagerFactory.class).of(Player.class);
        Player bob = players.create(Player.with(1, "bob", 100));
    }

    @Test
    public void injecting_an_entity() {
        Semla semla = Semla.configure()
            .withDefaultDatasource(InMemoryDatasource.configure())
            .withBindings(binder -> binder
                .bind(String.class).named("applicationName").to("myTest")
            )
            .create();

        EntityManager<User> users = semla.getInstance(EntityManagerFactory.class).of(User.class);

        User bob = users.newInstance().with("name", "bob").create();
        Assert.assertEquals("myTest", bob.applicationName);

        assertThat(semla.getInstance(String.class, Value.named("applicationName"))).isEqualTo("myTest");
    }

    @Entity
    public static class User {

        @Inject
        @Named("applicationName")
        public transient String applicationName;

        @Id
        @GeneratedValue
        public int id;

        public String name;
    }


    @Test
    public void objects_are_validated() {
        Semla semla = Semla.configure()
            .withDefaultDatasource(InMemoryDatasource::new)
            .create();
        EntityManager<ValidatedEntity> manager = semla.getInstance(EntityManagerFactory.class).of(ValidatedEntity.class);

        assertThatThrownBy(() -> manager.newInstance().with("age", -2).create())
            .hasMessageContainingAll("age: must be greater than or equal to 0", "name: must not be null");
        ValidatedEntity validatedEntity = manager.newInstance().with("name", "test").with("age", 20).create();

        validatedEntity.age = -3;
        assertThatThrownBy(() -> manager.update(validatedEntity))
            .hasMessageContainingAll("age: must be greater than or equal to 0");
    }

    @Entity
    public static class ValidatedEntity {

        @Id
        @GeneratedValue
        public int id;

        @NotNull
        public String name;

        @Min(0)
        public int age;
    }

    @Test
    public void a_minimal_entityManagerFactory() {
        Assert.assertNotNull(Semla.create());
    }

    @Test
    public void no_default_datasource() {
        assertThatThrownBy(() -> Semla.create().getInstance(EntityManagerFactory.class).of(Fruit.class))
            .hasMessage("no default datasource is set and class io.semla.model.Fruit hasn't been explicitely registered!");
    }

    @Test
    public void define_an_injector() {
        Semla semla = Semla.configure()
            .withInjector(SemlaInjector::create)
            .withBindings(binder -> binder
                .bind(String.class).named("applicationName").to("myTest")
            )
            .create();

        Something something = semla.getInstance(Something.class);
        Assert.assertEquals("myTest", something.name);
    }

    public static class Something {

        @Inject
        @Named("applicationName")
        private String name;

    }

    @Test
    public void define_a_module() {
        Semla semla = Semla.configure()
            .withModules(new SomethingModule())
            .create();

        Something something = semla.getInstance(Something.class);
        Assert.assertEquals("myTest", something.name);
    }

    public static class SomethingModule implements Module {

        @Override
        public void configure(Binder binder) {
            binder.bind(String.class).named("applicationName").to("myTest");
        }
    }

}
