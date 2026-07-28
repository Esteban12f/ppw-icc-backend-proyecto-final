package ec.edu.ups.icc.academicevents.reports.controllers;

import java.time.OffsetDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ec.edu.ups.icc.academicevents.reports.services.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(
        name = "Reports",
        description = "Reportes descargables de inscripciones"
)
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @Operation(
            summary = "Listado de inscritos en PDF",
            description = "Solo el organizador propietario del evento "
                    + "o un ADMIN pueden generarlo. Acepta filtros "
                    + "opcionales por rango de fechas (from, to en "
                    + "formato ISO 8601)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte generado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para este reporte"),
            @ApiResponse(responseCode = "404", description = "El evento no existe"),
            @ApiResponse(responseCode = "429", description = "Límite de reportes por minuto superado")
    })
    @GetMapping(
            value = "/reports/events/{eventId}/registrations.pdf",
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> registrationsPdf(
            @PathVariable Long eventId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime to,
            Authentication authentication
    ) {
        byte[] pdf = reportService.generateRegistrationsPdf(
                eventId,
                from,
                to,
                authentication
        );

        String filename =
                "registrations-event-" + eventId + ".pdf";

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(filename)
                                .build()
                                .toString()
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @Operation(
            summary = "Listado de inscritos en Excel",
            description = "Solo el organizador propietario del evento "
                    + "o un ADMIN pueden generarlo. Acepta filtros "
                    + "opcionales por rango de fechas (from, to en "
                    + "formato ISO 8601)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte generado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para este reporte"),
            @ApiResponse(responseCode = "404", description = "El evento no existe"),
            @ApiResponse(responseCode = "429", description = "Límite de reportes por minuto superado")
    })
    @GetMapping(
            value = "/reports/events/{eventId}/registrations.xlsx",
            produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    )
    public ResponseEntity<byte[]> registrationsExcel(
            @PathVariable Long eventId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime to,
            Authentication authentication
    ) {
        byte[] excel = reportService.generateRegistrationsExcel(
                eventId,
                from,
                to,
                authentication
        );

        String filename =
                "registrations-event-" + eventId + ".xlsx";

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(filename)
                                .build()
                                .toString()
                )
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .body(excel);
    }

    @Operation(
            summary = "Comprobante de inscripción en PDF",
            description = "Solo el participante propietario puede "
                    + "descargarlo, y la inscripción debe estar "
                    + "CONFIRMED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comprobante generado"),
            @ApiResponse(responseCode = "400", description = "La inscripción no está confirmada"),
            @ApiResponse(responseCode = "403", description = "El usuario no es el dueño de la inscripción"),
            @ApiResponse(responseCode = "404", description = "La inscripción no existe"),
            @ApiResponse(responseCode = "429", description = "Límite de reportes por minuto superado")
    })
    @GetMapping(
            value = "/registrations/{id}/certificate.pdf",
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> certificate(
            @PathVariable Long id,
            Authentication authentication
    ) {
        byte[] pdf = reportService.generateCertificate(
                id,
                authentication
        );

        String filename =
                "certificate-registration-" + id + ".pdf";

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(filename)
                                .build()
                                .toString()
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}