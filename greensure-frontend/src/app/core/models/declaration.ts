
export interface VehicleResponse {
  vehicleId: number;
  vehicleType: string;    // "TWO_WHEELER" | "FOUR_WHEELER" | "COMMERCIAL"
  fuelType: string;       // "PETROL" | "DIESEL" | "CNG" | "ELECTRIC"
  kmPerMonth: number;
  quantity: number;
}

export interface DeclarationResponse {
  declarationId: number;
  userId: number;
  declarationYear: number;
  status: string;         // "DRAFT" | "SUBMITTED" | "UNDER_VERIFICATION" | "VERIFIED" | "REJECTED"
  resubmissionCount: number;
  submittedAt?: string;
  createdAt: string;

  // Energy fields
  electricityUnits?: number;
  hasSolar?: boolean;
  solarUnits?: number;
  cookingFuelType?: string;   // "LPG" | "PNG" | "BIOMASS" | "ELECTRIC" | "NONE"
  lpgCylinders?: number;
  pngUnits?: number;
  biomassKgPerDay?: number;
  numAcUnits?: number;
  acHoursPerDay?: number;
  hasGenerator?: boolean;
  generatorHoursPerMonth?: number;

  // Transport fields
  usesPublicTransport?: boolean;
  publicTransportKm?: number;
  vehicles?: VehicleResponse[];

  // Lifestyle fields — Household only
  dietaryPattern?: string;          // "VEGAN" | "VEGETARIAN" | "EGGETARIAN" | "NON_VEGETARIAN" | "HEAVY_NON_VEGETARIAN"
  shoppingOrdersPerMonth?: string;  // "ZERO_TO_FIVE" | "SIX_TO_FIFTEEN" | "ABOVE_FIFTEEN"

  // Operations fields — MSME only
  hasCommercialVehicles?: boolean;
  commercialVehicleKm?: number;
  thirdPartyShipments?: number;
  employeesPrivateVehicle?: number;
  employeesPublicTransport?: number;
  generatorLitersPerMonth?: number;
  hasBoiler?: boolean;
  boilerFuelType?: string;          // "COAL" | "NATURAL_GAS" | "NONE"
  boilerCoalKg?: number;
  boilerGasScm?: number;
  paperReamsPerMonth?: number;
  usesRecycledPaper?: boolean;
  rawMaterialType?: string;         // "VIRGIN" | "RECYCLED" | "MIXED"
  rawMaterialKg?: number;

  assignedAgentId?: number;
  assignedAgentName?: string;
}

export interface VehicleRequest {
  vehicleType: string;
  fuelType: string;
  kmPerMonth: number;
  quantity: number;
}

export interface DeclarationRequest {
  // Energy
  electricityUnits?: number;
  hasSolar?: boolean;
  solarUnits?: number;
  cookingFuelType?: string;
  lpgCylinders?: number;
  pngUnits?: number;
  biomassKgPerDay?: number;
  numAcUnits?: number;
  acHoursPerDay?: number;
  hasGenerator?: boolean;
  generatorHoursPerMonth?: number;

  // Transport
  usesPublicTransport?: boolean;
  publicTransportKm?: number;
  vehicles?: VehicleRequest[];

  // Lifestyle — Household
  dietaryPattern?: string;
  shoppingOrdersPerMonth?: string;

  // Operations — MSME
  hasCommercialVehicles?: boolean;
  commercialVehicleKm?: number;
  thirdPartyShipments?: number;
  employeesPrivateVehicle?: number;
  employeesPublicTransport?: number;
  generatorLitersPerMonth?: number;
  hasBoiler?: boolean;
  boilerFuelType?: string;
  boilerCoalKg?: number;
  boilerGasScm?: number;
  paperReamsPerMonth?: number;
  usesRecycledPaper?: boolean;
  rawMaterialType?: string;
  rawMaterialKg?: number;
}