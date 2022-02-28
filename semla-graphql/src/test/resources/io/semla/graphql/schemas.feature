Feature: semla graphql generated schemas from entities

  Background:
    * source prepend "package {{run}};"
    * source prepend "import javax.persistence.*;"
    * source prepend "import java.util.*;"

  Scenario: we can generate the schema of an Entity
    * the graphql schema of io.semla.model.User is equal to:
      """graphql
      type Query {
          getUser(id: Int!): User
          firstUser(where: _UserPredicates, orderBy: _UserSorts, startAt: Int): User
          getUsers(ids: [Int!]!): [User!]!
          listUsers(where: _UserPredicates, orderBy: _UserSorts, startAt: Int, limitTo: Int): [User!]!
          countUsers(where: _UserPredicates, orderBy: _UserSorts, startAt: Int, limitTo: Int): Int!
      }

      type Mutation {
          createUser(user: _UserCreate!): User!
          createUsers(users: [_UserCreate!]!): [User!]!
          updateUser(user: _UserUpdate!): User!
          updateUsers(users: [_UserUpdate!]!): [User!]!
          patchUsers(values: _UserPatch!, where: _UserPredicates, orderBy: _UserSorts, startAt: Int, limitTo: Int): Int!
          deleteUser(id: Int!): Boolean
          deleteUsers(ids: [Int!]!): Int!
      }

      type User {
          id: Int!
          created: Int
          name: String!
          additionalNames: [String!]
          isCool: Boolean
          initial: String
          mask: Int
          powers: [Int!]
          age: Int
          percentage: Float
          height: Float
          birthdate: String!
          lastSeen: String
          lastLogin: String
          sqlDate: String
          sqlTime: String
          sqlTimestamp: String
          bigInteger: String
          bigDecimal: String
          calendar: String
          instant: String
          localDateTime: String
          nickname: String
          type: User_Type
          eyecolor: User_EyeColor
          version: Int
      }

      enum User_Type {admin, user}

      enum User_EyeColor {brown, blue, green, black}

      input _UserCreate {
          created: Int
          name: String!
          additionalNames: [String!]
          isCool: Boolean
          initial: String
          mask: Int
          powers: [Int!]
          age: Int
          percentage: Float
          height: Float
          birthdate: String!
          lastSeen: String
          lastLogin: String
          sqlDate: String
          sqlTime: String
          sqlTimestamp: String
          bigInteger: String
          bigDecimal: String
          calendar: String
          instant: String
          localDateTime: String
          nickname: String
          type: User_Type
          eyecolor: User_EyeColor
      }

      input _UserPredicates {
          id: [_IntPredicates!]
          created: [_IntPredicates!]
          name: [_StringPredicates!]
          additionalNames: [_StringPredicates!]
          isCool: [_BooleanPredicates!]
          initial: [_StringPredicates!]
          mask: [_IntPredicates!]
          powers: [_StringPredicates!]
          age: [_IntPredicates!]
          percentage: [_FloatPredicates!]
          height: [_FloatPredicates!]
          birthdate: [_StringPredicates!]
          lastSeen: [_StringPredicates!]
          lastLogin: [_StringPredicates!]
          sqlDate: [_StringPredicates!]
          sqlTime: [_StringPredicates!]
          sqlTimestamp: [_StringPredicates!]
          bigInteger: [_StringPredicates!]
          bigDecimal: [_StringPredicates!]
          calendar: [_StringPredicates!]
          instant: [_StringPredicates!]
          localDateTime: [_StringPredicates!]
          nickname: [_StringPredicates!]
          type: [_StringPredicates!]
          eyecolor: [_StringPredicates!]
          version: [_IntPredicates!]
      }

      input _UserSorts {
          id: _Sort
          created: _Sort
          name: _Sort
          additionalNames: _Sort
          isCool: _Sort
          initial: _Sort
          mask: _Sort
          powers: _Sort
          age: _Sort
          percentage: _Sort
          height: _Sort
          birthdate: _Sort
          lastSeen: _Sort
          lastLogin: _Sort
          sqlDate: _Sort
          sqlTime: _Sort
          sqlTimestamp: _Sort
          bigInteger: _Sort
          bigDecimal: _Sort
          calendar: _Sort
          instant: _Sort
          localDateTime: _Sort
          nickname: _Sort
          type: _Sort
          eyecolor: _Sort
          version: _Sort
      }

      input _UserUpdate {
          id: Int!
          name: String
          additionalNames: [String!]
          isCool: Boolean
          initial: String
          mask: Int
          powers: [Int!]
          age: Int
          percentage: Float
          height: Float
          birthdate: String
          lastSeen: String
          lastLogin: String
          sqlDate: String
          sqlTime: String
          sqlTimestamp: String
          bigInteger: String
          bigDecimal: String
          calendar: String
          instant: String
          localDateTime: String
          nickname: String
          type: User_Type
          eyecolor: User_EyeColor
      }

      input _UserPatch {
          name: String
          additionalNames: [String!]
          isCool: Boolean
          initial: String
          mask: Int
          powers: [Int!]
          age: Int
          percentage: Float
          height: Float
          birthdate: String
          lastSeen: String
          lastLogin: String
          sqlDate: String
          sqlTime: String
          sqlTimestamp: String
          bigInteger: String
          bigDecimal: String
          calendar: String
          instant: String
          localDateTime: String
          nickname: String
          type: User_Type
          eyecolor: User_EyeColor
      }

      input _IntPredicates {
          is: Int
          not: Int
          in: [Int!]
          notIn: [Int!]
          greaterOrEquals: Int
          greaterThan: Int
          lessOrEquals: Int
          lessThan: Int
      }

      input _FloatPredicates {
          is: Float
          not: Float
          in: [Float!]
          notIn: [Float!]
          greaterOrEquals: Float
          greaterThan: Float
          lessOrEquals: Float
          lessThan: Float
      }

      input _StringPredicates {
          is: String
          not: String
          in: [String!]
          notIn: [String!]
          like: String
          notLike: String
          contains: String
          doesNotContain: String
          containedIn: String
          notContainedIn: String
      }

      input _BooleanPredicates {
          is: Boolean
          not: Boolean
      }

      enum _Sort {
          asc
          desc
      }
      """

  Scenario: a simple entity
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
    Then the graphql schema is equal to:
      """graphql
      type Query {
          getSomething(id: Int!): Something
          firstSomething(where: _SomethingPredicates, orderBy: _SomethingSorts, startAt: Int): Something
          getSomethings(ids: [Int!]!): [Something!]!
          listSomethings(where: _SomethingPredicates, orderBy: _SomethingSorts, startAt: Int, limitTo: Int): [Something!]!
          countSomethings(where: _SomethingPredicates, orderBy: _SomethingSorts, startAt: Int, limitTo: Int): Int!
      }

      type Mutation {
          createSomething(something: _SomethingCreate!): Something!
          createSomethings(somethings: [_SomethingCreate!]!): [Something!]!
          updateSomething(something: _SomethingUpdate!): Something!
          updateSomethings(somethings: [_SomethingUpdate!]!): [Something!]!
          patchSomethings(values: _SomethingPatch!, where: _SomethingPredicates, orderBy: _SomethingSorts, startAt: Int, limitTo: Int): Int!
          deleteSomething(id: Int!): Boolean
          deleteSomethings(ids: [Int!]!): Int!
      }

      type Something {
          id: Int!
          name: String
      }

      input _SomethingCreate {
          name: String
      }

      input _SomethingPredicates {
          id: [_IntPredicates!]
          name: [_StringPredicates!]
      }

      input _SomethingSorts {
          id: _Sort
          name: _Sort
      }

      input _SomethingUpdate {
          id: Int!
          name: String
      }

      input _SomethingPatch {
          name: String
      }

      input _IntPredicates {
          is: Int
          not: Int
          in: [Int!]
          notIn: [Int!]
          greaterOrEquals: Int
          greaterThan: Int
          lessOrEquals: Int
          lessThan: Int
      }

      input _FloatPredicates {
          is: Float
          not: Float
          in: [Float!]
          notIn: [Float!]
          greaterOrEquals: Float
          greaterThan: Float
          lessOrEquals: Float
          lessThan: Float
      }

      input _StringPredicates {
          is: String
          not: String
          in: [String!]
          notIn: [String!]
          like: String
          notLike: String
          contains: String
          doesNotContain: String
          containedIn: String
          notContainedIn: String
      }

      input _BooleanPredicates {
          is: Boolean
          not: Boolean
      }

      enum _Sort {
          asc
          desc
      }
      """

  Scenario: bi-directional onetomany relationship
    Given those types:
      """java
      @Entity
      public class Parent {

          @Id
          public int id;

          @OneToMany(mappedBy = "parent")
          public List<Child> children;
      }

      @Entity
      public class Child {

          @Id
          public int id;

          @ManyToOne
          public Parent parent;

      }
      """
    Then the graphql schema is equal to:
      """graphql
      type Query {
          getChild(id: Int!): Child
          firstChild(where: _ChildPredicates, orderBy: _ChildSorts, startAt: Int): Child
          getChildren(ids: [Int!]!): [Child!]!
          listChildren(where: _ChildPredicates, orderBy: _ChildSorts, startAt: Int, limitTo: Int): [Child!]!
          countChildren(where: _ChildPredicates, orderBy: _ChildSorts, startAt: Int, limitTo: Int): Int!
          getParent(id: Int!): Parent
          firstParent(where: _ParentPredicates, orderBy: _ParentSorts, startAt: Int): Parent
          getParents(ids: [Int!]!): [Parent!]!
          listParents(where: _ParentPredicates, orderBy: _ParentSorts, startAt: Int, limitTo: Int): [Parent!]!
          countParents(where: _ParentPredicates, orderBy: _ParentSorts, startAt: Int, limitTo: Int): Int!
      }

      type Mutation {
          createChild(child: _ChildCreate!): Child!
          createChildren(children: [_ChildCreate!]!): [Child!]!
          updateChild(child: _ChildUpdate!): Child!
          updateChildren(children: [_ChildUpdate!]!): [Child!]!
          patchChildren(values: _ChildPatch!, where: _ChildPredicates, orderBy: _ChildSorts, startAt: Int, limitTo: Int): Int!
          deleteChild(id: Int!): Boolean
          deleteChildren(ids: [Int!]!): Int!
          createParent(parent: _ParentCreate!): Parent!
          createParents(parents: [_ParentCreate!]!): [Parent!]!
          deleteParent(id: Int!): Boolean
          deleteParents(ids: [Int!]!): Int!
      }

      type Child {
          id: Int!
          parent: Parent
      }

      input _ChildCreate {
          id: Int!
          parent: Int
      }

      input _ChildPredicates {
          id: [_IntPredicates!]
          parent: [_IntPredicates!]
      }

      input _ChildSorts {
          id: _Sort
          parent: _Sort
      }

      input _ChildUpdate {
          id: Int!
          parent: Int
      }

      input _ChildPatch {
          parent: Int
      }

      type Parent {
          id: Int!
          children: [Child!]
      }

      input _ParentCreate {
          id: Int!
      }

      input _ParentPredicates {
          id: [_IntPredicates!]
      }

      input _ParentSorts {
          id: _Sort
      }

      input _IntPredicates {
          is: Int
          not: Int
          in: [Int!]
          notIn: [Int!]
          greaterOrEquals: Int
          greaterThan: Int
          lessOrEquals: Int
          lessThan: Int
      }

      input _FloatPredicates {
          is: Float
          not: Float
          in: [Float!]
          notIn: [Float!]
          greaterOrEquals: Float
          greaterThan: Float
          lessOrEquals: Float
          lessThan: Float
      }

      input _StringPredicates {
          is: String
          not: String
          in: [String!]
          notIn: [String!]
          like: String
          notLike: String
          contains: String
          doesNotContain: String
          containedIn: String
          notContainedIn: String
      }

      input _BooleanPredicates {
          is: Boolean
          not: Boolean
      }

      enum _Sort {
          asc
          desc
      }
      """

  Scenario: bi-directional onetomany relationship with generated ids and cascadeType PERSIST and MERGE
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

          @ManyToOne(cascade = CascadeType.MERGE)
          public Parent parent;

      }
      """
    Then the graphql schema is equal to:
      """graphql
      type Query {
          getChild(id: Int!): Child
          firstChild(where: _ChildPredicates, orderBy: _ChildSorts, startAt: Int): Child
          getChildren(ids: [Int!]!): [Child!]!
          listChildren(where: _ChildPredicates, orderBy: _ChildSorts, startAt: Int, limitTo: Int): [Child!]!
          countChildren(where: _ChildPredicates, orderBy: _ChildSorts, startAt: Int, limitTo: Int): Int!
          getParent(id: Int!): Parent
          firstParent(where: _ParentPredicates, orderBy: _ParentSorts, startAt: Int): Parent
          getParents(ids: [Int!]!): [Parent!]!
          listParents(where: _ParentPredicates, orderBy: _ParentSorts, startAt: Int, limitTo: Int): [Parent!]!
          countParents(where: _ParentPredicates, orderBy: _ParentSorts, startAt: Int, limitTo: Int): Int!
      }

      type Mutation {
          createChild(child: _ChildCreate!): Child!
          createChildren(children: [_ChildCreate!]!): [Child!]!
          updateChild(child: _ChildUpdate!): Child!
          updateChildren(children: [_ChildUpdate!]!): [Child!]!
          patchChildren(values: _ChildPatch!, where: _ChildPredicates, orderBy: _ChildSorts, startAt: Int, limitTo: Int): Int!
          deleteChild(id: Int!): Boolean
          deleteChildren(ids: [Int!]!): Int!
          createParent(parent: _ParentCreate!): Parent!
          createParents(parents: [_ParentCreate!]!): [Parent!]!
          updateParent(parent: _ParentUpdate!): Parent!
          updateParents(parents: [_ParentUpdate!]!): [Parent!]!
          patchParents(values: _ParentPatch!, where: _ParentPredicates, orderBy: _ParentSorts, startAt: Int, limitTo: Int): Int!
          deleteParent(id: Int!): Boolean
          deleteParents(ids: [Int!]!): Int!
      }

      type Child {
          id: Int!
          name: String
          parent: Parent
      }

      input _ChildCreate {
          name: String
          parent: Int
      }

      input _ChildPredicates {
          id: [_IntPredicates!]
          name: [_StringPredicates!]
          parent: [_IntPredicates!]
      }

      input _ChildSorts {
          id: _Sort
          name: _Sort
          parent: _Sort
      }

      input _ChildUpdate {
          id: Int!
          name: String
          parent: _ParentUpdate
      }

      input _ChildPatch {
          name: String
          parent: _ParentUpdate
      }

      type Parent {
          id: Int!
          name: String
          children: [Child!]
      }

      input _ParentCreate {
          name: String
          children: [_ChildCreate!]
      }

      input _ParentPredicates {
          id: [_IntPredicates!]
          name: [_StringPredicates!]
      }

      input _ParentSorts {
          id: _Sort
          name: _Sort
      }

      input _ParentUpdate {
          id: Int!
          name: String
      }

      input _ParentPatch {
          name: String
      }

      input _IntPredicates {
          is: Int
          not: Int
          in: [Int!]
          notIn: [Int!]
          greaterOrEquals: Int
          greaterThan: Int
          lessOrEquals: Int
          lessThan: Int
      }

      input _FloatPredicates {
          is: Float
          not: Float
          in: [Float!]
          notIn: [Float!]
          greaterOrEquals: Float
          greaterThan: Float
          lessOrEquals: Float
          lessThan: Float
      }

      input _StringPredicates {
          is: String
          not: String
          in: [String!]
          notIn: [String!]
          like: String
          notLike: String
          contains: String
          doesNotContain: String
          containedIn: String
          notContainedIn: String
      }

      input _BooleanPredicates {
          is: Boolean
          not: Boolean
      }

      enum _Sort {
          asc
          desc
      }
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
    Then the graphql schema is equal to:
      """graphql
      type Query {
          getChild(id: Int!): Child
          firstChild(where: _ChildPredicates, orderBy: _ChildSorts, startAt: Int): Child
          getChildren(ids: [Int!]!): [Child!]!
          listChildren(where: _ChildPredicates, orderBy: _ChildSorts, startAt: Int, limitTo: Int): [Child!]!
          countChildren(where: _ChildPredicates, orderBy: _ChildSorts, startAt: Int, limitTo: Int): Int!
          getParent(id: Int!): Parent
          firstParent(where: _ParentPredicates, orderBy: _ParentSorts, startAt: Int): Parent
          getParents(ids: [Int!]!): [Parent!]!
          listParents(where: _ParentPredicates, orderBy: _ParentSorts, startAt: Int, limitTo: Int): [Parent!]!
          countParents(where: _ParentPredicates, orderBy: _ParentSorts, startAt: Int, limitTo: Int): Int!
          getParentChild(id: Int!): ParentChild
          firstParentChild(where: _ParentChildPredicates, orderBy: _ParentChildSorts, startAt: Int): ParentChild
          getParentChildren(ids: [Int!]!): [ParentChild!]!
          listParentChildren(where: _ParentChildPredicates, orderBy: _ParentChildSorts, startAt: Int, limitTo: Int): [ParentChild!]!
          countParentChildren(where: _ParentChildPredicates, orderBy: _ParentChildSorts, startAt: Int, limitTo: Int): Int!
      }

      type Mutation {
          createChild(child: _ChildCreate!): Child!
          createChildren(children: [_ChildCreate!]!): [Child!]!
          deleteChild(id: Int!): Boolean
          deleteChildren(ids: [Int!]!): Int!
          createParent(parent: _ParentCreate!): Parent!
          createParents(parents: [_ParentCreate!]!): [Parent!]!
          deleteParent(id: Int!): Boolean
          deleteParents(ids: [Int!]!): Int!
          createParentChild(parentChild: _ParentChildCreate!): ParentChild!
          createParentChildren(parentChildren: [_ParentChildCreate!]!): [ParentChild!]!
          updateParentChild(parentChild: _ParentChildUpdate!): ParentChild!
          updateParentChildren(parentChildren: [_ParentChildUpdate!]!): [ParentChild!]!
          patchParentChildren(values: _ParentChildPatch!, where: _ParentChildPredicates, orderBy: _ParentChildSorts, startAt: Int, limitTo: Int): Int!
          deleteParentChild(id: Int!): Boolean
          deleteParentChildren(ids: [Int!]!): Int!
      }

      type Child {
          id: Int!
          parents: [Parent!]
      }

      input _ChildCreate {
          id: Int!
      }

      input _ChildPredicates {
          id: [_IntPredicates!]
      }

      input _ChildSorts {
          id: _Sort
      }

      type Parent {
          id: Int!
          children: [Child!]
      }

      input _ParentCreate {
          id: Int!
      }

      input _ParentPredicates {
          id: [_IntPredicates!]
      }

      input _ParentSorts {
          id: _Sort
      }

      type ParentChild {
          id: Int!
          parent: Parent
          child: Child
      }

      input _ParentChildCreate {
          parent: Int
          child: Int
      }

      input _ParentChildPredicates {
          id: [_IntPredicates!]
          parent: [_IntPredicates!]
          child: [_IntPredicates!]
      }

      input _ParentChildSorts {
          id: _Sort
          parent: _Sort
          child: _Sort
      }

      input _ParentChildUpdate {
          id: Int!
          parent: Int
          child: Int
      }

      input _ParentChildPatch {
          parent: Int
          child: Int
      }

      input _IntPredicates {
          is: Int
          not: Int
          in: [Int!]
          notIn: [Int!]
          greaterOrEquals: Int
          greaterThan: Int
          lessOrEquals: Int
          lessThan: Int
      }

      input _FloatPredicates {
          is: Float
          not: Float
          in: [Float!]
          notIn: [Float!]
          greaterOrEquals: Float
          greaterThan: Float
          lessOrEquals: Float
          lessThan: Float
      }

      input _StringPredicates {
          is: String
          not: String
          in: [String!]
          notIn: [String!]
          like: String
          notLike: String
          contains: String
          doesNotContain: String
          containedIn: String
          notContainedIn: String
      }

      input _BooleanPredicates {
          is: Boolean
          not: Boolean
      }

      enum _Sort {
          asc
          desc
      }
      """