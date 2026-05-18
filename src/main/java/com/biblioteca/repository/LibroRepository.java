package com.biblioteca.repository;

import com.biblioteca.entity.Libro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LibroRepository extends JpaRepository<Libro, Long> {

    Optional<Libro> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);

    List<Libro> findByActivoTrue();

    List<Libro> findByCategoria(String categoria);

    Optional<Libro> findByGutendexId(String gutendexId);

    @Query("""
            SELECT l FROM Libro l
            WHERE l.activo = true
            AND l.ejemplaresDisponibles > 0
            """)
    List<Libro> findDisponibles();

    @Query("""
            SELECT l FROM Libro l
            WHERE l.activo = true
            AND (
                LOWER(l.titulo) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(l.autor) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(l.isbn) LIKE LOWER(CONCAT('%', :q, '%'))
            )
            ORDER BY l.titulo
            """)
    List<Libro> buscar(@Param("q") String q);

    @Query("""
            SELECT l, COUNT(p)
            FROM Libro l
            JOIN l.prestamos p
            WHERE p.fechaPrestamo >= :fechaInicio
            GROUP BY l
            ORDER BY COUNT(p) DESC
            """)
    List<Object[]> findLibrosMasPrestados(
            @Param("fechaInicio") LocalDateTime fechaInicio);

    @Query("""
            SELECT DISTINCT l.categoria
            FROM Libro l
            WHERE l.categoria IS NOT NULL
            ORDER BY l.categoria
            """)
    List<String> findCategorias();
}