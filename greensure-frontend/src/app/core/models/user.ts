export interface UserProfile {
  userId: number;
  userType: string;        // "HOUSEHOLD" | "MSME"
  fullName: string;
  email: string;
  mobile: string;
  address: string;
  pinCode: string;
  city: string;
  state: string;
  status: string;          // "ACTIVE" | "SUSPENDED" | "INACTIVE"
  createdAt: string;

  // Household profile fields
  numberOfMembers?: number;
  dwellingType?: string;

  // MSME profile fields
  businessName?: string;
  gstNumber?: string;
  businessType?: string;
  numEmployees?: number;
}

export interface DashboardResponse {
  userId: number;
  fullName: string;
  userType: string;
  hasDeclaration: boolean;
  currentDeclarationId?: number;
  declarationStatus?: string;
  declarationYear?: number;
  latestScore?: CarbonScoreResponse;  // defined in score model
  zone?: string;
  renewalDue?: boolean;
  unreadNotifications: number;
}

// Import needed — we reference CarbonScoreResponse here
// Angular resolves this at compile time, no circular dependency issue
import { CarbonScoreResponse } from './score';