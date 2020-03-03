package io.semla.model;

import io.semla.persistence.annotations.Index;
import io.semla.persistence.annotations.Indices;
import io.semla.persistence.annotations.Managed;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Managed
@Indices(
    @Index(properties = {"name", "value"})
)
public class MultipleIndices {

    @Id
    @GeneratedValue
    public int id;

    @Column(unique = true)
    public String name;

    @Column(name = "real_value")
    public String value;
}
