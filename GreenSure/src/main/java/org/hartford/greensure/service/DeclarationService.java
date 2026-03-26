package org.hartford.greensure.service;

/**
 * @deprecated Full declaration lifecycle is now split across:
 *   - DeclarationModuleService  → 7-module data saving + submit/resubmit
 *   - AgentVerificationService  → agent verification workflow
 *   - CarbonScoreService        → score calculation
 *
 * This class will be deleted in the next cleanup pass.
 */
@Deprecated
public class DeclarationService {}
