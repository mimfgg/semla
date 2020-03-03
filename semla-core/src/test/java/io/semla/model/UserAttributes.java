package io.semla.model;

import io.semla.persistence.annotations.Managed;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;


@Entity
@Managed
public class UserAttributes {

    @Id
    @GeneratedValue
    public int id;

    @ManyToOne
    public User user;

    public String name;
    public String value;

}
