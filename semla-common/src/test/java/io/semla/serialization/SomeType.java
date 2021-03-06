package io.semla.serialization;

import io.semla.serialization.annotations.Deserialize;
import io.semla.serialization.annotations.Serialize;

import java.util.*;

import static io.semla.serialization.annotations.When.NEVER;


public class SomeType {

    public int anInt;
    public Integer anInteger;
    public double aDouble;
    public Double anotherDouble;
    public float aFloat;
    public Float anotherFloat;
    public String aString;
    public boolean aBoolean;
    public Boolean anotherBoolean;

    public List<Float> list;
    public ArrayList<String> arrayList;
    public LinkedList<Integer> linkedList;

    public Set<Float> set;
    public LinkedHashSet<String> linkedHashSet;
    public TreeSet<Integer> treeSet;

    public Map<Integer, String> map;
    public LinkedHashMap<String, String> linkedHashMap;

    private int somePrivateValue;
    private int someReadOnlyValue = 10;

    @Serialize(NEVER)
    @Deserialize(NEVER)
    public String typeThatShouldBeIgnored;

    @Deserialize(NEVER)
    public int getSomeReadOnlyValue() {
        return someReadOnlyValue;
    }

    @Serialize
    public int someCalculation() {
        return someReadOnlyValue * 2;
    }

    @Serialize(as = "doubleThatCalculation")
    public int someCalculation2() {
        return someCalculation() * 2;
    }

    public String someMethodThatShouldNotBeRead() {
        throw new UnsupportedOperationException();
    }
}
