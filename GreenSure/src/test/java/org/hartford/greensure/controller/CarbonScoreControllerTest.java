package org.hartford.greensure.controller;

import org.hartford.greensure.dto.response.ApiResponse;
import org.hartford.greensure.dto.response.CarbonScoreResponse;
import org.hartford.greensure.engine.CarbonScoreService;
import org.hartford.greensure.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CarbonScoreControllerTest {

    @Mock
    private CarbonScoreService carbonScoreService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private CarbonScoreController carbonScoreController;

    private final String dummyToken = "Bearer dummy.jwt.token";
    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testGetMyScore() {
        when(request.getHeader("Authorization")).thenReturn(dummyToken);
        when(jwtUtil.extractId("dummy.jwt.token")).thenReturn(userId);

        CarbonScoreResponse mockScore = CarbonScoreResponse.builder()
                .scoreId(10L)
                .userId(userId)
                .totalCo2(500.0)
                .build();

        when(carbonScoreService.getMyScore(userId)).thenReturn(mockScore);

        ResponseEntity<ApiResponse<CarbonScoreResponse>> response = carbonScoreController.getMyScore(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Score fetched", response.getBody().getMessage());
        assertEquals(mockScore, response.getBody().getData());

        verify(jwtUtil, times(1)).extractId(anyString());
        verify(carbonScoreService, times(1)).getMyScore(userId);
    }

    @Test
    void testGetMyHistory() {
        when(request.getHeader("Authorization")).thenReturn(dummyToken);
        when(jwtUtil.extractId("dummy.jwt.token")).thenReturn(userId);

        CarbonScoreResponse score1 = CarbonScoreResponse.builder().scoreId(10L).userId(userId).scoreYear(2023).build();
        CarbonScoreResponse score2 = CarbonScoreResponse.builder().scoreId(11L).userId(userId).scoreYear(2024).build();
        List<CarbonScoreResponse> history = Arrays.asList(score2, score1);

        when(carbonScoreService.getScoreHistory(userId)).thenReturn(history);

        ResponseEntity<ApiResponse<List<CarbonScoreResponse>>> response = carbonScoreController.getMyHistory(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Score history fetched", response.getBody().getMessage());
        assertEquals(2, response.getBody().getData().size());
        assertEquals(history, response.getBody().getData());

        verify(jwtUtil, times(1)).extractId(anyString());
        verify(carbonScoreService, times(1)).getScoreHistory(userId);
    }

    @Test
    void testGetScoreByUserId() {
        CarbonScoreResponse mockScore = CarbonScoreResponse.builder()
                .scoreId(10L)
                .userId(userId)
                .totalCo2(500.0)
                .build();

        when(carbonScoreService.getMyScore(userId)).thenReturn(mockScore);

        ResponseEntity<ApiResponse<CarbonScoreResponse>> response = carbonScoreController.getScoreByUserId(userId);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Score fetched", response.getBody().getMessage());
        assertEquals(mockScore, response.getBody().getData());

        verify(carbonScoreService, times(1)).getMyScore(userId);
    }

    @Test
    void testGenerateScore() {
        doNothing().when(carbonScoreService).generateScore(100L);

        ResponseEntity<ApiResponse<Void>> response = carbonScoreController.generateScore(100L);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Score generation triggered", response.getBody().getMessage());
        assertNull(response.getBody().getData());

        verify(carbonScoreService, times(1)).generateScore(100L);
    }
}
