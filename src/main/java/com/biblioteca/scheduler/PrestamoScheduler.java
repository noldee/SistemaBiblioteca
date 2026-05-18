package com.biblioteca.scheduler;

import com.biblioteca.entity.Prestamo;
import com.biblioteca.service.EmailService;
import com.biblioteca.service.PrestamoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PrestamoScheduler {

    private final PrestamoService prestamoService;
    private final EmailService emailService;

    @Value("${app.prestamo.dias-aviso:2}")
    private int diasAviso;

    /**
     * Ejecuta todos los días a las 8:00 AM.
     * 1. Marca vencidos los préstamos expirados.
     * 2. Envía avisos 2 días antes del vencimiento.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void procesarVencimientos() {
        log.info("=== Scheduler: procesando vencimientos ===");

        // 1. Marcar préstamos como vencidos y bloquear acceso
        List<Prestamo> vencidos = prestamoService.findVencidosSinActualizar();
        log.info("Préstamos vencidos encontrados: {}", vencidos.size());

        vencidos.forEach(p -> {
            try {
                prestamoService.marcarVencido(p);
                emailService.enviarNotificacionVencido(p);
                log.info("Préstamo vencido procesado: id={} usuario={} libro={}",
                        p.getId(), p.getUsuario().getEmail(), p.getLibro().getTitulo());
            } catch (Exception e) {
                log.error("Error procesando préstamo vencido {}: {}", p.getId(), e.getMessage());
            }
        });

        // 2. Enviar avisos previos al vencimiento
        List<Prestamo> proximos = prestamoService.findProximosAVencer(diasAviso);
        log.info("Préstamos próximos a vencer: {}", proximos.size());

        proximos.forEach(p -> {
            try {
                emailService.enviarAvisoVencimiento(p);
                prestamoService.marcarAvisoEnviado(p);
                log.info("Aviso enviado: id={} usuario={} vence={}",
                        p.getId(), p.getUsuario().getEmail(), p.getFechaDevolucion());
            } catch (Exception e) {
                log.error("Error enviando aviso préstamo {}: {}", p.getId(), e.getMessage());
            }
        });

        log.info("=== Scheduler: completado. Vencidos={} Avisos={} ===",
                vencidos.size(), proximos.size());
    }
}