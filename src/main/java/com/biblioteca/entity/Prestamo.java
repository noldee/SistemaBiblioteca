package com.biblioteca.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "prestamos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prestamo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Relaciones @ManyToOne ──────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "libro_id", nullable = false)
    private Libro libro;

    // ── Fechas ─────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "fecha_prestamo", nullable = false, updatable = false)
    private LocalDateTime fechaPrestamo;

    @Column(name = "fecha_devolucion", nullable = false)
    private LocalDate fechaDevolucion;

    @Column(name = "fecha_devolucion_real")
    private LocalDateTime fechaDevolucionReal;

    @Column(name = "hora_devolucion")
    private LocalTime horaDevolucion;

    // ── Estado ─────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoPrestamo estado = EstadoPrestamo.PENDIENTE;

    @Column(name = "aviso_enviado", nullable = false)
    @Builder.Default
    private Boolean avisoEnviado = false;

    @Column(columnDefinition = "TEXT")
    private String notas;

    @Column(name = "cover_url")
    private String coverUrl;

    @Column(name = "pdf_url", columnDefinition = "TEXT")
    private String pdfUrl;

    @Column(name = "html_url", columnDefinition = "TEXT")
    private String htmlUrl;

    @Column(name = "gutendex_id")
    private Integer gutendexId;

    // ── Métodos de negocio ─────────────────────────────────
    public boolean isVencido() {
        return estado == EstadoPrestamo.ACTIVO
                && LocalDate.now().isAfter(fechaDevolucion);
    }

    public boolean isProximoAVencer(int diasAntelacion) {
        LocalDate limite = LocalDate.now().plusDays(diasAntelacion);
        return estado == EstadoPrestamo.ACTIVO
                && !fechaDevolucion.isBefore(LocalDate.now())
                && !fechaDevolucion.isAfter(limite);
    }

    public long getDiasRestantes() {
        return LocalDate.now().until(fechaDevolucion).getDays();
    }

    public long getDiasMora() {
        if (!isVencido())
            return 0;
        return fechaDevolucion.until(LocalDate.now()).getDays();
    }
}