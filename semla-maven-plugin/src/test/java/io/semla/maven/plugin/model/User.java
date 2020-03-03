package io.semla.maven.plugin.model;

import io.semla.persistence.annotations.Managed;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Managed
public class User {

    @Id
    @GeneratedValue
    public int id;

    public String name;
}
