package io.semla.model;

import io.semla.persistence.annotations.Managed;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@Entity
@Managed
public class Genus implements Serializable {

    @Id
    @GeneratedValue
    public int id;
    public String name;

    @ManyToOne(fetch = FetchType.LAZY)
    public Family family;

    @OneToMany(mappedBy = "genus", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<Fruit> fruits;

}
