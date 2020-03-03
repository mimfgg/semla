Feature: OneToOne relationships

  Background:
    * source prepend:
    """
    package {{run}};
    import javax.persistence.*;
    import java.util.*;
    import io.semla.serialization.annotations.*;
    """

  Scenario: uni-directional lazy onetoone relationship
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

        @OneToOne(fetch = FetchType.LAZY)
        public Parent parent;

    }
    """
    And that we create those:
    """
    parent:
      name: parent1
    child:
      name: child1
      parent: 1
    """
    Then fetching the first child where id is 1 returns:
    """
    id: 1
    name: child1
    parent: 1
    """
    And fetching the first child where id is 1 including its parent returns:
    """
    id: 1
    name: child1
    parent:
      id: 1
      name: parent1
    """
    And listing the children including their parent returns:
    """
    - id: 1
      name: child1
      parent:
        id: 1
        name: parent1
    """

  Scenario: bi-directional lazy onetoone relationship
    Given the classes:
    """
    @Entity
    public class Parent {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @OneToOne(fetch = FetchType.LAZY, mappedBy = "parent")
        public Child child;

    }
    ---
    @Entity
    public class Child {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @OneToOne(fetch = FetchType.LAZY)
        public Parent parent;

    }
    """
    And that we create this parent including its child:
    """
    name: parent1
    child:
      name: child1
    """
    Then fetching the first child where id is 1 returns:
    """
    id: 1
    name: child1
    parent: 1
    """
    And fetching the first child where id is 1 including its parent{child} returns:
    """
    id: 1
    name: child1
    parent:
      id: 1
      name: parent1
      child: 1
    """
    And listing the parents including their child returns:
    """
    - id: 1
      name: parent1
      child:
        id: 1
        name: child1
        parent: 1
    """
    When we delete the parent where id is 1
    Then fetching the first child returns:
    """
    id: 1
    name: child1
    parent: null
    """

  Scenario: uni-directional eager onetoone relationship
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

        @OneToOne
        public Parent parent;

    }
    """
    And that we create those:
    """
    parent:
      name: parent1
    child:
      name: child1
      parent: 1
    """
    Then fetching the first child where id is 1 returns:
    """
    id: 1
    name: child1
    parent:
      id: 1
      name: parent1
    """
    And listing the children returns:
    """
    - id: 1
      name: child1
      parent:
        id: 1
        name: parent1
    """

  Scenario: bi-directional eager onetoone relationship
    Given the classes:
    """
    @Entity
    public class Parent {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @OneToOne(mappedBy = "parent")
        public Child child;

    }
    ---
    @Entity
    public class Child {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @OneToOne
        public Parent parent;

    }
    """
    And that we create this parent including its child:
    """
    name: parent1
    child:
      name: child1
    """
    Then fetching the first child where id is 1 returns:
    """
    id: 1
    name: child1
    parent:
      id: 1
      name: parent1
      child: 1
    """
    And listing the parents returns:
    """
    - id: 1
      name: parent1
      child:
        id: 1
        name: child1
        parent: 1
    """
    And deleting all the parents including their child returns 1

  Scenario: bi-directional onetoone relationship with join table
    Given the classes:
    """
    @Entity
    public class Parent {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @OneToOne(fetch = FetchType.LAZY)
        @JoinTable(
            name = "r_parent_child",
            joinColumns = @JoinColumn(name = "parent_id"),
            inverseJoinColumns = @JoinColumn(name = "child_id")
        )
        public Child child;

    }
    ---
    @Entity
    public class Child {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @OneToOne(mappedBy="child", fetch = FetchType.LAZY)
        public Parent parent;

    }
    """
    And that we create this parent including its child:
    """
    name: parent1
    child:
      name: child1
    """
    Then fetching the first child where id is 1 including its parent returns:
    """
    id: 1
    name: child1
    parent:
      id: 1
      name: parent1
      child: null
    """
    And fetching the first parent where id is 1 including its child returns:
    """
    id: 1
    name: parent1
    child:
      id: 1
      name: child1
      parent: null
    """
    And fetching the first parent where id is 1 including its child{parent} returns:
    """
    id: 1
    name: parent1
    child:
      id: 1
      name: child1
      parent: 1
    """
    And listing the parents including their child returns:
    """
    - id: 1
      name: parent1
      child:
        id: 1
        name: child1
        parent: null
    """
    When we delete the parent where id is 1
    Then fetching the first child including its parent returns:
    """
    id: 1
    name: child1
    parent: null
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

        @OneToOne(fetch = FetchType.LAZY, mappedBy = "parent", cascade = CascadeType.PERSIST)
        public Child child;

    }
    ---
    @Entity
    public class Child {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
        public Parent parent;

    }
    """
    And that we create this parent:
    """
    name: parent1
    child:
      name: child1
    """
    Then fetching the first parent including its child returns:
    """
    id: 1
    name: parent1
    child:
      id: 1
      name: child1
      parent: 1
    """
    And when we create those parents:
    """
    - name: parent2
      child:
        name: child2
    - name: parent3
      child:
        name: child3
    """
    Then listing the parents including their child returns:
    """
    - id: 1
      name: parent1
      child:
        id: 1
        name: child1
        parent: 1
    - id: 2
      name: parent2
      child:
        id: 2
        name: child2
        parent: 2
    - id: 3
      name: parent3
      child:
        id: 3
        name: child3
        parent: 3
    """
    But if we update this parent:
    """
    id: 1
    name: parent1
    child:
      id: 1
      name: newNameForChild1
    """
    Then fetching the first parent where id is 1 including its child returns:
    """
    id: 1
    name: parent1
    child:
      id: 1
      name: child1
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

        @OneToOne(fetch = FetchType.LAZY, mappedBy = "parent", cascade = CascadeType.MERGE)
        public Child child;

    }
    ---
    @Entity
    public class Child {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
        public Parent parent;

    }
    """
    And that we create this parent including its child:
    """
    name: parent1
    child:
      name: child1
    """
    When updating this parent:
    """
    id: 1
    name: parent1
    child:
      id: 1
      name: anotherNameForChild1
      parent: 1
    """
    Then fetching the first child returns:
    """
    id: 1
    name: anotherNameForChild1
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

        @OneToOne(fetch = FetchType.LAZY, mappedBy = "parent", cascade = CascadeType.REMOVE)
        public Child child;

    }
    ---
    @Entity
    public class Child {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @OneToOne(fetch = FetchType.LAZY)
        public Parent parent;

    }
    """
    And that we create this parent including its child:
    """
    name: parent1
    child:
      name: child1
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

        @OneToOne(fetch = FetchType.LAZY, mappedBy = "parent", cascade = CascadeType.REMOVE, orphanRemoval = true)
        public Child child;

    }
    ---
    @Entity
    public class Child {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @OneToOne(fetch = FetchType.LAZY)
        public Parent parent;

    }
    """
    And that we create this parent including its child:
    """
    name: parent1
    child:
      name: child1
    """
    And that we create this child:
    """
    name: child2
    """
    When we delete the parent where id is 1
    Then listing the children returns:
    """
    []
    """

  Scenario: CascadeType.REMOVE operation with JoinTable
    Given the classes:
    """
    @Entity
    public class Parent {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
        @JoinTable(
            name = "r_parent_child",
            joinColumns = @JoinColumn(name = "parent_id"),
            inverseJoinColumns = @JoinColumn(name = "child_id")
        )
        public Child child;

    }
    ---
    @Entity
    public class Child {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @OneToOne(mappedBy="child", fetch = FetchType.LAZY)
        public Parent parent;

    }
    """
    And that we create this parent including its child:
    """
    name: parent1
    child:
      name: child1
    """
    When we delete the parent where id is 1
    Then counting the children returns 0

  Scenario: the "mappedBy" parameter is mandatory on the reverse @OneToOne annotation with JoinTable
    Given the classes:
    """
    @Entity
    public class Parent {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @OneToOne
        @JoinTable(
            name = "r_parent_child",
            joinColumns = @JoinColumn(name = "parent_id"),
            inverseJoinColumns = @JoinColumn(name = "child_id")
        )
        public Child child;

    }
    ---
    @Entity
    public class Child {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @OneToOne
        public Parent parent;

    }
    """
    When the model of {{run}}.Parent is generated
    Then an InvalidPersitenceAnnotationException is thrown with a message matching:
    """
    @OneToOne\.mappedBy on .+Parent\.child or .+Child\.parent needs to be set
    """
    When the model of {{run}}.Child is generated
    Then an InvalidPersitenceAnnotationException is thrown with a message matching:
    """
    @OneToOne\.mappedBy on .+Child\.parent or .+Parent\.child needs to be set
    """

  Scenario: the "mappedBy" parameter on the reverse @OneToOne annotation must point to a valid property
    Given the classes:
    """
    @Entity
    public class Parent {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @OneToOne
        public Child child;

    }
    ---
    @Entity
    public class Child {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @OneToOne(mappedBy = "kid")
        public Parent parent;

    }
    """
    When the model of {{run}}.Child is generated
    Then an InvalidPersitenceAnnotationException is thrown with a message matching:
    """
    @OneToOne\.mappedBy on .+Child\.parent defines a non existent member 'kid' on class .+\.Parent
    """

  Scenario: the "mappedBy" parameter on the reverse @OneToOne annotation must point to a field annotated with @OneToOne
    Given the classes:
    """
    @Entity
    public class Parent {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @OneToOne
        public Child child;

    }
    ---
    @Entity
    public class Child {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @OneToOne(mappedBy = "name")
        public Parent parent;

    }
    """
    When the model of {{run}}.Child is generated
    Then an InvalidPersitenceAnnotationException is thrown with a message matching:
    """
    @OneToOne\.mappedBy on .+Child\.parent defines a member 'name' on class .+\.Parent that is not annotated with @OneToOne
    """

  Scenario: the "mappedBy" parameter cannot be set on both sides of the @OneToOne
    Given the classes:
    """
    @Entity
    public class Parent {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @OneToOne(mappedBy = "parent")
        public Child child;

    }
    ---
    @Entity
    public class Child {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @OneToOne(mappedBy = "child")
        public Parent parent;

    }
    """
    When the model of {{run}}.Child is generated
    Then an InvalidPersitenceAnnotationException is thrown with a message matching:
    """
    @OneToOne\.mappedBy on .+Child\.parent defines a member 'child' on class .+\.Parent that also defines a member 'parent'. Only one of the two class can own this relationship!
    """

  Scenario: the "mappedBy" parameter on the @OneToOne annotation must point to a valid property
    Given the classes:
    """
    @Entity
    public class Parent {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @OneToOne(mappedBy = "daddy")
        public Child child;

    }
    ---
    @Entity
    public class Child {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @OneToOne
        public Parent parent;

    }
    """
    When the model of {{run}}.Parent is generated
    Then an InvalidPersitenceAnnotationException is thrown with a message matching:
    """
    @OneToOne\.mappedBy on .+Parent\.child defines a non existent member 'daddy' on class .+\.Child
    """
