package io.semla.relations;

import io.semla.cucumber.steps.EntitySteps;
import io.semla.model.*;
import io.semla.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class KeyValueRelationsTest {

    private AuthorManager authors;
    private BookManager books;

    @Before
    public void before() {
        authors = EntitySteps.getInstance(AuthorManager.class);
        books = EntitySteps.getInstance(BookManager.class);

        authors.newAuthor("author1")
            .books(Lists.of(Book.withName("book1"), Book.withName("book2"), Book.withName("book3")))
            .create(author -> author.books());
    }

    @Test
    public void fetchTheParentIncludesOnlyTheChildrenReferences() {
        Author author1 = authors.get(1L).get();
        assertThat(author1.books).isNotNull();
        assertThat(author1.books.stream().map(book -> book.id).collect(Collectors.toList())).isEqualTo(Lists.of(1, 2, 3));
        assertThat(EntityModel.isReference(author1.books.get(0))).isTrue();
    }

    @Test
    public void fetchOneParentWithItsChildren() {
        Author author1 = authors.get(1, author -> author.books()).get();
        assertThatauthor1BooksAreFetched(author1);
        author1 = authors.unwrap().get(1L, author -> author.include("books")).get();
        assertThat(author1.name).isEqualTo("author1");
        assertThatauthor1BooksAreFetched(author1);

    }

    @Test
    public void fetchAllTheParentWithTheirChildren() {
        Map<Long, Author> list = authors.get(Lists.of(1L), author -> author.books());
        Author author1 = list.get(1L);
        assertThatauthor1BooksAreFetched(author1);

        list = authors.unwrap().get(Lists.of(1L), author -> author.include("books"));
        author1 = list.get(1L);
        assertThatauthor1BooksAreFetched(author1);
    }

    private void assertThatauthor1BooksAreFetched(Author author1) {
        assertThat(author1.books).isNotNull();
        assertThat(author1.books.stream().map(book -> book.id).collect(Collectors.toList())).isEqualTo(Lists.of(1, 2, 3));
        assertThat(author1.books.stream().noneMatch(EntityModel::isReference)).isTrue();
    }

    @Test
    public void cascadePersist() {
        authors.create(Author.newAuthor("author2", Book.withName("book1")), author -> author.books());
        Author issac_asimov = authors.get(2, author -> author.books()).get();
        assertThat(issac_asimov.books).isNotNull();
        assertThat(issac_asimov.books).size().isEqualTo(1);

        authors.create(
            Lists.of(Author.newAuthor("author3", Book.withName("book1")), Author.newAuthor("author4", Book.withName("book1"))),
            author -> author.books()
        );
    }

    @Test
    public void cascadeMerge() {
        Author author2 = authors.newAuthor("author2").create();
        Book book1 = books.newBook().name("book1").create();
        author2.books = Lists.of(book1);
        authors.update(author2, author -> author.books());
        author2 = authors.get(2, author -> author.books(book -> book.author())).get();
        assertThat(author2.books).isNotNull();
        assertThat(author2.books).size().isEqualTo(1);
        assertThat(author2.books.get(0).author).isNotNull();
        assertThat(author2.books.get(0).author).isEqualTo(author2);
        assertThat(author2.books.get(0).author.name).isEqualTo("author2");

        author2.name = "newNameForAuthor2";
        author2.books.get(0).name = "newNameForBook1";

        authors.update(Lists.of(author2), author -> author.books());
        assertThat(books.get(4).get().name).isEqualTo("newNameForBook1");
    }

    @Test
    public void cascadeRemove() {
        authors.newAuthor("author2")
            .books(Lists.of(Book.withName("book1")))
            .create(author -> author.books());
        assertThat(books.get(4).get().name).isEqualTo("book1");
        authors.delete(2, author -> author.books());
        assertThat(authors.get(2).isPresent()).isFalse();
        assertThat(books.get(4).isPresent()).isFalse();

        authors.delete(Lists.of(1L), author -> author.books());
        assertThat(authors.get(1).isPresent()).isFalse();
        assertThat(books.get(1, 2, 3).values().stream().allMatch(Objects::isNull)).isTrue();
    }

    @After
    public void after() {
        EntitySteps.cleanup();
    }
}
