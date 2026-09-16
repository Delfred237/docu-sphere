package com.docusphere.config;

import com.docusphere.auth.domain.Permission;
import com.docusphere.auth.domain.PermissionName;
import com.docusphere.auth.domain.Role;
import com.docusphere.auth.domain.RoleName;
import com.docusphere.auth.repository.PermissionRepository;
import com.docusphere.auth.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
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
