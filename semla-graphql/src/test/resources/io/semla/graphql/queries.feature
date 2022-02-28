Feature: a graphql translation layer for semla

  Background:
    * source prepend "package {{run}};"
    * source prepend "import javax.persistence.*;"
    * source prepend "import java.util.*;"

  Scenario: we can get an entity by id
    Given this type:
      """java
      @Entity
      public class Something {

        @Id
        @GeneratedValue
        public int id;

        public String name;
      }
      """
    And that we query graphql with:
      """graphql
      mutation {
        createSomething(something: {
          name: "test"
        }) {
          id name
        }
      }
      """

    When we query graphql with:
      """graphql
      query {
        getSomething(id: 1) {
          id name
        }
      }
      """
    Then we receive:
      """yaml
      getSomething:
        id: 1
        name: test
      """

    When we query graphql with:
      """graphql
      mutation {
        updateSomething(something: {
          id: 1
          name: "bob"
        }) {
          id name
        }
      }
      """
    Then we receive:
      """yaml
      updateSomething:
        id: 1
        name: bob
      """

    When we query graphql with:
      """graphql
      mutation {
        updateSomethings(somethings: [{
          id: 1
          name: "max"
        }]) {
          id name
        }
      }
      """
    Then we receive:
      """yaml
      updateSomethings:
        - id: 1
          name: max
      """

    When we query graphql with:
      """graphql
      query {
        firstSomething(where: {name: {is: "max"}}){
          id name
        }
      }
      """
    Then we receive:
      """yaml
      firstSomething:
        id: 1
        name: max
      """

    When we query graphql with:
      """graphql
      mutation {
        deleteSomething(id: 1)
      }
      """
    Then we receive:
      """yaml
      deleteSomething: true
      """

  Scenario: bi-directional onetomany relationship
    Given those types:
      """java
      @Entity
      public class Parent {

          @Id
          @GeneratedValue
          public int id;

          public String name;

          @OneToMany(mappedBy = "parent", cascade = CascadeType.PERSIST)
          public List<Child> children;
      }

      @Entity
      public class Child {

          @Id
          @GeneratedValue
          public int id;

          public String name;

          @ManyToOne
          public Parent parent;

      }
      """
    And that we query graphql with:
      """graphql
      mutation {
        createParent(parent: {
          name: "parent1",
          children: [{
            name: "child1"
          },{
            name: "child2"
          }]
        }){
          id
        }
      }
      """

    When we query graphql with:
      """graphql
      query {
        getParent(id: 1) {
          id
          children {
            id
            parent {
              id
            }
          }
        }
      }
      """
    Then we receive:
      """yaml
      getParent:
        id: 1
        children:
          - id: 1
            parent:
              id: 1
          - id: 2
            parent:
              id: 1
      """

    When we query graphql with:
      """graphql
      query {
        listChildren(where: {parent: {is: 1}}, orderBy: {id: desc}) {
          id
        }
      }
      """
    Then we receive:
      """yaml
      listChildren:
        - id: 2
        - id: 1
      """

    When we query graphql with:
      """graphql
      query {
        listChildren(where: {parent: {is: 1}}, orderBy: {id: desc}, startAt: 1, limitTo: 1) {
          id
        }
      }
      """
    Then we receive:
      """yaml
      listChildren:
        - id: 1
      """

    When we query graphql with:
      """graphql
      query {
        getChildren(ids: [1, 2]) {
          id
          parent {
            id
          }
        }
      }
      """
    Then we receive:
      """yaml
      getChildren:
        - id: 1
          parent:
            id: 1
        - id: 2
          parent:
            id: 1
      """

    When we query graphql with:
      """graphql
      mutation {
        createParent(parent: {name: "parent2"} ) {
          id
        }
        updateChildren(children: [{
          id: 1
          parent: 2
        }]) {
          id
        }
      }
      """
    And that we query graphql with:
      """graphql
      query {
        listChildren(where: {parent: {is: 2}}) {
          id
        }
      }
      """
    Then we receive:
      """yaml
      listChildren:
        - id: 1
      """

    When we query graphql with:
      """graphql
      mutation {
        patchChildren(values: {parent: 2}, where: {parent: {is: 1}})
      }
      """
    And that we query graphql with:
      """graphql
      query {
        listChildren(where: {parent: {is: 2}}) {
          id
        }
      }
      """
    Then we receive:
      """yaml
      listChildren:
        - id: 1
        - id: 2
      """

    And we query graphql with:
      """graphql
      query {
        countChildren(where: {parent: {is: 2}})
      }
      """
    Then we receive:
      """yaml
      countChildren: 2
      """

    When we query graphql with:
      """graphql
      mutation {
        deleteChildren(ids: [1, 2])
      }
      """
    Then we receive:
      """yaml
      deleteChildren: 2
      """

  Scenario: bi-directional manytomany lazy relationship
    Given those types:
      """java
      @Entity
      public class Parent {

          @Id
          public int id;

          @ManyToMany
          public List<Child> children;

      }

      @Entity
      public class Child {

          @Id
          public int id;

          @ManyToMany(mappedBy="children")
          public List<Parent> parents;

      }
      """
    And that we query graphql with:
      """graphql
      mutation {
        createParent(parent: {id: 1} ) {
          id
        }

        createChild(child: {id: 1}) {
          id
        }

        createParentChild(parentChild: {parent: 1, child: 1}) {
          id
        }
      }
      """
    When we query graphql with:
      """graphql
      query {
        getParent(id: 1) {
          id
          children {
            id
            parents {
              id
            }
          }
        }
      }
      """
    Then we receive:
      """yaml
      getParent:
        id: 1
        children:
          - id: 1
            parents:
              - id: 1
      """