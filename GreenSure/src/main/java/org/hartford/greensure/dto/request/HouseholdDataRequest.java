package org.hartford.greensure.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/** Request DTO for Module 2 — Household Data. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HouseholdDataRequest {

    @NotNull(message = "Number of members is required")
    @Min(value = 1,  message = "Household must have at least 1 member")
    @Max(value = 20, message = "Household cannot have more than 20 members")
    private Integer numberOfMembers;
}
