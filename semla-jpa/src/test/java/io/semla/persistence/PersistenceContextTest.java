package io.semla.persistence;

import io.semla.cucumber.steps.EntitySteps;
import io.semla.model.Author;
import io.semla.model.AuthorManager;
import io.semla.model.IndexedUser;
import io.semla.model.IndexedUserManager;
import io.semla.util.Lists;
import io.semla.util.Pair;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Test;

import java.util.Collection;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class PersistenceContextTest {

    @Test
    public void sortPersistedNonGeneratedId() {
        IndexedUserManager manager = EntitySteps.getInstance(IndexedUserManager.class);
        IndexedUser indexedUser1 = manager.newIndexedUser(UUID.randomUUID()).create();
        IndexedUser indexedUser2 = manager.newIndexedUser(UUID.randomUUID()).create();
        IndexedUser indexedUser3 = new IndexedUser();
        indexedUser3.uuid = UUID.randomUUID();
        IndexedUser indexedUser4 = new IndexedUser();
        indexedUser4.uuid = UUID.randomUUID();
        Pair<Collection<IndexedUser>, Collection<IndexedUser>> sorted =
            EntitySteps.newContext().sortPersisted(Lists.of(indexedUser2, indexedUser4, indexedUser1, indexedUser3));
        assertThat(sorted.left()).contains(indexedUser1, indexedUser2);
        assertThat(sorted.right()).contains(indexedUser3, indexedUser4);
    }

    @Test
    public void sortPersistedGeneratedId() {
        AuthorManager manager = EntitySteps.getInstance(AuthorManager.class);
        Author author1 = manager.newAuthor("author1").create();
        Author author2 = manager.newAuthor("author2").create();
        Author author3 = new Author();
        Author author4 = new Author();
        Pair<Collection<Author>, Collection<Author>> sorted =
            EntitySteps.newContext().sortPersisted(Lists.of(author2, author4, author1, author3));
        assertThat(sorted.left()).contains(author1, author2);
        assertThat(sorted.right()).contains(author3, author4);
    }

    @Test
    public void sortPersistedEmpty() {
        Pair<Collection<Author>, Collection<Author>> sorted = EntitySteps.newContext().sortPersisted(Lists.empty());
        assertThat(sorted.left()).isNotNull().isEmpty();
        assertThat(sorted.right()).isNotNull().isEmpty();
    }

    @After
    public void after() {
        EntitySteps.cleanup();
    }
}
