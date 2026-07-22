package ec.edu.ups.icc.academicevents.users.entities;

/**
 * Estado administrativo permanente del usuario.
 *
 * Los bloqueos temporales por intentos fallidos se manejarán en Redis.
 */
public enum UserStatus {

    ACTIVE,
    BLOCKED
}