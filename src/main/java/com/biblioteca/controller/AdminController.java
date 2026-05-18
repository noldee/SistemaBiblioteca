package com.biblioteca.controller;

import com.biblioteca.dto.LibroFormDto;
import com.biblioteca.entity.EstadoPrestamo;
import com.biblioteca.entity.Prestamo;
import com.biblioteca.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final EmailService emailService;
    private final LibroService libroService;
    private final UsuarioService usuarioService;
    private final PrestamoService prestamoService;
    private final ReporteService reporteService;

    // ── Dashboard ─────────────────────────────────────────
    @GetMapping
    public String dashboard(Model model) {
        var todos = prestamoService.findAll();

        model.addAttribute("totalLibros", libroService.findAll().size());
        model.addAttribute("totalUsuarios", usuarioService.findAll().size());
        model.addAttribute("prestamosActivos",
                todos.stream().filter(p -> p.getEstado() == EstadoPrestamo.ACTIVO).count());
        model.addAttribute("prestamosPendientes",
                todos.stream().filter(p -> p.getEstado() == EstadoPrestamo.PENDIENTE).count());
        model.addAttribute("prestamosVencidos",
                todos.stream().filter(p -> p.getEstado() == EstadoPrestamo.VENCIDO).count());
        model.addAttribute("usuariosConMora", usuarioService.findConMora().size());
        model.addAttribute("masPrestados", libroService.findMasPrestados());

        // NUEVO: préstamos vencidos con detalle para la tabla de mora
        model.addAttribute("prestamosVencidosDetalle",
                todos.stream()
                        .filter(p -> p.getEstado() == EstadoPrestamo.VENCIDO)
                        .limit(5) // solo los 5 más recientes en el dashboard
                        .toList());

        return "admin/dashboard";
    }

    // ── Gestión de Libros ─────────────────────────────────
    @GetMapping("/libros")
    public String libros(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("libros", q != null ? libroService.buscar(q) : libroService.findAll());
        model.addAttribute("q", q);
        return "admin/libros";
    }

    @GetMapping("/libros/nuevo")
    public String nuevoLibroForm(Model model) {
        model.addAttribute("libro", new LibroFormDto());
        model.addAttribute("categorias", libroService.findCategorias());
        return "admin/libro-form";
    }

    @PostMapping("/libros/nuevo")
    public String crearLibro(
            @Valid @ModelAttribute("libro") LibroFormDto dto,
            BindingResult result,
            Model model,
            RedirectAttributes flash) {

        if (result.hasErrors()) {
            model.addAttribute("categorias", libroService.findCategorias());
            return "admin/libro-form";
        }
        try {
            libroService.crear(dto);
            flash.addFlashAttribute("success", "Libro creado exitosamente.");
            return "redirect:/admin/libros";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "admin/libro-form";
        }
    }

    @GetMapping("/libros/{id}/editar")
    public String editarLibroForm(@PathVariable Long id, Model model) {
        var libro = libroService.findById(id);
        LibroFormDto dto = LibroFormDto.builder()
                .id(libro.getId())
                .isbn(libro.getIsbn())
                .titulo(libro.getTitulo())
                .autor(libro.getAutor())
                .editorial(libro.getEditorial())
                .anioPublicacion(libro.getAnioPublicacion())
                .descripcion(libro.getDescripcion())
                .categoria(libro.getCategoria())
                .ejemplaresTotal(libro.getEjemplaresTotal())
                .coverUrl(libro.getCoverUrl())
                .openLibraryKey(libro.getOpenLibraryKey())
                .pdfUrl(libro.getPdfUrl())
                .build();
        model.addAttribute("libro", dto);
        model.addAttribute("categorias", libroService.findCategorias());
        return "admin/libro-form";
    }

    @GetMapping("/prestamos/{id}/editar")
    public String editarPrestamoForm(@PathVariable Long id, Model model) {
        model.addAttribute("prestamo", prestamoService.findById(id));
        model.addAttribute("estados", EstadoPrestamo.values());
        return "admin/prestamo-editar";
    }

    @PostMapping("/prestamos/{id}/editar")
    public String editarPrestamo(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDevolucion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaDevolucion,
            @RequestParam EstadoPrestamo estado,
            @RequestParam(required = false) String notas,
            RedirectAttributes flash) {
        try {
            prestamoService.editar(id, fechaDevolucion, horaDevolucion, estado, notas);
            flash.addFlashAttribute("success", "Préstamo actualizado correctamente.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/prestamos";
    }

    @PostMapping("/libros/{id}/editar")
    public String actualizarLibro(
            @PathVariable Long id,
            @Valid @ModelAttribute("libro") LibroFormDto dto,
            BindingResult result,
            Model model,
            RedirectAttributes flash) {

        if (result.hasErrors()) {
            model.addAttribute("categorias", libroService.findCategorias());
            return "admin/libro-form";
        }
        try {
            libroService.actualizar(id, dto);
            flash.addFlashAttribute("success", "Libro actualizado.");
            return "redirect:/admin/libros";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "admin/libro-form";
        }
    }

    @PostMapping("/libros/{id}/eliminar")
    public String eliminarLibro(@PathVariable Long id, RedirectAttributes flash) {
        libroService.eliminar(id);
        flash.addFlashAttribute("success", "Libro eliminado.");
        return "redirect:/admin/libros";
    }

    // Open Library lookup
    @GetMapping("/libros/buscar-isbn")
    @ResponseBody
    public LibroFormDto buscarIsbn(@RequestParam String isbn) {
        return libroService.buscarEnOpenLibrary(isbn);
    }

    // ── Gestión de Usuarios ───────────────────────────────
    @GetMapping("/usuarios")
    public String usuarios(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("usuarios", q != null ? usuarioService.buscar(q) : usuarioService.findAll());
        model.addAttribute("q", q);
        return "admin/usuarios";
    }

    @PostMapping("/usuarios/{id}/toggle")
    public String toggleUsuario(@PathVariable Long id, RedirectAttributes flash) {
        usuarioService.toggleActivo(id);
        flash.addFlashAttribute("success", "Estado del usuario actualizado.");
        return "redirect:/admin/usuarios";
    }

    // ── Gestión de Préstamos ──────────────────────────────
    @GetMapping("/prestamos")
    public String prestamos(
            @RequestParam(required = false) String estado,
            Model model) {

        var todos = prestamoService.findAll();
        if (estado != null && !estado.isBlank()) {
            try {
                EstadoPrestamo estadoEnum = EstadoPrestamo.valueOf(estado.toUpperCase());
                todos = todos.stream()
                        .filter(p -> p.getEstado() == estadoEnum).toList();
            } catch (IllegalArgumentException ignored) {
            }
        }

        model.addAttribute("prestamos", todos);
        model.addAttribute("estadoFiltro", estado);
        model.addAttribute("estados", EstadoPrestamo.values());
        return "admin/prestamos";
    }

    @PostMapping("/prestamos/{id}/devolver")
    public String devolverPrestamo(@PathVariable Long id, RedirectAttributes flash) {
        try {
            prestamoService.devolver(id);
            flash.addFlashAttribute("success", "Devolución registrada.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/prestamos";
    }

    @PostMapping("/prestamos/{id}/aceptar")
    public String aceptarPrestamo(@PathVariable Long id, RedirectAttributes flash) {
        try {
            prestamoService.aceptar(id);
            flash.addFlashAttribute("success", "✅ Préstamo aceptado. El usuario fue notificado.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/prestamos";
    }

    @PostMapping("/prestamos/{id}/notificar")
    public String notificarUsuario(@PathVariable Long id, RedirectAttributes flash) {
        try {
            Prestamo prestamo = prestamoService.findById(id);
            emailService.enviarRecordatorioDevolucion(prestamo); // ✅ envía el correo
            flash.addFlashAttribute("success",
                    "📧 Notificación enviada a " + prestamo.getUsuario().getEmail());
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/prestamos";
    }

    @PostMapping("/prestamos/{id}/rechazar")
    public String rechazarPrestamo(@PathVariable Long id, RedirectAttributes flash) {
        try {
            prestamoService.rechazar(id);
            flash.addFlashAttribute("success", "Solicitud rechazada.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/prestamos";
    }

    @PostMapping("/prestamos/procesar-vencimientos")
    public String procesarVencimientos(RedirectAttributes flash) {
        List<Prestamo> vencidos = prestamoService.findVencidosSinActualizar();
        vencidos.forEach(p -> {
            prestamoService.marcarVencido(p);
            emailService.enviarNotificacionVencido(p);
        });
        flash.addFlashAttribute("success",
                "Procesados " + vencidos.size() + " préstamos vencidos.");
        return "redirect:/admin/prestamos";
    }

    // ── Reportes ──────────────────────────────────────────
    @GetMapping("/reportes")
    public String reportes(Model model) {
        model.addAttribute("masPrestados", libroService.findMasPrestados());
        model.addAttribute("usuariosConMora", usuarioService.findConMora());
        return "admin/reportes";
    }

    @GetMapping("/reportes/pdf")
    public void descargarPdf(
            @RequestParam(defaultValue = "prestamos") String tipo,
            jakarta.servlet.http.HttpServletResponse response) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=reporte-" + tipo + ".pdf");
        reporteService.generarPdf(tipo, response.getOutputStream());
    }

    @GetMapping("/reportes/excel")
    public void descargarExcel(
            @RequestParam(defaultValue = "prestamos") String tipo,
            jakarta.servlet.http.HttpServletResponse response) throws Exception {
        response.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=reporte-" + tipo + ".xlsx");
        reporteService.generarExcel(tipo, response.getOutputStream());
    }

}