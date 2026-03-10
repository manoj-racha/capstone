

export interface AuthResponse {
  token: string;           // JWT token — save this to localStorage
  role: string;            // "USER" | "AGENT" | "ADMIN"
  userType: string;        // "HOUSEHOLD" | "MSME" — only for USER role
  id: number;              // userId or agentId depending on role
  fullName: string;
  email: string;
  isFirstLogin: boolean;   // true = no declaration yet → go to welcome page
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  userType: string;           // "HOUSEHOLD" | "MSME"
  fullName: string;
  email: string;
  mobile: string;
  password: string;
  address: string;
  pinCode: string;
  city: string;
  state: string;

  // Household only
  numberOfMembers?: number;
  dwellingType?: string;      // "APARTMENT" | "INDEPENDENT_HOUSE"

  // MSME only
  businessName?: string;
  gstNumber?: string;
  businessType?: string;      // "MANUFACTURING" | "RETAIL" | "SERVICE" | "FOOD"
  numEmployees?: number;
}