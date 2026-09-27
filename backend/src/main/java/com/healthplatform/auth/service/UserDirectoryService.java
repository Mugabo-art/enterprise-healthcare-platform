package com.healthplatform.auth.service;

import com.healthplatform.auth.dto.UserResponse;
import com.healthplatform.auth.model.Role;
import com.healthplatform.auth.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Read-only lookup of accounts for other modules (e.g. the doctor module's
 * doctor list), so they never touch UserRepository directly — see
 * docs/ARCHITECTURE.md module boundary rules.
 */
@Service
public class UserDirectoryService {

    private final UserRepository userRepository;

    public UserDirectoryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserResponse> listByRole(Role role) {
        return userRepository.findByRoleOrderByEmailAsc(role)
                .stream()
                .map(UserResponse::from)
                .toList();
    }
}
