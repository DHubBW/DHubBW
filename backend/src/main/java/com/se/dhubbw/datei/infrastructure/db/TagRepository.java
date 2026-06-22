package com.se.dhubbw.datei.infrastructure.db;

import com.se.dhubbw.student.infrastructure.db.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for database access to {@link Tag} entities.
 *
 * @architecture.component Dateiverwaltung
 * @architecture.layer Infrastructure Layer (data access layer)
 * @architecture.category T-Software (Spring Data JPA)
 */
@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {

    /**
     * Find a tag by its name (case-insensitive).
     *
     * @param name the tag name to search for
     * @return the matching tag, if any
     */
    @Query("SELECT t FROM Tag t WHERE LOWER(t.name) = LOWER(:name)")
    Optional<Tag> findByNameIgnoreCase(@Param("name") String name);

    /**
     * Retrieve all tags, sorted alphabetically by name.
     *
     * @return all tags, ascending by name
     */
    @Query("SELECT t FROM Tag t ORDER BY t.name ASC")
    List<Tag> findAllOrderByNameAsc();

    /**
     * Set the {@code ersteller} reference to {@code null} for every
     * tag created by the given student. Used by the account-delete
     * flow so deleting a KURSADMIN / SYSTEMADMIN does not break the
     * tag table — the tags themselves stay intact, they just lose
     * their author back-link.
     *
     * @param ersteller the student whose tags to detach
     */
    @Modifying
    @Query("UPDATE Tag t SET t.ersteller = NULL "
        + "WHERE t.ersteller = :ersteller")
    void setErstellerToNullByErsteller(
        @Param("ersteller") Student ersteller);
}
