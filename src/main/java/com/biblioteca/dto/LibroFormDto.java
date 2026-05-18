package com.biblioteca.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LibroFormDto {
    private Long id;

    @NotBlank(message = "El ISBN es obligatorio")
    private String isbn;

    @NotBlank(message = "El título es obligatorio")
    private String titulo;

    @NotBlank(message = "El autor es obligatorio")
    private String autor;

    private String editorial;
    private Integer anioPublicacion;
    private String descripcion;
    private String categoria;

    @Min(value = 1, message = "Debe haber al menos 1 ejemplar")
    private Integer ejemplaresTotal;

    private String coverUrl;
    private String openLibraryKey;
    private String pdfUrl;
}