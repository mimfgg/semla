# Semla

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.semla/semla/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.semla/semla)
![Build](https://github.com/mimfgg/semla/workflows/master/badge.svg)
[![codecov](https://codecov.io/gh/mimfgg/semla/branch/master/graph/badge.svg)](https://codecov.io/gh/mimfgg/semla)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
![lifecycle: beta](https://img.shields.io/badge/lifecycle-beta-509bf5.svg)

Semla is a lightweight library driven by the [Java Persistence API](https://en.wikipedia.org/wiki/Java_Persistence_API)
supporting most of the features required to persist, query, serialize/deserialize entities as well as injecting
dependencies.

It could be seen as Hibernate + Jackson + Guava + Guice, all in one.

Using reflection and static/dynamic source generation, it provides fluent and typed interfaces that can be used as DAOs.
The query language is independant of the storage vendor and remains the same if you migrate from one database vendor to
another.

**One biggest difference with other JPA frameworks is that there is no persistence context in Semla. 
All the objects you will get are ready to use and won't introduce any side effect due to Proxies not being initialized.**

Semla is fully extensible but comes with those maven modules:

* [semla-common](/semla-common): common library including a lot of utils as well as the json and yaml serializers
* [semla-common-test](/semla-common-test): common test library based on [tzatziki](https://github.com/Decathlon/tzatziki)
* [semla-inject](/semla-inject): dependency injection library.
* [semla-jpa](/semla-jpa): the base JPA library.
* [semla-jpa-test](/semla-jpa-test): test library for any datasource based on the JPA module
* [semla-jdbi](/semla-jdbi): generic SQL database support using [jdbi](http://jdbi.org/)
* [semla-logging](/semla-logging): Logging support using [logback](http://logback.qos.ch/)
* [semla-maven-plugin](/semla-maven-plugin): maven plugin to generate typed interfaces
* [semla-memcached](/semla-memcached): memcached support using [spymemcached](https://github.com/couchbase/spymemcached)
* [semla-mongodb](/semla-mongodb): Mongodb support using [mongo-java-driver](https://mongodb.github.io/mongo-java-driver/)
* [semla-mysql](/semla-mysql): Mysql support extending [semla-jdbi](/semla-jdbi)
* [semla-postgresql](/semla-postgresql): Postgresql support extending [semla-jdbi](/semla-jdbi)
* [semla-redis](/semla-redis): Redis support using [jedis](https://github.com/xetorthio/jedis)
* [semla-grapql](/semla-graphql): graphql support and autogenerated schema from your entities using [graphql-java](https://github.com/graphql-java/graphql-java)
* [semla-jackson](/semla-jackson): module to allow using semla's serializer/deserializer in jackson 

## Get started

*Example given with mysql, but you can replace the module with the vendor of your choice!*

Get it from maven central:

```xml

<dependency>
    <groupId>io.semla</groupId>
    <artifactId>semla-mysql</artifactId>
    <version>1.x.x</version>
    <scope>compile</scope>
</dependency>
```

*Semla uses names very similar to those used by JPA, but their usage and interface might differ a bit, for example:*

* `io.semla.datasource.Datasource<T>` is the low level datasource translating the query to the vendor API
* `io.semla.persistence.EntityManager<T>` is the class implementing all the query logic
* `io.semla.persistence.EntityManagerFactory` is the class generating the EntityManagers
* `io.semla.persistence.PersistenceContext` is local to a user query and will keep track of which entities and relations
  have been already fetched.

Semla comes with a plugin to generate typed EntityManagers extending `io.semla.persistence.TypedEntityManager` and
having type-safe methods for all the properties of your types.

Given that you annotate a `User` class with `io.semla.persistence.annotations.Managed` and that you add this plugin to
your project:

```xml

<plugin>
    <groupId>io.semla</groupId>
    <artifactId>semla-maven-plugin</artifactId>
    <version>1.x.x</version>
    <configuration>
        <sources>
            <source>/src/main/java/package/of/your/model/**</source>
        </sources>
    </configuration>
    <executions>
        <execution>
            <phase>generate-sources</phase>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Then running `mvn generate-sources` should generate a new class `UserManager` extending `TypedEntityManager`.

## Configuration

The main class is the `io.semla.Semla` class, which can be configured for example with a default mysql datasource:

```java
 Semla semla = Semla.configure()
    .withDefaultDatasource(MysqlDatasource.configure()
        .withJdbcUrl("url")
        .withUsername("username")
        .withPassword("password"))
    .create();
```

A datasource configuration shared for a set of entities:

```java
 Semla semla = Semla.configure()
     .withDatasourceOf(User.class, Group.class)
        .as(MysqlDatasource.configure()
            .withJdbcUrl("url")
            .withUsername("username")
            .withPassword("password"))
     .withDatasourceOf(Cache.class)
        .as(RedisDatasource.configure()
            .withHost("1.2.3.4"))         
     .create();
```

Or directly a specific datasource:

```java
 Semla semla = Semla.configure()
     .withDatasource(MysqlDatasource.configure()
         .withJdbcUrl("url")
         .withUsername("username")
         .withPassword("password"))
         .create(EntityModel.of(User.class)))
     .create();
```

Semla can easily mix different datasources and recursively query them. You can even write a Datasource for your favorite
vendor if it's not already supported!

By default, the following implementations are included in the library: 

- Postgresql
- MySQL
- MongoDB
- Redis
- Memcached

As well as some useful datasources:

- InMemoryDatasource: useful for prototyping, it is a non-expiring in-memory relational datasource backed by a HashMap.
- SoftKeyValueDatasource: SoftHashMap backed datasource that can be used for caching.
- KeyValueDatasource: NoSQL interface to extend in other Datasources (like memcached or redis)
- CachedDatasource: 2 layers datasource using a KeyValueDatasource as a cache layer
- MasterSlaveDatasource: "write one, read all" replicated datasource, to use for example with a Mysql cluster.
- ReadOneWriteAllDatasource: when you want replication to be handled by Semla.
- ShardedDatasource: shards on primary key and automatically rebalances if a shard is added.

Semla will create a model for each type it manages, mostly holding instances of everything obtained through reflection. If
the type is annotated with `javax.persistence.Entity`, it will create an `io.semla.model.EntityModel` that will also
contain information about the relational and column annotations present on the type.

## Dependency injection

Semla packs its own dependency injection framework which can be configured during the configuration:

```java
 Semla semla = Semla.configure()
    .withBindings(binder -> binder
        .bind(String.class).named("applicationName").to("myAwesomeService")
    )
    .create();
```

Bindings can also be organized in modules through the `io.semla.inject.Module` class:

```java
 Semla semla = Semla.configure()
    .withModules(new YourCustomModule())
    .create(); 
```

Explicit binding can be required with:

```java
 Semla semla = Semla.configure()
    .withBindings(Binder::requireExplicitBinding)
    .create(); 
```

Multibiding can be achieved with:

```java
 Semla semla = Semla.configure()
     .withBindings(binder -> binder
         .multiBind(Action.class).named("actions").add(ActionA.class) 
         .multiBind(Action.class).named("actions").add(Lists.of(ActionB.class)) // annotated
         .multiBind(Action.class).named("actions").add(new ActionC()) // will always return the same instance
     )
     .create();
 // actions will contain a new instance of ActionA, of ActionB and the implicit singleton of ActionC
 Set<Action> actions = injector.getInstance(Types.parameterized(Set.class).of(Action.class), Annotations.named("actions"));
```

You can intercept an injection (for debugging or testing purpose):

```java
 Semla semla = Semla.configure()
    .withBindings(binder -> binder
        .intercept(SomeObject.class).with(someObject -> {
            // do something with the object or swap it for another one
            return someObject;
        }))
    .create(); 
```

All the injector methods are available on the semla instance for convenience:

```java
 semla.getInstance(EntityManagerFactory.class);
 semla.getInstance(new TypeReference<EntityManager<User>>(){});
 semla.getInstance(YourType.class);
 semla.inject(yourInstance);
```

And if you are not interested in the entity management part of Semla, you can include solely the `semla-inject` module
and create the injector manually:

```java
  Injector injector = SemlaInjector.create(
          binder -> binder.bind(YourType.class).to(yourInstance));
```

### Factories

Factories are used by the injector to create all the instances and hold the singletons. A factory must implement
the `io.semla.inject.Factory` interface.

3 singleton factories are preconfigured:

- `io.semla.datasource.DatasourceFactory`: creates and holds all the `io.semla.datasource.Datasource<T>` instances (1
  per type)
- `io.semla.persistence.EntityManagerFactory`: creates and holds all the generic `io.semla.persistence.EntityManager<T>`
  instances
- `io.semla.persistence.TypedEntityManagerFactory`: creates and holds all the `io.semla.persistence.TypedEntityManager`
  implementations.

### Entity operations

Let's consider the 2 following classes:

```java
@Entity
@Managed
public class User {

  @Id
  @GeneratedValue
  public int id;

  @NotNull
  public String name;

  @ManyToOne
  public Group group;
}
```

```java
@Entity
@Managed
public class Group {

  @Id
  @GeneratedValue
  public int id;

  @NotNull
  public String name;

  @OneToMany(mappedBy = "group")
  public List<User> users;
}
```

Once your factory is configured, you can get an `io.semla.persistence.EntityManager` instance:

```java
 EntityManager<User> userManager = semla.getInstance(EntityManagerFactory.class).of(User.class);
```

This is a generic entity manager that will let you manipulate your entities and query your datasource.

However, if you have run the maven plugin to generate your TypeEntityManager classes, those 2 TypeEntityManager are
available:

```java
 UserManager userManager = semla.getInstance(UserManager.class);
 GroupManager groupManager = semla.getInstance(GroupManager.class);
```

You can either use the generic, or the generated manager to query your entities. Since the second is mostly a wrapper
around the first, their behaviour is the same.

*The methods on the generic EntityManager are the same, but they use a String parameter in place of field names and enum
values.*

To manipulate your entities, the following operations are available:

#### Create

```java
 Group defaultGroup = groupManager.newGroup("default").create();
 User user = userManager.newUser("bob").group(defaultGroup).create();
```

#### Get

```java
 Optional<User> user = userManager.get(1);
 Map<Integer, User> users = userManager.get(1, 2, 3); // values not found will be returned as null in the map
```

#### Update/patch

You can either update a modified entity:

```java
 user.name = "tom";
 userManager.update(user);
```

Or patch it directly through the manager:

```java
 userManager.set().name("tom").where().id().is(1).patch();
```

#### Delete

```java
 boolean deleted = userManager.delete(1);
 long deleted = userManager.delete(1, 2, 3);
 long deleted = userManager.delete(Lists.of(1, 2, 3));
 long deleted = userManager.where().name().is("bob").delete();
 long deleted = userManager.where().name().in("bob", "tom").delete();
```

#### First

```java
 Optional<User> user = userManager.where().name().is("bob").first();
```

#### List

```java
 List<User> users = userManager.where().name().like("b.*").list();
```

#### Count

```java
 long count = userManager.where().name().like("b.*").count();
```

#### Include sub entities

Semla supports all the relations defined by the JPA annotations, so we can easily fetch sub entities in the same query:

```java
 List<Group> groups = groupManager.list(group -> group.users());
 Optional<User> bob = userManager.where().name().is("bob").first(user -> user.group()); 
```

Note that we pass a function as a parameter. The query can be read
as: `get the first user named bob and for this user get its group`

Relations can be traversed in both directions. For example, we can fetch all the users in Bob's group:

```java
 List<User> users = userManager.where().name().is("bob")
   .first(user -> user.group(group -> group.users()))
   .get().group.users;
```

#### Asynchronous Queries

Semla will expose an `async()` method whenever it can be applied, usually just before the method you would otherwise call.
The type returned by the `async()` method should contain the same methods and parameters than their synchronous equivalent, 
but they will all return a `CompletionStage` of the result. 

For example:
```java
 userManager.where().name().is("bob")
   .async()
   .list(user -> user.group(group -> group.users()))
   .thenAccept(users -> ...)
 userManager.async().get(1).thenApply(user -> ...)
 CompletionStage<Long> count = userManager.async().count();
```

By default, all the asynchronous queries will be run on the common `ForkJoinPool`. 
Not to run into thread depletion when running blocking calls, semla uses the 
[ManagedBlocker](https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ForkJoinPool.ManagedBlocker.html) interface
so that the ForkJoinPool elastically extends until 256 threads before queueing the extra jobs.

This behaviour can be tweaked by providing your own `ExecutorService` with:
```java
 Async.setDefaultExecutorService(yourExecutorService)
```

*Note: if you provide your own instance of a ForkJoinPool, this one will also be extended to follow the demand of blocking threads, the parallelism parameter will not be honored*

#### Predicates and query language

To select entities, the following predicates are available:

 * is(Object object)
 * not(Object object)
 * in(Object[] objects)
 * in(Object object, Object... objects)
 * notIn(Object[] objects)
 * notIn(Object object, Object... objects)
 * greaterOrEquals(Number number)
 * greaterThan(Number number)
 * lessOrEquals(Number number)
 * lessThan(Number number)
 * like(String pattern)
 * notLike(String pattern)
 * contains(String pattern)
 * doesNotContain(String pattern)
 * containedIn(String pattern)
 * notContainedIn(String pattern)
 
They can be chained to make a query filter:

```java
 List<User> users = userManager.where().name().like("b.*").and().id().lessThan(10).list();
```

Semla comes with its own simple query language mapping the executed query.

```java
 Query<Group, Optional<Group>> query = Query.<Group, Optional<Group>>parse("get the group where id is 1 including its users");
 Optional<Group> group = query.in(entityManagerFactory.newContext());
```

It is mostly used by the tests and for debugging, as it allows for reparsing the query printed in the logs.

Every query is thus mapped to a humanly readable expression, and for example the above query would output:

```
DEBUG [i.s.p.EntityManager] executing: list all the users where group is 1 ordered by id took 0.130142ms and returned [{id: 1, name: bob, group: 1}]
DEBUG [i.s.p.EntityManager] executing: get the group where id is 1 including its users took 0.196899ms and returned {id: 1, name: admin, users: [{id: 1, name: bob, group: 1}]}
```

#### Pagination

Entities can be ordered using:

```java
 List<User> users = userManager.orderedBy(name().desc()).startAt(10).limitTo(30).list();
```

#### Caching

If the injector is configured to use a Cache: 

```java
 Semla semla = Semla.configure()
    .withBindings(binder -> binder
        .bind(Cache.class).to(MemcachedDatasource.configure().withHosts("ip:port").asCache())
    )
    .create();
```

Then you can easily cache all the read queries with:

```java
 userManager.where().name().is("bob").cachedFor(Duration.ofMinutes(3)).first();
 userManager.cachedFor(Duration.ofMinutes(3)).get(1);
```

To manually refresh the cache:

```java
 userManager.where().name().is("bob").invalidateCache().cachedFor(Duration.ofMinutes(3)).first();
```

Or evict it:

```java
 userManager.where().name().is("bob").evictCache().first(); // this returns a void
```

You can also use your cache for custom queries:

```java
 long users = semla.getInstance(Cache.class).get("onlineUsers", () -> computeUserCounts(), Duration.ofMinutes(1));
```

If you need multiple caches, with different datasources, you should name them:

```java
  Semla semla = Semla.configure()
    .withBindings(binder -> binder
        .bind(Cache.class).named("shared").to(MemcachedDatasource.configure().withHosts("ip:port").asCache())
    )
    .create();

  semla.getInstance(Cache.class,Annotations.named("shared")).get(...);
```

All the datasources can be used as a cache, even the sql ones.

## Indices

if `@StrictIndices` is added to the class, then only the primary key and the explicitly indexed properties will be
queryable. The typed manager will not have the non indexed methods, and the generic manager will reject the queries at
runtime.

Indices on columns can be defined on the class as:

```java

@StrictIndices
@Indices(
    @Index(name = "idx_name_value", properties = {"name", "value"}, unique = true)
)
public class YourEntity...
```

Or directly on the field:

```java
@Indexed(unique = false)
public String name;
```

## Serialization / Deserialization

Semla includes both a Json and a Yaml serializer/deserizalizer. Available as singletons through the `Json` and `Yaml`
classes, they are thread safe and can take Options directly as parameters. However, if you want those options to be
default, you can either configure them or create your own instance locally.

Here are some usage examples:

```java
 List<Integer> list = Json.read("[1,2,3,4,5]");
 List<Integer> list = Json.read("[1,2,3,4,5]", LinkedList.class);
 Set<Integer> list = Json.read("[1,2,3,4,5]", new TypeReference<LinkedHashSet<Integer>>(){});
 Map<String, Integer> map = Yaml.read(inputStream);

 String content = Json.write(list);
 String content = Yaml.write(list);
 String content = Json.write(list, JsonSerializer.PRETTY); // enable pretty serialization only for this method call
 Json.defaultSerializer().defaultOptions().add(JsonSerializer.PRETTY); // enable pretty serialization for all
```

While less configurable than Jackson, it should be sufficient for most projects. Current options are:

| option                                 | description                                                          |
|----------------------------------------|----------------------------------------------------------------------|
| YamlSerializer.NO_BREAK                | will not split the yaml at 80 columns                                |
| JsonSerializer.PRETTY                  | indented pretty json                                                 | 
| Deserializer.IGNORE_UNKNOWN_PROPERTIES | will ignore unknown properties instead of throwing an exception      |
| Deserializer.UNWRAP_STRINGS            | will unwrap string properties if the expected type is something else |

However, contrary to Jackson, it does support references and anchors as well as including sub files through
the `!include` tag:

```yaml
data:
  <<: !include base.yaml
  more: value
```

Field serialization/deserialization can be controlled with the `@Serialize` and `@Deserialize` annotations.

By default, all getters/setters with matching fields are serialized/deserialized. Chained setters are also supported (
ie: `public T withName(String value)`). Regular methods have to be explicitly annotated to be serialized/deserialized.
Relational graphs are handled natively, so references to values should be preserved after deserialization.

An enum `When` is also available to serialize/deserialize only on some cases, the supported values
are: `ALWAYS, NEVER, NOT_NULL, NOT_EMPTY, NOT_DEFAULT`

For example:

```java
 public class Character {

    private String internalName;
    @Serialize(When.NOT_NULL)
    public String alias;

    @Serialize(as = "name")
    public String name() {
        return internalName;
    }

    @Deserialize(from = "name")
    public Character withName(String name) {
        this.internalName = name;
        // do something with the name
        return this;
    }
}
 ```

Finally, polymorphism is supported via the `@TypeInfo(property = "type")` and `@TypeName("typename")` annotations,
example:

```java
 @TypeInfo // type is the default value
 public abstract class Character {
   public String name;
 }

 @TypeName("hero")
 public class Hero extends Character {
 }
```

The Hero type needs to be registered:

```java
 Types.registerSubTypes(Hero.class);
```

Then it can be serialized and deserialized properly:

```java
 List<Character> characters = Yaml.read(
   "- type: hero" +
   "  name: Luke" +
   "- type: hero" +
   "  name: Leia",
   Types.parameterized(List.class).of(Character.class)
 );
```

*Note: subtypes can also be deserialized from their typenames only:*

```java
 List<Character> characters = Yaml.read("[hero, hero]", Types.parameterized(List.class).of(Character.class)); // this will return 2 default heroes
```

## Logging

*semla-logging* provides a nice wrapper around Logback.

Setting the log level in your application or tests is as simple as:
```java
  Logging.setTo(Level.ERROR);
```

However, you can also customize the logger:
```java
  Logging
    .withLogLevel(Level.INFO) // set the default log level to INFO
    .withAppenderLevel("io.semla", Level.ALL) // but a specific appender to ALL
    .withPattern("%-5p [%t]: %m%n")
    .setup();
```

Capture all your logs to a specific appender:
```java
  ListAppender listAppender = new ListAppender();
  Logging.withAppender(listAppender).noConsole().withPattern("%-5p [%t]: %m%n").setup();
```

Or log to a file, optionally rolling:
```java
  Logging.configure()
    .withPattern("%-5p [%t]: %m%n")
    .noConsole()
    .withFileAppender()
    .withLogFilename("test.log")
    // if you want to keep the last 30 days    
    .keep(30).withLogFilenamePattern("test-%d.log.gz")
    .setup();
```

## GraphQL

The *semla-graphql* module provides support for graphql. You can enable it by adding the dependency to your project and
the `GraphQLModule` module to your configuration:

```java
  Semla semla = Semla.configure()
    .withModules(new GraphQLModule())
    .create();
```  

This will make a `GraphQL` and a `GraphQLProvider` instance available in your injector. The `GraphQL` instance will be
configured with the base schema for all your entities, so you should be able to access your database right away.

The generated schema is available through:

```java
  String schema = semla.getInstance(GraphQLProvider.class).getSchema()
```

See the tests for
the [queries](https://github.com/mimfgg/semla/blob/master/semla-graphql/src/test/resources/io/semla/graphql/queries.feature)
and
the [configuration](https://github.com/mimfgg/semla/blob/master/semla-graphql/src/test/java/io/semla/graphql/GraphQLSupplierTest.java)
for more examples or for how to add your own queries, types and mutations to the base schema.

## Examples

check https://github.com/mimfgg/semla-examples for more examples!
