package org.hartford.greensure.controller;

import org.hartford.greensure.dto.request.*;
import org.hartford.greensure.dto.response.*;
import org.hartford.greensure.security.SecurityUser;
import org.hartford.greensure.service.DeclarationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/declaration")
@PreAuthorize("hasRole('USER')")
public class DeclarationController {

    @Autowired private DeclarationService declarationService;

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<DeclarationResponse>>
            startDeclaration(@AuthenticationPrincipal SecurityUser user) {

        DeclarationResponse response = declarationService.startDeclaration(user.getId());
        return ResponseEntity.ok(
            ApiResponse.success("Declaration started", response));
    }

    @PutMapping("/{id}/save")
    public ResponseEntity<ApiResponse<DeclarationResponse>>
            saveDraft(
                @PathVariable Long id,
                @AuthenticationPrincipal SecurityUser user,
                @RequestBody DeclarationRequest body) {

        DeclarationResponse response = declarationService.saveDraft(id, user.getId(), body);
        return ResponseEntity.ok(
            ApiResponse.success("Draft saved", response));
    }

    @PutMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<DeclarationResponse>>
            submitDeclaration(
                @PathVariable Long id,
                @AuthenticationPrincipal SecurityUser user) {

        DeclarationResponse response = declarationService.submitDeclaration(id, user.getId());
        return ResponseEntity.ok(
            ApiResponse.success(
                "Declaration submitted successfully. An agent will visit you within 72 hours.",
                response));
    }

    @PostMapping("/{id}/vehicle")
    public ResponseEntity<ApiResponse<VehicleResponse>>
            addVehicle(
                @PathVariable Long id,
                @AuthenticationPrincipal SecurityUser user,
                @Valid @RequestBody VehicleRequest body) {

        VehicleResponse response = declarationService.addVehicle(id, user.getId(), body);
        return ResponseEntity.ok(
            ApiResponse.success("Vehicle added", response));
    }

    @DeleteMapping("/{id}/vehicle/{vehicleId}")
    public ResponseEntity<ApiResponse<Void>> removeVehicle(
            @PathVariable Long id,
            @PathVariable Long vehicleId,
            @AuthenticationPrincipal SecurityUser user) {

        declarationService.removeVehicle(id, user.getId(), vehicleId);
        return ResponseEntity.ok(
            ApiResponse.success("Vehicle removed"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DeclarationResponse>>
            getDeclaration(
                @PathVariable Long id,
                @AuthenticationPrincipal SecurityUser user) {

        DeclarationResponse response = declarationService.getDeclaration(id, user.getId());
        return ResponseEntity.ok(
            ApiResponse.success("Declaration fetched", response));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<DeclarationResponse>>>
            getHistory(@AuthenticationPrincipal SecurityUser user) {

        List<DeclarationResponse> history = declarationService.getHistory(user.getId());
        return ResponseEntity.ok(
            ApiResponse.success("Declaration history fetched", history));
    }
}
