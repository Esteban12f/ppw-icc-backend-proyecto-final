package ec.edu.ups.icc.academicevents.events.dtos;

import ec.edu.ups.icc.academicevents.events.entities.EventModality;
import ec.edu.ups.icc.academicevents.events.entities.EventStatus;

import java.time.OffsetDateTime;

public class EventResponse {

    private final Long id;

    private final String title;

    private final String description;

    private final EventModality modality;

    private final String location;

    private final String virtualUrl;

    private final int capacity;

    private final int availableCapacity;

    private final OffsetDateTime registrationStartAt;

    private final OffsetDateTime registrationEndAt;

    private final OffsetDateTime startAt;

    private final OffsetDateTime endAt;

    private final EventStatus status;

    private final Long organizerId;

    private final String organizerName;

    private final String organizerEmail;

    private final Long categoryId;

    private final String categoryName;

    private final Long version;

    private final OffsetDateTime createdAt;

    private final OffsetDateTime updatedAt;

    public EventResponse(
            Long id,
            String title,
            String description,
            EventModality modality,
            String location,
            String virtualUrl,
            int capacity,
            int availableCapacity,
            OffsetDateTime registrationStartAt,
            OffsetDateTime registrationEndAt,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            EventStatus status,
            Long organizerId,
            String organizerName,
            String organizerEmail,
            Long categoryId,
            String categoryName,
            Long version,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.modality = modality;
        this.location = location;
        this.virtualUrl = virtualUrl;
        this.capacity = capacity;
        this.availableCapacity = availableCapacity;
        this.registrationStartAt =
                registrationStartAt;
        this.registrationEndAt =
                registrationEndAt;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = status;
        this.organizerId = organizerId;
        this.organizerName = organizerName;
        this.organizerEmail = organizerEmail;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public EventModality getModality() {
        return modality;
    }

    public String getLocation() {
        return location;
    }

    public String getVirtualUrl() {
        return virtualUrl;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getAvailableCapacity() {
        return availableCapacity;
    }

    public OffsetDateTime getRegistrationStartAt() {
        return registrationStartAt;
    }

    public OffsetDateTime getRegistrationEndAt() {
        return registrationEndAt;
    }

    public OffsetDateTime getStartAt() {
        return startAt;
    }

    public OffsetDateTime getEndAt() {
        return endAt;
    }

    public EventStatus getStatus() {
        return status;
    }

    public Long getOrganizerId() {
        return organizerId;
    }

    public String getOrganizerName() {
        return organizerName;
    }

    public String getOrganizerEmail() {
        return organizerEmail;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public Long getVersion() {
        return version;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}