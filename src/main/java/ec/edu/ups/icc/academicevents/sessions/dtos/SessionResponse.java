package ec.edu.ups.icc.academicevents.sessions.dtos;

import java.time.OffsetDateTime;

public class SessionResponse {

    private final Long id;

    private final Long eventId;

    private final String eventTitle;

    private final String title;

    private final String description;

    private final OffsetDateTime startAt;

    private final OffsetDateTime endAt;

    private final String location;

    private final String virtualUrl;

    private final OffsetDateTime createdAt;

    private final OffsetDateTime updatedAt;

    public SessionResponse(
            Long id,
            Long eventId,
            String eventTitle,
            String title,
            String description,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            String location,
            String virtualUrl,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this.id = id;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.title = title;
        this.description = description;
        this.startAt = startAt;
        this.endAt = endAt;
        this.location = location;
        this.virtualUrl = virtualUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getEventId() {
        return eventId;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public OffsetDateTime getStartAt() {
        return startAt;
    }

    public OffsetDateTime getEndAt() {
        return endAt;
    }

    public String getLocation() {
        return location;
    }

    public String getVirtualUrl() {
        return virtualUrl;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}