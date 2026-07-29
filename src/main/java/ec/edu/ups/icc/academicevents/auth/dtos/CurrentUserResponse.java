package ec.edu.ups.icc.academicevents.auth.dtos;

import java.util.List;

public class CurrentUserResponse {

    private final Long id;

    private final String firstName;

    private final String lastName;

    private final String fullName;

    private final String email;

    private final String status;

    private final List<String> roles;

    public CurrentUserResponse(
            Long id,
            String firstName,
            String lastName,
            String fullName,
            String email,
            String status,
            List<String> roles
    ) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = fullName;
        this.email = email;
        this.status = status;
        this.roles = roles;
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getStatus() {
        return status;
    }

    public List<String> getRoles() {
        return roles;
    }
}