package ec.edu.ups.icc.academicevents.audit.entities;

import ec.edu.ups.icc.academicevents.users.entities.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(
            name = "action",
            nullable = false,
            length = 100
    )
    private String action;

    @Column(
            name = "entity_type",
            length = 100
    )
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(
            name = "result",
            nullable = false,
            length = 20
    )
    private String result;

    @Column(
            name = "details",
            columnDefinition = "TEXT"
    )
    private String details;

    @Column(
            name = "ip_address",
            length = 64
    )
    private String ipAddress;

    @Column(
            name = "created_at",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    public AuditLogEntity() {
    }

    public AuditLogEntity(
            UserEntity user,
            String action,
            String entityType,
            Long entityId,
            AuditResult result,
            String details,
            String ipAddress
    ) {
        this.user = user;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.result = result.name();
        this.details = details;
        this.ipAddress = ipAddress;
    }

    public Long getId() {
        return id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getResult() {
        return result;
    }

    public AuditResult getAuditResult() {
        return AuditResult.valueOf(result);
    }

    public void setResult(String result) {
        this.result = result;
    }

    public void setResult(AuditResult result) {
        this.result = result.name();
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AuditLogEntity auditLogEntity)) {
            return false;
        }

        return id != null && Objects.equals(id, auditLogEntity.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
