package com.biblioteca.service;

import com.biblioteca.entity.Prestamo;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

  private final Resend resend;

  @Value("${app.mail.from}")
  private String from;

  @Value("${app.mail.enabled:true}")
  private boolean mailEnabled;

  public EmailService(@Value("${resend.api.key}") String apiKey) {
    this.resend = new Resend(apiKey);
  }

  // ─────────────────────────────────────────
  // BIENVENIDA
  // ─────────────────────────────────────────
  @Async
  public void enviarBienvenida(String email, String nombre) {
    String cuerpo = """
        <h1>📚 Bienvenido</h1>
        <p>Hola <b>%s</b></p>
        <p>Tu cuenta fue creada correctamente.</p>
        """.formatted(nombre);
    enviar(email, "Bienvenido a Biblioteca", cuerpo);
  }

  // ─────────────────────────────────────────
  // PRÉSTAMO ACEPTADO (versión con parámetros)
  // ─────────────────────────────────────────
  @Async
  public void enviarPrestamoAceptado(
      String email,
      String nombre,
      String titulo,
      String fechaDevolucion,
      String pdfUrl,
      String htmlUrl) {

    StringBuilder enlaces = new StringBuilder();

    if (pdfUrl != null && !pdfUrl.isBlank()) {
      enlaces.append("""
          <br><br>
          <a href="%s"
             style="background:#16a34a;color:white;padding:10px 18px;
                    border-radius:6px;text-decoration:none;display:inline-block;">
             📄 Leer PDF
          </a>
          """.formatted(pdfUrl));
    }

    if (htmlUrl != null && !htmlUrl.isBlank()) {
      enlaces.append("""
          <br><br>
          <a href="%s"
             style="background:#2563eb;color:white;padding:10px 18px;
                    border-radius:6px;text-decoration:none;display:inline-block;">
             🌐 Leer Online
          </a>
          """.formatted(htmlUrl));
    }

    String cuerpo = """
        <!DOCTYPE html>
        <html>
        <body style="font-family:sans-serif;max-width:600px;margin:auto;">
          <div style="background:#16a34a;color:white;padding:24px;border-radius:8px 8px 0 0;">
            <h1 style="margin:0">📚 Biblioteca SuperLibrary</h1>
          </div>
          <div style="padding:24px;background:#f0fdf4;border:1px solid #bbf7d0;">
            <h2>✅ Solicitud aceptada</h2>
            <p>Hola <b>%s</b>,</p>
            <p>Tu préstamo del libro <b>"%s"</b> fue aprobado.</p>
            <p>Fecha límite de devolución: <b>%s</b></p>
            %s
          </div>
        </body>
        </html>
        """.formatted(nombre, titulo, fechaDevolucion, enlaces.toString());

    enviar(email, "✅ Préstamo aprobado - SuperLibrary", cuerpo);
  }

  // ─────────────────────────────────────────
  // PRÉSTAMO ACEPTADO (versión con objeto Prestamo)
  // ─────────────────────────────────────────
  @Async
  public void enviarPrestamoAceptado(Prestamo prestamo) {
    enviarPrestamoAceptado(
        prestamo.getUsuario().getEmail(),
        prestamo.getUsuario().getNombreCompleto(),
        prestamo.getLibro().getTitulo(),
        prestamo.getFechaDevolucion().toString(),
        prestamo.getLibro().getPdfUrl(),
        prestamo.getLibro().getHtmlUrl());
  }

  // ─────────────────────────────────────────
  // PRÉSTAMO RECHAZADO
  // ─────────────────────────────────────────
  @Async
  public void enviarPrestamoRechazado(
      String email,
      String nombre,
      String titulo) {

    String cuerpo = """
        <!DOCTYPE html>
        <html>
        <body style="font-family:sans-serif;max-width:600px;margin:auto;">
          <div style="background:#dc2626;color:white;padding:24px;border-radius:8px 8px 0 0;">
            <h1 style="margin:0">📚 Biblioteca SuperLibrary</h1>
          </div>
          <div style="padding:24px;background:#fef2f2;border:1px solid #fecaca;">
            <h2>❌ Solicitud rechazada</h2>
            <p>Hola <b>%s</b>,</p>
            <p>Tu solicitud del libro <b>"%s"</b> fue rechazada.</p>
            <p>Puedes volver a intentarlo más tarde.</p>
          </div>
        </body>
        </html>
        """.formatted(nombre, titulo);

    enviar(email, "❌ Solicitud rechazada - SuperLibrary", cuerpo);
  }

  // ─────────────────────────────────────────
  // PRÉSTAMO RECHAZADO (versión con objeto Prestamo)
  // ─────────────────────────────────────────
  @Async
  public void enviarPrestamoRechazado(Prestamo prestamo) {
    enviarPrestamoRechazado(
        prestamo.getUsuario().getEmail(),
        prestamo.getUsuario().getNombreCompleto(),
        prestamo.getLibro().getTitulo());
  }

  // ─────────────────────────────────────────
  // RECORDATORIO
  // ─────────────────────────────────────────
  @Async
  public void enviarRecordatorioDevolucion(
      String email,
      String nombre,
      String titulo,
      String fechaDevolucion) {

    String cuerpo = """
        <!DOCTYPE html>
        <html>
        <body style="font-family:sans-serif;max-width:600px;margin:auto;">
          <div style="background:#1e40af;color:white;padding:24px;border-radius:8px 8px 0 0;">
            <h1 style="margin:0">📚 Biblioteca SuperLibrary</h1>
          </div>
          <div style="padding:24px;background:#f8fafc;border:1px solid #e2e8f0;">
            <h2>📚 Recordatorio de devolución</h2>
            <p>Hola <b>%s</b>,</p>
            <p>Debes devolver el libro <b>"%s"</b> antes del <b>%s</b>.</p>
          </div>
        </body>
        </html>
        """.formatted(nombre, titulo, fechaDevolucion);

    enviar(email, "⏰ Recordatorio devolución - SuperLibrary", cuerpo);
  }

  // ─────────────────────────────────────────
  // AVISO VENCIMIENTO (scheduler)
  // ─────────────────────────────────────────
  @Async
  public void enviarAvisoVencimiento(Prestamo prestamo) {
    String cuerpo = """
        <!DOCTYPE html>
        <html>
        <body style="font-family:sans-serif;max-width:600px;margin:auto;">
          <div style="background:#1e40af;color:white;padding:24px;border-radius:8px 8px 0 0;">
            <h1 style="margin:0">📚 Biblioteca SuperLibrary</h1>
          </div>
          <div style="padding:24px;background:#f8fafc;border:1px solid #e2e8f0;">
            <h2>⏰ Tu préstamo vence pronto</h2>
            <p>Hola <b>%s</b>,</p>
            <p>El libro <b>"%s"</b> debe devolverse el <b>%s</b>.</p>
            <p>Te quedan <b>%d día(s)</b> para evitar el bloqueo.</p>
          </div>
        </body>
        </html>
        """.formatted(
        prestamo.getUsuario().getNombreCompleto(),
        prestamo.getLibro().getTitulo(),
        prestamo.getFechaDevolucion(),
        prestamo.getDiasRestantes());

    enviar(prestamo.getUsuario().getEmail(),
        "⏰ Préstamo próximo a vencer - SuperLibrary", cuerpo);
  }

  // ─────────────────────────────────────────
  // NOTIFICACIÓN VENCIDO (scheduler)
  // ─────────────────────────────────────────
  @Async
  public void enviarNotificacionVencido(Prestamo prestamo) {
    String cuerpo = """
        <!DOCTYPE html>
        <html>
        <body style="font-family:sans-serif;max-width:600px;margin:auto;">
          <div style="background:#dc2626;color:white;padding:24px;border-radius:8px 8px 0 0;">
            <h1 style="margin:0">⚠️ Préstamo Vencido</h1>
          </div>
          <div style="padding:24px;background:#fef2f2;border:1px solid #fecaca;">
            <h2>Tu acceso ha sido bloqueado</h2>
            <p>Hola <b>%s</b>,</p>
            <p>El libro <b>"%s"</b> venció hace <b>%d día(s)</b>.</p>
            <p>Acércate a la biblioteca para regularizar tu situación.</p>
          </div>
        </body>
        </html>
        """.formatted(
        prestamo.getUsuario().getNombreCompleto(),
        prestamo.getLibro().getTitulo(),
        prestamo.getDiasMora());

    enviar(prestamo.getUsuario().getEmail(),
        "🚨 Préstamo vencido - SuperLibrary", cuerpo);
  }

  // ─────────────────────────────────────────
  // MÉTODO CENTRAL
  // ─────────────────────────────────────────
  private void enviar(String destinatario, String asunto, String html) {

    if (!mailEnabled) {
      log.warn("⚠️ Emails deshabilitados");
      return;
    }

    if (destinatario == null || destinatario.isBlank()) {
      log.error("❌ Destinatario vacío");
      return;
    }

    try {
      CreateEmailOptions params = CreateEmailOptions.builder()
          .from(from)
          .to(destinatario)
          .subject(asunto)
          .html(html)
          .build();

      resend.emails().send(params);
      log.info("✅ Email enviado a {}: {}", destinatario, asunto);

    } catch (ResendException e) {
      log.error("❌ ERROR ENVIANDO EMAIL");
      log.error("Destino: {}", destinatario);
      log.error("Asunto: {}", asunto);
      log.error("Error: {}", e.getMessage());
    }
  }
}