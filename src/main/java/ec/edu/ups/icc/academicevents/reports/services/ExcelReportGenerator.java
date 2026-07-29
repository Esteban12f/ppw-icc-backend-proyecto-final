package ec.edu.ups.icc.academicevents.reports.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import ec.edu.ups.icc.academicevents.events.entities.EventEntity;
import ec.edu.ups.icc.academicevents.registrations.entities.RegistrationEntity;

/**
 * Genera archivos Excel (.xlsx) a partir de datos de
 * inscripciones, usando Apache POI. Todo se genera en
 * memoria, nada se guarda en disco.
 */
@Component
public class ExcelReportGenerator {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generateRegistrationsList(
            EventEntity event,
            List<RegistrationEntity> registrations
    ) {
        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Inscripciones");

            CellStyle headerStyle = createHeaderStyle(workbook);

            String[] headers = {
                    "Participante",
                    "Correo",
                    "Estado",
                    "Fecha de inscripción",
                    "Código de inscripción"
            };

            Row headerRow = sheet.createRow(0);

            for (int i = 0; i < headers.length; i++) {

                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;

            for (RegistrationEntity registration : registrations) {

                Row row = sheet.createRow(rowIndex++);

                row.createCell(0).setCellValue(
                        registration.getParticipant()
                                .getFullName()
                );

                row.createCell(1).setCellValue(
                        registration.getParticipant()
                                .getEmail()
                );

                row.createCell(2).setCellValue(
                        registration.getStatus().name()
                );

                row.createCell(3).setCellValue(
                        registration.getRegisteredAt()
                                .format(DATE_FORMAT)
                );

                row.createCell(4).setCellValue(
                        registration.getRegistrationCode()
                                .toString()
                );
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            workbook.write(outputStream);

            return outputStream.toByteArray();

        } catch (IOException exception) {

            throw new UncheckedIOException(
                    "No se pudo generar el reporte Excel",
                    exception
            );
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {

        CellStyle style = workbook.createCellStyle();

        style.setFillForegroundColor(
                IndexedColors.GREY_25_PERCENT.getIndex()
        );

        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Font font = workbook.createFont();
        font.setBold(true);

        style.setFont(font);

        return style;
    }
}