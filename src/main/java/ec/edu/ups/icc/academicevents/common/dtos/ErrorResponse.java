package ec.edu.ups.icc.academicevents.common.dtos;

import java.time.OffsetDateTime;
import java.util.List;

public class ErrorResponse {

    private final OffsetDateTime timestamp;

    private final int status;

    private final String code;

    private final String message;

    private final String path;

    private final List<FieldErrorDetail> errors;

    public ErrorResponse(
            int status,
            String code,
            String message,
            String path
    ) {
        this(status, code, message, path, null);
    }

    public ErrorResponse(
            int status,
            String code,
            String message,
            String path,
            List<FieldErrorDetail> errors
    ) {
        this.timestamp = OffsetDateTime.now();
        this.status = status;
        this.code = code;
        this.message = message;
        this.path = path;
        this.errors = errors;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public List<FieldErrorDetail> getErrors() {
        return errors;
    }

    public static class FieldErrorDetail {

        private final String field;

        private final String message;

        public FieldErrorDetail(
                String field,
                String message
        ) {
            this.field = field;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public String getMessage() {
            return message;
        }
    }
}