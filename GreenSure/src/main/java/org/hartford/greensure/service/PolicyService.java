package org.hartford.greensure.service;

import org.hartford.greensure.dto.request.BuyPolicyRequest;
import org.hartford.greensure.dto.response.PolicyPlanResponse;
import org.hartford.greensure.dto.response.PolicyResponse;
import org.hartford.greensure.dto.response.UserPolicyResponse;
import org.hartford.greensure.entity.Policy;
import org.hartford.greensure.entity.PolicyPlan;
import org.hartford.greensure.entity.User;
import org.hartford.greensure.entity.UserPolicy;
import org.hartford.greensure.exception.BadRequestException;
import org.hartford.greensure.exception.ResourceNotFoundException;
import org.hartford.greensure.repository.PolicyPlanRepository;
import org.hartford.greensure.repository.PolicyRepository;
import org.hartford.greensure.repository.UserPolicyRepository;
import org.hartford.greensure.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PolicyService {

    @Autowired private PolicyRepository policyRepository;
    @Autowired private PolicyPlanRepository policyPlanRepository;
    @Autowired private UserPolicyRepository userPolicyRepository;
    @Autowired private UserRepository userRepository;

    public List<PolicyResponse> getAvailablePolicies(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String userType = user.getUserType().name();
        
        // Let's assume HOUSEHOLD sees null eligibility, MSME sees ALL
        List<Policy> policies;
        if ("HOUSEHOLD".equals(userType)) {
             policies = policyRepository.findByEligibilityOrEligibilityIsNull(null);
        } else {
             // MSME can see MSME and null
             policies = policyRepository.findAll();
        }

        return policies.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public UserPolicyResponse buyPolicy(Long userId, BuyPolicyRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        PolicyPlan plan = policyPlanRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Policy Plan not found"));

        UserPolicy userPolicy = UserPolicy.builder()
                .user(user)
                .plan(plan)
                .durationMonths(request.getDurationMonths())
                .finalPrice(request.getFinalPrice())
                .purchasedAt(LocalDateTime.now())
                .build();

        userPolicy = userPolicyRepository.save(userPolicy);
        return mapToUserPolicyResponse(userPolicy);
    }
    
    public List<UserPolicyResponse> getMyPolicies(Long userId) {
        return userPolicyRepository.findByUserUserIdOrderByPurchasedAtDesc(userId)
                .stream()
                .map(this::mapToUserPolicyResponse)
                .collect(Collectors.toList());
    }

    private PolicyResponse mapToResponse(Policy policy) {
        List<PolicyPlanResponse> planResponses = policy.getPlans().stream()
                .map(this::mapPlanToResponse)
                .collect(Collectors.toList());

        return PolicyResponse.builder()
                .policyType(policy.getPolicyType())
                .name(policy.getName())
                .icon(policy.getIcon())
                .description(policy.getDescription())
                .eligibility(policy.getEligibility())
                .plans(planResponses)
                .build();
    }

    private PolicyPlanResponse mapPlanToResponse(PolicyPlan plan) {
        return PolicyPlanResponse.builder()
                .planId(plan.getPlanId())
                .planName(plan.getPlanName())
                .coverageAmount(plan.getCoverageAmount())
                .basePremiumYearly(plan.getBasePremiumYearly())
                .features(plan.getFeatures())
                .build();
    }
    
    private UserPolicyResponse mapToUserPolicyResponse(UserPolicy up) {
        return UserPolicyResponse.builder()
                .id(up.getId())
                .userId(up.getUser().getUserId())
                .planId(up.getPlan().getPlanId())
                .policyType(up.getPlan().getPolicy().getPolicyType())
                .policyName(up.getPlan().getPolicy().getName())
                .planName(up.getPlan().getPlanName())
                .coverageAmount(up.getPlan().getCoverageAmount())
                .durationMonths(up.getDurationMonths())
                .finalPrice(up.getFinalPrice())
                .purchasedAt(up.getPurchasedAt())
                .build();
    }
}
