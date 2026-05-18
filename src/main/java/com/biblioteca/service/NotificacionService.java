package com.biblioteca.service;

import com.biblioteca.entity.Notificacion;
import com.biblioteca.repository.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;

    @Transactional(readOnly = true)
    public List<Notificacion> findByUsuario(Long usuarioId) {
        return notificacionRepository.findByUsuarioIdOrderByFechaDesc(usuarioId);
    }

    @Transactional(readOnly = true)
    public long countNoLeidas(Long usuarioId) {
        return notificacionRepository.countNoLeidasByUsuario(usuarioId);
    }

    public void marcarTodasComoLeidas(Long usuarioId) {
        notificacionRepository.marcarTodasComoLeidas(usuarioId);
    }
}