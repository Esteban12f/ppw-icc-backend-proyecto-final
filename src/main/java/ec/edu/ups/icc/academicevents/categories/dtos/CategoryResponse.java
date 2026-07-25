package ec.edu.ups.icc.academicevents.categories.dtos;

import java.time.OffsetDateTime;

public class CategoryResponse {

    private final Long id;

    private final String name;

    private final String description;

    private final boolean active;

    private final OffsetDateTime createdAt;

    private final OffsetDateTime updatedAt;

    public CategoryResponse(
            Long id,
            String name,
            String description,
            boolean active,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
