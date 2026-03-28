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

export interface AdminAnalytics extends AdminOverview {
  averageCo2: number;
  declarationsByStatus?: Record<string, number>;
  zoneDistribution?: Record<string, number>;
  fraudRiskBreakdown?: Record<string, number>;
  agentPerformance: Array<{
    agentId: number;
    agentName: string;
    activeAssignments: number;
    completedAssignments: number;
    strikes: number;
    active: boolean;
  }>;
}

export interface ManualAssignRequest {
  declarationId: number;
  agentId: number;
  reason?: string;
}

export interface ReassignRequest {
  declarationId: number;
  newAgentId: number;
  reason: string;
}