Feature: OneToMany and ManyToOne relationships

  Background:
    * source prepend:
    """
    package {{run}};
    import javax.persistence.*;
    import java.util.*;
    import io.semla.serialization.annotations.*;
    """

  Scenario: uni-directional lazy onetomany relationship
    Given the classes:
    """
    @Entity
    public class Parent {

        @Id
        @GeneratedValue
        public int id;

        public String name;
    }
    ---
    @Entity
    public class Child {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @ManyToOne(fetch = FetchType.LAZY)
        public Parent parent;

    }
    """
    And that we create those:
    """
    parent:
      name: parent1
    children:
      - name: child1
        parent: 1
      - name: child2
        parent: 1
    """
    Then fetching the first parent where id is 1 returns:
    """
    id: 1
    name: parent1
    """
    But listing the children returns:
    """
    - id: 1
      name: child1
      parent: 1
    - id: 2
      name: child2
      parent: 1
    """

  Scenario: bi-directional lazy onetomany relationship
    Given the classes:
    """
    @Entity
    public class Parent {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @OneToMany(mappedBy = "parent")
        @Serialize(When.NOT_NULL)
        public List<Child> children;
    }
    ---
    @Entity
    public class Child {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @ManyToOne(fetch = FetchType.LAZY)
        public Parent parent;

    }
    """
    And that we create this parent including its children:
    """
    name: parent1
    children:
      - name: child1
      - name: child2
    """
    Then fetching the first parent where id is 1 returns:
    """
    id: 1
    name: parent1
    """
    And fetching the first parent where id is 1 including its children returns:
    """
    id: 1
    name: parent1
    children:
      - id: 1
        name: child1
        parent: 1
      - id: 2
        name: child2
        parent: 1
    """

  Scenario: uni-directional eager onetomany relationship
    Given the classes:
    """
    @Entity
    public class Parent {

        @Id
        @GeneratedValue
        public int id;

        public String name;
    }
    ---
    @Entity
    public class Child {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @ManyToOne(fetch = FetchType.EAGER)
        public Parent parent;

    }
    """
    And that we create those:
    """
    parent:
      name: parent1
    children:
      - name: child1
        parent: 1
      - name: child2
        parent: 1
    """
    Then fetching the first parent where id is 1 returns:
    """
    id: 1
    name: parent1
    """
    But listing the children returns:
    """
    - id: 1
      name: child1
      parent:
        id: 1
        name: parent1
    - id: 2
      name: child2
      parent: 1
    """

  Scenario: a bi-directional eager onetomany relationship is properly handled by the relations and the serializers
    Given the classes:
    """
    @Entity
    public class Parent {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @OneToMany(mappedBy = "parent", fetch = FetchType.EAGER)
        public List<Child> children;
    }
    ---
    @Entity
    public class Child {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @ManyToOne(fetch = FetchType.EAGER)
        public Parent parent;

    }
    """
    And that we create this parent including its children:
    """
    name: parent1
    children:
      - name: child1
      - name: child2
    """
    Then fetching the first parent where id is 1 returns:
    """
    id: 1
    name: parent1
    children:
      - id: 1
        name: child1
        parent: 1
      - id: 2
        name: child2
        parent: 1
    """
    But listing the children returns:
    """
    - id: 1
      name: child1
      parent:
        id: 1
        name: parent1
        children:
          - 1
          - id: 2
            name: child2
            parent: 1
    - 2
    """

  Scenario: no children returns an empty list
    Given the classes:
    """
    @Entity
    public class Parent {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @OneToMany(mappedBy = "parent")
        public List<Child> children;
    }
    ---
    @Entity
    public class Child {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @ManyToOne(fetch = FetchType.LAZY)
        public Parent parent;

    }
    """
    And that we create this parent:
    """
    name: parent1
    """
    Then fetching the first parent where id is 1 including its children returns:
    """
    id: 1
    name: parent1
    children: []
    """
    And listing the parents including their children returns:
    """
    - id: 1
      name: parent1
      children: []
    """

  Scenario: CascadeType.PERSIST operation
    Given the classes:
    """
    @Entity
    public class Parent {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @OneToMany(mappedBy = "parent", cascade = CascadeType.PERSIST)
        public List<Child> children;

    }
    ---
    @Entity
    public class Child {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @ManyToOne(fetch = FetchType.LAZY)
        public Parent parent;

    }
    """
    And that we create this parent:
    """
    name: parent1
    children:
      - name: child1
      - name: child2
    """
    Then fetching the first parent including its children returns:
    """
    id: 1
    name: parent1
    children:
      - id: 1
        name: child1
        parent: 1
      - id: 2
        name: child2
        parent: 1
    """
    And if we update this parent:
    """
    id: 1
    name: parent1
    children:
      - id: 1
        name: child1
        parent: 1
      - id: 2
        name: child2
        parent: 1
      - name: child3
    """
    Then listing the children returns:
    """
    - id: 1
      name: child1
      parent: 1
    - id: 2
      name: child2
      parent: 1
    - id: 3
      name: child3
      parent: 1
    """


  Scenario: CascadeType.MERGE operation
    Given the classes:
    """
    @Entity
    public class Parent {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @OneToMany(mappedBy = "parent", cascade = CascadeType.MERGE)
        public List<Child> children;
    }
    ---
    @Entity
    public class Child {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @ManyToOne(fetch = FetchType.LAZY)
        public Parent parent;

    }
    """
    And that we create this parent including its children:
    """
    name: parent1
    children:
      - name: child1
      - name: child2
    """
    When updating this parent:
    """
    id: 1
    name: parent1
    children:
      - id: 1
        name: child1
        parent: 1
      - id: 2
        name: anotherNameForChild2
        parent: 1
    """
    Then listing the children returns:
    """
    - id: 1
      name: child1
      parent: 1
    - id: 2
      name: anotherNameForChild2
      parent: 1
    """

  Scenario: CascadeType.REMOVE operation
    Given the classes:
    """
    @Entity
    public class Parent {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @OneToMany(mappedBy = "parent", cascade = CascadeType.REMOVE)
        public List<Child> children;
    }
    ---
    @Entity
    public class Child {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @ManyToOne(fetch = FetchType.LAZY)
        public Parent parent;

    }
    """
    And that we create this parent including its children:
    """
    name: parent1
    children:
      - name: child1
      - name: child2
    """
    When we delete the child where id is 1
    Then fetching the first parent including its children returns:
    """
    id: 1
    name: parent1
    children:
      - id: 2
        name: child2
        parent: 1
    """
    When we delete the parent where id is 1
    Then counting the children returns 0

  Scenario: CascadeType.REMOVE, orphanRemoval=true operation
    Given the classes:
    """
    @Entity
    public class Parent {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @OneToMany(mappedBy = "parent", cascade = CascadeType.REMOVE, orphanRemoval = true)
        public List<Child> children;
    }
    ---
    @Entity
    public class Child {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @ManyToOne(fetch = FetchType.LAZY)
        public Parent parent;

    }
    """
    And that we create those:
    """
    parent:
      name: parent1
    children:
      - name: child1
        parent: 1
      - name: child2
        parent: 1
      - name: child3
    """
    When we delete the parent where id is 1
    Then listing the children returns:
    """
    []
    """

  Scenario: the "mappedBy" parameter is mandatory on the @OneToMany annotation
    Given the classes:
    """
    @Entity
    public class Parent {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @OneToMany
        public List<Child> children;
    }
    ---
    @Entity
    public class Child {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @ManyToOne(fetch = FetchType.LAZY)
        public Parent parent;

    }
    """
    When the model of {{run}}.Parent is generated
    Then an InvalidPersitenceAnnotationException is thrown with a message matching:
    """
    @OneToMany\.mappedBy on .+Parent\.children needs to be set
    """

  Scenario: the "mappedBy" parameter on the @OneToMany annotation must point to a valid property
    Given the classes:
    """
    @Entity
    public class Parent {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @OneToMany(mappedBy = "daddy")
        public List<Child> children;
    }
    ---
    @Entity
    public class Child {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @ManyToOne(fetch = FetchType.LAZY)
        public Parent parent;

    }
    """
    When the model of {{run}}.Parent is generated
    Then an InvalidPersitenceAnnotationException is thrown with a message matching:
    """
    @OneToMany\.mappedBy on .+Parent\.children defines a non existent member 'daddy' on class .+\.Child
    """