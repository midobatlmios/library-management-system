package com.library.payload.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookDTO {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    /** ISBN-10 or ISBN-13; hyphens and spaces allowed (checked in the service). */
    @NotBlank(message = "ISBN is mandatory")
    @Size(max = 20, message = "ISBN must not exceed 20 characters")
    private String isbn;

    @NotBlank(message = "Title is mandatory")
    @Size(max = 255, message = "Title must be between 1 and 255 characters")
    private String title;

    @NotBlank(message = "Author is mandatory")
    @Size(max = 255, message = "Author name must be between 1 and 255 characters")
    private String author;

    @NotNull(message = "Genre is mandatory")
    private Long genreId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String genreName;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String genreCode;

    @Size(max = 100, message = "Publisher name must not exceed 100 characters")
    private String publisher;

    /** Format: 2018-01-06 */
    private LocalDate publicationDate;

    @Size(max = 20, message = "Language must not exceed 20 characters")
    private String language;

    @Min(value = 1, message = "Pages must be at least 1")
    @Max(value = 50000, message = "Pages must not exceed 50000")
    private Integer pages;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Total copies is mandatory")
    @Min(value = 0, message = "Total copies cannot be negative")
    private Integer totalCopies;

    /** On create: defaults to totalCopies. On update: ignored, the system keeps it in sync with loans. */
    @Min(value = 0, message = "Available copies cannot be negative")
    private Integer availableCopies;

    @DecimalMin(value = "0.0", message = "Price cannot be negative")
    @Digits(integer = 8, fraction = 2, message = "Price can have at most 8 digits and 2 decimals")
    private BigDecimal price;

    @Size(max = 500, message = "Cover image URL must not exceed 500 characters")
    private String coverImageUrl;

    /** On update, false/true hides or restores the book (same as soft delete). */
    private Boolean active;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime updatedAt;
}
