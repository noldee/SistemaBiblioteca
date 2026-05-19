package com.biblioteca.controller;

import com.biblioteca.dto.OpenLibraryResponseDto;

import com.biblioteca.service.NotificacionService;
import com.biblioteca.service.OpenLibraryService;
import com.biblioteca.service.PrestamoService;
import com.biblioteca.service.UsuarioService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
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

                model.addAttribute("hasPrev", page > 1);

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

                // ✅ ACTUALIZAR VENCIDOS PRIMERO
                prestamoService.actualizarPrestamosVencidos();

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

        @GetMapping("/mis-prestamos/estado")
        @ResponseBody
        public ResponseEntity<List<Map<String, Object>>> estadoPrestamos(
                        @AuthenticationPrincipal UserDetails userDetails) {

                var usuario = usuarioService.findByEmail(userDetails.getUsername());
                var prestamos = prestamoService.findByUsuario(usuario.getId());

                List<Map<String, Object>> resultado = prestamos.stream().map(p -> {
                        Map<String, Object> map = new java.util.HashMap<>();
                        map.put("id", p.getId());
                        map.put("estado", p.getEstado().name());
                        map.put("vencido", p.isVencido());
                        map.put("diasRestantes", p.getDiasRestantes());
                        map.put("diasMora", p.getDiasMora());
                        return map;
                }).toList();

                return ResponseEntity.ok(resultado);
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

        // ─────────────────────────────────────────────
        // Notificacion
        // ─────────────────────────────────────────────
        @GetMapping("/notificaciones")
        @ResponseBody
        public ResponseEntity<?> notificaciones(
                        @AuthenticationPrincipal UserDetails userDetails) {
                var usuario = usuarioService.findByEmail(userDetails.getUsername());
                var notifs = notificacionService.findByUsuario(usuario.getId());
                notificacionService.marcarTodasComoLeidas(usuario.getId());
                return ResponseEntity.ok(notifs.stream().limit(5).map(n -> Map.of(
                                "id", n.getId(),
                                "mensaje", n.getMensaje(),
                                "tipo", n.getTipo(),
                                "leida", n.getLeida(),
                                "fecha", n.getFecha().toString())).toList());
        }
}