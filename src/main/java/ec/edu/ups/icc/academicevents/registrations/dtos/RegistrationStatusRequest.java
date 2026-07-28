package ec.edu.ups.icc.academicevents.registrations.dtos;

import ec.edu.ups.icc.academicevents.registrations.entities.RegistrationStatus;
import jakarta.validation.constraints.NotNull;

public class RegistrationStatusRequest {

    @NotNull(message = "El nuevo estado es obligatorio")
    private RegistrationStatus status;

    public RegistrationStatusRequest() {
    }

    public RegistrationStatusRequest(RegistrationStatus status) {
        this.status = status;
    }

    public RegistrationStatus getStatus() {
        return status;
    }

    public void setStatus(RegistrationStatus status) {
        this.status = status;
    }
}