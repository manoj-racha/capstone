export interface AuthResponse {
  token: string;
  userId: number;
  email: string;
  role: 'USER' | 'AGENT' | 'ADMIN';
  fullName: string;
  expiresIn: number;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  phone: string;
  dateOfBirth: string;
  address: string;
  state: string;
  city: string;
  pinCode: string;
  password: string;
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