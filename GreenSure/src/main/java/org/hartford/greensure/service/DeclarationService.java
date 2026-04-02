package org.hartford.greensure.service;

import org.hartford.greensure.dto.response.DeclarationResponse;
import org.hartford.greensure.entity.CarbonDeclaration;
import org.springframework.stereotype.Service;

/**
 * @deprecated Full declaration lifecycle is now split across:
 *   - DeclarationModuleService  → 7-module data saving + submit/resubmit
 *   - AgentVerificationService  → agent verification workflow
 *   - CarbonScoreService        → score calculation
 *
 * This class will be deleted in the next cleanup pass.
 */
@Deprecated
@Service
public class DeclarationService {

	public DeclarationResponse mapToResponse(CarbonDeclaration d) {
		if (d == null) return null;
		return DeclarationResponse.builder()
				.declarationId(d.getDeclarationId())
				.userId(d.getUser() != null ? d.getUser().getUserId() : null)
				.declarationYear(d.getDeclarationYear())
				.status(d.getStatus())
				.resubmissionCount(d.getResubmissionCount())
				.submittedAt(d.getSubmittedAt())
				.createdAt(d.getCreatedAt())
				.build();
	}
}
