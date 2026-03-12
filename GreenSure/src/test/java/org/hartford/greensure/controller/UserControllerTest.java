package org.hartford.greensure.controller;

import org.hartford.greensure.dto.response.ApiResponse;
import org.hartford.greensure.dto.response.DashboardResponse;
import org.hartford.greensure.dto.response.UserProfileResponse;
import org.hartford.greensure.security.SecurityUser;
import org.hartford.greensure.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private SecurityUser securityUser;

    @InjectMocks
    private UserController userController;

    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        // Setup is done via Mockito annotations
    }

    @Test
    void testGetProfile() {
        when(securityUser.getId()).thenReturn(userId);

        UserProfileResponse mockProfile = UserProfileResponse.builder()
                .userId(userId)
                .fullName("John Doe")
                .build();
        
        when(userService.getProfile(userId)).thenReturn(mockProfile);

        ResponseEntity<ApiResponse<UserProfileResponse>> response = userController.getProfile(securityUser);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Profile fetched", response.getBody().getMessage());
        assertEquals(mockProfile, response.getBody().getData());

        verify(userService, times(1)).getProfile(userId);
    }

    @Test
    void testUpdateProfile() {
        when(securityUser.getId()).thenReturn(userId);

        UserProfileResponse updateRequest = UserProfileResponse.builder()
                .fullName("John Updated")
                .build();

        UserProfileResponse updatedProfile = UserProfileResponse.builder()
                .userId(userId)
                .fullName("John Updated")
                .build();

        when(userService.updateProfile(userId, updateRequest)).thenReturn(updatedProfile);

        ResponseEntity<ApiResponse<UserProfileResponse>> response = userController.updateProfile(securityUser, updateRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Profile updated", response.getBody().getMessage());
        assertEquals(updatedProfile, response.getBody().getData());

        verify(userService, times(1)).updateProfile(userId, updateRequest);
    }

    @Test
    void testGetDashboard() {
        when(securityUser.getId()).thenReturn(userId);

        DashboardResponse mockDashboard = DashboardResponse.builder()
                .userId(userId)
                .fullName("John Doe")
                .build();

        when(userService.getDashboard(userId)).thenReturn(mockDashboard);

        ResponseEntity<ApiResponse<DashboardResponse>> response = userController.getDashboard(securityUser);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Dashboard fetched", response.getBody().getMessage());
        assertEquals(mockDashboard, response.getBody().getData());

        verify(userService, times(1)).getDashboard(userId);
    }
}
