package io.semla.maven.plugin.model;

import io.semla.persistence.annotations.Indexed;
import io.semla.persistence.annotations.Managed;
import io.semla.persistence.annotations.StrictIndices;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;

@Entity
@StrictIndices
@Managed
public class Child {

    @Id
    @GeneratedValue
    public int id;

    @Indexed
    public boolean likesChocolate;

    @Indexed
    @NotNull
    public String name;

    public int height;

    @ManyToOne
    public Parent parent;

    @ManyToMany(mappedBy = "children")
    @JoinTable
    public List<Hobby> hobbies;

}
