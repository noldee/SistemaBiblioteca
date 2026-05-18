package com.biblioteca.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "libros")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "prestamos")
public class Libro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String isbn;

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 300)
    @Column(nullable = false)
    private String titulo;

    @NotBlank(message = "El autor es obligatorio")
    @Size(max = 200)
    @Column(nullable = false)
    private String autor;

    @Size(max = 150)
    private String editorial;

    @Column(name = "anio_publicacion")
    private Integer anioPublicacion;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Size(max = 100)
    private String categoria;

    @Min(value = 1)
    @Column(name = "ejemplares_total", nullable = false)
    @Builder.Default
    private Integer ejemplaresTotal = 1;

    @Min(value = 0)
    @Column(name = "ejemplares_disponibles", nullable = false)
    @Builder.Default
    private Integer ejemplaresDisponibles = 1;

    @Size(max = 500)
    @Column(name = "cover_url")
    private String coverUrl;

    @Size(max = 100)
    @Column(name = "open_library_key")
    private String openLibraryKey;

    @Size(max = 1000)
    @Column(name = "pdf_url")
    private String pdfUrl;

    @Size(max = 1000)
    @Column(name = "html_url")
    private String htmlUrl;

    @Size(max = 100)
    @Column(name = "gutendex_id", unique = true)
    private String gutendexId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    // RELACIONES

    @OneToMany(mappedBy = "libro", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Prestamo> prestamos = new ArrayList<>();

    // MÉTODOS

    public boolean isDisponible() {
        return activo && ejemplaresDisponibles > 0;
    }

    public void prestar() {

        if (!isDisponible()) {
            throw new IllegalStateException("Libro no disponible");
        }

        ejemplaresDisponibles--;
    }

    public void devolver() {

        if (ejemplaresDisponibles < ejemplaresTotal) {
            ejemplaresDisponibles++;
        }
    }

    public String getCoverUrlResolved() {

        if (coverUrl != null && !coverUrl.isBlank()) {
            return coverUrl;
        }

        return "/img/no-cover.png";
    }
}