Feature: JPA Entity Support

  Background:
    * source prepend:
    """
    package {{run}};
    import javax.persistence.*;
    import java.util.*;
    """

  Scenario: An entity with only public properties
    Given the class:
    """
    @Entity
    public class Something {

      @Id
      public int id;

      public String name;
    }
    """
    And that we create this something:
    """
    id: 1
    name: test
    """
    Then fetching the first something returns:
    """
    id: 1
    name: test
    """

  Scenario: An entity with a generated int id
    Given the class:
    """
    @Entity
    public class Something {

      @Id
      @GeneratedValue
      public int id;

      public String name;
    }
    """
    And that we create this something:
    """
    name: test
    """
    Then fetching the first something returns:
    """
    id: 1
    name: test
    """

  Scenario: An entity with a generated long id
    Given the class:
    """
    @Entity
    public class Something {

      @Id
      @GeneratedValue
      public long id;

      public String name;
    }
    """
    And that we create this something:
    """
    name: test
    """
    Then fetching the first something returns:
    """
    id: 1
    name: test
    """

  Scenario: An entity with a generated UUID id
    Given the class:
    """
    @Entity
    public class Something {

      @Id
      @GeneratedValue
      public UUID id;

      public String name;
    }
    """
    And that we create this something:
    """
    name: test
    """
    Then fetching the first something returns:
    """
    id: 00000000-0000-0000-0000-000000000001
    name: test
    """

  Scenario: An entity with a generated Integer id
    Given the class:
    """
    @Entity
    public class Something {

      @Id
      @GeneratedValue
      public Integer id;

      public String name;
    }
    """
    And that we create this something:
    """
    name: test
    """
    Then fetching the first something returns:
    """
    id: 1
    name: test
    """

  Scenario: A good old POJO
    Given the class:
    """
    @Entity
    public class Something {

      @Id
      private int id;

      private String name;

      public int getId() {
        return id;
      }

      public void setId(int id){
        this.id = id;
      }

      public String getName() {
        return name;
      }

      public void setName(String name){
        this.name = name;
      }
    }
    """
    And that we create this something:
    """
    id: 1
    name: test
    """
    Then fetching the first something returns:
    """
    id: 1
    name: test
    """

  Scenario: a class with different types of properties and methods
    Given this class:
    """
    @Entity
    public class Entry {

      @Id
      public int id;
      private String name;
      private String notThere;

      public String getName() {
        return this.name;
      }

      public void setName(String name) {
        this.name = name;
      }
    }
    """
    And that we create this entry:
    """
    id: 1
    name: test
    """
    Then fetching the first entry returns:
    """
    id: 1
    name: test
    """

  Scenario: an entity with an auto-incremented long key
    Given the class:
    """
    @Entity
    public class Something {

      @Id
      @GeneratedValue
      public long id;

      public String name;
    }
    """
    And that we create this something:
    """
    name: test
    """
    Then fetching the first something returns:
    """
    id: 1
    name: test
    """

  Scenario: @PrePersist Callback
    Given the class:
    """
    @Entity
    public class Something {

      @Id
      @GeneratedValue
      public long id;

      public String name;

      @PrePersist
      public void prePersist() {
        this.name = "anonymous";
      }
    }
    """
    And that we create this something:
    """
    name: test
    """
    Then fetching the first something returns:
    """
    id: 1
    name: anonymous
    """

  Scenario: @PostPersist Callback
    Given the class:
    """
    @Entity
    public class Something {

      @Id
      @GeneratedValue
      public long id;

      public String name;

      @PostPersist
      public void postPersist() {
        // some audit
      }
    }
    """
    And that we create this something:
    """
    name: test
    """
    Then fetching the first something returns:
    """
    id: 1
    name: test
    """

  Scenario: @EntityListeners support
    Given those classes:
    """
    @Entity
    @EntityListeners(SomethingListener.class)
    public class Something {

      @Id
      @GeneratedValue
      public long id;

      public String name;
    }

    ---

    public class SomethingListener {

      @PrePersist
      public void prePersist(Something something) {
        something.name = "anonymous";
      }

    }
    """
    And that we create this something:
    """
    name: test
    """
    Then fetching the first something returns:
    """
    id: 1
    name: anonymous
    """

  Scenario: missing @Id on Entity
    Given the class:
    """
    @Entity
    public class Something {

        public int id;

        public String name;
    }
    """
    When the model of {{run}}.Something is generated
    Then an InvalidPersitenceAnnotationException is thrown with a message matching:
    """
    @Id is missing for on class .+\.Something
    """

  Scenario: multiple @Id annotations are not supported
    Given the class:
    """
    @Entity
    public class Something {

        @Id
        @GeneratedValue
        public int id;

        @Id
        public String name;
    }
    """
    When the model of {{run}}.Something is generated
    Then an InvalidPersitenceAnnotationException is thrown with a message matching:
    """
    multiple @Id columns \(composite keys\) are not supported on class .+\.Something
    """

  Scenario: @EmbeddedId annotations are not supported
    Given the class:
    """
    @Entity
    public class Something {

        @EmbeddedId
        public Key key;

    }
    ---
    public class Key {

        @Id
        @GeneratedValue
        public int id;

        @Id
        public String name;
    }
    """
    When the model of {{run}}.Something is generated
    Then an InvalidPersitenceAnnotationException is thrown with a message matching:
    """
    @EmbeddedId \(nested composite keys\) are not supported on class .+\.Something
    """

  Scenario: Relation on a non entity
    Given the class:
    """
    @Entity
    public class Something {

        @Id
        public int id;

        @OneToOne
        public String something;

    }
    """
    When the model of {{run}}.Something is generated
    Then an InvalidPersitenceAnnotationException is thrown with a message matching:
    """
    .+\.Something\.something does not refer to an entity!
    """