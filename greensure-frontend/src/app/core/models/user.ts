import { CarbonScoreDetail } from './score';

export interface UserProfile {
  userId: number;
  fullName: string;
  email: string;
  phone: string;
  dateOfBirth?: string;
  address?: string;
  state?: string;
  city?: string;
  pincode?: string;
  householdSize?: number;
  role: 'USER' | 'AGENT' | 'ADMIN';
  status?: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
  mobile?: string;
  pinCode?: string;
  userType?: 'USER' | 'AGENT' | 'ADMIN';
  createdAt?: string;
}

export interface DashboardResponse {
  hasDeclaration: boolean;
  declarationStatus: 'DRAFT' | 'SUBMITTED' | 'UNDER_VERIFICATION' | 'VERIFIED' | 'REJECTED' | null;
  declarationYear?: number;
  currentDeclarationId?: number;
  latestScore: CarbonScoreDetail | null;
  zone: string | null;
  discountPercentage: number | null;
  renewalDue: boolean;
  renewalDate?: string;
  unreadNotifications: number;
  policiesCount: number;
}