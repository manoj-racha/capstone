export interface AdminOverview {
  totalUsers: number;
  totalAgents: number;
  totalDeclarations: number;
  pendingVerifications: number;
  totalScoresGenerated: number;
  flaggedAgents: number;
}

export interface CreateAgentRequest {
  agentType: string;       // "FIELD_AGENT" | "ADMIN"
  fullName: string;
  email: string;
  mobile: string;
  password: string;
  employeeId: string;
  assignedZones: string;   // comma-separated pin codes "560001,560002"
}

export interface UnassignedDeclaration {
  declarationId: number;
  userId: number;
  userName: string;
  userType: string;
  submittedAt: string;
  pinCode: string;
}

export interface AvailableAgent {
  agentId: number;
  fullName: string;
  email: string;
  employeeId: string;
  assignedZones: string;
  strikeCount: number;
}