
export interface CarbonScoreResponse {
  scoreId: number;
  userId: number;
  scoreYear: number;

  // CO2 values in kg
  energyCo2: number;
  transportCo2: number;
  lifestyleCo2?: number;    // Household only
  operationsCo2?: number;   // MSME only
  totalCo2: number;
  perCapitaCo2: number;

  // Zone classification
  zone: string;             // "GREEN_CHAMPION" | "GREEN_IMPROVER" | "GREEN_DEFAULTER"

  // Percentage breakdown — used for charts
  energyPercentage: number;
  transportPercentage: number;
  lifestylePercentage?: number;
  operationsPercentage?: number;

  // Comparison data
  cityAverage?: number;
  nationalAverage?: number;
  previousYearCo2?: number;
  improvementPercentage?: number;

  generatedAt: string;

  // Recommendations attached to score
  recommendations?: RecommendationResponse[];
}

export interface RecommendationResponse {
  recommendationId: number;
  category: string;    // "ENERGY" | "TRANSPORT" | "LIFESTYLE" | "OPERATIONS"
  priority: string;    // "HIGH" | "MEDIUM" | "LOW"
  recommendationText: string;
  generatedAt: string;
}