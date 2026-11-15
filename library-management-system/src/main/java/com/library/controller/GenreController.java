package com.library.controller;


import com.library.exception.GenreException;
import com.library.model.Genre;
import com.library.payload.dto.GenreDTO;
import com.library.payload.response.ApiResponse;
import com.library.service.GenreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/genres")
public class GenreController {


    private final GenreService genreService;

    @PostMapping("/create")
    public ResponseEntity<GenreDTO> addGenre(@RequestBody GenreDTO genre) {
        GenreDTO createGenre=genreService.createGenre(genre);
        return ResponseEntity.ok(createGenre);

    }

    @GetMapping()
    public ResponseEntity<?> getAllGenres() {
        List<GenreDTO> genres=genreService.getAllGenres();
        return ResponseEntity.ok(genres);

    }
    @GetMapping("/{genreId}")
    public ResponseEntity<?> getGenreById(
            @PathVariable("genreId") Long genreId ) throws GenreException {
        GenreDTO genres=genreService.getGenreById(genreId);
        return ResponseEntity.ok(genres);

    }
    @PutMapping("/{genreId}")
    public ResponseEntity<?> updateGenre(
            @PathVariable("genreId") Long genreId,
            @RequestBody GenreDTO genre
    ) throws GenreException {
        GenreDTO genres=genreService.updateGenre(genreId,genre);
        return ResponseEntity.ok(genres);

    }
    @DeleteMapping("/{genreId}")
    public ResponseEntity<?> DeleteGenre(
            @PathVariable("genreId") Long genreId

    ) throws GenreException {
        genreService.deleteGenre(genreId);
        ApiResponse response=new ApiResponse("genre deleted successfully",true);
        return ResponseEntity.ok(response);

    }
    @DeleteMapping("/{genreId}/hard")
    public ResponseEntity<?> hardDeleteGenre(
            @PathVariable("genreId") Long genreId

            ) throws GenreException {
        genreService.hardDeleteGenre(genreId);
        ApiResponse response=new ApiResponse("genre deleted successfully hard delete",true);
        return ResponseEntity.ok(response);

    }
    @GetMapping("/top-level")
    public ResponseEntity<?> getTopLevelGenres() {
        List<GenreDTO> genres=genreService.getTopLevelGenres();
        return ResponseEntity.ok(genres);

    }
    @GetMapping("/count")
    public ResponseEntity<?> getTotalActiveGenres() {
        Long genres=genreService.getTotalActiveGenres();
        return ResponseEntity.ok(genres);

    }
    @GetMapping("/{id}/book-count")
    public ResponseEntity<?> getBookCountByGenres(
            @PathVariable Long id
    ) {
        Long count=genreService.getBookCountByGenres(id);
        return ResponseEntity.ok(count);

    }


}
