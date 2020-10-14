Feature: a yaml serializer/deserializer
  #from http://yaml.org/spec/1.2/spec.html

  Scenario: Example 2.1. Sequence of Scalars (ball players)
    When the following yaml gets parsed as a java.util.List:
    """
      - Mark McGwire
      - Sammy Sosa
      - Ken Griffey
    """
    Then it gets serialized as this json:
    """
    ["Mark McGwire","Sammy Sosa","Ken Griffey"]
    """
    And it gets serialized as this yaml:
    """
    - Mark McGwire
    - Sammy Sosa
    - Ken Griffey
    """

  Scenario: Example 2.2. Mapping Scalars to Scalars (player statistics)
    When the following yaml gets parsed as a java.util.Map:
    """
      hr:  65    # Home runs
      avg: 0.278 # Batting average
      rbi: 147   # Runs Batted In
    """
    Then it gets serialized as this json:
    """
    {"hr":65,"avg":0.278,"rbi":147}
    """
    And it gets serialized as this yaml:
    """
    hr: 65
    avg: 0.278
    rbi: 147
    """

  Scenario: Example 2.3. Mapping Scalars to Sequences (ball clubs in each league)
    When the following yaml gets parsed as a java.util.Map:
    """
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
    """
    {"american":["Boston Red Sox","Detroit Tigers","New York Yankees"],"national":["New York Mets","Chicago Cubs","Atlanta Braves"]}
    """
    And it gets serialized as the same yaml

  Scenario: Example 2.4. Sequence of Mappings (players’ statistics)
    When the following yaml gets parsed as a java.util.List:
    """
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
    """
    [{"name":"Mark McGwire","hr":65,"avg":0.278},{"name":"Sammy Sosa","hr":63,"avg":0.288}]
    """
    And it gets serialized as this yaml:
    """
    - name: Mark McGwire
      hr: 65
      avg: 0.278
    - name: Sammy Sosa
      hr: 63
      avg: 0.288
    """

  Scenario: Example 2.5. Sequence of Sequences
    When the following yaml gets parsed as a java.util.List:
    """
    - [name        , hr, avg  ]
    - [Mark McGwire, 65, 0.278]
    - [Sammy Sosa  , 63, 0.288]
    """
    Then it gets serialized as this json:
    """
    [["name","hr","avg"],["Mark McGwire",65,0.278],["Sammy Sosa",63,0.288]]
    """
    And it gets serialized as this yaml:
    """
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
    When the following yaml gets parsed as a java.util.Map:
    """
    Mark McGwire: {hr: 65, avg: 0.278}
    Sammy Sosa: {
        hr: 63,
        avg: 0.288
      }
    """
    Then it gets serialized as this json:
    """
    {"Mark McGwire":{"hr":65,"avg":0.278},"Sammy Sosa":{"hr":63,"avg":0.288}}
    """
    And it gets serialized as this yaml:
    """
    Mark McGwire:
      hr: 65
      avg: 0.278
    Sammy Sosa:
      hr: 63
      avg: 0.288
    """

  Scenario: Example 2.7. Two Documents in a Stream (each with a leading comment) (using that example for leading comments and ---)
    When the following yaml gets parsed as a java.util.List:
    """
    # Ranking of 1998 home runs
    ---
    - Mark McGwire
    - Sammy Sosa
    - Ken Griffey

    # Team ranking
    #---
    #- Chicago Cubs
    #- St Louis Cardinals
    """
    Then it gets serialized as this json:
    """
    ["Mark McGwire","Sammy Sosa","Ken Griffey"]
    """

  Scenario: Example 2.8. Play by Play Feed from a Game (using that example for ...)
    When the following yaml gets parsed as a java.util.Map:
    """
    ---
    time: 20:03:20
    player: Sammy Sosa
    action: strike (miss)
    ...
    """
    Then it gets serialized as this json:
    """
    {"time":"20:03:20","player":"Sammy Sosa","action":"strike (miss)"}
    """
    And it gets serialized as this yaml:
    """
    time: 20:03:20
    player: Sammy Sosa
    action: strike (miss)
    """

  Scenario: Example 2.9. Single Document with Two Comments
    When the following yaml gets parsed as a java.util.Map:
    """
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
    """
    {"hr":["Mark McGwire","Sammy Sosa"],"rbi":["Sammy Sosa","Ken Griffey"]}
    """
    And it gets serialized as this yaml:
    """
    hr:
      - Mark McGwire
      - Sammy Sosa
    rbi:
      - Sammy Sosa
      - Ken Griffey
    """

  Scenario: Example 2.10. Node for “Sammy Sosa” appears twice in this document
    When the following yaml gets parsed as a java.util.Map:
    """
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
    """
    {"hr":["Mark McGwire","Sammy Sosa"],"rbi":["Sammy Sosa","Ken Griffey"]}
    """
    And it gets serialized as this yaml:
    """
    hr:
      - Mark McGwire
      - Sammy Sosa
    rbi:
      - Sammy Sosa
      - Ken Griffey
    """


  Scenario: Example 2.11. Mapping between Sequences
    When the following yaml gets parsed as a java.util.Map:
    """
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
    """
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
    When the following yaml gets parsed as a java.util.List:
    """
    ---
    # Products purchased
    - item    : Super Hoop
      quantity: 1
    - item    : Basketball
      quantity: 4
    - item    : Big Shoes
      quantity: 1

    ...
    """
    Then it gets serialized as this json:
    """
    [{"item":"Super Hoop","quantity":1},{"item":"Basketball","quantity":4},{"item":"Big Shoes","quantity":1}]
    """
    And it gets serialized as this yaml:
    """
    - item: Super Hoop
      quantity: 1
    - item: Basketball
      quantity: 4
    - item: Big Shoes
      quantity: 1
    """

  Scenario: Example 2.13. In literals, newlines are preserved
    When the following yaml gets parsed as a java.lang.String:
    """
    # ASCII Art
    --- |
      \//||\/||
      // ||  ||__
    """
    Then it gets serialized as this yaml:
    """
    |
      \//||\/||
      // ||  ||__
    """

  Scenario: Example 2.14. In the folded scalars, newlines become spaces
    When the following yaml gets parsed as a java.lang.String:
    """
    --- >
      Mark McGwire's
      year was crippled
      by a knee injury.
    """
    Then it gets serialized as this yaml:
    """
    Mark McGwire's year was crippled by a knee injury.
    """

  Scenario: null values are properly handled
    When the following yaml gets parsed as a java.util.List:
    """
    [null, ~, 2]
    """
    Then it gets serialized as this json:
    """
    [null,null,2]
    """
    And it gets serialized as this yaml:
    """
    - null
    - null
    - 2
    """

  Scenario: "null" strings are properly handled
    When the following yaml gets parsed as a java.util.List:
    """
    ["null", 1, 2]
    """
    Then it gets serialized as this json:
    """
    ["null",1,2]
    """
    And it gets serialized as this yaml:
    """
    - "null"
    - 1
    - 2
    """

  Scenario: Map of List of Object
    When the following yaml gets parsed as a java.util.Map:
    """
    things:
      - id: 1
        title: something
      - id: 2
        title: something else
    """
    Then it gets serialized as the same yaml

  Scenario: Object with a null property
    When the following yaml gets parsed as a java.util.Map:
    """
    object:
      property: null
    """
    Then it gets serialized as the same yaml

  Scenario: nested List of Objects
    When the following yaml gets parsed as a java.util.List:
    """
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
    Then it gets serialized as the same yaml

  Scenario: empty lists
    When the following yaml gets parsed as a java.util.Map:
    """
    id: 1
    children: []
    """
    Then it gets serialized as the same yaml

  Scenario: a simple entity
    When the following yaml gets parsed as a io.semla.model.Fruit:
    """
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
    """
    {"id":1,"name":"banana","price":1,"genus":{"id":2,"name":null,"family":1,"fruits":[1,2,3]}}
    """
    And it gets serialized as the same yaml


  Scenario: polymorphic serialization/deserialization
    Given this class:
    """
    package io.semla.serialization.yaml;

    public class Datasources {
      public java.util.List<io.semla.config.DatasourceConfiguration> datasources;
    }
    """
    When the following yaml gets parsed as a io.semla.serialization.yaml.Datasources:
    """
    datasources:
      - type: in-memory
    """
    Then it gets serialized as this yaml:
    """
    datasources:
      - type: in-memory
    """

  Scenario: quoted content
    When the following yaml gets parsed as a java.util.Map:
    """
    quoted: ":-{}[]\"\\!#|>&%@*,"
    """
    Then it gets serialized as this json:
    """
    {"quoted":":-{}[]\"\\!#|>&%@*,"}
    """

  Scenario: block in map
    When the following yaml gets parsed as a java.util.Map:
    """
    block: >
      something that doesn't
      have to be super long
    end: something else
    """
    Then it gets serialized as this json:
    """
    {"block":"something that doesn't have to be super long\n","end":"something else"}
    """

  Scenario: big block
    When the following yaml gets parsed as a java.util.Map:
    """
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
    """
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
    When the following yaml gets parsed as a java.util.Map:
    """
    example: >-
      Several lines of text,
      with some "quotes" of various 'types',
      and also a blank line:

      plus another line at the end.



    """
    Then it gets serialized as this json:
    """
    {"example":"Several lines of text, with some \"quotes\" of various 'types', and also a blank line:\nplus another line at the end."}
    """

  Scenario: block example with >
    When the following yaml gets parsed as a java.util.Map:
    """
    example: >
      Several lines of text,
      with some "quotes" of various 'types',
      and also a blank line:

      plus another line at the end.



    """
    Then it gets serialized as this json:
    """
    {"example":"Several lines of text, with some \"quotes\" of various 'types', and also a blank line:\nplus another line at the end.\n"}
    """

  Scenario: block example with >+
    When the following yaml gets parsed as a java.util.Map:
    """
    example: >+
      Several lines of text,
      with some "quotes" of various 'types',
      and also a blank line:

      plus another line at the end.



    """
    Then it gets serialized as this json:
    """
    {"example":"Several lines of text, with some \"quotes\" of various 'types', and also a blank line:\nplus another line at the end.\n\n\n"}
    """

  Scenario: block example with |-
    When the following yaml gets parsed as a java.util.Map:
    """
    example: |-
      Several lines of text,
      with some "quotes" of various 'types',
      and also a blank line:

      plus another line at the end.



    """
    Then it gets serialized as this json:
    """
    {"example":"Several lines of text,\nwith some \"quotes\" of various 'types',\nand also a blank line:\n\nplus another line at the end."}
    """

  Scenario: block example with |
    When the following yaml gets parsed as a java.util.Map:
    """
    example: |
      Several lines of text,
      with some "quotes" of various 'types',
      and also a blank line:

      plus another line at the end.



    """
    Then it gets serialized as this json:
    """
    {"example":"Several lines of text,\nwith some \"quotes\" of various 'types',\nand also a blank line:\n\nplus another line at the end.\n"}
    """

  Scenario: block example with |+
    When the following yaml gets parsed as a java.util.Map:
    """
    example: |+
      Several lines of text,
      with some "quotes" of various 'types',
      and also a blank line:

      plus another line at the end.



    """
    Then it gets serialized as this json:
    """
    {"example":"Several lines of text,\nwith some \"quotes\" of various 'types',\nand also a blank line:\n\nplus another line at the end.\n\n\n"}
    """

  Scenario: single quoted flow scalar
    When the following yaml gets parsed as a java.util.Map:
    """
    example: 'Several lines of text,
    containing ''single quotes''. Escapes (like \n) don''t do anything.

    Newlines can be added by leaving a blank line.
      Leading whitespace on lines is ignored.'
    """
    Then it gets serialized as this json:
    """
    {"example":"Several lines of text, containing 'single quotes'. Escapes (like \\n) don't do anything.\nNewlines can be added by leaving a blank line. Leading whitespace on lines is ignored."}
    """

  Scenario: double quoted flow scalar
    When the following yaml gets parsed as a java.util.Map:
    """
    example: "Several lines of text,
    containing \"double quotes\". Escapes (like \\n) work.\nIn addition,
    newlines can be esc\
    aped to prevent them from being converted to a space.

    Newlines can also be added by leaving a blank line.
      Leading whitespace on lines is ignored."
    """
    Then it gets serialized as this json:
    """
    {"example":"Several lines of text, containing \"double quotes\". Escapes (like \\n) work.\nIn addition, newlines can be escaped to prevent them from being converted to a space.\nNewlines can also be added by leaving a blank line. Leading whitespace on lines is ignored."}
    """

  Scenario: Plain flow scalar
    When the following yaml gets parsed as a java.util.Map:
    """
    example: Several lines of text,
      with some "quotes" of various 'types'.
      Escapes (like \n) don't do anything.

      Newlines can be added by leaving a blank line.
        Additional leading whitespace is ignored.
    """
    Then it gets serialized as this json:
    """
    {"example":"Several lines of text, with some \"quotes\" of various 'types'. Escapes (like \\n) don't do anything.\nNewlines can be added by leaving a blank line. Additional leading whitespace is ignored."}
    """

  Scenario: Quote in normal content
    When the following yaml gets parsed as a java.util.Map:
    """
    question: Don't you know?
    """
    Then it gets serialized as this json:
    """
    {"question":"Don't you know?"}
    """

  Scenario: anchor should work in arrays
    When the following yaml gets parsed as a java.util.List:
    """
    - &flag Apple
    - Beachball
    - Cartoon
    - Duckface
    - *flag
    """
    Then it gets serialized as this yaml:
    """
    - Apple
    - Beachball
    - Cartoon
    - Duckface
    - Apple
    """

  Scenario: advanced anchors
    When the following yaml gets parsed as a java.util.Map:
    """
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
    """
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
    When the following yaml gets parsed as a java.util.Map:
    """
    content: *value
    """
    Then an DeserializationException is thrown with a message matching:
    """
    didn't have stored value for anchor 'value' @column: 14 line: 0 character: '￿' @14/15
    """

  Scenario Template: incorrect yaml
    When the following yaml gets parsed as a java.util.Map:
    """
    <input>
    """
    Then an DeserializationException is thrown with a message matching:
    """
    <message>
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
    When the following yaml gets parsed as a java.util.Map:
    """
    content: <input>
    """
    Then it gets serialized as this json:
    """
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
    When the following yaml gets parsed as a java.util.Map:
    """
    content: <input>
    """
    Then it gets serialized as this yaml:
    """
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
    When the following yaml gets parsed as a java.util.Map:
    """
    number: "1234"
    bool: 'true'
    """
    Then it gets serialized as this json:
    """
    {"number":"1234","bool":"true"}
    """

  Scenario: Map of List of List with values with white spaces
    When the following yaml gets parsed as a java.util.Map:
    """
    test:
      - attributes:
        - name: name 1
          value: value 1
        - name: name 2
          value: value 2
    result: true
    """
    Then it gets serialized as this json:
    """
    {"test":[{"attributes":[{"name":"name 1","value":"value 1"},{"name":"name 2","value":"value 2"}]}],"result":true}
    """
