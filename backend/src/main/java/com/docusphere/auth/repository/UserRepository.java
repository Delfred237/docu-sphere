package com.docusphere.auth.repository;

import com.docusphere.auth.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    // Requête optimisée pour l'authentification (User + Roles + Permissions)
    @Query("""
        SELECT DISTINCT u FROM User u 
        LEFT JOIN FETCH u.roles r 
        LEFT JOIN FETCH r.permissions 
        WHERE LOWER(u.email) = LOWER(:email)
    """)
    Optional<User> findByEmailIgnoreCaseWithRolesAndPermissions(@Param("email") String email);

    // Requête pour la liste paginée (sans FETCH pour éviter le warning HHH000104 sur les collections avec pagination)
    // Spring Data gère le comptage et la pagination efficacement ici.
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles")
    Page<User> findAllWithRoles(Pageable pageable);

    Optional<User> findByPublicId(String userPublicId);
}
