package com.library.payload.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Every filter is optional. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookSearchRequest {
    /** Matches title, author or ISBN. */
    private String searchTerm;
    private Long genreId;
    private Boolean availableOnly = false;
    private Integer page = 0;
    private Integer size = 20;
    private String sortBy = "createdAt";
    private String sortDirection = "DESC";
}
