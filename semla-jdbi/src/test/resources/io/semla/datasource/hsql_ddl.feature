Feature: HSQL DDL

  Background:
    Given that io.semla.datasource.HsqlDatasource is the default datasource

  Scenario: the HSqlDatasource can create a schema from an entity
    Then the schema of io.semla.model.User is equal to:
      """SQL
      CREATE TABLE "people" (
        "id" INT GENERATED BY DEFAULT AS IDENTITY (START WITH 1, INCREMENT BY 1) NOT NULL,
        "created" BIGINT,
        "name" VARCHAR(255) NOT NULL,
        "additional_names" CLOB,
        "is_cool" BOOLEAN,
        "initial" CHAR,
        "mask" TINYINT,
        "powers" VARCHAR(255),
        "age" SMALLINT,
        "rating" FLOAT,
        "height" DOUBLE,
        "birthdate" DATE NOT NULL,
        "last_seen" TIME,
        "last_login" TIMESTAMP(3),
        "sql_date" DATE,
        "sql_time" TIME,
        "sql_timestamp" TIMESTAMP(3),
        "big_integer" BIGINT,
        "big_decimal" DECIMAL(65,30),
        "calendar" TIMESTAMP(3),
        "instant" TIMESTAMP(3),
        "local_date_time" TIMESTAMP(3),
        "nickname" VARCHAR(255),
        "type" INT,
        "eyecolor" VARCHAR(255),
        "version" INT,
        PRIMARY KEY ("id")
      );
      CREATE UNIQUE INDEX "people_name_idx" ON "people" ("name");
      """

  Scenario: uuid based primary key
    Then the schema of io.semla.model.IndexedUser is equal to:
      """SQL
      CREATE TABLE "indexed_user" (
        "uuid" VARCHAR(36) NOT NULL,
        "name" VARCHAR(255),
        "age" INT,
        "group" INT,
        PRIMARY KEY ("uuid")
      );
      CREATE INDEX "indexed_user_name_idx" ON "indexed_user" ("name");
      CREATE INDEX "indexed_user_group_idx" ON "indexed_user" ("group");
      """

  Scenario: multiple indexes
    * the schema of io.semla.model.MultipleIndices is equal to:
      """SQL
      CREATE TABLE "multiple_indices" (
        "id" INT GENERATED BY DEFAULT AS IDENTITY (START WITH 1, INCREMENT BY 1) NOT NULL,
        "name" VARCHAR(255),
        "real_value" VARCHAR(255),
        PRIMARY KEY ("id")
      );
      CREATE UNIQUE INDEX "multiple_indices_name_idx" ON "multiple_indices" ("name");
      CREATE INDEX "multiple_indices_name_real_value_idx" ON "multiple_indices" ("name", "real_value");
      """

  Scenario: default bi-directional manytomany relationship
    * source prepend:
      """java
      package io.semla.model.ddl1;
      import javax.persistence.*;
      import java.util.*;
      """
    Given those types:
      """java
      @Entity
      public class Parent {

          @Id
          @GeneratedValue
          public int id;

          public String name;

          @ManyToMany
          @JoinTable
          public List<Child> children;

      }

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
    Then the schema of io.semla.model.ddl1.ParentChild is equal to:
      """SQL
      CREATE TABLE "parent_child" (
        "id" INT GENERATED BY DEFAULT AS IDENTITY (START WITH 1, INCREMENT BY 1) NOT NULL,
        "parent" INT,
        "child" INT,
        PRIMARY KEY ("id")
      );
      CREATE INDEX "parent_child_parent_idx" ON "parent_child" ("parent");
      CREATE INDEX "parent_child_child_idx" ON "parent_child" ("child");
      """

  Scenario: explicit joinTable on a bi-directional manytomany lazy relationship
    * source prepend:
      """java
      package io.semla.model.ddl2;
      import javax.persistence.*;
      import java.util.*;
      """
    Given those types:
      """java
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
          public List<Child> children;

      }

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
    Then the schema of io.semla.model.ddl2.ParentChild is equal to:
      """SQL
      CREATE TABLE "r_parent_child" (
        "id" INT GENERATED BY DEFAULT AS IDENTITY (START WITH 1, INCREMENT BY 1) NOT NULL,
        "parent_id" INT,
        "child_id" INT,
        PRIMARY KEY ("id")
      );
      CREATE INDEX "r_parent_child_parent_idx" ON "r_parent_child" ("parent_id");
      CREATE INDEX "r_parent_child_child_idx" ON "r_parent_child" ("child_id");
      """