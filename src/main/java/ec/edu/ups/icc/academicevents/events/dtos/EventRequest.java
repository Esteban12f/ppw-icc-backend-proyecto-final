package ec.edu.ups.icc.academicevents.events.dtos;

import ec.edu.ups.icc.academicevents.events.entities.EventModality;
import ec.edu.ups.icc.academicevents.events.entities.EventStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public class EventRequest {

    @NotBlank
    @Size(max = 160)
    private String title;

    @NotBlank
    private String description;

    @NotNull
    private EventModality modality;

    @Size(max = 200)
    private String location;

    @Size(max = 500)
    private String virtualUrl;

    @NotNull
    @Positive
    private Integer capacity;

    @NotNull
    private OffsetDateTime registrationStartAt;

    @NotNull
    private OffsetDateTime registrationEndAt;

    @NotNull
    private OffsetDateTime startAt;

    @NotNull
    private OffsetDateTime endAt;

    @NotNull
    private EventStatus status;

    @NotNull
    @Positive
    private Long categoryId;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(
            String description
    ) {
        this.description = description;
    }

    public EventModality getModality() {
        return modality;
    }

    public void setModality(
            EventModality modality
    ) {
        this.modality = modality;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getVirtualUrl() {
        return virtualUrl;
    }

    public void setVirtualUrl(
            String virtualUrl
    ) {
        this.virtualUrl = virtualUrl;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public OffsetDateTime getRegistrationStartAt() {
        return registrationStartAt;
    }

    public void setRegistrationStartAt(
            OffsetDateTime registrationStartAt
    ) {
        this.registrationStartAt =
                registrationStartAt;
    }

    public OffsetDateTime getRegistrationEndAt() {
        return registrationEndAt;
    }

    public void setRegistrationEndAt(
            OffsetDateTime registrationEndAt
    ) {
        this.registrationEndAt =
                registrationEndAt;
    }

    public OffsetDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(
            OffsetDateTime startAt
    ) {
        this.startAt = startAt;
    }

    public OffsetDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(
            OffsetDateTime endAt
    ) {
        this.endAt = endAt;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(
            EventStatus status
    ) {
        this.status = status;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(
            Long categoryId
    ) {
        this.categoryId = categoryId;
    }
}
