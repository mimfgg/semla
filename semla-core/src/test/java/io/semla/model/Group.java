package io.semla.model;

import io.semla.persistence.annotations.Managed;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;


@Entity
@Managed
public class Group {

    @Id
    @GeneratedValue
    public int id;

    @NotNull
    public String name;

}
