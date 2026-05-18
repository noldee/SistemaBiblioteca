package com.biblioteca.repository;

import com.biblioteca.entity.EstadoPrestamo;
import com.biblioteca.entity.Libro;
import com.biblioteca.entity.Prestamo;
import com.biblioteca.entity.Usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {

  List<Prestamo> findByUsuarioId(Long usuarioId);

  List<Prestamo> findByLibroId(Long libroId);

  List<Prestamo> findByEstado(EstadoPrestamo estado);

  // Préstamos activos de un usuario
  @Query("""
      SELECT p FROM Prestamo p
      JOIN FETCH p.libro
      WHERE p.usuario.id = :usuarioId AND p.estado = 'ACTIVO'
      ORDER BY p.fechaDevolucion
      """)
  List<Prestamo> findActivosByUsuario(@Param("usuarioId") Long usuarioId);

  // Contar préstamos activos de un usuario (para validador de límite)
  @Query("""
      SELECT COUNT(p) FROM Prestamo p
      WHERE p.usuario.id = :usuarioId AND p.estado = 'ACTIVO'
      """)
  long countActivosByUsuario(@Param("usuarioId") Long usuarioId);

  // Préstamos próximos a vencer (para notificaciones automáticas)
  @Query("""
      SELECT p FROM Prestamo p
      JOIN FETCH p.usuario
      JOIN FETCH p.libro
      WHERE p.estado = 'ACTIVO'
        AND p.avisoEnviado = false
        AND p.fechaDevolucion BETWEEN :hoy AND :limite
      """)
  List<Prestamo> findProximosAVencer(
      @Param("hoy") LocalDate hoy,
      @Param("limite") LocalDate limite);

  // Préstamos vencidos sin actualizar estado
  @Query("""
      SELECT p FROM Prestamo p
      JOIN FETCH p.usuario
      JOIN FETCH p.libro
      WHERE p.estado = 'ACTIVO'
        AND p.fechaDevolucion < :hoy
      """)
  List<Prestamo> findVencidosSinActualizar(@Param("hoy") LocalDate hoy);

  // Todos los préstamos con sus relaciones cargadas (para admin)
  @Query("""
      SELECT p FROM Prestamo p
      JOIN FETCH p.usuario
      JOIN FETCH p.libro
      ORDER BY p.fechaPrestamo DESC
      """)
  List<Prestamo> findAllConDetalle();

  // Verificar si un usuario ya tiene prestado un libro
  @Query("""
      SELECT COUNT(p) > 0 FROM Prestamo p
      WHERE p.usuario.id = :usuarioId
        AND p.libro.id = :libroId
        AND p.estado = 'ACTIVO'
      """)
  boolean existsPrestamoActivo(
      @Param("usuarioId") Long usuarioId,
      @Param("libroId") Long libroId);

  boolean existsByUsuarioAndLibroAndEstado(
      Usuario usuario,
      Libro libro,
      EstadoPrestamo estado);

  // Todos los préstamos de un usuario con relaciones cargadas
  @Query("""
      SELECT p FROM Prestamo p
      JOIN FETCH p.libro
      WHERE p.usuario.id = :usuarioId
      ORDER BY p.fechaPrestamo DESC
      """)
  List<Prestamo> findByUsuarioIdConDetalle(@Param("usuarioId") Long usuarioId);
}