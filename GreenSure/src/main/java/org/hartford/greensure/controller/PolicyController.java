package org.hartford.greensure.controller;

import jakarta.validation.Valid;
import org.hartford.greensure.dto.request.BuyPolicyRequest;
import org.hartford.greensure.dto.response.ApiResponse;
import org.hartford.greensure.dto.response.PolicyResponse;
import org.hartford.greensure.dto.response.UserPolicyResponse;
import org.hartford.greensure.security.SecurityUser;
import org.hartford.greensure.service.PolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    @Autowired
    private PolicyService policyService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PolicyResponse>>> getAvailablePolicies(@AuthenticationPrincipal SecurityUser securityUser) {
        List<PolicyResponse> policies = policyService.getAvailablePolicies(securityUser.getId());
        return ResponseEntity.ok(ApiResponse.<List<PolicyResponse>>builder()
                .success(true)
                .data(policies)
                .build());
    }

    @PostMapping("/buy")
    public ResponseEntity<ApiResponse<UserPolicyResponse>> buyPolicy(
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody BuyPolicyRequest request) {
        UserPolicyResponse response = policyService.buyPolicy(securityUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.<UserPolicyResponse>builder()
                .success(true)
                .data(response)
                .build());
    }

    @GetMapping("/my-policies")
    public ResponseEntity<ApiResponse<List<UserPolicyResponse>>> getMyPolicies(@AuthenticationPrincipal SecurityUser securityUser) {
        List<UserPolicyResponse> myPolicies = policyService.getMyPolicies(securityUser.getId());
        return ResponseEntity.ok(ApiResponse.<List<UserPolicyResponse>>builder()
                .success(true)
                .data(myPolicies)
                .build());
    }
}
