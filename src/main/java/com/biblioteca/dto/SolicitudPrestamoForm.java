package com.biblioteca.dto;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudPrestamoForm {

    private String gutendexId;  // ✅ era Integer, Open Library usa Strings como "/works/OL34977686W"
    private String titulo;
    private String autor;
    private String cover;
    private String pdfUrl;
    private String htmlUrl;
    private String email;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaDevolucion;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime horaDevolucion;
}