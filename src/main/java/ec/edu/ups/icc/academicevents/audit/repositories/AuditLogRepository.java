package ec.edu.ups.icc.academicevents.audit.repositories;

import ec.edu.ups.icc.academicevents.audit.entities.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository
        extends JpaRepository<AuditLogEntity, Long> {

    Page<AuditLogEntity> findByUserId(
            Long userId,
            Pageable pageable
    );

    Page<AuditLogEntity> findByActionIgnoreCase(
            String action,
            Pageable pageable
    );
}
