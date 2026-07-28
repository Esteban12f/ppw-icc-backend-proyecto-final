package ec.edu.ups.icc.academicevents.registrations.dtos;

import java.time.OffsetDateTime;
import java.util.UUID;

import ec.edu.ups.icc.academicevents.registrations.entities.RegistrationStatus;

public class RegistrationResponse {

    private final Long id;

    private final UUID registrationCode;

    private final Long eventId;

    private final String eventTitle;

    private final Long participantId;

    private final String participantName;

    private final String participantEmail;

    private final RegistrationStatus status;

    private final OffsetDateTime registeredAt;

    private final OffsetDateTime statusUpdatedAt;

    private final OffsetDateTime confirmedAt;

    private final OffsetDateTime cancelledAt;

    private final Long version;

    public RegistrationResponse(
            Long id,
            UUID registrationCode,
            Long eventId,
            String eventTitle,
            Long participantId,
            String participantName,
            String participantEmail,
            RegistrationStatus status,
            OffsetDateTime registeredAt,
            OffsetDateTime statusUpdatedAt,
            OffsetDateTime confirmedAt,
            OffsetDateTime cancelledAt,
            Long version
    ) {
        this.id = id;
        this.registrationCode = registrationCode;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.participantId = participantId;
        this.participantName = participantName;
        this.participantEmail = participantEmail;
        this.status = status;
        this.registeredAt = registeredAt;
        this.statusUpdatedAt = statusUpdatedAt;
        this.confirmedAt = confirmedAt;
        this.cancelledAt = cancelledAt;
        this.version = version;
    }

    public Long getId() {
        return id;
    }

    public UUID getRegistrationCode() {
        return registrationCode;
    }

    public Long getEventId() {
        return eventId;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public Long getParticipantId() {
        return participantId;
    }

    public String getParticipantName() {
        return participantName;
    }

    public String getParticipantEmail() {
        return participantEmail;
    }

    public RegistrationStatus getStatus() {
        return status;
    }

    public OffsetDateTime getRegisteredAt() {
        return registeredAt;
    }

    public OffsetDateTime getStatusUpdatedAt() {
        return statusUpdatedAt;
    }

    public OffsetDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public OffsetDateTime getCancelledAt() {
        return cancelledAt;
    }

    public Long getVersion() {
        return version;
    }
}