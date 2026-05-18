package com.biblioteca.controller;

import com.biblioteca.dto.OpenLibraryResponseDto;

import com.biblioteca.service.NotificacionService;
import com.biblioteca.service.OpenLibraryService;
import com.biblioteca.service.PrestamoService;
import com.biblioteca.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class CatalogoController {

        private final OpenLibraryService openLibraryService;
        private final PrestamoService prestamoService;
        private final UsuarioService usuarioService;
        private final NotificacionService notificacionService;

        // ─────────────────────────────────────────────
        // CATÁLOGO
        // ─────────────────────────────────────────────
        @GetMapping("/catalogo")
        public String catalogo(
                        @RequestParam(required = false) String q,
                        @RequestParam(defaultValue = "1") Integer page,
                        Model model) {

                OpenLibraryResponseDto response = openLibraryService.buscarLibros(q, page);

                model.addAttribute("libros", response.getDocs());
                model.addAttribute("q", q);
                model.addAttribute("page", page);

                // ✅ hay siguiente página si el total supera lo que ya mostramos
                int totalMostrado = page * 15;
                model.addAttribute("hasNext",
                                response.getNumFound() != null && response.getNumFound() > totalMostrado);

                return "user/catalogo";
        }

        // ─────────────────────────────────────────────
        // DETALLE LIBRO
        // ─────────────────────────────────────────────
        @GetMapping("/libros/{id}")
        public String detalle(
                        @PathVariable Long id,
                        Model model) {

                return "redirect:/catalogo";
        }

        // ─────────────────────────────────────────────
        // MIS PRÉSTAMOS
        // ─────────────────────────────────────────────
        @GetMapping("/mis-prestamos")
        public String misPrestamos(
                        @AuthenticationPrincipal UserDetails userDetails,
                        Model model) {

                var usuario = usuarioService.findByEmail(
                                userDetails.getUsername());

                var prestamos = prestamoService.findByUsuario(
                                usuario.getId());

                model.addAttribute("prestamos", prestamos);

                model.addAttribute("usuario", usuario);

                model.addAttribute(
                                "notificacionesCount",
                                notificacionService.countNoLeidas(usuario.getId()));

                return "user/mis-prestamos";
        }

        // ─────────────────────────────────────────────
        // DEVOLVER
        // ─────────────────────────────────────────────
        @PostMapping("/prestamos/{id}/devolver")
        public String devolver(
                        @PathVariable Long id,
                        RedirectAttributes flash) {

                try {

                        prestamoService.devolver(id);

                        flash.addFlashAttribute(
                                        "success",
                                        "Devolución registrada correctamente.");

                } catch (Exception e) {

                        flash.addFlashAttribute(
                                        "error",
                                        e.getMessage());
                }

                return "redirect:/mis-prestamos";
        }
}