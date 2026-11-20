package com.library.service.impl;

import com.library.exception.GenreException;
import com.library.mapper.GenreMapper;
import com.library.model.Genre;
import com.library.payload.dto.GenreDTO;
import com.library.payload.response.PagesResponse;
import com.library.repository.BookRepository;
import com.library.repository.GenreRepository;
import com.library.service.GenreService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = GenreException.class)
public class GenreServiceImpl implements GenreService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "name", "code", "displayOrder", "createdAt");
    private static final int MAX_PAGE_SIZE = 100;

    private final GenreRepository genreRepository;
    private final BookRepository bookRepository;
    private final GenreMapper genreMapper;

    // ------------------------------------------------------------------ create

    @Override
    public GenreDTO createGenre(GenreDTO dto) throws GenreException {
        return genreMapper.toDTO(genreRepository.save(buildNew(dto)));
    }

    @Override
    public List<GenreDTO> createGenres(List<GenreDTO> dtos) throws GenreException {
        if (dtos == null || dtos.isEmpty()) {
            throw new GenreException("At least one genre is required");
        }
        Set<String> codesInRequest = new HashSet<>();
        List<Genre> saved = new ArrayList<>();
        for (GenreDTO dto : dtos) {
            if (!codesInRequest.add(dto.getCode())) {
                throw new GenreException("Code '" + dto.getCode() + "' appears twice in the request", HttpStatus.CONFLICT);
            }
            // Saved one by one so a later item can use an earlier one as its parent (parentGenreCode).
            saved.add(genreRepository.saveAndFlush(buildNew(dto)));
        }
        return genreMapper.toDTOList(saved);
    }

    private Genre buildNew(GenreDTO dto) throws GenreException {
        if (genreRepository.existsByCode(dto.getCode())) {
            throw new GenreException("Genre with code '" + dto.getCode() + "' already exists", HttpStatus.CONFLICT);
        }
        Genre genre = new Genre();
        genreMapper.copySimpleFields(dto, genre);
        genre.setParentGenre(resolveParent(dto));
        return genre;
    }

    // ------------------------------------------------------------------ read

    @Override
    @Transactional(readOnly = true)
    public GenreDTO getGenreById(Long id, boolean includeInactive) throws GenreException {
        Genre genre = findGenre(id);
        if (!includeInactive && !genre.getActive()) {
            throw notFound("id: " + id);
        }
        return withBookCount(genreMapper.toDTO(genre, true), id);
    }

    @Override
    @Transactional(readOnly = true)
    public GenreDTO getGenreByCode(String code, boolean includeInactive) throws GenreException {
        Genre genre = genreRepository.findByCode(code.trim().toUpperCase())
                .filter(g -> includeInactive || g.getActive())
                .orElseThrow(() -> notFound("code: " + code));
        return withBookCount(genreMapper.toDTO(genre, true), genre.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<GenreDTO> getAllGenres(boolean includeInactive) {
        return genreMapper.toDTOList(includeInactive
                ? genreRepository.findAllByOrderByDisplayOrderAscNameAsc()
                : genreRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public List<GenreDTO> getActiveGenres() {
        return genreMapper.toDTOList(genreRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public List<GenreDTO> getActiveHierarchy() {
        return genreMapper.toTreeList(genreRepository.findByParentGenreIsNullAndActiveTrueOrderByDisplayOrderAscNameAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public List<GenreDTO> getTopLevelGenres() {
        return genreMapper.toDTOList(genreRepository.findByParentGenreIsNullAndActiveTrueOrderByDisplayOrderAscNameAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public List<GenreDTO> getSubGenres(Long parentId) throws GenreException {
        findGenre(parentId);
        return genreMapper.toDTOList(genreRepository.findByParentGenreIdAndActiveTrueOrderByDisplayOrderAscNameAsc(parentId));
    }

    @Override
    @Transactional(readOnly = true)
    public PagesResponse<GenreDTO> searchGenres(String name, boolean includeInactive, int page, int size,
                                                String sortBy, String sortDir) throws GenreException {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new GenreException("page must be 0 or more and size between 1 and " + MAX_PAGE_SIZE);
        }
        if (!SORTABLE_FIELDS.contains(sortBy)) {
            throw new GenreException("Cannot sort by '" + sortBy + "'. Allowed: " + SORTABLE_FIELDS);
        }
        Sort sort = "DESC".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        String term = name == null || name.isBlank() ? null : name.trim();
        Page<Genre> p = genreRepository.search(term, includeInactive,
                PageRequest.of(page, size, sort.and(Sort.by("id").ascending())));
        return new PagesResponse<>(genreMapper.toDTOList(p.getContent()),
                p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages(),
                p.isLast(), p.isFirst(), p.isEmpty());
    }

    // ------------------------------------------------------------------ update / delete

    /** Full update (PUT): a missing parent means the genre becomes top-level. */
    @Override
    public GenreDTO updateGenre(Long id, GenreDTO dto) throws GenreException {
        Genre genre = findGenre(id);
        if (genreRepository.existsByCodeAndIdNot(dto.getCode(), id)) {
            throw new GenreException("Genre with code '" + dto.getCode() + "' already exists", HttpStatus.CONFLICT);
        }
        Genre newParent = resolveParent(dto);
        if (newParent != null && createsCycle(genre, newParent)) {
            throw new GenreException("A genre cannot be placed under itself or one of its own sub-genres");
        }
        genreMapper.copySimpleFields(dto, genre);
        genre.setParentGenre(newParent);
        return genreMapper.toDTO(genreRepository.save(genre));
    }

    /** Soft delete: hidden from public lists; books keep their genre. Restore with PUT "active": true. */
    @Override
    public void deleteGenre(Long id) throws GenreException {
        Genre genre = findGenre(id);
        genre.setActive(false);
        genreRepository.save(genre);
    }

    /** Permanent. Refused while books or sub-genres still use this genre. */
    @Override
    public void hardDeleteGenre(Long id) throws GenreException {
        Genre genre = findGenre(id);
        long books = bookRepository.countByGenre_Id(id);
        if (books > 0) {
            throw new GenreException("Genre is used by " + books + " book(s). Move them to another genre first, "
                    + "or use soft delete.", HttpStatus.CONFLICT);
        }
        if (genreRepository.existsByParentGenreId(id)) {
            throw new GenreException("Genre has sub-genres. Delete or move them first.", HttpStatus.CONFLICT);
        }
        genreRepository.delete(genre);
    }

    // ------------------------------------------------------------------ statistics

    @Override
    @Transactional(readOnly = true)
    public long getTotalActiveGenres() {
        return genreRepository.countByActiveTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public long getBookCount(Long genreId) throws GenreException {
        findGenre(genreId);
        return bookRepository.countByGenre_Id(genreId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInUse(Long genreId) throws GenreException {
        return getBookCount(genreId) > 0;
    }

    // ------------------------------------------------------------------ helpers

    private Genre findGenre(Long id) throws GenreException {
        return genreRepository.findById(id).orElseThrow(() -> notFound("id: " + id));
    }

    private static GenreException notFound(String what) {
        return new GenreException("Genre not found with " + what, HttpStatus.NOT_FOUND);
    }

    /** Parent from parentGenreId, else parentGenreCode, else none. */
    private Genre resolveParent(GenreDTO dto) throws GenreException {
        if (dto.getParentGenreId() != null) {
            return genreRepository.findById(dto.getParentGenreId())
                    .orElseThrow(() -> new GenreException(
                            "Parent genre not found with id: " + dto.getParentGenreId(), HttpStatus.NOT_FOUND));
        }
        if (dto.getParentGenreCode() != null && !dto.getParentGenreCode().isBlank()) {
            return genreRepository.findByCode(dto.getParentGenreCode().trim())
                    .orElseThrow(() -> new GenreException(
                            "Parent genre not found with code: " + dto.getParentGenreCode(), HttpStatus.NOT_FOUND));
        }
        return null;
    }

    /** True if newParent is the genre itself or one of its descendants. */
    private static boolean createsCycle(Genre genre, Genre newParent) {
        for (Genre g = newParent; g != null; g = g.getParentGenre()) {
            if (g.getId().equals(genre.getId())) {
                return true;
            }
        }
        return false;
    }

    private GenreDTO withBookCount(GenreDTO dto, Long genreId) {
        dto.setBookCount(bookRepository.countByGenre_Id(genreId));
        return dto;
    }
}
