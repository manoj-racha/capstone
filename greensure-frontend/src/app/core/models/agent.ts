export interface AgentTaskResponse {
  assignmentId: number;
  status: string;        // "ASSIGNED" | "IN_PROGRESS" | "COMPLETED" | "REASSIGNED"
  assignedAt: string;
  deadline: string;
  completedAt?: string;
  isOverdue: boolean;

  // User info — who the agent visits
  userId: number;
  userName: string;
  userAddress: string;
  userPinCode: string;
  userCity: string;
  userMobile: string;
  userType: string;

  // Declaration info
  declarationId: number;
  declarationYear: number;
}

export interface AgentPerformanceResponse {
  agentId: number;
  fullName: string;
  employeeId: string;
  strikeCount: number;

  // Assignment stats
  totalAssignments: number;
  completedAssignments: number;
  reassignedAssignments: number;

  // Verification stats
  totalVerifications: number;
  confirmedCount: number;
  modifiedCount: number;
  rejectedCount: number;

  // Rates — percentages
  completionRate: number;
  modificationRate: number;
  confirmationRate: number;
}

export interface VerificationRequest {
  // All corrected fields — null means agent confirmed declared value
  correctedElectricityUnits?: number;
  correctedSolarUnits?: number;
  correctedCookingFuelType?: string;
  correctedLpgCylinders?: number;
  correctedPngUnits?: number;
  correctedBiomassKg?: number;
  correctedGeneratorHours?: number;
  correctedPublicTransportKm?: number;
  correctedDietaryPattern?: string;
  correctedShoppingOrders?: string;
  correctedCommercialVehicleKm?: number;
  correctedThirdPartyShipments?: number;
  correctedGeneratorLiters?: number;
  correctedBoilerCoalKg?: number;
  correctedBoilerGasScm?: number;
  correctedPaperReams?: number;
  correctedRawMaterialKg?: number;
  correctedVehicles?: VerifiedVehicleRequest[];

  // Required fields
  overallAction: string;   // "CONFIRMED" | "MODIFIED" | "REJECTED"
  agentRemarks?: string;   // Required if MODIFIED or REJECTED
  agentGpsLat: number;
  agentGpsLng: number;
}

export interface VerifiedVehicleRequest {
  vehicleId: number;
  correctedFuelType?: string;
  correctedKm?: number;
}