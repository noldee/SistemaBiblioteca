package com.biblioteca.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit; // Importante para el cálculo de días precisos
import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "prestamo", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Notificacion> notificaciones = new ArrayList<>();

    // ── Métodos de negocio corregidos ───────────────────────

    /**
     * Une la fecha de devolución con su hora exacta para la evaluación del sistema.
     * Si no hay hora definida, se asume el último minuto del día (23:59:59).
     */
    public LocalDateTime getFechaHoraVencimiento() {
        LocalTime hora = (this.horaDevolucion != null) ? this.horaDevolucion : LocalTime.MAX;
        return LocalDateTime.of(this.fechaDevolucion, hora);
    }

    public boolean isVencido() {
        return estado == EstadoPrestamo.ACTIVO
                && LocalDateTime.now().isAfter(getFechaHoraVencimiento());
    }

    public boolean isProximoAVencer(int diasAntelacion) {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime limite = ahora.plusDays(diasAntelacion);
        LocalDateTime vencimiento = getFechaHoraVencimiento();

        return estado == EstadoPrestamo.ACTIVO
                && !vencimiento.isBefore(ahora)
                && !vencimiento.isAfter(limite);
    }

    public long getDiasRestantes() {
        long dias = ChronoUnit.DAYS.between(LocalDate.now(), fechaDevolucion);
        return Math.max(0, dias); // Evita números negativos si el vencimiento es hoy por la tarde
    }

    public long getDiasMora() {
        if (!isVencido()) {
            return 0;
        }
        long dias = ChronoUnit.DAYS.between(fechaDevolucion, LocalDate.now());
        return Math.max(0, dias); // Si vence hoy mismo por horas, devolverá 0 días de mora (mora por horas)
    }
}