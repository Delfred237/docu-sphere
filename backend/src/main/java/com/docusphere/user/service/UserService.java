package com.docusphere.user.service;

import com.docusphere.auth.domain.User;
import com.docusphere.auth.repository.UserRepository;
import com.docusphere.common.exception.ResourceNotFoundException;
import com.docusphere.user.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(User user) {
        return UserProfileResponse.fromEntity(user);
    }
}
