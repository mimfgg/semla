package io.semla.maven.plugin.model;

import io.semla.persistence.annotations.Managed;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Entity
@Managed
public class Pet {

    @Id
    @GeneratedValue
    public int id;

    public String name;

    @OneToOne
    public User user;
}
