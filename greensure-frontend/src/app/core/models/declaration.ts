import { CarbonScoreDetail } from './score';

export type DeclarationStatus =
  'DRAFT' | 'SUBMITTED' | 'UNDER_VERIFICATION' | 'VERIFIED' | 'REJECTED';

export type FuelType = 'EV' | 'PETROL' | 'DIESEL' | 'CNG';

export type MileageBand =
  'BAND_1' | 'BAND_2' | 'BAND_3' | 'BAND_4' | 'BAND_5';

export const MILEAGE_BAND_LABELS: Record<MileageBand, string> = {
  BAND_1: 'Under 5,000 km',
  BAND_2: '5,000–10,000 km',
  BAND_3: '10,000–15,000 km',
  BAND_4: '15,000–20,000 km',
  BAND_5: 'Above 20,000 km'
};

export type CookingFuel = 'LPG' | 'PNG' | 'ELECTRIC' | 'BIOGAS';

export type DataSource = 'VAHAN' | 'DIGILOCKER' | 'MANUAL';

export type PublicTransportUsage =
  'NEVER' | 'RARELY' | 'SOMETIMES' | 'OFTEN' | 'ALWAYS';

export type VehicleCategory =
  'TWO_WHEELER' | 'THREE_WHEELER' |
  'FOUR_WHEELER' | 'OTHER';

export const VEHICLE_CATEGORY_LABELS: Record<VehicleCategory, string> = {
  TWO_WHEELER:   '2 Wheeler — Bike / Scooter',
  THREE_WHEELER: '3 Wheeler — Auto / E-Rickshaw',
  FOUR_WHEELER:  '4 Wheeler — Car / SUV / Van',
  OTHER:         'Other — Tractor / Truck / Special'
};

export type VehicleDocumentType =
  'RC_BOOK' | 'INSURANCE' |
  'POLLUTION_CERTIFICATE' | 'OTHER';

export const VEHICLE_DOCUMENT_LABELS: Record<VehicleDocumentType, string> = {
  RC_BOOK:               'Registration Certificate (RC)',
  INSURANCE:             'Vehicle Insurance',
  POLLUTION_CERTIFICATE: 'Pollution Under Control (PUC)',
  OTHER:                 'Other Document'
};

export interface VehicleDocument {
  documentId: number;
  documentType: VehicleDocumentType;
  documentUrl: string;
  originalFileName: string;
  mimeType: string;
  fileSizeBytes: number;
  verified: boolean;
  agentNote: string | null;
  uploadedAt: string;
}

export interface VehicleData {
  vehicleId: number;
  vehicleCategory: VehicleCategory;
  vehicleNickname: string | null;
  registrationNumber: string;
  make: string;
  model: string;
  year: number;
  fuelType: FuelType;
  mileageBand: MileageBand;
  dataSource: DataSource;
  documents: VehicleDocument[];
}

export interface ElectricityData {
  provider: string;
  consumerNumber: string;
  userDeclaredMonthlyKwh: number;
  ocrComputedMonthlyKwh: number | null;
  billsUploaded: number;
  agentCorrectedMonthlyKwh: number | null;
  billUrls?: string[];
}

export interface ElectricityBill {
  billingMonth: string;
  unitsKwh: number;
  amount: number;
  billUrl: string;
  ocrConfidenceScore: number;
}

/** Per-bill row returned on declaration detail (review). */
export interface ElectricityBillSummary {
  billingMonth?: string;
  unitsKwh?: number | null;
  amount?: number | null;
  billUrl?: string | null;
  ocrConfidenceScore?: number | null;
  aiAnomalyFlag?: boolean | null;
}

export interface CookingData {
  fuelType: CookingFuel;
  pngConsumerNumber: string | null;
  userDeclaredCylinders: number | null;
  ocrComputedCylinders: number | null;
  agentCorrectedFuelType: CookingFuel | null;
  agentCorrectedCylinders: number | null;
  billUrls?: string[];
}

export interface SolarData {
  hasSolar: boolean;
  capacityKw: number | null;
  certificateUrl: string | null;
  mnreVerified: boolean;
  agentCorrectedCapacityKw: number | null;
  agentVerifiedSolar: boolean;
}

export interface LifestyleData {
  publicTransportUsage: PublicTransportUsage;
  wastesRecycling: boolean;
}

export interface DeclarationSummary {
  declarationId: number;
  userId: number;
  fullName: string;
  declarationYear: number;
  status: DeclarationStatus;
  submittedAt: string | null;
  assignedAgentName: string | null;
  deadline: string | null;
  fraudRiskLevel: string | null;
}

export interface DeclarationResponse extends DeclarationSummary {
  assignedAgentId?: number;
}

export interface DeclarationDetail extends DeclarationSummary {
  householdSize: number;
  vehicles: VehicleData[];
  electricityData: ElectricityData | null;
  /** Bill-level AI/OCR confidence (when returned by API). */
  electricityBills?: ElectricityBillSummary[];
  cookingData: CookingData | null;
  solarData: SolarData | null;
  lifestyleData: LifestyleData | null;
  carbonScore: CarbonScoreDetail | null;
  resubmissionCount?: number;
  rejectionReason?: string;
}

export interface HouseholdDataRequest {
  numberOfMembers: number;
}

export interface AddVehicleRequest {
  vehicleCategory: VehicleCategory;
  vehicleNickname?: string;
  vin?: string;
  registrationNumber: string;
  make: string;
  model: string;
  year: number;
  fuelType: FuelType;
  mileageBand: MileageBand;
  dataSource: DataSource;
}

export interface UploadVehicleDocumentRequest {
  vehicleId: number;
  documentType: VehicleDocumentType;
  documentUrl: string;
  originalFileName: string;
  mimeType: string;
  fileSizeBytes: number;
}

export interface ElectricityDataRequest {
  provider: string;
  consumerNumber: string;
  userDeclaredMonthlyKwh: number;
}

export interface CookingDataRequest {
  fuelType: CookingFuel;
  pngConsumerNumber?: string;
  userDeclaredCylinders?: number;
  billUrls?: string;
}

export interface SolarDataRequest {
  hasSolar: boolean;
  capacityKw?: number;
  certificateUrl?: string;
}

export interface LifestyleDataRequest {
  publicTransportUsage: PublicTransportUsage;
  wastesRecycling: boolean;
}
