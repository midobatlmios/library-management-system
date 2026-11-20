package com.library.payload.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenreDTO {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotBlank(message = "Genre code is required")
    @Size(min = 2, max = 50, message = "Genre code must be 2-50 characters")
    @Pattern(regexp = "^[A-Z_]+$", message = "Genre code must use only uppercase letters and _ (e.g. DETECTIVE_FICTION)")
    private String code;

    @NotBlank(message = "Genre name is required")
    @Size(min = 2, max = 100, message = "Genre name must be 2-100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Min(value = 0, message = "Display order cannot be negative")
    private Integer displayOrder;

    private Boolean active;

    /** Parent by id ... */
    private Long parentGenreId;

    /** ... or by code (useful in bulk create, where the parent is created in the same request). */
    private String parentGenreCode;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String parentGenreName;

    /** Only filled by endpoints that return the tree. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private List<GenreDTO> subGenres;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long bookCount;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime updatedAt;
}
