package com.biblioteca.service;

import com.biblioteca.entity.Prestamo;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

  private final JavaMailSender mailSender;

  @Value("${app.mail.from}")
  private String from;

  @Value("${app.mail.enabled:true}")
  private boolean mailEnabled;

  @Async
  public void enviarRecordatorioDevolucion(Prestamo prestamo) {
    if (!mailEnabled) {
      log.info("[EMAIL SIMULADO] Recordatorio a {}", prestamo.getUsuario().getEmail());
      return;
    }
    String cuerpo = """
        <!DOCTYPE html>
        <html>
        <body style="font-family: sans-serif; max-width: 600px; margin: auto;">
          <div style="background: #1e40af; color: white; padding: 24px; border-radius: 8px 8px 0 0;">
            <h1 style="margin:0">📚 Biblioteca Universitaria</h1>
          </div>
          <div style="padding: 24px; background: #f8fafc; border: 1px solid #e2e8f0;">
            <h2>Recordatorio de devolución</h2>
            <p>Hola <strong>%s</strong>,</p>
            <p>Te recordamos que debes devolver el libro <strong>"%s"</strong> antes del <strong>%s</strong>.</p>
            <p>Por favor asegúrate de devolverlo a tiempo para evitar penalidades.</p>
            <br>
            <p>Gracias,<br>BiblioUNI</p>
          </div>
        </body>
        </html>
        """.formatted(
        prestamo.getUsuario().getNombreCompleto(),
        prestamo.getLibro().getTitulo(),
        prestamo.getFechaDevolucion().toString());

    enviar(prestamo.getUsuario().getEmail(), "📚 Recordatorio de devolución - BiblioUNI", cuerpo); // ✅ era enviarEmail
  }
  // ← ELIMINADA la línea "private final EmailService emailService"

  @Async
  public void enviarAvisoVencimiento(Prestamo prestamo) {
    if (!mailEnabled) {
      log.info("[EMAIL SIMULADO] Aviso vencimiento a {}", prestamo.getUsuario().getEmail());
      return;
    }
    enviar(prestamo.getUsuario().getEmail(),
        "⏰ Tu préstamo vence pronto - Biblioteca",
        buildHtmlAviso(prestamo));
  }

  @Async
  public void enviarNotificacionVencido(Prestamo prestamo) {
    if (!mailEnabled) {
      log.info("[EMAIL SIMULADO] Notificación vencido a {}", prestamo.getUsuario().getEmail());
      return;
    }
    enviar(prestamo.getUsuario().getEmail(),
        "🚨 Préstamo vencido - Acceso bloqueado",
        buildHtmlVencido(prestamo));
  }

  @Async
  public void enviarBienvenida(String email, String nombre) {
    if (!mailEnabled)
      return;
    String cuerpo = """
        <h2>¡Hola, %s!</h2>
        <p>Tu cuenta en la Biblioteca Universitaria ha sido creada exitosamente.</p>
        <p>Puedes explorar nuestro catálogo y solicitar hasta 3 préstamos simultáneos.</p>
        """.formatted(nombre);
    enviar(email, "¡Bienvenido a la Biblioteca! 📚", cuerpo);
  }

  @Async
  public void enviarPrestamoAceptado(Prestamo prestamo) {
    if (!mailEnabled) {
      log.info("[EMAIL SIMULADO] Préstamo aceptado a {}", prestamo.getUsuario().getEmail());
      return;
    }
    String cuerpo = """
        <!DOCTYPE html>
        <html>
        <body style="font-family: sans-serif; max-width: 600px; margin: auto;">
          <div style="background: #16a34a; color: white; padding: 24px; border-radius: 8px 8px 0 0;">
            <h1 style="margin:0">📚 Biblioteca Universitaria</h1>
          </div>
          <div style="padding: 24px; background: #f0fdf4; border: 1px solid #bbf7d0;">
            <h2>¡Tu solicitud fue aceptada!</h2>
            <p>Hola <strong>%s</strong>,</p>
            <p>El libro <strong>"%s"</strong> ya está disponible para ti.</p>
            <p>Fecha límite de devolución: <strong>%s</strong></p>
            %s
          </div>
        </body>
        </html>
        """.formatted(
        prestamo.getUsuario().getNombreCompleto(),
        prestamo.getLibro().getTitulo(),
        prestamo.getFechaDevolucion(),
        buildEnlacesLectura(prestamo));
    enviar(prestamo.getUsuario().getEmail(),
        "✅ Tu solicitud fue aceptada - Biblioteca", cuerpo);
  }

  @Async
  public void enviarPrestamoRechazado(Prestamo prestamo) {
    if (!mailEnabled) {
      log.info("[EMAIL SIMULADO] Préstamo rechazado a {}", prestamo.getUsuario().getEmail());
      return;
    }
    String cuerpo = """
        <!DOCTYPE html>
        <html>
        <body style="font-family: sans-serif; max-width: 600px; margin: auto;">
          <div style="background: #dc2626; color: white; padding: 24px; border-radius: 8px 8px 0 0;">
            <h1 style="margin:0">📚 Biblioteca Universitaria</h1>
          </div>
          <div style="padding: 24px; background: #fef2f2; border: 1px solid #fecaca;">
            <h2>Solicitud no disponible</h2>
            <p>Hola <strong>%s</strong>,</p>
            <p>Lo sentimos, tu solicitud del libro <strong>"%s"</strong>
               no pudo ser procesada en este momento.</p>
            <p>Puedes intentarlo nuevamente más tarde o contactar a la biblioteca.</p>
          </div>
        </body>
        </html>
        """.formatted(
        prestamo.getUsuario().getNombreCompleto(),
        prestamo.getLibro().getTitulo());
    enviar(prestamo.getUsuario().getEmail(),
        "❌ Solicitud no disponible - Biblioteca", cuerpo);
  }

  // ── Privados ──────────────────────────────────────────

  private void enviar(String destinatario, String asunto, String cuerpoHtml) {
    try {
      MimeMessage mensaje = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
      helper.setFrom(from);
      helper.setTo(destinatario);
      helper.setSubject(asunto);
      helper.setText(cuerpoHtml, true);
      mailSender.send(mensaje);
      log.info("Email enviado a {}: {}", destinatario, asunto);
    } catch (MessagingException e) {
      log.error("Error enviando email a {}: {}", destinatario, e.getMessage());
    }
  }

  private String buildEnlacesLectura(Prestamo prestamo) {
    StringBuilder sb = new StringBuilder();
    if (prestamo.getPdfUrl() != null && !prestamo.getPdfUrl().isBlank()) {
      sb.append("""
          <a href="%s" style="background:#16a34a;color:white;padding:10px 20px;
             border-radius:6px;text-decoration:none;display:inline-block;margin:8px 4px 0 0;">
            📄 Leer PDF
          </a>
          """.formatted(prestamo.getPdfUrl()));
    }
    if (prestamo.getHtmlUrl() != null && !prestamo.getHtmlUrl().isBlank()) {
      sb.append("""
          <a href="%s" style="background:#1e40af;color:white;padding:10px 20px;
             border-radius:6px;text-decoration:none;display:inline-block;margin:8px 0 0 0;">
            🌐 Leer Online
          </a>
          """.formatted(prestamo.getHtmlUrl()));
    }
    return sb.toString();
  }

  private String buildHtmlAviso(Prestamo p) {
    return """
        <!DOCTYPE html>
        <html>
        <body style="font-family: sans-serif; max-width: 600px; margin: auto;">
          <div style="background: #1e40af; color: white; padding: 24px; border-radius: 8px 8px 0 0;">
            <h1 style="margin:0">📚 Biblioteca Universitaria</h1>
          </div>
          <div style="padding: 24px; background: #f8fafc; border: 1px solid #e2e8f0;">
            <h2>Tu préstamo vence pronto</h2>
            <p>Hola <strong>%s</strong>,</p>
            <p>El libro <strong>"%s"</strong> debe ser devuelto el <strong>%s</strong>.</p>
            <p>Te quedan <strong>%d día(s)</strong> para devolver y evitar el bloqueo.</p>
          </div>
        </body>
        </html>
        """.formatted(
        p.getUsuario().getNombreCompleto(),
        p.getLibro().getTitulo(),
        p.getFechaDevolucion(),
        p.getDiasRestantes());
  }

  private String buildHtmlVencido(Prestamo p) {
    return """
        <!DOCTYPE html>
        <html>
        <body style="font-family: sans-serif; max-width: 600px; margin: auto;">
          <div style="background: #dc2626; color: white; padding: 24px; border-radius: 8px 8px 0 0;">
            <h1 style="margin:0">⚠️ Préstamo Vencido</h1>
          </div>
          <div style="padding: 24px; background: #fef2f2; border: 1px solid #fecaca;">
            <h2>Tu acceso ha sido bloqueado</h2>
            <p>Hola <strong>%s</strong>,</p>
            <p>El préstamo del libro <strong>"%s"</strong> venció hace <strong>%d día(s)</strong>.</p>
            <p>Acércate a la biblioteca para regularizar tu situación.</p>
          </div>
        </body>
        </html>
        """.formatted(
        p.getUsuario().getNombreCompleto(),
        p.getLibro().getTitulo(),
        p.getDiasMora());
  }
}