package ec.edu.ups.icc.academicevents.roles.repositories;

import ec.edu.ups.icc.academicevents.roles.entities.RoleEntity;
import ec.edu.ups.icc.academicevents.roles.entities.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByName(String name);

    default Optional<RoleEntity> findByName(RoleName roleName) {
        return findByName(roleName.name());
    }

    boolean existsByName(String name);
}