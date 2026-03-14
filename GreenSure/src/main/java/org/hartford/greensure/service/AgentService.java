package org.hartford.greensure.service;

import org.hartford.greensure.dto.request.VerificationRequest;
import org.hartford.greensure.dto.response.*;
import org.hartford.greensure.entity.*;
import org.hartford.greensure.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AgentService {

        @Autowired
        private AgentRepository agentRepository;
        @Autowired
        private AgentAssignmentRepository assignmentRepo;
        @Autowired
        private VerificationRepository verificationRepo;
        @Autowired
        private CarbonDeclarationRepository declarationRepo;
        @Autowired
        private DeclarationVehicleRepository vehicleRepo;
        @Autowired
        private VerifiedVehicleRepository verifiedVehicleRepo;
        @Autowired
        private NotificationService notificationService;
        @Autowired
        private org.hartford.greensure.engine.CarbonScoreService carbonScoreService;

        public List<AgentTaskResponse> getDashboard(Long agentId) {
                return assignmentRepo.findByAgentAgentIdOrderByDeadlineAsc(agentId)
                                .stream()
                                .map(this::mapToTaskResponse)
                                .collect(Collectors.toList());
        }

        public List<AgentTaskResponse> getAssignments(Long agentId, AgentAssignment.AssignmentStatus status) {
                List<AgentAssignment> assignments = status != null
                                ? assignmentRepo.findByAgentAgentIdAndStatus(agentId, status)
                                : assignmentRepo.findByAgentAgentIdOrderByDeadlineAsc(agentId);

                return assignments.stream()
                                .map(this::mapToTaskResponse)
                                .collect(Collectors.toList());
        }

        @Transactional
        public AgentTaskResponse startAssignment(Long assignmentId, Long agentId) {
                AgentAssignment assignment = getAndValidateAssignment(assignmentId, agentId);

                if (assignment.getStatus() != AgentAssignment.AssignmentStatus.ASSIGNED) {
                        throw new RuntimeException("Assignment is not in ASSIGNED status");
                }

                assignment.setStatus(AgentAssignment.AssignmentStatus.IN_PROGRESS);
                assignment = assignmentRepo.save(assignment);
                return mapToTaskResponse(assignment);
        }

        @Transactional
        public void submitVerification(Long assignmentId, Long agentId, VerificationRequest request) {
                AgentAssignment assignment = getAndValidateAssignment(assignmentId, agentId);

                if (assignment.getStatus() != AgentAssignment.AssignmentStatus.IN_PROGRESS) {
                        throw new RuntimeException("Assignment must be IN_PROGRESS before submitting verification");
                }

                if ((request.getOverallAction() == Verification.VerificationAction.MODIFIED ||
                                request.getOverallAction() == Verification.VerificationAction.REJECTED) &&
                                (request.getAgentRemarks() == null || request.getAgentRemarks().isBlank())) {
                        throw new RuntimeException("Agent remarks are required when action is MODIFIED or REJECTED");
                }

                if (verificationRepo.existsByDeclarationDeclarationId(
                                assignment.getDeclaration().getDeclarationId())) {
                        throw new RuntimeException("Verification already submitted for this declaration");
                }

                CarbonDeclaration declaration = assignment.getDeclaration();
                Agent agent = assignment.getAgent();

                Verification verification = Verification.builder()
                                .declaration(declaration)
                                .agent(agent)
                                .correctedElectricityUnits(request.getCorrectedElectricityUnits())
                                .correctedSolarUnits(request.getCorrectedSolarUnits())
                                .correctedCookingFuelType(request.getCorrectedCookingFuelType())
                                .correctedLpgCylinders(request.getCorrectedLpgCylinders())
                                .correctedPngUnits(request.getCorrectedPngUnits())
                                .correctedBiomassKg(request.getCorrectedBiomassKg())
                                .correctedGeneratorHours(request.getCorrectedGeneratorHours())
                                .correctedPublicTransportKm(request.getCorrectedPublicTransportKm())
                                .correctedDietaryPattern(request.getCorrectedDietaryPattern())
                                .correctedShoppingOrders(request.getCorrectedShoppingOrders())
                                .correctedCommercialVehicleKm(request.getCorrectedCommercialVehicleKm())
                                .correctedThirdPartyShipments(request.getCorrectedThirdPartyShipments())
                                .correctedGeneratorLiters(request.getCorrectedGeneratorLiters())
                                .correctedBoilerCoalKg(request.getCorrectedBoilerCoalKg())
                                .correctedBoilerGasScm(request.getCorrectedBoilerGasScm())
                                .correctedPaperReams(request.getCorrectedPaperReams())
                                .correctedRawMaterialKg(request.getCorrectedRawMaterialKg())
                                .overallAction(request.getOverallAction())
                                .agentRemarks(request.getAgentRemarks())
                                .agentGpsLat(request.getAgentGpsLat())
                                .agentGpsLng(request.getAgentGpsLng())
                                .build();

                verification = verificationRepo.save(verification);

                if (request.getCorrectedVehicles() != null) {
                        for (var vr : request.getCorrectedVehicles()) {
                                DeclarationVehicle dv = vehicleRepo.findById(vr.getVehicleId())
                                                .orElseThrow(() -> new RuntimeException(
                                                                "Vehicle not found: " + vr.getVehicleId()));

                                VerifiedVehicle vv = VerifiedVehicle.builder()
                                                .verification(verification)
                                                .declarationVehicle(dv)
                                                .correctedFuelType(vr.getCorrectedFuelType())
                                                .correctedKm(vr.getCorrectedKm())
                                                .build();

                                verifiedVehicleRepo.save(vv);
                        }
                }

                assignment.setStatus(AgentAssignment.AssignmentStatus.COMPLETED);
                assignment.setCompletedAt(LocalDateTime.now());
                assignmentRepo.save(assignment);

                if (request.getOverallAction() == Verification.VerificationAction.REJECTED) {
                        declaration.setStatus(CarbonDeclaration.DeclarationStatus.REJECTED);
                        declaration.setRejectionReason(request.getAgentRemarks());
                        declaration.setResubmissionCount(declaration.getResubmissionCount() + 1);
                        declarationRepo.save(declaration);

                        notificationService.sendToUser(
                                        declaration.getUser().getUserId(),
                                        Notification.NotificationType.REJECTION_ALERT,
                                        "Your declaration was rejected. Reason: " + request.getAgentRemarks() +
                                                        ". You may resubmit with corrected information.");
                } else {
                        declaration.setStatus(CarbonDeclaration.DeclarationStatus.VERIFIED);
                        declarationRepo.save(declaration);

                        // Generate actual footprint score using the verification data engine
                        carbonScoreService.generateScore(declaration.getDeclarationId());

                        notificationService.sendToUser(
                                        declaration.getUser().getUserId(),
                                        Notification.NotificationType.SCORE_READY,
                                        "Your carbon score has been calculated. Login to view your Green Score and personalized recommendations.");
                }
        }

        public AgentPerformanceResponse getPerformance(Long agentId) {
                Agent agent = agentRepository.findById(agentId)
                                .orElseThrow(() -> new RuntimeException("Agent not found"));

                long total = assignmentRepo.countByAgentAgentIdAndStatus(agentId,
                                AgentAssignment.AssignmentStatus.COMPLETED) +
                                assignmentRepo.countByAgentAgentIdAndStatus(agentId,
                                                AgentAssignment.AssignmentStatus.REASSIGNED);

                long completed = assignmentRepo.countByAgentAgentIdAndStatus(agentId,
                                AgentAssignment.AssignmentStatus.COMPLETED);
                long reassigned = assignmentRepo.countByAgentAgentIdAndStatus(agentId,
                                AgentAssignment.AssignmentStatus.REASSIGNED);

                long confirmed = verificationRepo.countByAgentAgentIdAndOverallAction(agentId,
                                Verification.VerificationAction.CONFIRMED);
                long modified = verificationRepo.countByAgentAgentIdAndOverallAction(agentId,
                                Verification.VerificationAction.MODIFIED);
                long rejected = verificationRepo.countByAgentAgentIdAndOverallAction(agentId,
                                Verification.VerificationAction.REJECTED);

                long totalVerifications = confirmed + modified + rejected;

                double completionRate = total > 0 ? (completed * 100.0 / total) : 0;
                double modificationRate = totalVerifications > 0 ? (modified * 100.0 / totalVerifications) : 0;
                double confirmationRate = totalVerifications > 0 ? (confirmed * 100.0 / totalVerifications) : 0;

                return AgentPerformanceResponse.builder()
                                .agentId(agentId)
                                .fullName(agent.getFullName())
                                .employeeId(agent.getEmployeeId())
                                .strikeCount(agent.getStrikeCount())
                                .totalAssignments(total)
                                .completedAssignments(completed)
                                .reassignedAssignments(reassigned)
                                .totalVerifications(totalVerifications)
                                .confirmedCount(confirmed)
                                .modifiedCount(modified)
                                .rejectedCount(rejected)
                                .completionRate(completionRate)
                                .modificationRate(modificationRate)
                                .confirmationRate(confirmationRate)
                                .build();
        }

        private AgentAssignment getAndValidateAssignment(Long assignmentId, Long agentId) {
                AgentAssignment assignment = assignmentRepo.findById(assignmentId)
                                .orElseThrow(() -> new RuntimeException("Assignment not found"));

                if (!assignment.getAgent().getAgentId().equals(agentId)) {
                        throw new RuntimeException("Access denied — this assignment does not belong to you");
                }

                return assignment;
        }

        private AgentTaskResponse mapToTaskResponse(AgentAssignment a) {
                User user = a.getDeclaration().getUser();
                return AgentTaskResponse.builder()
                                .assignmentId(a.getAssignmentId())
                                .status(a.getStatus())
                                .assignedAt(a.getAssignedAt())
                                .deadline(a.getDeadline())
                                .completedAt(a.getCompletedAt())
                                .isOverdue(LocalDateTime.now().isAfter(a.getDeadline()) &&
                                                a.getStatus() != AgentAssignment.AssignmentStatus.COMPLETED)
                                .userId(user.getUserId())
                                .userName(user.getFullName())
                                .userAddress(user.getAddress())
                                .userPinCode(user.getPinCode())
                                .userCity(user.getCity())
                                .userMobile(user.getMobile())
                                .userType(user.getUserType())
                                .declarationId(a.getDeclaration().getDeclarationId())
                                .declarationYear(a.getDeclaration().getDeclarationYear())
                                .build();
        }

        public DeclarationResponse getDeclarationForAssignment(Long assignmentId, Long agentId) {
                AgentAssignment assignment = getAndValidateAssignment(assignmentId, agentId);

                // Use the existing logic to map the declaration to a response object
                DeclarationService declarationService = new DeclarationService();
                return mapDeclarationToResponse(assignment.getDeclaration());
        }

        private DeclarationResponse mapDeclarationToResponse(CarbonDeclaration d) {
                List<VehicleResponse> vehicles = vehicleRepo.findByDeclarationDeclarationId(d.getDeclarationId())
                                .stream()
                                .map(v -> VehicleResponse.builder()
                                                .vehicleId(v.getVehicleId())
                                                .vehicleType(v.getVehicleType())
                                                .fuelType(v.getFuelType())
                                                .kmPerMonth(v.getKmPerMonth())
                                                .quantity(v.getQuantity())
                                                .build())
                                .collect(Collectors.toList());

                return DeclarationResponse.builder()
                                .declarationId(d.getDeclarationId())
                                .userId(d.getUser().getUserId())
                                .declarationYear(d.getDeclarationYear())
                                .status(d.getStatus())
                                .resubmissionCount(d.getResubmissionCount())
                                .submittedAt(d.getSubmittedAt())
                                .createdAt(d.getCreatedAt())
                                .rejectionReason(d.getRejectionReason())
                                .electricityUnits(d.getElectricityUnits())
                                .hasSolar(d.getHasSolar())
                                .solarUnits(d.getSolarUnits())
                                .cookingFuelType(d.getCookingFuelType())
                                .lpgCylinders(d.getLpgCylinders())
                                .pngUnits(d.getPngUnits())
                                .biomassKgPerDay(d.getBiomassKgPerDay())
                                .numAcUnits(d.getNumAcUnits())
                                .acHoursPerDay(d.getAcHoursPerDay())
                                .hasGenerator(d.getHasGenerator())
                                .generatorHoursPerMonth(d.getGeneratorHoursPerMonth())
                                .usesPublicTransport(d.getUsesPublicTransport())
                                .publicTransportKm(d.getPublicTransportKm())
                                .vehicles(vehicles)
                                .dietaryPattern(d.getDietaryPattern())
                                .shoppingOrdersPerMonth(d.getShoppingOrdersPerMonth())
                                .hasCommercialVehicles(d.getHasCommercialVehicles())
                                .commercialVehicleKm(d.getCommercialVehicleKm())
                                .thirdPartyShipments(d.getThirdPartyShipments())
                                .employeesPrivateVehicle(d.getEmployeesPrivateVehicle())
                                .employeesPublicTransport(d.getEmployeesPublicTransport())
                                .generatorLitersPerMonth(d.getGeneratorLitersPerMonth())
                                .hasBoiler(d.getHasBoiler())
                                .boilerFuelType(d.getBoilerFuelType())
                                .boilerCoalKg(d.getBoilerCoalKg())
                                .boilerGasScm(d.getBoilerGasScm())
                                .paperReamsPerMonth(d.getPaperReamsPerMonth())
                                .usesRecycledPaper(d.getUsesRecycledPaper())
                                .rawMaterialType(d.getRawMaterialType())
                                .rawMaterialKg(d.getRawMaterialKg())
                                .build();
        }
}
