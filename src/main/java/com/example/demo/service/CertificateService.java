package com.example.demo.service;

import com.example.demo.model.Participante;
import com.example.demo.repository.ParticipanteRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class CertificateService {

    @Autowired
    private ParticipanteRepository repository;

    // Genera un archivo ZIP con todos los PDFs seleccionados
    public byte[] generateCertificatesZip(List<Long> ids) throws IOException {
        if (ids == null || ids.isEmpty())
            return new ByteArrayOutputStream().toByteArray();

        List<Participante> participantes = repository.findAllById(ids);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Participante p : participantes) {
                try {
                    String safeName = (p.getNombres() + "_" + p.getApellidos()).replaceAll("[^a-zA-Z0-9]", "_");
                    String filename = p.getDni() + "_" + safeName + ".pdf";

                    zos.putNextEntry(new ZipEntry(filename));
                    byte[] pdfBytes = generateCertificatePdf(p);

                    if (pdfBytes.length > 0) {
                        zos.write(pdfBytes);
                    }
                    zos.closeEntry();
                } catch (Exception e) {
                    System.err.println("Error con participante ID: " + p.getId());
                }
            }
        }
        return baos.toByteArray();
    }

    // Genera el PDF individual con posicionamiento exacto
    private byte[] generateCertificatePdf(Participante p) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            // 1. Cargar Plantilla de fondo
            org.springframework.core.io.ClassPathResource templateResource = new org.springframework.core.io.ClassPathResource(
                    "static/ejemplo_pdf.pdf");
            com.lowagie.text.pdf.PdfReader reader = new com.lowagie.text.pdf.PdfReader(
                    templateResource.getInputStream());

            // 2. Configuración de Márgenes (Margen inferior en 0 para permitir
            // posicionamiento absoluto al final)
            float marginSide = mmToPt(20);
            float marginTop = mmToPt(50);

            Document document = new Document(reader.getPageSize(1), marginSide, marginSide, marginTop, 0);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            // 3. Dibujar la plantilla como fondo
            com.lowagie.text.pdf.PdfContentByte cb = writer.getDirectContentUnder();
            cb.addTemplate(writer.getImportedPage(reader, 1), 0, 0);

            // 4. Fuentes
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

            // Preparar datos variables
            String dateRangeText = formatDateRange(p.getFechaInicio(), p.getFechaFinal());
            String fullName = (p.getNombres() + " " + p.getApellidos()).toUpperCase();

            // Espaciador inicial para que el primer párrafo respete el SpacingBefore
            document.add(new Paragraph(" "));

            // PARRAFO 1: INTRODUCCIÓN
            Paragraph p1 = new Paragraph();
            p1.setSpacingBefore(mmToPt(10));
            p1.setAlignment(Element.ALIGN_JUSTIFIED);
            p1.add(new Chunk("CREA MÁS PERU", boldFont));
            p1.add(new Chunk(
                    " (en adelante, Crea+) es una asociación civil sin fines de lucro compuesta por un equipo multidisciplinario de jóvenes, el cual busca fortalecer el nivel educativo nacional, formando capacidades de liderazgo y otorgando herramientas para el crecimiento a través de un voluntariado profesional.",
                    bodyFont));
            document.add(p1);

            // PARRAFO 2: DATOS (Uso de formatDateRange)
            Paragraph p2 = new Paragraph();
            p2.setSpacingBefore(mmToPt(5));
            p2.setAlignment(Element.ALIGN_JUSTIFIED);
            p2.add(new Chunk("Mediante el presente, Crea+ deja constancia que ", bodyFont));
            p2.add(new Chunk(fullName, boldFont));
            p2.add(new Chunk(
                    " con cédula Nº " + p.getDni() + ", participó como voluntario(a) profesional en el programa \""
                            + p.getPrograma() + "\" " + dateRangeText + " en el rol de " + p.getRol()
                            + " cumpliendo con " + p.getHoras() + " horas de voluntariado profesional.",
                    bodyFont));
            document.add(p2);

            // PARRAFO 3: CERTIFICACIÓN
            Paragraph p3 = new Paragraph();
            p3.setSpacingBefore(mmToPt(5));
            p3.add(new Chunk("Certificamos que " + fullName
                    + " demostró responsabilidad y compromiso en el desarrollo de sus funciones.", bodyFont));
            document.add(p3);

            // PARRAFO 4: CIERRE
            Paragraph p4 = new Paragraph(
                    "Se expide el presente certificado para los fines que se estimen convenientes.", bodyFont);
            p4.setSpacingBefore(mmToPt(15));
            document.add(p4);

            // PARRAFO 5: FIRMA
            Paragraph p5 = new Paragraph("Atentamente,", bodyFont);
            p5.setSpacingBefore(mmToPt(20));
            document.add(p5);

            // --- PARRAFO 6: POSICIÓN ABSOLUTA (Fijado al fondo de la hoja) ---
            com.lowagie.text.pdf.PdfContentByte canvas = writer.getDirectContent();
            canvas.beginText();
            canvas.setFontAndSize(FontFactory.getFont(FontFactory.HELVETICA).getBaseFont(), 8);

            // Coordenadas fijas (X horizontal, Y vertical desde abajo)
            float x = mmToPt(20);
            float y = mmToPt(25); // <--- Ajusta este valor (8 o 12) para subir o bajar el footer al límite

            String footerText = "Este certificado es emitido por CREA MÁS PERU. Verificador: https://creamas.org/voluntariado/verificador/ Código: "
                    + p.getCodigoVerificacion();

            // showTextAligned ignora los márgenes y lo estampa en la coordenada exacta
            canvas.showTextAligned(Element.ALIGN_LEFT, footerText, x, y, 0);
            canvas.endText();

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
        return out.toByteArray();
    }

    // Conversión de Milímetros a Puntos (1mm = 2.83465pt)
    private float mmToPt(float mm) {
        return mm * 2.83465f;
    }

    // Lógica para formatear el rango de fechas en español
    private String formatDateRange(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null)
            return " [FECHAS INVÁLIDAS] ";

        Locale spanish = Locale.forLanguageTag("es-ES");
        String mesI = inicio.getMonth().getDisplayName(TextStyle.FULL, spanish);
        String mesF = fin.getMonth().getDisplayName(TextStyle.FULL, spanish);

        if (inicio.getYear() == fin.getYear()) {
            return "de " + mesI + " a " + mesF + " de " + inicio.getYear();
        }
        return "de " + mesI + " de " + inicio.getYear() + " a " + mesF + " de " + fin.getYear();
    }
}