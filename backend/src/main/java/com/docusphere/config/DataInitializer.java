package com.docusphere.config;

import com.docusphere.auth.domain.*;
import com.docusphere.auth.repository.PermissionRepository;
import com.docusphere.auth.repository.RoleRepository;
import com.docusphere.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;


    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Initializing default permissions and roles...");

        // 1. Create Permissions
        Set<Permission> allPermissions = Arrays.stream(PermissionName.values())
                .map(name -> permissionRepository.findByName(name)
                        .orElseGet(() -> {
                            Permission p = new Permission();
                            p.setName(name);
                            return permissionRepository.save(p);
                        }))
                .collect(Collectors.toSet());

        // 2. Create Roles and assign permissions
        createRoleIfNotExists(RoleName.USER,
                Set.of(PermissionName.DOCUMENT_READ, PermissionName.DOCUMENT_CREATE,
                        PermissionName.DOCUMENT_UPDATE, PermissionName.DOCUMENT_DELETE,
                        PermissionName.DOCUMENT_DOWNLOAD), allPermissions);

        createRoleIfNotExists(RoleName.REVIEWER,
                Set.of(PermissionName.DOCUMENT_READ, PermissionName.DOCUMENT_DOWNLOAD,
                        PermissionName.DOCUMENT_VALIDATE, PermissionName.DOCUMENT_REJECT), allPermissions);

        createRoleIfNotExists(RoleName.MANAGER,
                Set.of(PermissionName.DOCUMENT_READ, PermissionName.DOCUMENT_CREATE,
                        PermissionName.DOCUMENT_UPDATE, PermissionName.DOCUMENT_DELETE,
                        PermissionName.DOCUMENT_DOWNLOAD, PermissionName.DOCUMENT_VALIDATE,
                        PermissionName.DOCUMENT_REJECT, PermissionName.AUDIT_READ), allPermissions);

        createRoleIfNotExists(RoleName.ADMIN,
                Arrays.stream(PermissionName.values()).collect(Collectors.toSet()), allPermissions);

        log.info("Default permissions and roles initialized.");

        // 3. Create Default Admin User (NOUVEAU)
        createDefaultAdminUser();
    }

    private void createDefaultAdminUser() {
        if (userRepository.existsByEmailIgnoreCase(adminProperties.getEmail())) {
            log.debug("Default admin user already exists. Skipping creation.");
            return;
        }

        log.warn("⚠️ Creating default admin user. This should NOT happen in production without secure credentials!");

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new IllegalStateException("ADMIN role not found during initialization."));

        User admin = new User();
        admin.setEmail(adminProperties.getEmail().toLowerCase());
        admin.setPassword(passwordEncoder.encode(adminProperties.getPassword()));
        admin.setFirstName(adminProperties.getFirstName());
        admin.setLastName(adminProperties.getLastName());
        admin.setEmailVerified(true); // Crucial : l'admin doit pouvoir se connecter immédiatement
        admin.setActive(true);
        admin.addRole(adminRole);

        userRepository.save(admin);
        log.info("✅ Default admin user created: {}", adminProperties.getEmail());
    }

    private void createRoleIfNotExists(RoleName roleName, Set<PermissionName> permissionNames, Set<Permission> allPermissions) {
        if (roleRepository.findByName(roleName).isEmpty()) {
            Role role = new Role();
            role.setName(roleName);

            Set<Permission> rolePermissions = allPermissions.stream()
                    .filter(p -> permissionNames.contains(p.getName()))
                    .collect(Collectors.toSet());

            role.setPermissions(rolePermissions);
            roleRepository.save(role);
        }
    }
}
