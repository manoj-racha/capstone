package org.hartford.greensure.dto.request;

/**
 * @deprecated Legacy verification request DTO superseded by the per-action DTOs:
 *   - Confirm → via /agent/verify/{id}/confirm endpoint (no body needed)
 *   - Modify  → AgentModifyRequest
 *   - Reject  → AgentRejectRequest
 *
 * Kept as empty stub to prevent import errors.
 */
@Deprecated
public class VerificationRequest {}
