package io.semla.maven.plugin.model;

import io.semla.persistence.annotations.Managed;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.List;

@Entity
@Managed
public class Parent {

    @Id
    @GeneratedValue
    public int id;

    @OneToMany(mappedBy = "parent")
    public List<Child> children;

}
