Feature: Query parsing

  Background:
    Given that the model of io.semla.model.Fruit is generated
    And that we create those fruits:
      | name   | price |
      | banana | 1     |
      | apple  | 2     |
      | peach  | 5     |

  Scenario: count
    Then counting the fruits returns 3
    And counting the fruits where price lessThan 3 returns 2
    And counting the fruits where price is 5 returns 1
    And counting the fruits where name contains "e" returns 2
    And counting the fruits where name contains "a" and price is 1 returns 1

  Scenario: first
    Then fetching the first fruit returns:
      | id | name   | price |
      | 1  | banana | 1     |
    And fetching the first fruit where price is 5 returns:
      | id | name  | price |
      | 3  | peach | 5     |
    And fetching the first fruit where name is "apple" returns:
      | id | name  | price |
      | 2  | apple | 2     |

  Scenario: list
    Then listing the fruits returns:
      | id | name   | price |
      | 1  | banana | 1     |
      | 2  | apple  | 2     |
      | 3  | peach  | 5     |
    And listing the fruits where name contains "e" returns:
      | id | name  | price |
      | 2  | apple | 2     |
      | 3  | peach | 5     |
    And listing the fruits where id in [2, 3] returns:
      | id | name  | price |
      | 2  | apple | 2     |
      | 3  | peach | 5     |

  Scenario: update
    When we patch the fruits where name contains "e" with:
    """
    price: 4
    """
    Then counting the fruits where price is 4 returns 2

  Scenario: order
    Then listing the fruits ordered by name returns:
      | id | name   | price |
      | 2  | apple  | 2     |
      | 1  | banana | 1     |
      | 3  | peach  | 5     |
    And listing the fruits ordered by price desc returns:
      | id | name   | price |
      | 3  | peach  | 5     |
      | 2  | apple  | 2     |
      | 1  | banana | 1     |

  Scenario: start and limit
    Then listing the fruits ordered by price desc start at 1 returns:
      | id | name   | price |
      | 2  | apple  | 2     |
      | 1  | banana | 1     |
    And listing the fruits ordered by price desc start at 1 limit to 1 returns:
      | id | name  | price |
      | 2  | apple | 2     |

