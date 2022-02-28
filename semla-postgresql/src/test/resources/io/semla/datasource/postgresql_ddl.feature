Feature: POSTGRESQL DDL

  Background:
    Given that io.semla.datasource.PostgresqlDatasource is the default datasource

  Scenario: the MysqlDatasource can create a schema from an entity
    Then the schema of io.semla.model.User is equal to:
      """SQL
      CREATE TABLE "people" (
        "id" SERIAL PRIMARY KEY,
        "created" BIGINT,
        "name" VARCHAR(255) NOT NULL,
        "additional_names" TEXT,
        "is_cool" BOOLEAN,
        "initial" CHAR,
        "mask" SMALLINT,
        "powers" VARCHAR(255),
        "age" SMALLINT,
        "rating" FLOAT,
        "height" DOUBLE PRECISION,
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
        "version" INT
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
        "group" INT
      );
      CREATE INDEX "indexed_user_name_idx" ON "indexed_user" ("name");
      CREATE INDEX "indexed_user_group_idx" ON "indexed_user" ("group");
      """

  Scenario: multiple indexes
    Then the schema of io.semla.model.MultipleIndices is equal to:
      """SQL
      CREATE TABLE "multiple_indices" (
        "id" SERIAL PRIMARY KEY,
        "name" VARCHAR(255),
        "real_value" VARCHAR(255)
      );
      CREATE UNIQUE INDEX "multiple_indices_name_idx" ON "multiple_indices" ("name");
      CREATE INDEX "multiple_indices_name_real_value_idx" ON "multiple_indices" ("name", "real_value");
      """

  Scenario: foreign keys are automatically indexed
    * source prepend:
      """java
      package io.semla.model.ddl;
      import javax.persistence.*;
      import java.util.*;
      """
    Given the types:
      """java
      @Entity
      public class Parent {

          @Id
          @GeneratedValue
          public int id;

          @OneToMany(mappedBy = "parent")
          public List<Child> children;
      }

      @Entity
      public class Child {

          @Id
          @GeneratedValue
          public int id;

          @ManyToOne(fetch = FetchType.LAZY)
          public Parent parent;

          @OneToOne(fetch = FetchType.LAZY, mappedBy = "child")
          public Pet pet;

          @ManyToMany
          public List<Child> sibblings;

      }

      @Entity
      public class Pet {

          @Id
          @GeneratedValue
          public int id;

          @OneToOne(fetch = FetchType.LAZY)
          public Child child;

      }
      """
    Then the schema of io.semla.model.ddl.Parent is equal to:
      """SQL
      CREATE TABLE "parent" (
        "id" SERIAL PRIMARY KEY
      );
      """
    And the schema of io.semla.model.ddl.Child is equal to:
      """SQL
      CREATE TABLE "child" (
        "id" SERIAL PRIMARY KEY,
        "parent" INT
      );
      CREATE INDEX "child_parent_idx" ON "child" ("parent");
      """
    And the schema of io.semla.model.ddl.Pet is equal to:
      """SQL
      CREATE TABLE "pet" (
        "id" SERIAL PRIMARY KEY,
        "child" INT
      );
      CREATE INDEX "pet_child_idx" ON "pet" ("child");
      """
    And the schema of io.semla.model.ddl.ChildChild is equal to:
      """SQL
      CREATE TABLE "child_child" (
        "id" SERIAL PRIMARY KEY,
        "child_a" INT,
        "child_b" INT
      );
      CREATE INDEX "child_child_child_a_idx" ON "child_child" ("child_a");
      CREATE INDEX "child_child_child_b_idx" ON "child_child" ("child_b");
      """