package io.semla.model;

import io.semla.persistence.annotations.Managed;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;
import java.util.UUID;

@Entity
@Managed
public class VersionedEntity {

    @Id
    public UUID uuid;

    public String name;

    public int value;

    @Version
    public int version;
}
