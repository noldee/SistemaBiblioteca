package com.biblioteca.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LibroDisponibilidadDto {
    private Long id;
    private String isbn;
    private String titulo;
    private String autor;
    private String editorial;
    private Integer anioPublicacion;
    private Integer ejemplaresDisponibles;
    private Integer ejemplaresTotal;
    private boolean disponible;
    private String coverUrl;
    private String categoria;
}