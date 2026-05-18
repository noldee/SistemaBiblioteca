package com.biblioteca.service;

import com.biblioteca.dto.RegistroUsuarioDto;
import com.biblioteca.entity.Usuario;
import com.biblioteca.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    // ── Registro ───────────────────────────────────────────
    public Usuario registrar(RegistroUsuarioDto dto) {
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Ya existe un usuario con ese email");
        }
        if (!dto.getPassword().equals(dto.getConfirmarPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }

        Usuario usuario = Usuario.builder()
                .nombre(dto.getNombre())
                .apellido(dto.getApellido())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .rol(Usuario.Rol.USUARIO)
                .codigoAlumno(dto.getCodigoAlumno())
                .carrera(dto.getCarrera())
                .build();

        log.info("Nuevo usuario registrado: {}", dto.getEmail());
        return usuarioRepository.save(usuario);
    }

    // ── Consultas ──────────────────────────────────────────
    @Transactional(readOnly = true)
    public Usuario findById(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public Usuario findByEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + email));
    }

    @Transactional(readOnly = true)
    public List<Usuario> findAll() {
        return usuarioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Usuario> findConMora() {
        return usuarioRepository.findUsuariosConMora();
    }

    @Transactional(readOnly = true)
    public List<Usuario> buscar(String q) {
        return usuarioRepository.buscar(q);
    }

    // ── Actualización ──────────────────────────────────────
    public Usuario actualizar(Long id, RegistroUsuarioDto dto) {
        Usuario usuario = findById(id);
        usuario.setNombre(dto.getNombre());
        usuario.setApellido(dto.getApellido());
        usuario.setCarrera(dto.getCarrera());
        usuario.setCodigoAlumno(dto.getCodigoAlumno());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        return usuarioRepository.save(usuario);
    }

    public void toggleActivo(Long id) {
        Usuario usuario = findById(id);
        usuario.setActivo(!usuario.getActivo());
        usuarioRepository.save(usuario);
        log.info("Usuario {} -> activo={}", id, usuario.getActivo());
    }

    public void eliminar(Long id) {
        usuarioRepository.deleteById(id);
        log.info("Usuario eliminado: {}", id);
    }
}