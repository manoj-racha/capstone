export interface NotificationResponse {
  notificationId: number;
  notificationType: string;   // "ASSIGNMENT_ALERT" | "DEADLINE_REMINDER" | "SCORE_READY" | "REJECTION_ALERT" | "RENEWAL_REMINDER" | "REASSIGNMENT_ALERT"
  channel: string;            // "EMAIL" | "SMS"
  message: string;
  status: string;             // "SENT" | "DELIVERED" | "FAILED"
  sentAt: string;
}

