package com.biblioteca.controller;

import com.biblioteca.dto.SolicitudPrestamoForm;
import com.biblioteca.entity.EstadoPrestamo;
import com.biblioteca.entity.Libro;
import com.biblioteca.entity.Prestamo;
import com.biblioteca.entity.Usuario;
import com.biblioteca.repository.LibroRepository;
import com.biblioteca.repository.PrestamoRepository;
import com.biblioteca.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class PrestamoController {

    private final PrestamoRepository prestamoRepository;
    private final LibroRepository libroRepository;
    private final UsuarioRepository usuarioRepository;

    // =========================================
    // FORMULARIO
    // =========================================

    @PostMapping("/prestamos/formulario")
    public String formularioPrestamo(
            @ModelAttribute SolicitudPrestamoForm form,
            Model model) {

        if (form.getGutendexId() == null || form.getTitulo() == null) {
            return "redirect:/catalogo";
        }

        model.addAttribute("form", form);
        return "user/formulario-prestamo";
    }

    // =========================================
    // SOLICITAR
    // =========================================

    @PostMapping("/prestamos/solicitar")
    public String solicitarPrestamo(
            @ModelAttribute SolicitudPrestamoForm form,
            Authentication authentication,
            RedirectAttributes flash) {

        if (form.getFechaDevolucion() == null) {
            flash.addFlashAttribute("error", "Selecciona una fecha de devolución.");
            return "redirect:/catalogo";
        }

        if (form.getFechaDevolucion().isBefore(LocalDate.now())) {
            flash.addFlashAttribute("error", "La fecha no puede ser en el pasado.");
            return "redirect:/catalogo";
        }

        if (form.getFechaDevolucion().isAfter(LocalDate.now().plusDays(30))) {
            flash.addFlashAttribute("error", "Máximo 30 días de préstamo.");
            return "redirect:/catalogo";
        }

        if (form.getEmail() == null || !form.getEmail().matches(
                "^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            flash.addFlashAttribute("error", "Correo electrónico no válido.");
            return "redirect:/catalogo";
        }

        Usuario usuario = usuarioRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // ✅ buscar libro existente o crear uno nuevo — cualquier usuario puede pedir el
        // mismo libro
        Libro libro = libroRepository
                .findByGutendexId(form.getGutendexId())
                .orElse(null);

        if (libro == null) {
            libro = Libro.builder()
                    .gutendexId(form.getGutendexId())
                    .titulo(form.getTitulo())
                    .autor(form.getAutor())
                    .coverUrl(form.getCover())
                    .pdfUrl(form.getPdfUrl())
                    .htmlUrl(form.getHtmlUrl())
                    .isbn("OL-" + form.getGutendexId().replace("/works/", ""))
                    .ejemplaresTotal(99) // ✅ digital = ejemplares ilimitados
                    .ejemplaresDisponibles(99) // ✅ cualquiera puede pedirlo
                    .activo(true)
                    .build();
            libroRepository.save(libro);
        }

        // ✅ verificar que el mismo usuario no tenga ya ese libro PENDIENTE o ACTIVO
        boolean yaLoTiene = prestamoRepository
                .existsByUsuarioIdAndLibroIdAndEstadoIn(
                        usuario.getId(),
                        libro.getId(),
                        List.of(EstadoPrestamo.PENDIENTE, EstadoPrestamo.ACTIVO));

        if (yaLoTiene) {
            flash.addFlashAttribute("error", "Ya tienes una solicitud activa o pendiente para este libro.");
            return "redirect:/catalogo";
        }

        Prestamo prestamo = Prestamo.builder()
                .usuario(usuario)
                .libro(libro)
                .fechaDevolucion(form.getFechaDevolucion())
                .horaDevolucion(form.getHoraDevolucion())
                .estado(EstadoPrestamo.PENDIENTE)
                .notas("Email: " + form.getEmail() + " | Solicitud desde catálogo")
                .build();

        prestamoRepository.save(prestamo);

        flash.addFlashAttribute("success",
                "✅ Solicitud enviada. Espera a que un administrador la acepte.");

        return "redirect:/catalogo";
    }
}