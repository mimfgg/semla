Feature: a json serializer/deserializer

  Scenario: a json document can be parsed as a Map
    Given that it is a Map:
      """json
      {
        "id": 1,
        "name": "banana",
        "price": 1,
        "genus": {
          "id": 2,
          "name": "musa",
          "family": 1,
          "fruits": [
            1,
            2,
            null
          ]
        }
      }
      """
    Then it gets serialized as this json:
      """json
      {"id":1,"name":"banana","price":1,"genus":{"id":2,"name":"musa","family":1,"fruits":[1,2,null]}}
      """

  Scenario: a simple object in a really malformed json
    When it is a Map:
      """json
      {"id":1,     "name":
      "banana",


      "price":1,
      "genus":
      {"id":2,
      "name":null,       "family"
      : 1,              "fruits":[                1,      2,3]
      }
      }
      """
    Then it gets serialized as this json:
      """json
      {"id":1,"name":"banana","price":1,"genus":{"id":2,"name":null,"family":1,"fruits":[1,2,3]}}
      """
    And it gets pretty serialized as this json:
      """json
      {
        "id": 1,
        "name": "banana",
        "price": 1,
        "genus": {
          "id": 2,
          "name": null,
          "family": 1,
          "fruits": [
            1,
            2,
            3
          ]
        }
      }
      """

  Scenario: some type that contains most of the java types
    When it is a io.semla.serialization.SomeType:
      """json
      {
        "anInt": 1,
        "anInteger": 2,
        "aDouble": 3.0,
        "anotherDouble": 4.0,
        "aFloat": 5.0,
        "anotherFloat": 6.0,
        "aString": "test",
        "aBoolean": false,
        "anotherBoolean": true,
        "list": [1.0, 2.3],
        "arrayList": ["1","2","3"],
        "linkedList": [1,2,3],
        "set": [0, .5],
        "linkedHashSet": ["a","b"],
        "treeSet": [1, 2],
        "map": null,
        "linkedHashMap": {
          "key1": "value1",
          "key2": "value2"
        }
      }
      """
    Then it gets pretty serialized as this json:
      """json
      {
        "anInt": 1,
        "anInteger": 2,
        "aDouble": 3.0,
        "anotherDouble": 4.0,
        "aFloat": 5.0,
        "anotherFloat": 6.0,
        "aString": "test",
        "aBoolean": false,
        "anotherBoolean": true,
        "list": [
          1.0,
          2.3
        ],
        "arrayList": [
          "1",
          "2",
          "3"
        ],
        "linkedList": [
          1,
          2,
          3
        ],
        "set": [
          0.0,
          0.5
        ],
        "linkedHashSet": [
          "a",
          "b"
        ],
        "treeSet": [
          1,
          2
        ],
        "map": null,
        "linkedHashMap": {
          "key1": "value1",
          "key2": "value2"
        },
        "someReadOnlyValue": 10,
        "someCalculation": 20,
        "doubleThatCalculation": 40
      }
      """

  Scenario: a java bean as a json
    When score is a io.semla.model.Score:
      """json
      {"name":"test","score":1}
      """
    Then score gets pretty serialized as this json:
      """json
      {
        "name": "test",
        "score": 1
      }
      """

  Scenario: polymorphic serialization/deserialization
    Given this type:
      """java
      package io.semla.serialization.json;

      public class Elements {
        public java.util.List<io.semla.model.Parent> elements;
      }
      """
    And that we register Child as a subtype
    When elements is a io.semla.serialization.json.Elements:
      """json
      {"elements":[
        {
          "type": "child"
        }
      ]}
      """
    Then elements gets pretty serialized as this json:
      """json
      {
        "elements": [
          {
            "type": "child",
            "name": null
          }
        ]
      }
      """

  Scenario: a type with some corner cases
    Given this type:
      """java
      package io.semla.serialization.json;

      import io.semla.serialization.annotations.*;
      import io.semla.util.*;
      import java.util.*;

      public class AnonymousType {
        public List list;
        public int[] missing;
        @Deserialize(When.NOT_NULL)
        public String value = "defaultValue";
        @Deserialize(When.NOT_EMPTY)
        public List<Integer> values = Lists.of(1,2,3);
        @Deserialize(When.NOT_EMPTY)
        public Map<String, Integer> map = Maps.of("first",1,"second",2);
        @Serialize(When.NOT_DEFAULT)
        public String other = "Test";
        @Serialize(When.NOT_EMPTY)
        public List<Integer> otherValues;
        @Serialize(When.NOT_EMPTY)
        public Map<String, Integer> otherMap;
      }
      """
    When it is an io.semla.serialization.json.AnonymousType:
      """json
      {"list":[1,2,true,4,"false"],"missing":null,"value":null,"values":[],"map":{},"other":"Test","otherValues":[],"otherMap":{}}
      """
    Then it gets pretty serialized as this json:
      """json
      {
        "list": [
          1,
          2,
          true,
          4,
          "false"
        ],
        "missing": null,
        "value": "defaultValue",
        "values": [
          1,
          2,
          3
        ],
        "map": {
          "first": 1,
          "second": 2
        }
      }
      """
