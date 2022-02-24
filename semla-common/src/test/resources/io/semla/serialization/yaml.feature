Feature: a yaml serializer/deserializer
  #from http://yaml.org/spec/1.2/spec.html

  Scenario: Example 2.1. Sequence of Scalars (ball players)
    When it is a List:
      """yaml
        - Mark McGwire
        - Sammy Sosa
        - Ken Griffey
      """
    Then it gets serialized as this json:
      """json
      ["Mark McGwire","Sammy Sosa","Ken Griffey"]
      """
    And it gets serialized as this yaml:
      """yaml
      - Mark McGwire
      - Sammy Sosa
      - Ken Griffey
      """

  Scenario: Example 2.2. Mapping Scalars to Scalars (player statistics)
    When it is a Map:
      """yaml
      hr:  65    # Home runs
      avg: 0.278 # Batting average
      rbi: 147   # Runs Batted In
      """
    Then it gets serialized as this json:
      """json
      {"hr":65,"avg":0.278,"rbi":147}
      """
    And it gets serialized as this yaml:
      """yaml
      hr: 65
      avg: 0.278
      rbi: 147
      """

  Scenario: Example 2.3. Mapping Scalars to Sequences (ball clubs in each league)
    When it is a Map:
      """yaml
      american:
        - Boston Red Sox
        - Detroit Tigers
        - New York Yankees
      national:
        - New York Mets
        - Chicago Cubs
        - Atlanta Braves
      """
    Then it gets serialized as this json:
      """json
      {"american":["Boston Red Sox","Detroit Tigers","New York Yankees"],"national":["New York Mets","Chicago Cubs","Atlanta Braves"]}
      """
    And it gets serialized as this yaml:
      """yaml
      american:
        - Boston Red Sox
        - Detroit Tigers
        - New York Yankees
      national:
        - New York Mets
        - Chicago Cubs
        - Atlanta Braves
      """

  Scenario: Example 2.4. Sequence of Mappings (players’ statistics)
    When it is a List:
      """yaml
      -
        name: Mark McGwire
        hr:   65
        avg:  0.278
      -
        name: Sammy Sosa
        hr:   63
        avg:  0.288
      """
    Then it gets serialized as this json:
      """json
      [{"name":"Mark McGwire","hr":65,"avg":0.278},{"name":"Sammy Sosa","hr":63,"avg":0.288}]
      """
    And it gets serialized as this yaml:
      """yaml
      - name: Mark McGwire
        hr: 65
        avg: 0.278
      - name: Sammy Sosa
        hr: 63
        avg: 0.288
      """

  Scenario: Example 2.5. Sequence of Sequences
    When it is a List:
      """yaml
      - [name        , hr, avg  ]
      - [Mark McGwire, 65, 0.278]
      - [Sammy Sosa  , 63, 0.288]
      """
    Then it gets serialized as this json:
      """json
      [["name","hr","avg"],["Mark McGwire",65,0.278],["Sammy Sosa",63,0.288]]
      """
    And it gets serialized as this yaml:
      """yaml
      -
        - name
        - hr
        - avg
      -
        - Mark McGwire
        - 65
        - 0.278
      -
        - Sammy Sosa
        - 63
        - 0.288
      """

  Scenario: Example 2.6. Mapping of Mappings
    When it is a Map:
      """yaml
      Mark McGwire: {hr: 65, avg: 0.278}
      Sammy Sosa: {
          hr: 63,
          avg: 0.288
        }
      """
    Then it gets serialized as this json:
      """json
      {"Mark McGwire":{"hr":65,"avg":0.278},"Sammy Sosa":{"hr":63,"avg":0.288}}
      """
    And it gets serialized as this yaml:
      """yaml
      Mark McGwire:
        hr: 65
        avg: 0.278
      Sammy Sosa:
        hr: 63
        avg: 0.288
      """

  @ignore
  Scenario: Example 2.7. Two Documents in a Stream (each with a leading comment) (using that example for leading comments and ---)
    When it is a List:
      """yaml
      # Ranking of 1998 home runs
      ---
      - Mark McGwire
      - Sammy Sosa
      - Ken Griffey

      # Team ranking
      ---
      - Chicago Cubs
      - St Louis Cardinals
      """
    Then it gets serialized as this json:
      """json
      [["Mark McGwire","Sammy Sosa","Ken Griffey"],["Chicago Cubs","St Louis Cardinals"]]
      """

  Scenario: Example 2.8. Play by Play Feed from a Game (using that example for ...)
    When it is a Map:
      """yaml
      ---
      time: 20:03:20
      player: Sammy Sosa
      action: strike (miss)
      ...
      """
    Then it gets serialized as this json:
      """json
      {"time":"20:03:20","player":"Sammy Sosa","action":"strike (miss)"}
      """
    And it gets serialized as this yaml:
      """yaml
      time: 20:03:20
      player: Sammy Sosa
      action: strike (miss)
      """

  Scenario: Example 2.9. Single Document with Two Comments
    When it is a Map:
      """yaml
      ---
      hr: # 1998 hr ranking
        - Mark McGwire
        - Sammy Sosa
      rbi:
        # 1998 rbi ranking
        - Sammy Sosa
        - Ken Griffey
      """
    Then it gets serialized as this json:
      """json
      {"hr":["Mark McGwire","Sammy Sosa"],"rbi":["Sammy Sosa","Ken Griffey"]}
      """
    And it gets serialized as this yaml:
      """yaml
      hr:
        - Mark McGwire
        - Sammy Sosa
      rbi:
        - Sammy Sosa
        - Ken Griffey
      """

  Scenario: Example 2.10. Node for “Sammy Sosa” appears twice in this document
    When it is a Map:
      """yaml
      ---
      hr:
        - Mark McGwire
        # Following node labeled SS
        - &SS Sammy Sosa
      rbi:
        - *SS # Subsequent occurrence
        - Ken Griffey
      """
    Then it gets serialized as this json:
      """json
      {"hr":["Mark McGwire","Sammy Sosa"],"rbi":["Sammy Sosa","Ken Griffey"]}
      """
    And it gets serialized as this yaml:
      """yaml
      hr:
        - Mark McGwire
        - Sammy Sosa
      rbi:
        - Sammy Sosa
        - Ken Griffey
      """


  Scenario: Example 2.11. Mapping between Sequences
    When it is a Map:
      """yaml
      ? - Detroit Tigers
        - Chicago cubs
      :
        - 2001-07-23

      ? [ New York Yankees,
          Atlanta Braves ]
      : [ 2001-07-02, 2001-08-12,
          2001-08-14 ]
      """
    Then it gets serialized as this yaml:
      """yaml
      ? - Detroit Tigers
        - Chicago cubs
      : - 2001-07-23
      ? - New York Yankees
        - Atlanta Braves
      : - 2001-07-02
        - 2001-08-12
        - 2001-08-14
      """
