package io.semla.model;

import io.semla.persistence.annotations.Managed;

import javax.persistence.*;

@Entity
@Managed
public class Book {

    @Id
    @GeneratedValue
    public int id;

    public String name;

    @ManyToOne(fetch = FetchType.LAZY)
    public Author author;

    public static Book withName(String name) {
        Book book = new Book();
        book.name = name;
        return book;
    }
}
