Feature: MYSQL DDL

  Background:
    Given that io.semla.datasource.MysqlDatasource is the default datasource

  Scenario: the MysqlDatasource can create a schema from an entity
    Then the schema of io.semla.model.User is equal to:
      """SQL
      CREATE TABLE `people` (
        `id` INT AUTO_INCREMENT NOT NULL,
        `created` BIGINT,
        `name` VARCHAR(255) NOT NULL,
        `additional_names` TEXT,
        `is_cool` BIT(1),
        `initial` CHAR,
        `mask` TINYINT,
        `powers` VARCHAR(255),
        `age` SMALLINT,
        `rating` FLOAT,
        `height` DOUBLE,
        `birthdate` DATE NOT NULL,
        `last_seen` TIME,
        `last_login` TIMESTAMP(3),
        `sql_date` DATE,
        `sql_time` TIME,
        `sql_timestamp` TIMESTAMP(3),
        `big_integer` BIGINT,
        `big_decimal` DECIMAL(65,30),
        `calendar` TIMESTAMP(3),
        `instant` TIMESTAMP(3),
        `local_date_time` TIMESTAMP(3),
        `nickname` VARCHAR(255),
        `type` INT,
        `eyecolor` VARCHAR(255),
        `version` INT,
        UNIQUE INDEX `name_idx` (`name`),
        PRIMARY KEY (`id`)
      );
      """

  Scenario: uuid based primary key
    Then the schema of io.semla.model.IndexedUser is equal to:
      """SQL
      CREATE TABLE `indexed_user` (
        `uuid` VARCHAR(36) NOT NULL,
        `name` VARCHAR(255),
        `age` INT,
        `group` INT,
        INDEX `name_idx` (`name`),
        INDEX `group_idx` (`group`),
        PRIMARY KEY (`uuid`)
      );
      """

  Scenario: multiple indexes
    Then the schema of io.semla.model.MultipleIndices is equal to:
      """SQL
      CREATE TABLE `multiple_indices` (
        `id` INT AUTO_INCREMENT NOT NULL,
        `name` VARCHAR(255),
        `real_value` VARCHAR(255),
        UNIQUE INDEX `name_idx` (`name`),
        INDEX `name_real_value_idx` (`name`, `real_value`),
        PRIMARY KEY (`id`)
      );
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
      CREATE TABLE `parent` (
        `id` INT AUTO_INCREMENT NOT NULL,
        PRIMARY KEY (`id`)
      );
      """
    And the schema of io.semla.model.ddl.Child is equal to:
      """SQL
      CREATE TABLE `child` (
        `id` INT AUTO_INCREMENT NOT NULL,
        `parent` INT,
        INDEX `parent_idx` (`parent`),
        PRIMARY KEY (`id`)
      );
      """
    And the schema of io.semla.model.ddl.Pet is equal to:
      """SQL
      CREATE TABLE `pet` (
        `id` INT AUTO_INCREMENT NOT NULL,
        `child` INT,
        INDEX `child_idx` (`child`),
        PRIMARY KEY (`id`)
      );
      """
    And the schema of io.semla.model.ddl.ChildChild is equal to:
      """SQL
      CREATE TABLE `child_child` (
        `id` INT AUTO_INCREMENT NOT NULL,
        `child_a` INT,
        `child_b` INT,
        INDEX `child_a_idx` (`child_a`),
        INDEX `child_b_idx` (`child_b`),
        PRIMARY KEY (`id`)
      );
      """