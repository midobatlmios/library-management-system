package com.library.payload.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenreDTO {
    private Long id;

    @NotBlank(message = "genre code is mandatory")
    private String code ;

    @NotBlank(message = "genre name is mandatory")
    private String name;

    @Size(max = 500 , message = " description must not exceed 500 characters")
    private String description;

    @Min(value=0, message = "display  order cannot be negatie")
    private Integer displayOrder=0;

    private Boolean active;

    private Long parentGenreId;

    private List<GenreDTO> subGenre;

    private Long bookCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void setParentGenreName(@NotBlank(message = "genre name is mandatory") String name) {

    }
}
