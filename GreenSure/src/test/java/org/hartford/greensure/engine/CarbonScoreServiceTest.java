package org.hartford.greensure.engine;

import org.hartford.greensure.dto.response.CarbonScoreResponse;
import org.hartford.greensure.entity.*;
import org.hartford.greensure.repository.*;
import org.hartford.greensure.service.RecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CarbonScoreServiceTest {

    @Mock private CarbonDeclarationRepository declarationRepo;
    @Mock private VerificationRepository verificationRepo;
    @Mock private DeclarationVehicleRepository vehicleRepo;
    @Mock private VerifiedVehicleRepository verifiedVehicleRepo;
    @Mock private HouseholdProfileRepository householdRepo;
    @Mock private MsmeProfileRepository msmeRepo;
    @Mock private CarbonScoreRepository scoreRepo;
    @Mock private RecommendationService recommendationService;

    @InjectMocks
    private CarbonScoreService carbonScoreService;

    private User householdUser;
    private CarbonDeclaration declaration;
    private Verification verification;
    private CarbonScore score;

    @BeforeEach
    void setUp() {
        householdUser = new User();
        householdUser.setUserId(1L);
        householdUser.setUserType(User.UserType.HOUSEHOLD);

        declaration = new CarbonDeclaration();
        declaration.setDeclarationId(100L);
        declaration.setUser(householdUser);
        declaration.setDeclarationYear(2023);

        verification = new Verification();
        verification.setVerificationId(200L);
        verification.setDeclaration(declaration);

        score = new CarbonScore();
        score.setScoreId(300L);
        score.setUser(householdUser);
        score.setDeclaration(declaration);
        score.setScoreYear(2023);
        score.setEnergyCo2(100.0);
        score.setTransportCo2(200.0);
        score.setLifestyleCo2(300.0);
        score.setTotalCo2(600.0);
        score.setPerCapitaCo2(150.0);
        score.setZone(CarbonScore.CarbonZone.GREEN_IMPROVER);
    }

    @Test
    void testGetMyScore() {
        when(scoreRepo.findTopByUserUserIdOrderByScoreYearDesc(1L)).thenReturn(Optional.of(score));

        CarbonScoreResponse response = carbonScoreService.getMyScore(1L);

        assertNotNull(response);
        assertEquals(300L, response.getScoreId());
        assertEquals(1L, response.getUserId());
        assertEquals(600.0, response.getTotalCo2());
        assertEquals(CarbonScore.CarbonZone.GREEN_IMPROVER, response.getZone());

        verify(scoreRepo, times(1)).findTopByUserUserIdOrderByScoreYearDesc(1L);
    }

    @Test
    void testGetMyScore_NotFound() {
        when(scoreRepo.findTopByUserUserIdOrderByScoreYearDesc(99L)).thenReturn(Optional.empty());

        Exception e = assertThrows(RuntimeException.class, () -> {
            carbonScoreService.getMyScore(99L);
        });

        assertEquals("No score found yet", e.getMessage());
    }

    @Test
    void testGetScoreHistory() {
        CarbonScore score2 = new CarbonScore();
        score2.setScoreId(301L);
        score2.setUser(householdUser);
        score2.setScoreYear(2022);
        score2.setEnergyCo2(200.0);
        score2.setTransportCo2(200.0);
        score2.setLifestyleCo2(400.0);
        score2.setTotalCo2(800.0);
        score2.setPerCapitaCo2(200.0);
        score2.setZone(CarbonScore.CarbonZone.GREEN_DEFAULTER);

        when(scoreRepo.findByUserUserIdOrderByScoreYearDesc(1L)).thenReturn(Arrays.asList(score, score2));

        List<CarbonScoreResponse> response = carbonScoreService.getScoreHistory(1L);

        assertNotNull(response);
        assertEquals(2, response.size());
        assertEquals(300L, response.get(0).getScoreId());
        assertEquals(301L, response.get(1).getScoreId());

        verify(scoreRepo, times(1)).findByUserUserIdOrderByScoreYearDesc(1L);
    }

    @Test
    void testMapToResponse() {
        CarbonScoreResponse response = carbonScoreService.mapToResponse(score);

        assertNotNull(response);
        assertEquals(300L, response.getScoreId());
        assertEquals(100.0, response.getEnergyCo2());
        assertEquals(200.0, response.getTransportCo2());
        assertEquals(300.0, response.getLifestyleCo2());
        assertEquals(600.0, response.getTotalCo2());
        assertEquals(150.0, response.getPerCapitaCo2());

        // percentages
        assertEquals(Math.round(100.0 * 100.0 / 600.0 * 100.0) / 100.0, response.getEnergyPercentage());
        assertEquals(Math.round(200.0 * 100.0 / 600.0 * 100.0) / 100.0, response.getTransportPercentage());
        assertEquals(Math.round(300.0 * 100.0 / 600.0 * 100.0) / 100.0, response.getLifestylePercentage());
    }

    @Test
    void testGenerateScore_BasicHousehold() {
        // Setup simple household declaration
        declaration.setElectricityUnits(100.0);
        declaration.setHasSolar(false);
        declaration.setCookingFuelType(CarbonDeclaration.CookingFuelType.LPG);
        declaration.setLpgCylinders(1.0);
        declaration.setHasGenerator(false);
        declaration.setUsesPublicTransport(false);
        declaration.setDietaryPattern(CarbonDeclaration.DietaryPattern.VEGETARIAN);
        declaration.setShoppingOrdersPerMonth(CarbonDeclaration.ShoppingOrders.ZERO_TO_FIVE);

        when(scoreRepo.existsByDeclarationDeclarationId(100L)).thenReturn(false);
        when(declarationRepo.findById(100L)).thenReturn(Optional.of(declaration));
        when(verificationRepo.findByDeclarationDeclarationId(100L)).thenReturn(Optional.of(verification));
        when(vehicleRepo.findByDeclarationDeclarationId(100L)).thenReturn(Collections.emptyList());

        HouseholdProfile hp = new HouseholdProfile();
        hp.setNumberOfMembers(2);
        when(householdRepo.findByUserUserId(1L)).thenReturn(Optional.of(hp));

        when(scoreRepo.save(any(CarbonScore.class))).thenAnswer(i -> i.getArguments()[0]);
        doNothing().when(recommendationService).generateRecommendations(any(CarbonScore.class));

        carbonScoreService.generateScore(100L);

        ArgumentCaptor<CarbonScore> scoreCaptor = ArgumentCaptor.forClass(CarbonScore.class);
        verify(scoreRepo, times(1)).save(scoreCaptor.capture());
        CarbonScore savedScore = scoreCaptor.getValue();

        assertNotNull(savedScore);
        assertEquals(1L, savedScore.getUser().getUserId());
        assertEquals(100L, savedScore.getDeclaration().getDeclarationId());
        assertTrue(savedScore.getEnergyCo2() > 0);
        assertEquals(0.0, savedScore.getTransportCo2()); // No vehicles, no public transport
        assertTrue(savedScore.getLifestyleCo2() > 0);
        assertNull(savedScore.getOperationsCo2()); // Household doesn't have operations co2
    }

    @Test
    void testGenerateScore_AlreadyGenerated() {
        when(scoreRepo.existsByDeclarationDeclarationId(100L)).thenReturn(true);

        carbonScoreService.generateScore(100L);

        verify(declarationRepo, never()).findById(anyLong());
        verify(scoreRepo, never()).save(any(CarbonScore.class));
    }
}
