package io.semla.model;

import io.semla.persistence.annotations.Managed;

import javax.persistence.*;
import java.util.List;

@Entity
@Managed(className = "Families", packageName = "io.semla.persistence")
public class Family {

    @Id
    @GeneratedValue
    public Integer id;
    public String name;

    @OneToMany(mappedBy = "family", cascade = CascadeType.REMOVE)
    public List<Genus> genuses;

    @Embedded
    public Information information;

}
