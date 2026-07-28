package ec.edu.ups.icc.academicevents.reports.services;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Component;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import ec.edu.ups.icc.academicevents.events.entities.EventEntity;
import ec.edu.ups.icc.academicevents.registrations.entities.RegistrationEntity;

/**
 * Genera archivos PDF a partir de datos de inscripciones,
 * usando OpenPDF. Todo se genera en memoria, nada se guarda
 * en disco.
 */
@Component
public class PdfReportGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generateRegistrationsList(
            EventEntity event,
            List<RegistrationEntity> registrations) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            Document document = new Document(PageSize.A4);

            PdfWriter.getInstance(document, outputStream);

            document.open();

            Font titleFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD, 16);

            Font normalFont = FontFactory.getFont(
                    FontFactory.HELVETICA, 10);

            Font headerFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD, 10);

            Paragraph title = new Paragraph(
                    "Listado de inscritos",
                    titleFont);

            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph eventInfo = new Paragraph(
                    "Evento: " + event.getTitle()
                            + "\nTotal de inscripciones: "
                            + registrations.size(),
                    normalFont);

            eventInfo.setSpacingBefore(10f);
            eventInfo.setSpacingAfter(15f);
            document.add(eventInfo);

            PdfPTable table = new PdfPTable(4);

            table.setWidthPercentage(100);

            table.setWidths(new float[] { 30f, 30f, 20f, 20f });

            addHeaderCell(table, "Participante", headerFont);
            addHeaderCell(table, "Correo", headerFont);
            addHeaderCell(table, "Estado", headerFont);
            addHeaderCell(table, "Fecha de inscripción", headerFont);

            for (RegistrationEntity registration : registrations) {

                table.addCell(new PdfPCell(new Paragraph(
                        registration.getParticipant().getFullName(),
                        normalFont)));

                table.addCell(new PdfPCell(new Paragraph(
                        registration.getParticipant().getEmail(),
                        normalFont)));

                table.addCell(new PdfPCell(new Paragraph(
                        registration.getStatus().name(),
                        normalFont)));

                table.addCell(new PdfPCell(new Paragraph(
                        registration.getRegisteredAt()
                                .format(DATE_FORMAT),
                        normalFont)));
            }

            document.add(table);

            document.close();

            return outputStream.toByteArray();

        } catch (DocumentException exception) {

            throw new IllegalStateException(
                    "No se pudo generar el reporte PDF",
                    exception);
        }
    }

    public byte[] generateCertificate(
            RegistrationEntity registration) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            Document document = new Document(PageSize.A4);

            PdfWriter.getInstance(document, outputStream);

            document.open();

            Font titleFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD, 20);

            Font normalFont = FontFactory.getFont(
                    FontFactory.HELVETICA, 12);

            Paragraph title = new Paragraph(
                    "Comprobante de inscripción",
                    titleFont);

            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(30f);
            document.add(title);

            EventEntity event = registration.getEvent();

            String bodyText = "Se certifica que "
                    + registration.getParticipant()
                            .getFullName()
                    + " ("
                    + registration.getParticipant()
                            .getEmail()
                    + ") se encuentra inscrito en "
                    + "el evento \""
                    + event.getTitle()
                    + "\", programado del "
                    + event.getStartAt().format(DATE_FORMAT)
                    + " al "
                    + event.getEndAt().format(DATE_FORMAT)
                    + ".\n\n"
                    + "Código de inscripción: "
                    + registration.getRegistrationCode()
                    + "\n"
                    + "Estado: "
                    + registration.getStatus().name()
                    + "\n"
                    + "Fecha de inscripción: "
                    + registration.getRegisteredAt()
                            .format(DATE_FORMAT);

            Paragraph body = new Paragraph(bodyText, normalFont);

            body.setAlignment(Element.ALIGN_LEFT);
            document.add(body);

            document.close();

            return outputStream.toByteArray();

        } catch (DocumentException exception) {

            throw new IllegalStateException(
                    "No se pudo generar el certificado PDF",
                    exception);
        }
    }

    private void addHeaderCell(
            PdfPTable table,
            String text,
            Font font) {
        PdfPCell cell = new PdfPCell(
                new Paragraph(text, font));

        cell.setBackgroundColor(
                new Color(230, 230, 230));

        cell.setPadding(6f);

        table.addCell(cell);
    }
}