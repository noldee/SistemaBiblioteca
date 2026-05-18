package com.biblioteca.service;

import com.biblioteca.dto.LibroDisponibilidadDto;
import com.biblioteca.dto.LibroFormDto;
import com.biblioteca.entity.Libro;
import com.biblioteca.repository.LibroRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LibroService {

    private final LibroRepository libroRepository;
    private final WebClient.Builder webClientBuilder;

    // ── Consultas ──────────────────────────────────────────
    @Transactional(readOnly = true)
    @Cacheable("libros")
    public List<Libro> findAll() {
        return libroRepository.findByActivoTrue();
    }

    @Transactional(readOnly = true)
    public Libro findById(Long id) {
        return libroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Libro no encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public List<Libro> buscar(String q) {
        if (q == null || q.isBlank())
            return findAll();
        return libroRepository.buscar(q);
    }

    @Transactional(readOnly = true)
    public List<Libro> findDisponibles() {
        return libroRepository.findDisponibles();
    }

    @Transactional(readOnly = true)
    public List<String> findCategorias() {
        return libroRepository.findCategorias();
    }

    @Transactional(readOnly = true)
    public List<Libro> findMasPrestados() {
        LocalDateTime inicio = LocalDateTime.now().minusMonths(1);
        return libroRepository.findLibrosMasPrestados(inicio)
                .stream()
                .map(row -> (Libro) row[0])
                .limit(5)
                .toList();
    }

    // Para reportes PDF/Excel (necesita libro + conteo)
    @Transactional(readOnly = true)
    public List<Object[]> findMasPrestadosConConteo() {
        LocalDateTime inicio = LocalDateTime.now().minusMonths(1);
        return libroRepository.findLibrosMasPrestados(inicio);
    }

    // ── CRUD ───────────────────────────────────────────────
    @CacheEvict(value = "libros", allEntries = true)
    public Libro crear(LibroFormDto dto) {
        if (libroRepository.existsByIsbn(dto.getIsbn())) {
            throw new IllegalArgumentException("Ya existe un libro con ese ISBN");
        }
        Libro libro = mapDtoToLibro(new Libro(), dto);
        log.info("Libro creado: {} - {}", dto.getIsbn(), dto.getTitulo());
        return libroRepository.save(libro);
    }

    @CacheEvict(value = "libros", allEntries = true)
    public Libro actualizar(Long id, LibroFormDto dto) {
        Libro libro = findById(id);
        mapDtoToLibro(libro, dto);
        log.info("Libro actualizado: {}", id);
        return libroRepository.save(libro);
    }

    @CacheEvict(value = "libros", allEntries = true)
    public void eliminar(Long id) {
        Libro libro = findById(id);
        libro.setActivo(false);
        libroRepository.save(libro);
        log.info("Libro desactivado: {}", id);
    }

    // ── Integración Open Library ───────────────────────────
    @SuppressWarnings("unchecked")
    public LibroFormDto buscarEnOpenLibrary(String isbn) {
        try {
            WebClient client = webClientBuilder.baseUrl("https://openlibrary.org").build();

            Map<String, Object> respuesta = client.get()
                    .uri("/api/books?bibkeys=ISBN:{isbn}&format=json&jscmd=data", isbn)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (respuesta == null || respuesta.isEmpty())
                return null;

            Map<String, Object> bookData = (Map<String, Object>) respuesta.get("ISBN:" + isbn);
            if (bookData == null)
                return null;

            LibroFormDto dto = new LibroFormDto();
            dto.setIsbn(isbn);
            dto.setTitulo((String) bookData.get("title"));

            // Autor
            List<Map<String, String>> authors = (List<Map<String, String>>) bookData.get("authors");
            if (authors != null && !authors.isEmpty()) {
                dto.setAutor(authors.stream()
                        .map(a -> a.get("name"))
                        .collect(Collectors.joining(", ")));
            }

            // Editorial
            List<Map<String, String>> publishers = (List<Map<String, String>>) bookData.get("publishers");
            if (publishers != null && !publishers.isEmpty()) {
                dto.setEditorial(publishers.get(0).get("name"));
            }

            // Año
            String publishDate = (String) bookData.get("publish_date");
            if (publishDate != null && publishDate.matches(".*\\d{4}.*")) {
                dto.setAnioPublicacion(Integer.parseInt(
                        publishDate.replaceAll(".*?(\\d{4}).*", "$1")));
            }

            // Portada
            Map<String, String> cover = (Map<String, String>) bookData.get("cover");
            if (cover != null) {
                dto.setCoverUrl(cover.getOrDefault("medium", cover.get("large")));
            } else {
                dto.setCoverUrl("https://covers.openlibrary.org/b/isbn/" + isbn + "-M.jpg");
            }

            // Key Open Library
            Map<String, String> key = (Map<String, String>) bookData.get("key");
            if (key != null)
                dto.setOpenLibraryKey(key.get("key"));

            return dto;

        } catch (Exception e) {
            log.warn("Error consultando Open Library para ISBN {}: {}", isbn, e.getMessage());
            return null;
        }
    }

    // ── API REST DTO ───────────────────────────────────────
    public LibroDisponibilidadDto toDisponibilidadDto(Libro libro) {
        return LibroDisponibilidadDto.builder()
                .id(libro.getId())
                .isbn(libro.getIsbn())
                .titulo(libro.getTitulo())
                .autor(libro.getAutor())
                .editorial(libro.getEditorial())
                .anioPublicacion(libro.getAnioPublicacion())
                .ejemplaresDisponibles(libro.getEjemplaresDisponibles())
                .ejemplaresTotal(libro.getEjemplaresTotal())
                .disponible(libro.isDisponible())
                .coverUrl(libro.getCoverUrlResolved())
                .categoria(libro.getCategoria())
                .build();
    }

    // ── Helpers ────────────────────────────────────────────
    private Libro mapDtoToLibro(Libro libro, LibroFormDto dto) {
        libro.setIsbn(dto.getIsbn());
        libro.setTitulo(dto.getTitulo());
        libro.setAutor(dto.getAutor());
        libro.setEditorial(dto.getEditorial());
        libro.setAnioPublicacion(dto.getAnioPublicacion());
        libro.setDescripcion(dto.getDescripcion());
        libro.setCategoria(dto.getCategoria());
        if (dto.getEjemplaresTotal() != null) {
            libro.setEjemplaresTotal(dto.getEjemplaresTotal());
            if (libro.getEjemplaresDisponibles() == null)
                libro.setEjemplaresDisponibles(dto.getEjemplaresTotal());
        }
        libro.setCoverUrl(dto.getCoverUrl());
        libro.setOpenLibraryKey(dto.getOpenLibraryKey());
        libro.setPdfUrl(dto.getPdfUrl());
        return libro;
    }
}