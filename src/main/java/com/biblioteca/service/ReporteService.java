package com.biblioteca.service;

import com.biblioteca.entity.EstadoPrestamo;
import com.biblioteca.entity.Libro;
import com.biblioteca.entity.Prestamo;
import com.biblioteca.entity.Usuario;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReporteService {

    private final PrestamoService prestamoService;
    private final UsuarioService usuarioService;
    private final LibroService libroService;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // =========================================================
    // PDF
    // =========================================================

    public void generarPdf(String tipo, OutputStream out) throws Exception {

        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);

        try (Document document = new Document(pdf)) {

            Paragraph titulo = new Paragraph(getTitulo(tipo))
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.DARK_GRAY);

            document.add(titulo);
            document.add(new Paragraph(" "));

            switch (tipo) {
                case "prestamos" -> agregarTablaPrestamos(document);
                case "mora" -> agregarTablaMora(document);
                default -> agregarTablaLibrosMasPrestados(document);
            }

            log.info("PDF generado: tipo={}", tipo);
        }
    }

    private void agregarTablaPrestamos(Document doc) {

        List<Prestamo> prestamos = prestamoService.findAll();

        Table table = new Table(
                UnitValue.createPercentArray(new float[] { 2, 3, 3, 2, 2 })).useAllAvailableWidth();

        String[] headers = { "ID", "Usuario", "Libro", "Vence", "Estado" };

        for (String h : headers) {
            table.addHeaderCell(
                    new com.itextpdf.layout.element.Cell()
                            .add(new Paragraph(h).setBold())
                            .setBackgroundColor(ColorConstants.LIGHT_GRAY));
        }

        for (Prestamo p : prestamos) {

            table.addCell(String.valueOf(p.getId()));

            table.addCell(
                    p.getUsuario() != null
                            ? p.getUsuario().getNombreCompleto()
                            : "-");

            table.addCell(
                    p.getLibro() != null
                            ? p.getLibro().getTitulo()
                            : "-");

            table.addCell(
                    p.getFechaDevolucion() != null
                            ? p.getFechaDevolucion().format(FMT)
                            : "-");

            table.addCell(
                    p.getEstado() != null
                            ? p.getEstado().name()
                            : "-");
        }

        doc.add(table);

        doc.add(
                new Paragraph("Total: " + prestamos.size() + " préstamos")
                        .setItalic());
    }

    private void agregarTablaMora(Document doc) {

        List<Usuario> usuarios = usuarioService.findConMora();

        Table table = new Table(
                UnitValue.createPercentArray(new float[] { 3, 3, 2 })).useAllAvailableWidth();

        String[] headers = { "Nombre", "Email", "Libros en mora" };

        for (String h : headers) {

            table.addHeaderCell(
                    new com.itextpdf.layout.element.Cell()
                            .add(new Paragraph(h).setBold())
                            .setBackgroundColor(ColorConstants.LIGHT_GRAY));
        }

        for (Usuario u : usuarios) {

            table.addCell(u.getNombreCompleto());
            table.addCell(u.getEmail());

            long mora = u.getPrestamos()
                    .stream()
                    .filter(p -> p.getEstado() == EstadoPrestamo.VENCIDO)
                    .count();

            table.addCell(String.valueOf(mora));
        }

        doc.add(table);
    }

    private void agregarTablaLibrosMasPrestados(Document doc) {

        List<Object[]> datos = libroService.findMasPrestadosConConteo();

        Table table = new Table(
                UnitValue.createPercentArray(new float[] { 3, 2, 2 })).useAllAvailableWidth();

        String[] headers = { "Título", "Autor", "Veces prestado" };

        for (String h : headers) {

            table.addHeaderCell(
                    new com.itextpdf.layout.element.Cell()
                            .add(new Paragraph(h).setBold())
                            .setBackgroundColor(ColorConstants.LIGHT_GRAY));
        }

        for (Object[] row : datos) {

            Libro libro = (Libro) row[0];
            Long total = (Long) row[1];

            table.addCell(libro.getTitulo());
            table.addCell(libro.getAutor());
            table.addCell(String.valueOf(total));
        }

        doc.add(table);
    }

    // =========================================================
    // EXCEL
    // =========================================================

    public void generarExcel(String tipo, OutputStream out) throws Exception {

        try (Workbook wb = new XSSFWorkbook()) {

            Sheet sheet = wb.createSheet(getTitulo(tipo));

            CellStyle headerStyle = wb.createCellStyle();

            Font font = wb.createFont();
            font.setBold(true);

            headerStyle.setFont(font);

            headerStyle.setFillForegroundColor(
                    IndexedColors.LIGHT_BLUE.getIndex());

            headerStyle.setFillPattern(
                    FillPatternType.SOLID_FOREGROUND);

            switch (tipo) {
                case "prestamos" -> generarExcelPrestamos(sheet, headerStyle);
                case "mora" -> generarExcelMora(sheet, headerStyle);
                default -> generarExcelLibros(sheet, headerStyle);
            }

            if (sheet.getRow(0) != null) {

                for (int i = 0; i < sheet.getRow(0).getLastCellNum(); i++) {
                    sheet.autoSizeColumn(i);
                }
            }

            wb.write(out);

            log.info("Excel generado: tipo={}", tipo);
        }
    }

    private void generarExcelPrestamos(
            Sheet sheet,
            CellStyle headerStyle) {

        Row header = sheet.createRow(0);

        String[] cols = {
                "ID",
                "Usuario",
                "Email",
                "Libro",
                "ISBN",
                "Fecha Préstamo",
                "Fecha Devolución",
                "Estado"
        };

        for (int i = 0; i < cols.length; i++) {

            org.apache.poi.ss.usermodel.Cell cell = header.createCell(i);

            cell.setCellValue(cols[i]);
            cell.setCellStyle(headerStyle);
        }

        List<Prestamo> prestamos = prestamoService.findAll();

        int rowNum = 1;

        for (Prestamo p : prestamos) {

            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(p.getId());

            row.createCell(1).setCellValue(
                    p.getUsuario().getNombreCompleto());

            row.createCell(2).setCellValue(
                    p.getUsuario().getEmail());

            row.createCell(3).setCellValue(
                    p.getLibro().getTitulo());

            row.createCell(4).setCellValue(
                    p.getLibro().getIsbn());

            row.createCell(5).setCellValue(
                    p.getFechaPrestamo()
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

            row.createCell(6).setCellValue(
                    p.getFechaDevolucion().format(FMT));

            row.createCell(7).setCellValue(
                    p.getEstado().name());
        }
    }

    private void generarExcelMora(
            Sheet sheet,
            CellStyle headerStyle) {

        Row header = sheet.createRow(0);

        String[] cols = {
                "Nombre",
                "Apellido",
                "Email",
                "Código Alumno",
                "Libros en Mora"
        };

        for (int i = 0; i < cols.length; i++) {

            org.apache.poi.ss.usermodel.Cell cell = header.createCell(i);

            cell.setCellValue(cols[i]);
            cell.setCellStyle(headerStyle);
        }

        List<Usuario> usuarios = usuarioService.findConMora();

        int rowNum = 1;

        for (Usuario u : usuarios) {

            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(u.getNombre());
            row.createCell(1).setCellValue(u.getApellido());
            row.createCell(2).setCellValue(u.getEmail());

            row.createCell(3).setCellValue(
                    u.getCodigoAlumno() != null
                            ? u.getCodigoAlumno()
                            : "");

            long mora = u.getPrestamos()
                    .stream()
                    .filter(p -> p.getEstado() == EstadoPrestamo.VENCIDO)
                    .count();

            row.createCell(4).setCellValue(mora);
        }
    }

    private void generarExcelLibros(
            Sheet sheet,
            CellStyle headerStyle) {

        Row header = sheet.createRow(0);

        String[] cols = {
                "Título",
                "Autor",
                "ISBN",
                "Categoría",
                "Veces Prestado"
        };

        for (int i = 0; i < cols.length; i++) {

            org.apache.poi.ss.usermodel.Cell cell = header.createCell(i);

            cell.setCellValue(cols[i]);
            cell.setCellStyle(headerStyle);
        }

        List<Object[]> datos = libroService.findMasPrestadosConConteo();

        int rowNum = 1;

        for (Object[] row : datos) {

            Libro libro = (Libro) row[0];
            Long total = (Long) row[1];

            Row excelRow = sheet.createRow(rowNum++);

            excelRow.createCell(0).setCellValue(libro.getTitulo());
            excelRow.createCell(1).setCellValue(libro.getAutor());
            excelRow.createCell(2).setCellValue(libro.getIsbn());

            excelRow.createCell(3).setCellValue(
                    libro.getCategoria() != null
                            ? libro.getCategoria()
                            : "");

            excelRow.createCell(4).setCellValue(total);
        }
    }

    private String getTitulo(String tipo) {

        return switch (tipo) {

            case "mora" ->
                "Reporte de Usuarios en Mora";

            case "libros" ->
                "Libros Más Prestados";

            default ->
                "Reporte de Préstamos";
        };
    }
}