Feature: ManyToMany relationships

  Background:
    * source prepend:
    """
    package {{run}};
    import javax.persistence.*;
    import java.util.*;
    import io.semla.serialization.annotations.*;
    """

  Scenario: defaulted uni-directional manytomany lazy relationship
    Given the classes:
    """
    @Entity
    public class Parent {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @ManyToMany
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

    }
    """
    And that we create this parent including its children:
    """
    name: parent1
    children:
      - name: child1
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
    """
    And listing the parents including their children returns:
    """
    - id: 1
      name: parent1
      children:
        - id: 1
          name: child1
    """
    And deleting all the parents including their children returns 1


  Scenario: defaulted bi-directional manytomany lazy relationship
    Given the classes:
    """
    @Entity
    public class Parent {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @ManyToMany
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

        @ManyToMany(mappedBy="children")
        @Serialize(When.NOT_NULL)
        public List<Parent> parents;

    }
    """
    And that we create this parent including its children:
    """
    name: parent1
    children:
      - name: child1
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
    """
    And fetching the first parent where id is 1 including its children{parents} returns:
    """
    id: 1
    name: parent1
    children:
      - id: 1
        name: child1
        parents:
          - 1
    """
    And listing the parents including their children{parents} returns:
    """
    - id: 1
      name: parent1
      children:
        - id: 1
          name: child1
          parents:
            - 1
    """

  Scenario: bi-directional manytomany lazy relationship with explicit JoinTable
    Given the classes:
    """
    @Entity
    public class Parent {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @ManyToMany
        @JoinTable(
            name = "r_parent_child",
            joinColumns = @JoinColumn(name = "parent_id"),
            inverseJoinColumns = @JoinColumn(name = "child_id")
        )
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

        @ManyToMany(mappedBy="children")
        @Serialize(When.NOT_NULL)
        public List<Parent> parents;

    }
    """
    And that we create this parent including its children:
    """
    name: parent1
    children:
      - name: child1
    """
    Then fetching the first parent where id is 1 returns:
    """
    id: 1
    name: parent1
    """
    But fetching the first parent where id is 1 including its children returns:
    """
    id: 1
    name: parent1
    children:
      - id: 1
        name: child1
    """
    And fetching the first parent where id is 1 including its children{parents} returns:
    """
    id: 1
    name: parent1
    children:
      - id: 1
        name: child1
        parents:
          - 1
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

        @ManyToMany
        public List<Child> children;

    }
    ---
    @Entity
    public class Child {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @ManyToMany(mappedBy="children")
        public List<Parent> parents;

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

        @ManyToMany(cascade = CascadeType.PERSIST)
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

        @ManyToMany(mappedBy="children", cascade = CascadeType.PERSIST)
        @Serialize(When.NOT_NULL)
        public List<Parent> parents;

    }
    """
    And that we create this parent:
    """
    name: parent1
    children:
      - name: child1
      - name: child2
    """
    Then fetching the first parent including its children{parents} returns:
    """
    id: 1
    name: parent1
    children:
      - id: 1
        name: child1
        parents:
          - 1
      - id: 2
        name: child2
        parents:
          - 1
    """
    And if we update those parents:
    """
    - id: 1
      name: parent1
      children:
        - id: 1
          name: child1
          parents:
            - 1
        - id: 2
          name: child2
          parents:
            - 1
        - name: child3
    """
    Then listing the children including their parents returns:
    """
    - id: 1
      name: child1
      parents:
        - id: 1
          name: parent1
    - id: 2
      name: child2
      parents:
        - 1
    - id: 3
      name: child3
      parents:
        - 1
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

        @ManyToMany(cascade = CascadeType.MERGE)
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

        @ManyToMany(mappedBy="children", cascade = CascadeType.MERGE)
        @Serialize(When.NOT_NULL)
        public List<Parent> parents;

    }
    """
    And that we create this parent including its children:
    """
    name: parent1
    children:
      - name: child1
    """
    When we update this parent:
    """
    id: 1
    name: parent1
    children:
      - id: 1
        name: newNameForChild1
    """
    Then fetching the first child returns:
    """
    id: 1
    name: newNameForChild1
    """
    And if we update those children:
    """
    - id: 1
      name: newNameForChild1
      parents:
        - id: 1
          name: newNameForParent1
    """
    Then listing the parents including their children returns:
    """
    - id: 1
      name: newNameForParent1
      children:
        - id: 1
          name: newNameForChild1
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

        @ManyToMany(cascade = CascadeType.REMOVE)
        public List<Child> children;

    }
    ---
    @Entity
    public class Child {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @ManyToMany(mappedBy="children")
        public List<Parent> parents;

    }
    """
    And that we create this parent including its children:
    """
    name: parent1
    children:
      - name: child1
    """
    When we delete the parent where id is 1
    Then counting the children returns 0

  Scenario: the "mappedBy" parameter is mandatory on the reverse @ManyToMany annotation
    Given the classes:
    """
    @Entity
    public class Parent {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @ManyToMany
        public List<Child> children;

    }
    ---
    @Entity
    public class Child {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @ManyToMany
        public List<Parent> parents;

    }
    """
    When the model of {{run}}.Parent is generated
    Then an InvalidPersitenceAnnotationException is thrown with a message matching:
    """
    @ManyToMany\.mappedBy on .+Parent\.children or .+Child\.parents needs to be set
    """
    When the model of {{run}}.Child is generated
    Then an InvalidPersitenceAnnotationException is thrown with a message matching:
    """
    @ManyToMany\.mappedBy on .+Child\.parents or .+Parent\.children needs to be set
    """

  Scenario: the "mappedBy" parameter on the reverse @ManyToMany annotation must point to a valid property
    Given the classes:
    """
    @Entity
    public class Parent {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @ManyToMany
        public List<Child> children;

    }
    ---
    @Entity
    public class Child {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @ManyToMany(mappedBy = "kids")
        public List<Parent> parents;

    }
    """
    When the model of {{run}}.Parent is generated
    Then an InvalidPersitenceAnnotationException is thrown with a message matching:
    """
    @ManyToMany\.mappedBy on .+Child\.parents defines a non existent member 'kids' on class .+\.Parent
    """
    When the model of {{run}}.Child is generated
    Then an InvalidPersitenceAnnotationException is thrown with a message matching:
    """
    @ManyToMany\.mappedBy on .+Child\.parents defines a non existent member 'kids' on class .+\.Parent
    """

  Scenario: the "mappedBy" parameter on the reverse @ManyToMany annotation must point to a field annotated with @ManyToMany
    Given the classes:
    """
    @Entity
    public class Parent {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @ManyToMany
        public List<Child> children;

    }
    ---
    @Entity
    public class Child {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @ManyToMany(mappedBy = "name")
        public List<Parent> parents;

    }
    """
    When the model of {{run}}.Child is generated
    Then an InvalidPersitenceAnnotationException is thrown with a message matching:
    """
    @ManyToMany\.mappedBy on .+Child\.parents defines a member 'name' on class .+\.Parent that is not annotated with @ManyToMany
    """

  Scenario: the "mappedBy" parameter cannot be set on both sides of the @ManyToMany
    Given the classes:
    """
    @Entity
    public class Parent {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @ManyToMany(mappedBy = "parents")
        public List<Child> children;

    }
    ---
    @Entity
    public class Child {

        @Id
        @GeneratedValue
        public int id;

        public String name;

        @ManyToMany(mappedBy = "children")
        public List<Parent> parents;

    }
    """
    When the model of {{run}}.Child is generated
    Then an InvalidPersitenceAnnotationException is thrown with a message matching:
    """
    @ManyToMany\.mappedBy on .+Child\.parents defines a member 'children' on class .+\.Parent that also defines a member 'parents'. Only one of the two class can own this relationship!
    """