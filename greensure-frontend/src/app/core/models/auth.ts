export interface AuthResponse {
  token: string;
  userId: number;
  id?: number;
  email: string;
  role: 'USER' | 'AGENT' | 'ADMIN';
  fullName: string;
  expiresIn: number;
  userType?: string;
  isFirstLogin?: boolean;
}

export interface RegisterRequest {
  userType?: 'HOUSEHOLD' | 'MSME' | 'AGENT' | 'ADMIN';
  fullName: string;
  email: string;
  mobile?: string;
  phone?: string;
  address: string;
  state: string;
  city: string;
  pinCode: string;
  password: string;
  numberOfMembers?: number | null;
  dwellingType?: string;
  businessName?: string;
  gstNumber?: string;
  businessType?: string;
  numEmployees?: number | null;
}

export interface OtpVerifyRequest {
  email: string;
  otp: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}