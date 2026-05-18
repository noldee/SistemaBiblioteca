package com.biblioteca.service;

import com.biblioteca.entity.*;
import com.biblioteca.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PrestamoService {

    private final EmailService emailService;
    private final PrestamoRepository prestamoRepository;
    private final LibroRepository libroRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionRepository notificacionRepository;

    @Value("${app.prestamo.limite-por-usuario:3}")
    private int limitePorUsuario;

    @Value("${app.prestamo.dias-maximo:14}")
    private int diasMaximo;

    // ── Crear préstamo con validaciones ───────────────────
    public Prestamo crear(Long usuarioId, Long libroId, LocalDate fechaDevolucion) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Libro libro = libroRepository.findById(libroId)
                .orElseThrow(() -> new RuntimeException("Libro no encontrado"));

        // VALIDADOR 1: Límite de préstamos por usuario
        long prestamosActivos = prestamoRepository.countActivosByUsuario(usuarioId);
        if (prestamosActivos >= limitePorUsuario) {
            throw new IllegalStateException(
                    "No puedes tener más de " + limitePorUsuario +
                            " libros prestados al mismo tiempo. Tienes " + prestamosActivos + " activos.");
        }

        // VALIDADOR 2: Libro disponible
        if (!libro.isDisponible()) {
            throw new IllegalStateException(
                    "El libro '" + libro.getTitulo() + "' no está disponible actualmente.");
        }

        // VALIDADOR 3: No tener ya ese libro
        if (prestamoRepository.existsPrestamoActivo(usuarioId, libroId)) {
            throw new IllegalStateException(
                    "Ya tienes prestado el libro '" + libro.getTitulo() + "'.");
        }

        // VALIDADOR 4: Fecha de devolución válida
        LocalDate maxFecha = LocalDate.now().plusDays(diasMaximo);
        if (fechaDevolucion.isAfter(maxFecha)) {
            throw new IllegalArgumentException(
                    "La fecha de devolución no puede ser mayor a " + diasMaximo + " días.");
        }

        // Crear préstamo
        libro.prestar();
        libroRepository.save(libro);

        Prestamo prestamo = Prestamo.builder()
                .usuario(usuario)
                .libro(libro)
                .fechaDevolucion(fechaDevolucion)
                .estado(EstadoPrestamo.ACTIVO)
                .build();

        prestamo = prestamoRepository.save(prestamo);

        // Notificación al usuario
        crearNotificacion(usuario, prestamo,
                "PRESTAMO_NUEVO",
                "Préstamo creado: '" + libro.getTitulo() +
                        "'. Devuelve antes del " + fechaDevolucion);

        log.info("Préstamo creado: usuario={} libro={} vence={}", usuarioId, libroId, fechaDevolucion);
        return prestamo;
    }

    // ── Devolver libro ────────────────────────────────────
    public Prestamo devolver(Long prestamoId) {
        Prestamo prestamo = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));

        if (prestamo.getEstado() != EstadoPrestamo.ACTIVO
                && prestamo.getEstado() != EstadoPrestamo.VENCIDO) {
            throw new IllegalStateException("Este préstamo no se puede devolver");
        }

        prestamo.setEstado(EstadoPrestamo.DEVUELTO);
        prestamo.setFechaDevolucionReal(LocalDateTime.now());
        prestamo.getLibro().devolver();

        libroRepository.save(prestamo.getLibro());
        prestamo = prestamoRepository.save(prestamo);

        crearNotificacion(prestamo.getUsuario(), prestamo,
                "DEVOLUCION",
                "Devolución registrada: '" + prestamo.getLibro().getTitulo() + "'. ¡Gracias!");

        log.info("Préstamo devuelto: {}", prestamoId);
        return prestamo;
    }

    // ── Consultas ─────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<Prestamo> findAll() {
        return prestamoRepository.findAllConDetalle();
    }

    @Transactional(readOnly = true)
    public List<Prestamo> findByUsuario(Long usuarioId) {
        return prestamoRepository.findByUsuarioIdConDetalle(usuarioId);
    }

    @Transactional(readOnly = true)
    public List<Prestamo> findActivosByUsuario(Long usuarioId) {
        return prestamoRepository.findActivosByUsuario(usuarioId);
    }

    @Transactional(readOnly = true)
    public Prestamo findById(Long id) {
        return prestamoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public List<Prestamo> findVencidos() {
        return prestamoRepository.findByEstado(EstadoPrestamo.VENCIDO);
    }

    // ── Scheduler support ─────────────────────────────────
    public List<Prestamo> findProximosAVencer(int dias) {
        return prestamoRepository.findProximosAVencer(
                LocalDate.now(), LocalDate.now().plusDays(dias));
    }

    // PrestamoService.java - reemplaza este método
    public List<Prestamo> findVencidosSinActualizar() {
        // Traer todos los ACTIVO y filtrar con isVencido() que ya compara fecha + hora
        return prestamoRepository.findByEstado(EstadoPrestamo.ACTIVO)
                .stream()
                .filter(Prestamo::isVencido)
                .toList();
    }

    public void marcarVencido(Prestamo prestamo) {
        prestamo.setEstado(EstadoPrestamo.VENCIDO);
        prestamoRepository.save(prestamo);

        crearNotificacion(prestamo.getUsuario(), prestamo,
                "PRESTAMO_VENCIDO",
                "⚠️ Tu préstamo de '" + prestamo.getLibro().getTitulo() +
                        "' ha vencido. El acceso ha sido bloqueado.");
    }

    public void marcarAvisoEnviado(Prestamo prestamo) {
        prestamo.setAvisoEnviado(true);
        prestamoRepository.save(prestamo);
    }

    // ── Helpers ───────────────────────────────────────────
    private void crearNotificacion(Usuario usuario, Prestamo prestamo,
            String tipo, String mensaje) {
        notificacionRepository.save(Notificacion.builder()
                .usuario(usuario)
                .prestamo(prestamo)
                .tipo(tipo)
                .mensaje(mensaje)
                .build());
    }

    public Prestamo aceptar(Long prestamoId) {
        Prestamo prestamo = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));

        if (prestamo.getEstado() != EstadoPrestamo.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden aceptar solicitudes pendientes");
        }

        // ✅ si ejemplaresTotal es alto (digital), no decrementar ni validar
        Libro libro = prestamo.getLibro();
        if (libro.getEjemplaresTotal() <= 10) {
            // libro físico — validar disponibilidad
            if (!libro.isDisponible()) {
                throw new IllegalStateException("El libro ya no está disponible");
            }
            libro.prestar();
            libroRepository.save(libro);
        }
        // si ejemplaresTotal > 10 = digital, no tocar ejemplares

        prestamo.setEstado(EstadoPrestamo.ACTIVO);
        prestamo = prestamoRepository.save(prestamo);

        crearNotificacion(prestamo.getUsuario(), prestamo,
                "PRESTAMO_ACEPTADO",
                "✅ Tu solicitud del libro '" + libro.getTitulo() + "' fue aceptada. ¡Ya puedes leerlo!");

        emailService.enviarPrestamoAceptado(
                prestamo.getUsuario().getEmail(),
                prestamo.getUsuario().getNombreCompleto(),
                prestamo.getLibro().getTitulo(),
                prestamo.getFechaDevolucion().toString(),
                prestamo.getPdfUrl(),
                prestamo.getHtmlUrl());

        log.info("Préstamo aceptado: id={} usuario={} libro={}",
                prestamoId, prestamo.getUsuario().getEmail(), libro.getTitulo());

        return prestamo;
    }

    public Prestamo rechazar(Long prestamoId) {
        Prestamo prestamo = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));

        if (prestamo.getEstado() != EstadoPrestamo.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden rechazar solicitudes pendientes");
        }

        prestamo.setEstado(EstadoPrestamo.RECHAZADO);
        prestamo = prestamoRepository.save(prestamo);

        crearNotificacion(prestamo.getUsuario(), prestamo,
                "PRESTAMO_RECHAZADO",
                "❌ Tu solicitud del libro '" + prestamo.getLibro().getTitulo() + "' fue rechazada.");

        emailService.enviarPrestamoRechazado(
                prestamo.getUsuario().getEmail(),
                prestamo.getUsuario().getNombreCompleto(),
                prestamo.getLibro().getTitulo());

        log.info("Préstamo rechazado: id={}", prestamoId);
        return prestamo;
    }

    // ── Eliminar préstamo ─────────────────────────────────
    public void eliminar(Long id) {

        Prestamo prestamo = prestamoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));

        Libro libro = prestamo.getLibro();

        // SOLO manejar stock si es libro físico
        boolean esLibroFisico = libro.getEjemplaresTotal() <= 10;

        // devolver ejemplar únicamente si estaba prestado
        if (esLibroFisico &&
                (prestamo.getEstado() == EstadoPrestamo.ACTIVO
                        || prestamo.getEstado() == EstadoPrestamo.VENCIDO)) {

            // evitar sobrepasar el stock máximo
            if (libro.getEjemplaresDisponibles() < libro.getEjemplaresTotal()) {

                libro.devolver();
                libroRepository.save(libro);
            }
        }

        // eliminar notificaciones asociadas primero
        if (prestamo.getNotificaciones() != null) {
            prestamo.getNotificaciones().clear();
        }

        prestamoRepository.delete(prestamo);

        log.info("Préstamo eliminado correctamente: id={}", id);
    }

    // ── Editar fechas ─────────────────────────────────────
    public Prestamo editar(Long id, LocalDate fechaDevolucion, LocalTime horaDevolucion,
            EstadoPrestamo estado, String notas) {
        Prestamo prestamo = findById(id);

        // si cambia a DEVUELTO desde ACTIVO/VENCIDO, devolver ejemplar
        if (estado == EstadoPrestamo.DEVUELTO
                && (prestamo.getEstado() == EstadoPrestamo.ACTIVO
                        || prestamo.getEstado() == EstadoPrestamo.VENCIDO)) {
            prestamo.getLibro().devolver();
            libroRepository.save(prestamo.getLibro());
            prestamo.setFechaDevolucionReal(LocalDateTime.now());
        }

        // si cambia a ACTIVO desde PENDIENTE, descontar ejemplar
        if (estado == EstadoPrestamo.ACTIVO
                && prestamo.getEstado() == EstadoPrestamo.PENDIENTE) {
            prestamo.getLibro().prestar();
            libroRepository.save(prestamo.getLibro());
        }

        prestamo.setFechaDevolucion(fechaDevolucion);
        prestamo.setHoraDevolucion(horaDevolucion);
        prestamo.setEstado(estado);
        prestamo.setNotas(notas);

        prestamo = prestamoRepository.save(prestamo);
        log.info("Préstamo editado: id={}", id);
        return prestamo;
    }
}