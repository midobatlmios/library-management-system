package com.library.mapper;

import com.library.model.Genre;
import com.library.payload.dto.GenreDTO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GenreMapper {

    /** Flat view: no children. */
    public GenreDTO toDTO(Genre genre) {
        return toDTO(genre, false);
    }

    /** With withTree = true, active sub-genres are included recursively. */
    public GenreDTO toDTO(Genre genre, boolean withTree) {
        if (genre == null) {
            return null;
        }
        GenreDTO dto = GenreDTO.builder()
                .id(genre.getId())
                .code(genre.getCode())
                .name(genre.getName())
                .description(genre.getDescription())
                .displayOrder(genre.getDisplayOrder())
                .active(genre.getActive())
                .createdAt(genre.getCreatedAt())
                .updatedAt(genre.getUpdatedAt())
                .build();
        if (genre.getParentGenre() != null) {
            dto.setParentGenreId(genre.getParentGenre().getId());
            dto.setParentGenreCode(genre.getParentGenre().getCode());
            dto.setParentGenreName(genre.getParentGenre().getName());
        }
        if (withTree) {
            dto.setSubGenres(genre.getSubGenres().stream()
                    .filter(Genre::getActive)
                    .map(child -> toDTO(child, true))
                    .toList());
        }
        return dto;
    }

    public List<GenreDTO> toDTOList(List<Genre> genres) {
        return genres.stream().map(this::toDTO).toList();
    }

    public List<GenreDTO> toTreeList(List<Genre> genres) {
        return genres.stream().map(g -> toDTO(g, true)).toList();
    }

    /** Copies the simple fields. The parent is resolved by the service (it needs the database). */
    public void copySimpleFields(GenreDTO dto, Genre genre) {
        genre.setCode(dto.getCode().trim());
        genre.setName(dto.getName().trim());
        genre.setDescription(dto.getDescription());
        genre.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);
        if (dto.getActive() != null) {
            genre.setActive(dto.getActive());
        }
    }
}
