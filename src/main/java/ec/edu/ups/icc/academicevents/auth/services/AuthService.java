package ec.edu.ups.icc.academicevents.auth.services;

import ec.edu.ups.icc.academicevents.auth.dtos.*;
import ec.edu.ups.icc.academicevents.roles.entities.RoleEntity;
import ec.edu.ups.icc.academicevents.roles.entities.RoleName;
import ec.edu.ups.icc.academicevents.roles.repositories.RoleRepository;
import ec.edu.ups.icc.academicevents.security.jwt.JwtService;
import ec.edu.ups.icc.academicevents.users.entities.UserEntity;
import ec.edu.ups.icc.academicevents.users.repositories.UserRepository;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;

    }

    @Transactional
    public void register(RegisterRequest request) {

        if (userRepository
                .existsByEmailIgnoreCase(request.getEmail())) {

            throw new RuntimeException(
                    "El correo ya está registrado");
        }

        UserEntity user = new UserEntity(

                request.getFirstName(),

                request.getLastName(),

                request.getEmail(),

                passwordEncoder.encode(
                        request.getPassword())

        );

        RoleEntity participant = roleRepository
                .findByName(RoleName.PARTICIPANT)
                .orElseThrow();

        user.addRole(participant);

        userRepository.save(user);

    }

    public AuthResponse login(
            LoginRequest request) {

        authenticationManager.authenticate(

                new UsernamePasswordAuthenticationToken(

                        request.getEmail(),

                        request.getPassword()

                )

        );

        String token = jwtService.generateToken(
                request.getEmail());

        return new AuthResponse(
                token,
                1800000L);

    }

}