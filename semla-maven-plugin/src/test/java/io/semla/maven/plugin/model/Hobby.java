package io.semla.maven.plugin.model;

import io.semla.persistence.annotations.Managed;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import java.util.List;

@Entity
@Managed
public class Hobby {

    @Id
    @GeneratedValue
    public int id;

    @ManyToMany
    public List<Child> children;

}
