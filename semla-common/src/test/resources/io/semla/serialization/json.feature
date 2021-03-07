Feature: a json serializer/deserializer

  Scenario: a json document can be parsed as a Map
    When the following json gets parsed as a java.util.Map:
    """
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
    """
    {"id":1,"name":"banana","price":1,"genus":{"id":2,"name":"musa","family":1,"fruits":[1,2,null]}}
    """

  Scenario: a simple object in a really malformed json
    When the following json gets parsed as a java.util.Map:
    """
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
    """
    {"id":1,"name":"banana","price":1,"genus":{"id":2,"name":null,"family":1,"fruits":[1,2,3]}}
    """
    And it gets pretty serialized as this json:
    """
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
    When the following json gets parsed as a io.semla.serialization.SomeType:
    """
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
      },
      "someReadOnlyValue": 5
    }
    """
    Then it gets pretty serialized as this json:
    """
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
    When the following json gets parsed as a io.semla.model.Score:
    """
    {"name":"test","score":1}
    """
    Then it gets pretty serialized as this json:
    """
    {
      "name": "test",
      "score": 1
    }
    """

  Scenario: polymorphic serialization/deserialization
    Given this class:
    """
    package io.semla.serialization.json;

    public class Elements {
      public java.util.List<io.semla.model.Parent> elements;
    }
    """
    When the following json gets parsed as a io.semla.serialization.json.Elements:
    """
    {"elements":[
      {
        "type": "child"
      }
    ]}
    """
    Then it gets pretty serialized as this json:
    """
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
    Given this class:
    """
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
    When the following json gets parsed as a io.semla.serialization.json.AnonymousType:
    """
    {"list":[1,2,true,4,"false"],"missing":null,"value":null,"values":[],"map":{},"other":"Test","otherValues":[],"otherMap":{}}
    """
    Then it gets pretty serialized as this json:
    """
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
