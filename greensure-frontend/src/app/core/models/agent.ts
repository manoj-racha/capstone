export interface AgentTaskResponse {
  assignmentId: number;
  status: string;        // "ASSIGNED" | "IN_PROGRESS" | "COMPLETED" | "REASSIGNED"
  assignmentStatus?: string; // "ACTIVE" | "COMPLETED" | "REASSIGNED" | "CANCELLED"
  assignedAt: string;
  deadline: string;
  completedAt?: string;
  isOverdue: boolean;
  assignedBy?: string;
  reassignReason?: string;

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

  agentId?: number;
  agentName?: string;
}

export type AgentTaskSummary = AgentTaskResponse;

export interface AgentProfile {
  agentId: number;
  fullName: string;
  email?: string;
  mobile?: string;
  employeeId?: string;
  assignedZones?: string;
  strikeCount?: number;
  strikes: number;
  active: boolean;
  activeAssignments: number;
  status?: string;
  createdAt?: string;
}

export type MatchStatus = 'MATCH' | 'MISMATCH' | 'UNVERIFIED' | 'NOT_APPLICABLE';

export interface AgentChecklistItem {
  priority: string;
  category: string;
  finding: string;
  action: string;
}

export interface AiElectricityAnalysis {
  providerMatch: boolean;
  providerOnBills: string | null;
  consumerNumberMatch: boolean;
  consumerNumberOnBills: string | null;
  consumerNumberConsistent: boolean;
  aiComputedMonthlyAvgKwh: number | null;
  userDeclaredMonthlyKwh: number;
  kwhDifference: number | null;
  kwhMatch: boolean;
  billsCovered: number;
  duplicateMonths: string[];
  missingMonths: string[];
  findings: string[];
}

export interface AiVehicleAnalysis {
  vehicleIndex: number;
  registrationNumberOnDoc: string | null;
  registrationNumberMatch: boolean;
  fuelTypeOnDoc: string | null;
  fuelTypeMatch: boolean;
  documentsRead: number;
  findings: string[];
}

export interface AiCookingAnalysis {
  fuelType: string;
  receiptsFound: number;
  declaredCylinders: number | null;
  cylinderCountMatch: boolean;
  distributorName: string | null;
  consumerNumberConsistent: boolean;
  findings: string[];
}

export interface AiSolarAnalysis {
  capacityOnCertificate: number | null;
  declaredCapacity: number | null;
  capacityMatch: boolean;
  addressOnCertificate: string | null;
  addressMatch: boolean;
  findings: string[];
}

export interface AiDocumentAnalysisResult {
  electricity: AiElectricityAnalysis | null;
  vehicles: AiVehicleAnalysis[];
  cooking: AiCookingAnalysis | null;
  solar: AiSolarAnalysis | null;
  overallFindings: AgentChecklistItem[];
  analysisSuccess: boolean;
  errorMessage: string | null;
  analysedAt: string;
}

export interface AgentWorkspace {
  assignmentId: number;
  declarationId: number;
  declarationYear?: number;
  userId?: number;
  userName?: string;
  userPhone?: string;
  userAddress?: string;
  userPinCode?: string;
  userCity?: string;
  userMobile?: string;
  userType?: string;
  riskLevel?: string;
  fraudRiskLevel?: string;
  fraudScore?: number;
  fraudFlags?: string[];
  fraudFlagDescriptions?: string[];
  aiVerificationChecklist?: AgentChecklistItem[];
  comparisonTable: FieldComparison[];
  vehicles: VehicleComparison[];
  electricityComparison?: FieldComparison[];
  cookingComparison?: FieldComparison[];
  solarComparison?: FieldComparison[];
  electricityDocumentUrls?: string[];
  cookingDocumentUrls?: string[];
  solarDocumentUrls?: string[];
  matchStatus?: MatchStatus;
}

export interface FieldComparison {
  fieldName: string;
  userClaim: string | number | null;
  systemValue: string | number | null;
  matchStatus: MatchStatus;
}

export interface VehicleDocument {
  documentId: number;
  documentUrl: string;
  originalFileName?: string;
}

export interface VehicleComparison {
  vehicleLabel: string;
  comparisons: FieldComparison[];
  documents?: VehicleDocument[];
}

export interface AgentModifyRequest {
  correctedFuelType?: string;
  correctedMileageBand?: string;
  correctedMonthlyKwh?: number;
  correctedCookingFuelType?: string;
  correctedAnnualCylinders?: number;
  correctedSolarCapacityKw?: number;
  agentVerifiedSolar?: boolean;
  correctionNotes: string;
}

export interface AgentRejectRequest {
  rejectionReason: string;
}

export interface AgentPerformance extends AgentPerformanceResponse {
  strikes: number;
  agentName?: string;
  email?: string;
  pinCode?: string;
  active: boolean;
  activeAssignments: number;
  recentHistory?: AgentTaskSummary[];
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