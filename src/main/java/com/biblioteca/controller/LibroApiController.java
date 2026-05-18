package com.biblioteca.controller;

import com.biblioteca.dto.LibroDisponibilidadDto;
import com.biblioteca.service.LibroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API REST pública para consultar disponibilidad de libros
 * desde otras aplicaciones.
 */
@RestController
@RequestMapping("/api/libros")
@RequiredArgsConstructor
@Tag(name = "Libros API", description = "Consulta de disponibilidad de libros")
public class LibroApiController {

    private final LibroService libroService;

    @GetMapping("/disponibilidad")
    @Operation(summary = "Listar todos los libros con disponibilidad")
    public List<LibroDisponibilidadDto> listarDisponibilidad() {
        return libroService.findAll().stream()
                .map(libroService::toDisponibilidadDto)
                .toList();
    }

    @GetMapping("/disponibilidad/{id}")
    @Operation(summary = "Consultar disponibilidad de un libro por ID")
    public ResponseEntity<LibroDisponibilidadDto> disponibilidadPorId(@PathVariable Long id) {
        try {
            var libro = libroService.findById(id);
            return ResponseEntity.ok(libroService.toDisponibilidadDto(libro));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/disponibilidad/isbn/{isbn}")
    @Operation(summary = "Consultar disponibilidad por ISBN")
    public ResponseEntity<LibroDisponibilidadDto> disponibilidadPorIsbn(@PathVariable String isbn) {
        return libroService.findAll().stream()
                .filter(l -> l.getIsbn().equals(isbn))
                .findFirst()
                .map(l -> ResponseEntity.ok(libroService.toDisponibilidadDto(l)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/buscar")
    @Operation(summary = "Buscar libros por título, autor o ISBN")
    public List<LibroDisponibilidadDto> buscar(@RequestParam String q) {
        return libroService.buscar(q).stream()
                .map(libroService::toDisponibilidadDto)
                .toList();
    }
}