# todo:
#    And it gets serialized as this json:
#    """
#    [{"key":["Detroit Tigers","Chicago cubs"],"value":["2001-07-23"]},{"key":["New York Yankees","Atlanta Braves"],"value":["2001-07-02","2001-08-12","2001-08-14"]}]
#    """

  Scenario: Example 2.12. Compact Nested Mapping
    When it is a List:
      """yaml
      ---
      # Products purchased
      - item    : Super Hoop
        quantity: 1
      - item    : Basketball
        quantity: 4
      - item    : Big Shoes
        quantity: 1
      """
    Then it gets serialized as this json:
      """json
      [{"item":"Super Hoop","quantity":1},{"item":"Basketball","quantity":4},{"item":"Big Shoes","quantity":1}]
      """
    And it gets serialized as this yaml:
      """yaml
      - item: Super Hoop
        quantity: 1
      - item: Basketball
        quantity: 4
      - item: Big Shoes
        quantity: 1
      """

  Scenario: Example 2.13. In literals, newlines are preserved
    When it is a String:
      """yaml
      # ASCII Art
      --- |
        \//||\/||
        // ||  ||__
      """
    Then it gets serialized as this yaml:
      """yaml
      |
        \//||\/||
        // ||  ||__
      """

  Scenario: Example 2.14. In the folded scalars, newlines become spaces
    When it is a String:
      """yaml
      --- >
        Mark McGwire's
        year was crippled
        by a knee injury.
      """
    Then it gets serialized as this yaml:
      """yaml
      Mark McGwire's year was crippled by a knee injury.
      """

  Scenario: null values are properly handled
    When it is a List:
      """yaml
      ---
      [null, ~, 2]
      """
    Then it gets serialized as this json:
      """json
      [null,null,2]
      """
    And it gets serialized as this yaml:
      """yaml
      - null
      - null
      - 2
      """

  Scenario: "null" strings are properly handled
    When it is a List:
      """yaml
      ["null", 1, 2]
      """
    Then it gets serialized as this json:
      """json
      ["null",1,2]
      """
    And it gets serialized as this yaml:
      """yaml
      - "null"
      - 1
      - 2
      """

  Scenario: Map of List of Object
    When it is a Map:
      """yaml
      things:
        - id: 1
          title: something
        - id: 2
          title: something else
      """
    Then it gets pretty serialized as this json:
      """json
      {
        "things": [
          {
            "id": 1,
            "title": "something"
          },
          {
            "id": 2,
            "title": "something else"
          }
        ]
      }
      """

  Scenario: Object with a null property
    When it is a Map:
      """yaml
      object:
        property: null
      """
    Then it gets pretty serialized as this json:
      """json
      {
        "object": {
          "property": null
        }
      }
      """

  Scenario: nested List of Objects
    When it is a List:
      """yaml
      - id: 1
        parent:
          id: 1
          children:
            - id: 1
              parent: 1
            - id: 2
              parent: 1
      - id: 2
        parent:
          id: 1
          children:
            - id: 1
              parent: 1
            - id: 2
              parent: 1
      """
    Then it gets pretty serialized as this json:
      """json
      [
        {
          "id": 1,
          "parent": {
            "id": 1,
            "children": [
              {
                "id": 1,
                "parent": 1
              },
              {
                "id": 2,
                "parent": 1
              }
            ]
          }
        },
        {
          "id": 2,
          "parent": {
            "id": 1,
            "children": [
              {
                "id": 1,
                "parent": 1
              },
              {
                "id": 2,
                "parent": 1
              }
            ]
          }
        }
      ]
      """

  Scenario: empty lists
    When it is a Map:
      """yaml
      id: 1
      children: []
      """
    Then it gets pretty serialized as this json:
      """json
      {
        "id": 1,
        "children": [
        ]
      }
      """

  Scenario: a simple entity
    When it is a Map:
      """yaml
      id: 1
      name: banana
      price: 1
      genus:
        id: 2
        name: null
        family: 1
        fruits:
          - 1
          - 2
          - 3
      """
    Then it gets serialized as this json:
      """json
      {"id":1,"name":"banana","price":1,"genus":{"id":2,"name":null,"family":1,"fruits":[1,2,3]}}
      """

  Scenario: polymorphic serialization/deserialization
    Given this type:
      """java
      package io.semla.serialization.yaml;

      public class Elements {
        public java.util.List<io.semla.model.Parent> elements;
      }
      """
    And that we register Child as a subtype
    When it is an io.semla.serialization.yaml.Elements:
      """yaml
      elements:
        - type: child
      """
    Then it gets serialized as this yaml:
      """yaml
      elements:
        - type: child
          name: null
      """

  Scenario: quoted content
    When it is a Map:
      """yaml
      quoted: ":-{}[]\"\\!#|>&%@*,"
      """
    Then it gets serialized as this json:
      """json
      {"quoted":":-{}[]\"\\!#|>&%@*,"}
      """

  Scenario: block in map
    When it is a Map:
      """yaml
      block: >
        something that doesn't
        have to be super long
      end: something else
      """
    Then it gets serialized as this json:
      """json
      {"block":"something that doesn't have to be super long\n","end":"something else"}
      """

  Scenario: big block
    When it is a Map:
      """yaml
      value: null
      value2: "null"
      block: >
        1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890
        1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890
        1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890
        1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890
      end: something else
      """
    Then it gets serialized as this yaml:
      """yaml
      value: null
      value2: "null"
      block: >
        1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890
        1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890
        1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890
        1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890
        1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890
        1234567890 1234567890 1234567890 1234567890 1234567890
      end: something else
      """

  Scenario: block example with >-
    When it is a Map:
      """yaml
      example: >-
        Several lines of text,
        with some "quotes" of various 'types',
        and also a blank line:

        plus another line at the end.



      """
    Then it gets serialized as this json:
      """json
      {"example":"Several lines of text, with some \"quotes\" of various 'types', and also a blank line:\nplus another line at the end."}
      """

  Scenario: block example with >
    When it is a Map:
      """yaml
      example: >
        Several lines of text,
        with some "quotes" of various 'types',
        and also a blank line:

        plus another line at the end.



      """
    Then it gets serialized as this json:
      """json
      {"example":"Several lines of text, with some \"quotes\" of various 'types', and also a blank line:\nplus another line at the end.\n"}
      """

  Scenario: block example with >+
    When it is a Map:
      """yaml
      example: >+
        Several lines of text,
        with some "quotes" of various 'types',
        and also a blank line:

        plus another line at the end.



      """
    Then it gets serialized as this json:
      """json
      {"example":"Several lines of text, with some \"quotes\" of various 'types', and also a blank line:\nplus another line at the end.\n\n\n"}
      """

  Scenario: block example with |-
    When it is a Map:
      """yaml
      example: |-
        Several lines of text,
        with some "quotes" of various 'types',
        and also a blank line:

        plus another line at the end.



      """
    Then it gets serialized as this json:
      """json
      {"example":"Several lines of text,\nwith some \"quotes\" of various 'types',\nand also a blank line:\n\nplus another line at the end."}
      """

  Scenario: block example with |
    When it is a Map:
      """yaml
      example: |
        Several lines of text,
        with some "quotes" of various 'types',
        and also a blank line:

        plus another line at the end.



      """
    Then it gets serialized as this json:
      """json
      {"example":"Several lines of text,\nwith some \"quotes\" of various 'types',\nand also a blank line:\n\nplus another line at the end.\n"}
      """

  Scenario: block example with |+
    When it is a Map:
      """yaml
      example: |+
        Several lines of text,
        with some "quotes" of various 'types',
        and also a blank line:

        plus another line at the end.



      """
    Then it gets serialized as this json:
      """json
      {"example":"Several lines of text,\nwith some \"quotes\" of various 'types',\nand also a blank line:\n\nplus another line at the end.\n\n\n"}
      """

  Scenario: single quoted flow scalar
    When it is a Map:
      """yaml
      example: 'Several lines of text,
      containing ''single quotes''. Escapes (like \n) don''t do anything.

      Newlines can be added by leaving a blank line.
        Leading whitespace on lines is ignored.'
      """
    Then it gets serialized as this json:
      """json
      {"example":"Several lines of text, containing 'single quotes'. Escapes (like \\n) don't do anything.\nNewlines can be added by leaving a blank line. Leading whitespace on lines is ignored."}
      """

  Scenario: double quoted flow scalar
    When it is a Map:
      """yaml
      example: "Several lines of text,
      containing \"double quotes\". Escapes (like \\n) work.\nIn addition,
      newlines can be esc\
      aped to prevent them from being converted to a space.

      Newlines can also be added by leaving a blank line.
        Leading whitespace on lines is ignored."
      """
    Then it gets serialized as this json:
      """json
      {"example":"Several lines of text, containing \"double quotes\". Escapes (like \\n) work.\nIn addition, newlines can be escaped to prevent them from being converted to a space.\nNewlines can also be added by leaving a blank line. Leading whitespace on lines is ignored."}
      """

  Scenario: Plain flow scalar
    When it is a Map:
      """yaml
      example: Several lines of text,
        with some "quotes" of various 'types'.
        Escapes (like \n) don't do anything.

        Newlines can be added by leaving a blank line.
          Additional leading whitespace is ignored.
      """
    Then it gets serialized as this json:
      """json
      {"example":"Several lines of text, with some \"quotes\" of various 'types'. Escapes (like \\n) don't do anything.\nNewlines can be added by leaving a blank line. Additional leading whitespace is ignored."}
      """

  Scenario: Quote in normal content
    When it is a Map:
      """yaml
      question: Don't you know?
      """
    Then it gets serialized as this json:
      """json
      {"question":"Don't you know?"}
      """

  Scenario: anchor should work in arrays
    When it is a List:
      """yaml
      - &flag Apple
      - Beachball
      - Cartoon
      - Duckface
      - *flag
      """
    Then it gets serialized as this yaml:
      """yaml
      - Apple
      - Beachball
      - Cartoon
      - Duckface
      - Apple
      """

  Scenario: advanced anchors
    When it is a Map:
      """yaml
      definitions:
        steps:
          - step: &build-test
              name: Build and test
              script:
                - mvn package
              artifacts:
                - target/**
          - step: &deploy
              name: Deploy
              deployment: test
              script:
                - ./deploy.sh target/my-app.jar
      pipelines:
        branches:
          develop:
            - step: *build-test
            - step: *deploy
          master:
            - step: *build-test
            - step:
                <<: *deploy
                deployment: production
                trigger: manual
      """
    Then it gets serialized as this yaml:
      """yaml
      definitions:
        steps:
          - step:
              name: Build and test
              script:
                - mvn package
              artifacts:
                - target/**
          - step:
              name: Deploy
              deployment: test
              script:
                - ./deploy.sh target/my-app.jar
      pipelines:
        branches:
          develop:
            - step:
                name: Build and test
                script:
                  - mvn package
                artifacts:
                  - target/**
            - step:
                name: Deploy
                deployment: test
                script:
                  - ./deploy.sh target/my-app.jar
          master:
            - step:
                name: Build and test
                script:
                  - mvn package
                artifacts:
                  - target/**
            - step:
                name: Deploy
                deployment: production
                script:
                  - ./deploy.sh target/my-app.jar
                trigger: manual
      """

  Scenario: missing anchor
    * an exception DeserializationException is thrown when it is a Map:
      """yaml
      content: *value
      """
    And exception.message is equal to:
      """
      didn't have stored value for anchor 'value' @column: 14 line: 0 character: '￿' @14/15
      """

  Scenario Template: incorrect yaml
    * an exception DeserializationException is thrown when it is a Map:
      """
      <input>
      """
    And exception.message is equal to:
      """
      ?e <message>
      """
    Examples:
      | input                        | message                                                             |
      | something: .[                | unexpected character '\[' while buffer already contains \..*        |
      | something: .]                | unexpected character ']' while no array has been created .*         |
      | something: .{                | unexpected character '\{' while buffer already contains \..*        |
      | something: {:}               | unexpected character ':' while buffer already contains .*           |
      | something: .}                | unexpected character '}' while no object has been created .*        |
      | something: >/                | unsupported BlockChomping: /                                        |
      | something: !include nop.yaml | while including nop.yaml @column: 27 line: 0 character: '.*' @27/28 |
      | something: !someType test    | unknown class: someType @column: 24 line: 0 character: '.*' @24/25  |

  Scenario Outline: explicit typing
    When it is a Map:
      """yaml
      content: <input>
      """
    Then it gets serialized as this json:
      """json
      {"content":<output>}
      """
    Examples:
      | input                                 | output               |
      | !!str 3                               | "3"                  |
      | !!int "3"                             | 3                    |
      | !java.lang.Float "3.2"                | 3.2                  |
      | !!bool "true"                         | true                 |
      | !!str true                            | "true"               |
      | !!map test: something                 | {"test":"something"} |
      | !!seq - something                     | ["something"]        |
      | !include src/test/resources/test.yaml | {"key":"value"}      |

  Scenario Outline: booleans!
    When it is a Map:
      """yaml
      content: <input>
      """
    Then it gets serialized as this yaml:
      """yaml
      content: <output>
      """
    Examples:
      | input | output |
      | y     | true   |
      | Y     | true   |
      | yes   | true   |
      | Yes   | true   |
      | YES   | true   |
      | true  | true   |
      | True  | true   |
      | TRUE  | true   |
      | on    | true   |
      | On    | true   |
      | ON    | true   |
      | n     | false  |
      | N     | false  |
      | no    | false  |
      | No    | false  |
      | NO    | false  |
      | false | false  |
      | False | false  |
      | FALSE | false  |
      | off   | false  |
      | Off   | false  |
      | OFF   | false  |

  Scenario: Map of Strings that look like primitives
    When it is a Map:
      """yaml
      number: "1234"
      bool: 'true'
      """
    Then it gets serialized as this json:
      """json
      {"number":"1234","bool":"true"}
      """

  Scenario: Map of List of List with values with white spaces
    When it is a Map:
      """yaml
      test:
        - attributes:
          - name: name 1
            value: value 1
          - name: name 2
            value: value 2
      result: true
      """
    Then it gets serialized as this json:
      """json
      {"test":[{"attributes":[{"name":"name 1","value":"value 1"},{"name":"name 2","value":"value 2"}]}],"result":true}
      """
