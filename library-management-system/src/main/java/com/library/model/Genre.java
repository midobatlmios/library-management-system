package com.library.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A book category. Genres form a tree: Fiction -> Mystery -> Detective Fiction ...
 */
@Entity
@Table(indexes = {
        @Index(name = "idx_genre_code", columnList = "code"),
        @Index(name = "idx_genre_name", columnList = "name"),
        @Index(name = "idx_genre_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /** Uppercase system code, e.g. DETECTIVE_FICTION. Unique. */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Builder.Default
    private Integer displayOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_genre_id")
    private Genre parentGenre;

    /** Children, filled from parentGenre (mappedBy), so the tree is always consistent. */
    @OneToMany(mappedBy = "parentGenre")
    @OrderBy("displayOrder ASC, name ASC")
    @Builder.Default
    private List<Genre> subGenres = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ---- helpers from the documentation ----

    public void addSubGenre(Genre child) {
        subGenres.add(child);
        child.setParentGenre(this);
    }

    public void removeSubGenre(Genre child) {
        subGenres.remove(child);
        child.setParentGenre(null);
    }
}
