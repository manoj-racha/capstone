package org.hartford.greensure.engine;

import org.hartford.greensure.dto.response.CarbonScoreResponse;
import org.hartford.greensure.entity.CarbonDeclaration;
import org.hartford.greensure.entity.CarbonScore;
import org.hartford.greensure.entity.CookingData;
import org.hartford.greensure.entity.ElectricityData;
import org.hartford.greensure.entity.HouseholdProfile;
import org.hartford.greensure.entity.User;
import org.hartford.greensure.enums.CookingFuel;
import org.hartford.greensure.enums.Zone;
import org.hartford.greensure.repository.CarbonDeclarationRepository;
import org.hartford.greensure.repository.CarbonScoreRepository;
import org.hartford.greensure.repository.CookingDataRepository;
import org.hartford.greensure.repository.DeclarationVehicleDataRepository;
import org.hartford.greensure.repository.HouseholdProfileRepository;
import org.hartford.greensure.repository.SolarDataRepository;
import org.hartford.greensure.service.RecommendationService;
import org.hartford.greensure.service.ai.GeminiApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarbonScoreServiceTest {

    @Mock private CarbonDeclarationRepository declarationRepo;
    @Mock private HouseholdProfileRepository householdRepo;
    @Mock private CarbonScoreRepository scoreRepo;
    @Mock private RecommendationService recommendationService;
    @Mock private GeminiApiService geminiApiService;
    @Mock private DeclarationVehicleDataRepository vehicleRepository;
    @Mock private CookingDataRepository cookingDataRepository;
    @Mock private SolarDataRepository solarDataRepository;

    @InjectMocks
    private CarbonScoreService carbonScoreService;

    private User user;
    private CarbonDeclaration declaration;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(1L);
        user.setUserType(User.UserType.HOUSEHOLD);

        declaration = new CarbonDeclaration();
        declaration.setDeclarationId(100L);
        declaration.setDeclarationYear(2025);
        declaration.setUser(user);

        ElectricityData electricity = new ElectricityData();
        electricity.setUserDeclaredMonthlyKwh(100.0);
        declaration.setElectricityData(electricity);

        CookingData cooking = new CookingData();
        cooking.setFuelType(CookingFuel.LPG);
        cooking.setUserDeclaredCylinders(6);
        declaration.setCookingData(cooking);
    }

    @Test
    void getMyScore_mapsEntity() {
        CarbonScore score = CarbonScore.builder()
                .scoreId(10L)
                .scoreYear(2025)
                .totalCo2(123.45)
                .perCapitaCo2(61.72)
                .zone(Zone.IMPROVER)
                .build();
        when(scoreRepo.findTopByUserUserIdOrderByScoreYearDesc(1L)).thenReturn(Optional.of(score));

        CarbonScoreResponse response = carbonScoreService.getMyScore(1L);

        assertNotNull(response);
        assertEquals(10L, response.getScoreId());
        assertEquals("IMPROVER", response.getZone());
    }

    @Test
    void generateScore_savesWhenMissing() {
        when(scoreRepo.existsByDeclarationDeclarationId(100L)).thenReturn(false);
        when(declarationRepo.findById(100L)).thenReturn(Optional.of(declaration));
        when(householdRepo.findByUserUserId(1L)).thenReturn(Optional.of(HouseholdProfile.builder().numberOfMembers(2).build()));
        when(scoreRepo.save(any(CarbonScore.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.findByDeclarationDeclarationId(100L)).thenReturn(Collections.emptyList());
        when(cookingDataRepository.findByDeclarationDeclarationId(100L)).thenReturn(Optional.of(declaration.getCookingData()));
        when(solarDataRepository.findByDeclarationDeclarationId(100L)).thenReturn(Optional.empty());
        when(scoreRepo.findTopByUserUserIdAndScoreYearLessThanOrderByScoreYearDesc(1L, 2025))
                .thenReturn(Optional.empty());
        when(geminiApiService.generateScoreExplanation(any(), any(), any(), any(), any(), any()))
                .thenReturn("AI explanation text");

        carbonScoreService.generateScore(100L);

        verify(scoreRepo, times(2)).save(any(CarbonScore.class));
        verify(recommendationService, times(1)).generateRecommendations(any(CarbonScore.class));
        verify(geminiApiService, times(1))
                .generateScoreExplanation(any(), any(), eq(Collections.emptyList()), any(), any(), isNull());
    }

    @Test
    void generateScore_skipsWhenAlreadyPresent() {
        when(scoreRepo.existsByDeclarationDeclarationId(100L)).thenReturn(true);

        carbonScoreService.generateScore(100L);

        verify(declarationRepo, never()).findById(any());
        verify(scoreRepo, never()).save(any());
    }

    @Test
    void getScoreHistory_mapsList() {
        CarbonScore s1 = CarbonScore.builder().scoreId(1L).scoreYear(2025).zone(Zone.GREEN_CHAMPION).build();
        CarbonScore s2 = CarbonScore.builder().scoreId(2L).scoreYear(2024).zone(Zone.DEFAULTER).build();
        when(scoreRepo.findByUserUserIdOrderByScoreYearDesc(1L)).thenReturn(List.of(s1, s2));

        List<CarbonScoreResponse> history = carbonScoreService.getScoreHistory(1L);

        assertEquals(2, history.size());
        assertEquals(1L, history.get(0).getScoreId());
        assertEquals(2L, history.get(1).getScoreId());
    }
}
