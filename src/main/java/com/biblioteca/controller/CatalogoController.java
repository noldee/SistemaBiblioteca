package com.biblioteca.controller;

import com.biblioteca.dto.GutendexResponseDto;

import com.biblioteca.service.GutendexService;
import com.biblioteca.service.NotificacionService;
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

        private final GutendexService gutendexService;
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

                GutendexResponseDto response = gutendexService.buscarLibros(q, page);

                model.addAttribute("libros", response.getResults());
                model.addAttribute("q", q);
                model.addAttribute("page", page);

                model.addAttribute("hasNext",
                                response.getNext() != null);

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