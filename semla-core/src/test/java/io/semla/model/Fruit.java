package io.semla.model;

import io.semla.persistence.annotations.Indexed;
import io.semla.persistence.annotations.Managed;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Managed
public class Fruit implements Serializable {

    @Id
    @GeneratedValue
    public int id;
    @Indexed
    public String name;
    @Indexed
    public int price;

    @ManyToOne(fetch = FetchType.LAZY)
    public Genus genus;

}