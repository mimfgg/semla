package io.semla.model;

import io.semla.persistence.annotations.Indexed;
import io.semla.persistence.annotations.Managed;

import javax.persistence.Column;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

/**
 * This is a test object containing most of the types and annotations supported by semla.io
 */
@Entity
@Managed
@Table(name = "people")
public class User {

    private static final String FORMAT = "something to reuse";

    public enum EyeColor {brown, blue, green, black}

    public enum Type {admin, user}

    public transient long start;
    @Transient
    public long stop;

    // Primitive Types
    @Id
    @GeneratedValue
    public int id;
    @Column(updatable = false)
    public long created;

    @NotNull
    @Indexed(name = "name_idx", unique = true)
    public String name;
    @Embedded
    @Column(length = 1024)
    public List<String> additionalNames;
    public boolean isCool;
    public char initial;
    public byte mask;
    public int[] powers;
    public short age;
    @Column(name = "rating")
    public float percentage;
    public double height;
    @Temporal(TemporalType.DATE)
    @NotNull
    public java.util.Date birthdate;
    @Temporal(TemporalType.TIME)
    public java.util.Date lastSeen;
    @Temporal(TemporalType.TIMESTAMP)
    public Calendar lastLogin;
    public java.sql.Date sqlDate;
    public java.sql.Time sqlTime;
    public java.sql.Timestamp sqlTimestamp;
    public BigInteger bigInteger;
    public BigDecimal bigDecimal;
    public Calendar calendar;
    public Instant instant;
    public LocalDateTime localDateTime;
    public Optional<String> nickname;
    @Enumerated(EnumType.ORDINAL)
    public Type type;
    @Enumerated(EnumType.STRING)
    public EyeColor eyecolor;

    @Version
    public int version;

}