package com.biblioteca.service;

import com.biblioteca.entity.Prestamo;
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

  // ─────────────────────────────────────────
  // BIENVENIDA
  // ─────────────────────────────────────────
  @Async
  public void enviarBienvenida(String email, String nombre) {

    String cuerpo = """
        <h1>📚 Bienvenido</h1>

        <p>Hola <b>%s</b></p>

        <p>
        Tu cuenta fue creada correctamente.
        </p>
        """.formatted(nombre);

    enviar(email, "Bienvenido a Biblioteca", cuerpo);
  }

  // ─────────────────────────────────────────
  // PRÉSTAMO ACEPTADO
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
             style="
             background:#16a34a;
             color:white;
             padding:10px 18px;
             border-radius:6px;
             text-decoration:none;
             display:inline-block;">
             📄 Leer PDF
          </a>
          """.formatted(pdfUrl));
    }

    if (htmlUrl != null && !htmlUrl.isBlank()) {

      enlaces.append("""
          <br><br>

          <a href="%s"
             style="
             background:#2563eb;
             color:white;
             padding:10px 18px;
             border-radius:6px;
             text-decoration:none;
             display:inline-block;">
             🌐 Leer Online
          </a>
          """.formatted(htmlUrl));
    }

    String cuerpo = """
        <h1>✅ Solicitud aceptada</h1>

        <p>Hola <b>%s</b></p>

        <p>
        Tu préstamo del libro
        <b>%s</b>
        fue aprobado.
        </p>

        <p>
        Fecha devolución:
        <b>%s</b>
        </p>

        %s
        """.formatted(
        nombre,
        titulo,
        fechaDevolucion,
        enlaces.toString());

    enviar(email, "Préstamo aprobado", cuerpo);
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
        <h1>❌ Solicitud rechazada</h1>

        <p>Hola <b>%s</b></p>

        <p>
        Tu solicitud del libro
        <b>%s</b>
        fue rechazada.
        </p>

        <p>
        Puedes volver a intentarlo más tarde.
        </p>
        """.formatted(nombre, titulo);

    enviar(email, "Solicitud rechazada", cuerpo);
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
        <h1>📚 Recordatorio</h1>

        <p>Hola %s</p>

        <p>
        Debes devolver el libro <b>%s</b>
        antes del %s
        </p>
        """.formatted(
        nombre,
        titulo,
        fechaDevolucion);

    enviar(
        email,
        "Recordatorio devolución",
        cuerpo);
  }

  // ─────────────────────────────────────────
  // PRÉSTAMO VENCIDO
  // ─────────────────────────────────────────
  @Async
  public void enviarNotificacionVencido(Prestamo prestamo) {

    String cuerpo = """
        <h1>⚠️ Préstamo vencido</h1>

        <p>
        Hola <b>%s</b>
        </p>

        <p>
        El préstamo del libro
        <b>%s</b>
        ha vencido.
        </p>

        <p>
        Regulariza tu situación en biblioteca.
        </p>
        """.formatted(
        prestamo.getUsuario().getNombreCompleto(),
        prestamo.getLibro().getTitulo());

    enviar(
        prestamo.getUsuario().getEmail(),
        "Préstamo vencido",
        cuerpo);
  }

  // ─────────────────────────────────────────
  // MÉTODO CENTRAL
  // ─────────────────────────────────────────
  private void enviar(
      String destinatario,
      String asunto,
      String html) {

    if (!mailEnabled) {

      log.warn("⚠️ Emails deshabilitados");

      return;
    }

    if (destinatario == null || destinatario.isBlank()) {

      log.error("❌ Destinatario vacío");

      return;
    }

    try {

      MimeMessage mensaje = mailSender.createMimeMessage();

      MimeMessageHelper helper = new MimeMessageHelper(
          mensaje,
          true,
          "UTF-8");

      helper.setFrom(from);
      helper.setTo(destinatario);
      helper.setSubject(asunto);

      // true = HTML
      helper.setText(html, true);

      mailSender.send(mensaje);

      log.info("✅ Email enviado a {}", destinatario);

    } catch (Exception e) {

      log.error("❌ ERROR ENVIANDO EMAIL");
      log.error("Destino: {}", destinatario);
      log.error("Asunto: {}", asunto);
      log.error("Error: {}", e.getMessage());

      e.printStackTrace();
    }
  }

  @Async
  public void enviarAvisoVencimiento(Prestamo prestamo) {

    String cuerpo = """
        <h1>⏰ Tu préstamo vence pronto</h1>

        <p>
        Hola <b>%s</b>
        </p>

        <p>
        El libro <b>%s</b>
        debe devolverse el día
        <b>%s</b>
        </p>
        """.formatted(
        prestamo.getUsuario().getNombreCompleto(),
        prestamo.getLibro().getTitulo(),
        prestamo.getFechaDevolucion());

    enviar(
        prestamo.getUsuario().getEmail(),
        "Préstamo próximo a vencer",
        cuerpo);
  }

}