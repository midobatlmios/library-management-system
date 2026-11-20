package com.library.repository;

import com.library.model.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GenreRepository extends JpaRepository<Genre, Long> {

    Optional<Genre> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    List<Genre> findAllByOrderByDisplayOrderAscNameAsc();

    List<Genre> findByActiveTrueOrderByDisplayOrderAscNameAsc();

    List<Genre> findByParentGenreIsNullAndActiveTrueOrderByDisplayOrderAscNameAsc();

    List<Genre> findByParentGenreIdAndActiveTrueOrderByDisplayOrderAscNameAsc(Long parentGenreId);

    boolean existsByParentGenreId(Long parentGenreId);

    long countByActiveTrue();

    /** Name or code contains the text (case-insensitive). Admins may include inactive genres. */
    @Query("select g from Genre g where " +
            "(:name is null or lower(g.name) like lower(concat('%', :name, '%')) " +
            "   or lower(g.code) like lower(concat('%', :name, '%'))) and " +
            "(:includeInactive = true or g.active = true)")
    Page<Genre> search(@Param("name") String name,
                       @Param("includeInactive") boolean includeInactive,
                       Pageable pageable);
}
