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