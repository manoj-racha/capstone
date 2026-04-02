package org.hartford.greensure.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiElectricityAnalysis {

    @JsonAlias("provider_match")
    private boolean providerMatch;

    @JsonAlias("provider_on_bills")
    private String providerOnBills;

    @JsonAlias("consumer_number_match")
    private boolean consumerNumberMatch;

    @JsonAlias("consumer_number_on_bills")
    private String consumerNumberOnBills;

    @JsonAlias("consumer_number_consistent")
    private boolean consumerNumberConsistent;

    @JsonAlias("ai_computed_monthly_avg_kwh")
    private Double aiComputedMonthlyAvgKwh;

    @JsonAlias("user_declared_monthly_kwh")
    private Double userDeclaredMonthlyKwh;

    @JsonAlias("kwh_difference")
    private Double kwhDifference;

    @JsonAlias("kwh_match")
    private boolean kwhMatch;

    @JsonAlias("bills_covered")
    private int billsCovered;

    @JsonAlias("duplicate_months")
    @Builder.Default
    private List<String> duplicateMonths = new ArrayList<>();

    @JsonAlias("missing_months")
    @Builder.Default
    private List<String> missingMonths = new ArrayList<>();

    @Builder.Default
    private List<String> findings = new ArrayList<>();
}
