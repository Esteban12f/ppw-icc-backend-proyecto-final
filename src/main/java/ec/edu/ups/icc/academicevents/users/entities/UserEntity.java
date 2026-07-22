package ec.edu.ups.icc.academicevents.users.entities;

import ec.edu.ups.icc.academicevents.roles.entities.RoleEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "first_name",
            nullable = false,
            length = 80
    )
    private String firstName;

    @Column(
            name = "last_name",
            nullable = false,
            length = 80
    )
    private String lastName;

    @Column(
            name = "email",
            nullable = false,
            unique = true,
            length = 160
    )
    private String email;

    @Column(
            name = "password_hash",
            nullable = false,
            length = 255
    )
    private String passwordHash;

    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private String status = UserStatus.ACTIVE.name();

    @Column(
            name = "created_at",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private OffsetDateTime updatedAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = {
                    @JoinColumn(
                            name = "user_id",
                            nullable = false
                    )
            },
            inverseJoinColumns = {
                    @JoinColumn(
                            name = "role_id",
                            nullable = false
                    )
            }
    )
    private Set<RoleEntity> roles = new HashSet<>();

    public UserEntity() {
    }

    public UserEntity(
            String firstName,
            String lastName,
            String email,
            String passwordHash
    ) {
        this.firstName = firstName;
        this.lastName = lastName;
        setEmail(email);
        this.passwordHash = passwordHash;
        this.status = UserStatus.ACTIVE.name();
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = normalizeText(firstName);
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = normalizeText(lastName);
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null
                ? null
                : email.trim().toLowerCase();
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getStatus() {
        return status;
    }

    public UserStatus getUserStatus() {
        return UserStatus.valueOf(status);
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setStatus(UserStatus status) {
        this.status = status.name();
    }

    public boolean isActive() {
        return UserStatus.ACTIVE.name().equals(status);
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Set<RoleEntity> getRoles() {
        return roles;
    }

    public void setRoles(Set<RoleEntity> roles) {
        this.roles = roles == null
                ? new HashSet<>()
                : roles;
    }

    public void addRole(RoleEntity role) {
        if (role != null) {
            roles.add(role);
        }
    }

    public void removeRole(RoleEntity role) {
        roles.remove(role);
    }

    private String normalizeText(String value) {
        return value == null
                ? null
                : value.trim();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof UserEntity userEntity)) {
            return false;
        }

        return id != null && Objects.equals(id, userEntity.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}