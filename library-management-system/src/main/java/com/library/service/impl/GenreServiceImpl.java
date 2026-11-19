package com.library.service.impl;


import org.springframework.http.HttpStatus;
import com.library.exception.GenreException;
import com.library.mapper.GenreMapper;
import com.library.model.Genre;
import com.library.payload.dto.GenreDTO;
import com.library.repository.GenreRepository;
import com.library.service.GenreService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor

public class GenreServiceImpl implements GenreService {


    private final GenreRepository genreRepository;
    private final GenreMapper genreMapper;



    @Override
    public GenreDTO createGenre(GenreDTO genreDTO) {

        Genre genre= genreMapper.toEntity(genreDTO);

        Genre savedGenre = genreRepository.save(genre);

        return genreMapper.toDTO(savedGenre);
    }

    @Override
    public List<GenreDTO> getAllGenres() {
        return genreRepository.findAll().stream()
                .map(genreMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public GenreDTO getGenreById(Long genreId) throws GenreException {
        Genre genre= genreRepository.findById(genreId).orElseThrow(
                ()-> new GenreException("Genre not found with id: " + genreId, HttpStatus.NOT_FOUND)
        );
        return genreMapper.toDTO(genre);
    }

    @Override
    public GenreDTO updateGenre(Long genreId, GenreDTO genreDTO) throws GenreException {
        Genre existingGenre= genreRepository.findById(genreId).orElseThrow(
                ()-> new GenreException("Genre not found with id: " + genreId, HttpStatus.NOT_FOUND)
        );

        genreMapper.updateEntityFromDTO(genreDTO,existingGenre);

        Genre updateGenre=genreRepository.save(existingGenre);
        return genreMapper.toDTO(updateGenre);
    }

    @Override
    public void deleteGenre(Long genreId) throws GenreException {
        Genre existingGenre= genreRepository.findById(genreId).orElseThrow(
                ()-> new GenreException("Genre not found with id: " + genreId, HttpStatus.NOT_FOUND)
        );
        existingGenre.setActive(false);
        genreRepository.save(existingGenre);

    }

    @Override
    public void hardDeleteGenre(Long genreId) throws GenreException {
        Genre existingGenre= genreRepository.findById(genreId).orElseThrow(
                ()-> new GenreException("Genre not found with id: " + genreId, HttpStatus.NOT_FOUND)
        );
        genreRepository.delete(existingGenre);

    }

    @Override
    public List<GenreDTO> getAllActiveGenresWithSubGenres() {
        List<Genre> toplevelGenres=genreRepository
                .findByParentGenreIsNullAndActiveTrueOrderByDisplayOrderAsc();

        return genreMapper.toDTOList(toplevelGenres) ;
    }

    @Override
    public List<GenreDTO> getTopLevelGenres() {
        List<Genre> toplevelGenres=genreRepository
                .findByParentGenreIsNullAndActiveTrueOrderByDisplayOrderAsc();

        return genreMapper.toDTOList(toplevelGenres) ;
    }

    @Override
    public Page<GenreDTO> searchGenres(String searchTerm, Pageable pageable) {
        return null;
    }

    @Override
    public long getTotalActiveGenres() {
        return genreRepository.countByActiveTrue();
    }


    @Override
    public long getBookCountByGenreId(Long genreId) {
        return 0;
    }

    @Override
    public long getBookCountByGenre(Long genreId) {
        return 0;
    }

    @Override
    public Long getBookCountByGenres(Long id) {
        return 0L;
    }


}
