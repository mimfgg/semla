package io.semla.model;

import io.semla.persistence.annotations.Indexed;
import io.semla.persistence.annotations.StrictIndices;
import io.semla.persistence.annotations.Managed;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.util.UUID;


@Entity
@Managed
@StrictIndices
public class IndexedUser {

    @Id
    public UUID uuid;

    @Indexed
    public String name;

    public int age;

    @ManyToOne
    public Group group;
}