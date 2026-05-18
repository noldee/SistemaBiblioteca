package com.biblioteca.repository;

import com.biblioteca.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Usuario> findByActivoTrue();

    List<Usuario> findByRol(Usuario.Rol rol);

    // Usuarios con mora (préstamos vencidos) - JPQL
    @Query("""
            SELECT DISTINCT u FROM Usuario u
            JOIN u.prestamos p
            WHERE p.estado = 'ACTIVO'
              AND p.fechaDevolucion < CURRENT_DATE
            ORDER BY u.apellido
            """)
    List<Usuario> findUsuariosConMora();

    // Búsqueda por nombre/email
    @Query("""
            SELECT u FROM Usuario u
            WHERE LOWER(u.nombre) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(u.apellido) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))
            """)
    List<Usuario> buscar(String q);
}