package io.semla.model;

import io.semla.persistence.annotations.Managed;
import io.semla.util.Lists;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;

@Entity
@Managed
public class Author {

    @Id
    @GeneratedValue
    public long id;

    @NotNull
    public String name;

    @Embedded
    @OneToMany(mappedBy = "author")
    @SuppressWarnings({"JpaAttributeTypeInspection", "JpaAttributeMemberSignatureInspection"}) // we want to store the relation on the owning side
    public List<Book> books;

    public static Author newAuthor(String name, Book... books) {
        Author author = new Author();
        author.name = name;
        author.books = Lists.fromArray(books);
        return author;
    }

}
