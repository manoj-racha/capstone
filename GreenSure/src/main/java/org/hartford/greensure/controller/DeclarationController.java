package org.hartford.greensure.controller;

import org.hartford.greensure.dto.request.*;
import org.hartford.greensure.dto.response.*;
import org.hartford.greensure.security.JwtUtil;
import org.hartford.greensure.service.DeclarationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/declaration")
@PreAuthorize("hasRole('USER')")
public class DeclarationController {

    @Autowired private DeclarationService declarationService;
    @Autowired private JwtUtil jwtUtil;

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<DeclarationResponse>>
            startDeclaration(HttpServletRequest request) {

        Long userId = extractUserId(request);
        DeclarationResponse response = declarationService.startDeclaration(userId);
        return ResponseEntity.ok(
            ApiResponse.success("Declaration started", response));
    }

    @PutMapping("/{id}/save")
    public ResponseEntity<ApiResponse<DeclarationResponse>>
            saveDraft(
                @PathVariable Long id,
                HttpServletRequest request,
                @RequestBody DeclarationRequest body) {

        Long userId = extractUserId(request);
        DeclarationResponse response = declarationService.saveDraft(id, userId, body);
        return ResponseEntity.ok(
            ApiResponse.success("Draft saved", response));
    }

    @PutMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<DeclarationResponse>>
            submitDeclaration(
                @PathVariable Long id,
                HttpServletRequest request) {

        Long userId = extractUserId(request);
        DeclarationResponse response = declarationService.submitDeclaration(id, userId);
        return ResponseEntity.ok(
            ApiResponse.success(
                "Declaration submitted successfully. An agent will visit you within 72 hours.",
                response));
    }

    @PostMapping("/{id}/vehicle")
    public ResponseEntity<ApiResponse<VehicleResponse>>
            addVehicle(
                @PathVariable Long id,
                HttpServletRequest request,
                @Valid @RequestBody VehicleRequest body) {

        Long userId = extractUserId(request);
        VehicleResponse response = declarationService.addVehicle(id, userId, body);
        return ResponseEntity.ok(
            ApiResponse.success("Vehicle added", response));
    }

    @DeleteMapping("/{id}/vehicle/{vehicleId}")
    public ResponseEntity<ApiResponse<Void>> removeVehicle(
            @PathVariable Long id,
            @PathVariable Long vehicleId,
            HttpServletRequest request) {

        Long userId = extractUserId(request);
        declarationService.removeVehicle(id, userId, vehicleId);
        return ResponseEntity.ok(
            ApiResponse.success("Vehicle removed"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DeclarationResponse>>
            getDeclaration(
                @PathVariable Long id,
                HttpServletRequest request) {

        Long userId = extractUserId(request);
        DeclarationResponse response = declarationService.getDeclaration(id, userId);
        return ResponseEntity.ok(
            ApiResponse.success("Declaration fetched", response));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<DeclarationResponse>>>
            getHistory(HttpServletRequest request) {

        Long userId = extractUserId(request);
        List<DeclarationResponse> history = declarationService.getHistory(userId);
        return ResponseEntity.ok(
            ApiResponse.success("Declaration history fetched", history));
    }

    private Long extractUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtUtil.extractId(token);
    }
}
