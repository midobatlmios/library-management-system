package com.library.controller;

import com.library.exception.GenreException;
import com.library.payload.dto.GenreDTO;
import com.library.payload.response.ApiResponse;
import com.library.payload.response.CountResponse;
import com.library.payload.response.PagesResponse;
import com.library.service.GenreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Reading genres is public; creating, changing and deleting is admin only.
 * Admins also see deactivated genres.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/genres")
public class GenreController {

    private final GenreService genreService;

    // ------------------------------------------------------------------ admin: create / update / delete

    /** POST /api/genres (the old path /api/genres/create still works). */
    @PostMapping({"", "/create"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenreDTO> create(@Valid @RequestBody GenreDTO dto) throws GenreException {
        return ResponseEntity.status(HttpStatus.CREATED).body(genreService.createGenre(dto));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<GenreDTO>> createBulk(@RequestBody List<@Valid GenreDTO> dtos) throws GenreException {
        return ResponseEntity.status(HttpStatus.CREATED).body(genreService.createGenres(dtos));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenreDTO> update(@PathVariable Long id, @Valid @RequestBody GenreDTO dto)
            throws GenreException {
        return ResponseEntity.ok(genreService.updateGenre(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> softDelete(@PathVariable Long id) throws GenreException {
        genreService.deleteGenre(id);
        return ResponseEntity.ok(new ApiResponse("Genre deactivated successfully", true));
    }

    @DeleteMapping("/{id}/hard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> hardDelete(@PathVariable Long id) throws GenreException {
        genreService.hardDeleteGenre(id);
        return ResponseEntity.ok(new ApiResponse("Genre permanently deleted", true));
    }

    // ------------------------------------------------------------------ public reads

    @GetMapping
    public ResponseEntity<List<GenreDTO>> getAll() {
        return ResponseEntity.ok(genreService.getAllGenres(isAdmin()));
    }

    @GetMapping("/active")
    public ResponseEntity<List<GenreDTO>> getActive() {
        return ResponseEntity.ok(genreService.getActiveGenres());
    }

    @GetMapping("/active/hierarchy")
    public ResponseEntity<List<GenreDTO>> getHierarchy() {
        return ResponseEntity.ok(genreService.getActiveHierarchy());
    }

    @GetMapping("/top-level")
    public ResponseEntity<List<GenreDTO>> getTopLevel() {
        return ResponseEntity.ok(genreService.getTopLevelGenres());
    }

    /** Example: /api/genres/search?name=fiction&page=0&size=10&sortBy=name&sortDir=ASC */
    @GetMapping("/search")
    public ResponseEntity<PagesResponse<GenreDTO>> search(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) throws GenreException {
        return ResponseEntity.ok(genreService.searchGenres(name, isAdmin(), page, size, sortBy, sortDir));
    }

    @GetMapping("/count")
    public ResponseEntity<CountResponse> countActive() {
        return ResponseEntity.ok(new CountResponse(genreService.getTotalActiveGenres()));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<GenreDTO> getByCode(@PathVariable String code) throws GenreException {
        return ResponseEntity.ok(genreService.getGenreByCode(code, isAdmin()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GenreDTO> getById(@PathVariable Long id) throws GenreException {
        return ResponseEntity.ok(genreService.getGenreById(id, isAdmin()));
    }

    @GetMapping("/{parentId}/sub-genres")
    public ResponseEntity<List<GenreDTO>> getSubGenres(@PathVariable Long parentId) throws GenreException {
        return ResponseEntity.ok(genreService.getSubGenres(parentId));
    }

    @GetMapping("/{id}/book-count")
    public ResponseEntity<CountResponse> bookCount(@PathVariable Long id) throws GenreException {
        return ResponseEntity.ok(new CountResponse(genreService.getBookCount(id)));
    }

    @GetMapping("/{id}/in-use")
    public ResponseEntity<Map<String, Boolean>> inUse(@PathVariable Long id) throws GenreException {
        return ResponseEntity.ok(Map.of("inUse", genreService.isInUse(id)));
    }

    private static boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
