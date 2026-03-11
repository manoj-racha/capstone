package org.hartford.greensure.service;

import org.hartford.greensure.dto.response.CarbonScoreResponse;
import org.hartford.greensure.dto.response.DashboardResponse;
import org.hartford.greensure.dto.response.UserProfileResponse;
import org.hartford.greensure.entity.*;
import org.hartford.greensure.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private HouseholdProfileRepository householdRepo;

    @Mock
    private MsmeProfileRepository msmeRepo;

    @Mock
    private CarbonDeclarationRepository declarationRepo;

    @Mock
    private CarbonScoreRepository scoreRepository;

    @Mock
    private NotificationRepository notificationRepo;

    @InjectMocks
    private UserService userService;

    private User householdUser;
    private User msmeUser;

    @BeforeEach
    void setUp() {
        householdUser = new User();
        householdUser.setUserId(1L);
        householdUser.setUserType(User.UserType.HOUSEHOLD);
        householdUser.setFullName("Household Name");
        householdUser.setEmail("household@test.com");
        householdUser.setCity("CityA");

        msmeUser = new User();
        msmeUser.setUserId(2L);
        msmeUser.setUserType(User.UserType.MSME);
        msmeUser.setFullName("MSME Name");
        msmeUser.setEmail("msme@test.com");
        msmeUser.setCity("CityB");
    }

    @Test
    void testGetProfile_Household() {
        HouseholdProfile hp = new HouseholdProfile();
        hp.setNumberOfMembers(4);
        hp.setDwellingType(HouseholdProfile.DwellingType.APARTMENT);

        when(userRepository.findById(1L)).thenReturn(Optional.of(householdUser));
        when(householdRepo.findByUserUserId(1L)).thenReturn(Optional.of(hp));

        UserProfileResponse response = userService.getProfile(1L);

        assertNotNull(response);
        assertEquals("Household Name", response.getFullName());
        assertEquals(4, response.getNumberOfMembers());
        assertEquals(HouseholdProfile.DwellingType.APARTMENT, response.getDwellingType());

        verify(userRepository, times(1)).findById(1L);
        verify(householdRepo, times(1)).findByUserUserId(1L);
    }

    @Test
    void testGetProfile_Msme() {
        MsmeProfile mp = new MsmeProfile();
        mp.setBusinessName("Test Business");
        mp.setNumEmployees(10);

        when(userRepository.findById(2L)).thenReturn(Optional.of(msmeUser));
        when(msmeRepo.findByUserUserId(2L)).thenReturn(Optional.of(mp));

        UserProfileResponse response = userService.getProfile(2L);

        assertNotNull(response);
        assertEquals("MSME Name", response.getFullName());
        assertEquals("Test Business", response.getBusinessName());
        assertEquals(10, response.getNumEmployees());

        verify(userRepository, times(1)).findById(2L);
        verify(msmeRepo, times(1)).findByUserUserId(2L);
    }

    @Test
    void testGetProfile_UserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        Exception e = assertThrows(RuntimeException.class, () -> {
            userService.getProfile(99L);
        });

        assertEquals("User not found", e.getMessage());
    }

    @Test
    void testUpdateProfile() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(householdUser));
        HouseholdProfile hp = new HouseholdProfile();
        when(householdRepo.findByUserUserId(1L)).thenReturn(Optional.of(hp));

        UserProfileResponse updateRequest = UserProfileResponse.builder()
                .fullName("Updated Name")
                .city("Updated City")
                .build();

        UserProfileResponse response = userService.updateProfile(1L, updateRequest);

        assertNotNull(response);
        assertEquals("Updated Name", response.getFullName());
        assertEquals("Updated City", response.getCity());

        verify(userRepository, times(1)).save(householdUser);
    }

    @Test
    void testGetDashboard_NoDeclaration() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(householdUser));
        when(declarationRepo.existsByUserUserId(1L)).thenReturn(false);

        DashboardResponse response = userService.getDashboard(1L);

        assertNotNull(response);
        assertEquals(1L, response.getUserId());
        assertEquals("Household Name", response.getFullName());
        assertFalse(response.isHasDeclaration());

        verify(declarationRepo, times(1)).existsByUserUserId(1L);
    }

    @Test
    void testGetDashboard_WithDeclarationAndScore() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(householdUser));
        when(declarationRepo.existsByUserUserId(1L)).thenReturn(true);

        int currentYear = java.time.LocalDate.now().getYear();

        CarbonDeclaration declaration = new CarbonDeclaration();
        declaration.setDeclarationId(10L);
        declaration.setStatus(CarbonDeclaration.DeclarationStatus.VERIFIED);
        declaration.setDeclarationYear(currentYear);
        when(declarationRepo.findByUserUserIdAndDeclarationYear(1L, currentYear)).thenReturn(Optional.of(declaration));

        CarbonScore score = new CarbonScore();
        score.setScoreId(100L);
        score.setUser(householdUser);
        score.setScoreYear(currentYear);
        score.setTotalCo2(1000.0);
        score.setPerCapitaCo2(250.0);
        score.setZone(CarbonScore.CarbonZone.GREEN_CHAMPION);
        when(scoreRepository.findTopByUserUserIdOrderByScoreYearDesc(1L)).thenReturn(Optional.of(score));

        // Renewal due
        when(declarationRepo.findByUserUserIdAndDeclarationYear(1L, currentYear - 1)).thenReturn(Optional.empty());
        when(scoreRepository.existsByDeclarationDeclarationId(-1L)).thenReturn(false);

        when(notificationRepo.countByRecipientTypeAndRecipientIdAndStatus(
                Notification.RecipientType.USER, 1L, Notification.NotificationStatus.SENT)).thenReturn(5L);

        DashboardResponse response = userService.getDashboard(1L);

        assertNotNull(response);
        assertTrue(response.isHasDeclaration());
        assertEquals(10L, response.getCurrentDeclarationId());
        assertEquals(CarbonDeclaration.DeclarationStatus.VERIFIED, response.getDeclarationStatus());
        assertNotNull(response.getLatestScore());
        assertEquals(1000.0, response.getLatestScore().getTotalCo2());
        assertEquals(CarbonScore.CarbonZone.GREEN_CHAMPION, response.getZone());
        assertFalse(response.isRenewalDue());
        assertEquals(5L, response.getUnreadNotifications());
    }
}
