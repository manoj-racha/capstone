package org.hartford.greensure.service;

import org.hartford.greensure.dto.response.*;
import org.hartford.greensure.engine.CarbonScoreService;
import org.hartford.greensure.entity.*;
import org.hartford.greensure.exception.UserNotFoundException;
import org.hartford.greensure.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private HouseholdProfileRepository householdRepo;
    @Autowired
    private CarbonDeclarationRepository declarationRepo;
    @Autowired
    private CarbonScoreRepository scoreRepository;
    @Autowired
    private NotificationRepository notificationRepo;
    @Autowired
    private CarbonScoreService carbonScoreService;

    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        UserProfileResponse.UserProfileResponseBuilder builder = UserProfileResponse.builder()
                .userId(user.getUserId())
                .userType(user.getUserType())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .address(user.getAddress())
                .pinCode(user.getPinCode())
                .city(user.getCity())
                .state(user.getState())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt());

        if (user.getUserType() == User.UserType.HOUSEHOLD) {
            householdRepo.findByUserUserId(userId).ifPresent(p -> {
                builder.numberOfMembers(p.getNumberOfMembers());
                builder.dwellingType(p.getDwellingType());
            });
        }

        return builder.build();
    }

    public UserProfileResponse updateProfile(Long userId, UserProfileResponse request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        if (request.getPinCode() != null) {
            user.setPinCode(request.getPinCode());
        }
        if (request.getCity() != null) {
            user.setCity(request.getCity());
        }
        if (request.getState() != null) {
            user.setState(request.getState());
        }

        userRepository.save(user);
        return getProfile(userId);
    }

    public DashboardResponse getDashboard(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        boolean hasDeclaration = declarationRepo.existsByUserUserId(userId);

        DashboardResponse.DashboardResponseBuilder builder = DashboardResponse.builder()
                .userId(userId)
                .fullName(user.getFullName())
                .userType(user.getUserType())
                .hasDeclaration(hasDeclaration);

        if (!hasDeclaration) {
            return builder.build();
        }

        int currentYear = java.time.LocalDate.now().getYear();
        declarationRepo.findByUserUserIdAndDeclarationYear(userId, currentYear).ifPresent(d -> {
            builder.currentDeclarationId(d.getDeclarationId());
            builder.declarationStatus(d.getStatus());
            builder.declarationYear(d.getDeclarationYear());
        });

        carbonScoreService.backfillAiExplanationForLatestScoreIfMissing(userId);

        scoreRepository.findTopByUserUserIdOrderByScoreYearDesc(userId).ifPresent(score -> {
            builder.latestScore(buildScoreResponse(score));
            builder.zone(score.getZone());
        });

        boolean renewalDue = scoreRepository.existsByDeclarationDeclarationId(
                declarationRepo.findByUserUserIdAndDeclarationYear(userId, currentYear - 1)
                        .map(d -> d.getDeclarationId())
                        .orElse(-1L));

        builder.renewalDue(renewalDue);

        long unread = notificationRepo.countByRecipientTypeAndRecipientIdAndStatus(
                Notification.RecipientType.USER,
                userId,
                Notification.NotificationStatus.SENT);

        builder.unreadNotifications(unread);

        return builder.build();
    }

    private CarbonScoreResponse buildScoreResponse(CarbonScore score) {
        return CarbonScoreResponse.builder()
                .scoreId(score.getScoreId())
                .scoreYear(score.getScoreYear())
                .vehicleCo2(score.getVehicleCo2())
                .electricityCo2(score.getElectricityCo2())
                .cookingCo2(score.getCookingCo2())
                .solarOffset(score.getSolarOffset())
                .lifestyleBonus(score.getLifestyleBonus())
                .totalCo2(score.getTotalCo2())
                .perCapitaCo2(score.getPerCapitaCo2())
                .zone(score.getZone() != null ? score.getZone().name() : null)
                .discountPercent(score.getDiscountPercent())
                .discountBreakdown(score.getDiscountBreakdown())
                .aiExplanation(score.getAiExplanation())
                .build();
    }
}
