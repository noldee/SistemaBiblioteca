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

        // LOG TEMPORAL - bórralo después
        System.out.println("=== FORM RECIBIDO ===");
        System.out.println("gutendexId: " + form.getGutendexId());
        System.out.println("fechaDevolucion: " + form.getFechaDevolucion());
        System.out.println("horaDevolucion: " + form.getHoraDevolucion());
        System.out.println("email: " + form.getEmail());
        System.out.println("====================");

        // 1. Verificar null ANTES de cualquier comparación
        if (form.getFechaDevolucion() == null) {
            flash.addFlashAttribute("error", "Selecciona una fecha de devolución.");
            return "redirect:/catalogo";
        }

        // 2. Permitir desde HOY en adelante (no mañana, para no confundir)
        if (form.getFechaDevolucion().isBefore(LocalDate.now())) {
            flash.addFlashAttribute("error", "La fecha no puede ser en el pasado.");
            return "redirect:/catalogo";
        }

        if (form.getFechaDevolucion().isAfter(LocalDate.now().plusDays(30))) {
            flash.addFlashAttribute("error", "Máximo 30 días de préstamo.");
            return "redirect:/catalogo";
        }

        // Email
        if (form.getEmail() == null || !form.getEmail().matches(
                "^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            flash.addFlashAttribute("error", "Correo electrónico no válido.");
            return "redirect:/catalogo";
        }

        Usuario usuario = usuarioRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

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
                    .isbn("GUTENDEX-" + form.getGutendexId())
                    .ejemplaresTotal(1)
                    .ejemplaresDisponibles(1)
                    .activo(true)
                    .build();
            libroRepository.save(libro);
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