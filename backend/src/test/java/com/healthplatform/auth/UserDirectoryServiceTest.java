package com.healthplatform.auth;

import com.healthplatform.auth.dto.UserResponse;
import com.healthplatform.auth.model.Role;
import com.healthplatform.auth.model.User;
import com.healthplatform.auth.repository.UserRepository;
import com.healthplatform.auth.service.UserDirectoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDirectoryServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks
    private UserDirectoryService userDirectoryService;

    @Test
    void listByRole_mapsAccountsWithoutExposingCredentials() {
        User doctor = User.builder()
                .id(UUID.randomUUID())
                .email("a.doctor@hospital.test")
                .passwordHash("hash")
                .role(Role.DOCTOR)
                .build();
        when(userRepository.findByRoleOrderByEmailAsc(Role.DOCTOR)).thenReturn(List.of(doctor));

        List<UserResponse> result = userDirectoryService.listByRole(Role.DOCTOR);

        assertEquals(1, result.size());
        assertEquals(doctor.getId(), result.get(0).id());
        assertEquals("a.doctor@hospital.test", result.get(0).email());
        assertEquals(Role.DOCTOR, result.get(0).role());
    }

    @Test
    void listByRole_returnsEmptyWhenNoAccountsHaveTheRole() {
        when(userRepository.findByRoleOrderByEmailAsc(Role.DOCTOR)).thenReturn(List.of());

        assertTrue(userDirectoryService.listByRole(Role.DOCTOR).isEmpty());
    }
}
