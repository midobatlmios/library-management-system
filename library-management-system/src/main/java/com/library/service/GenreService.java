package com.library.service;

import com.library.exception.GenreException;
import com.library.payload.dto.GenreDTO;
import com.library.payload.response.PagesResponse;

import java.util.List;

public interface GenreService {

    GenreDTO createGenre(GenreDTO dto) throws GenreException;

    /** All or nothing. Items may point to a parent created earlier in the same list via parentGenreCode. */
    List<GenreDTO> createGenres(List<GenreDTO> dtos) throws GenreException;

    /** includeInactive: admins see deactivated genres, everyone else gets 404 for them. */
    GenreDTO getGenreById(Long id, boolean includeInactive) throws GenreException;

    GenreDTO getGenreByCode(String code, boolean includeInactive) throws GenreException;

    /** Flat list. Admins get all genres, others only active ones. */
    List<GenreDTO> getAllGenres(boolean includeInactive);

    List<GenreDTO> getActiveGenres();

    /** Top-level active genres, each with its active sub-genres (recursive). */
    List<GenreDTO> getActiveHierarchy();

    /** Top-level active genres, without children. */
    List<GenreDTO> getTopLevelGenres();

    List<GenreDTO> getSubGenres(Long parentId) throws GenreException;

    PagesResponse<GenreDTO> searchGenres(String name, boolean includeInactive, int page, int size,
                                         String sortBy, String sortDir) throws GenreException;

    GenreDTO updateGenre(Long id, GenreDTO dto) throws GenreException;

    void deleteGenre(Long id) throws GenreException;

    void hardDeleteGenre(Long id) throws GenreException;

    long getTotalActiveGenres();

    long getBookCount(Long genreId) throws GenreException;

    boolean isInUse(Long genreId) throws GenreException;
}
