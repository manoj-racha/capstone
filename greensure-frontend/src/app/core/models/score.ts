export type Zone = 'GREEN_CHAMPION' | 'IMPROVER' | 'DEFAULTER';

export const ZONE_LABELS: Record<Zone, string> = {
  GREEN_CHAMPION: 'Green Champion',
  IMPROVER: 'Improver',
  DEFAULTER: 'Defaulter'
};

export const ZONE_COLORS: Record<Zone, string> = {
  GREEN_CHAMPION: 'green',
  IMPROVER: 'amber',
  DEFAULTER: 'red'
};

export interface Recommendation {
  category: string;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  description: string;
}

export interface CarbonScoreDetail {
  totalCo2: number;
  vehicleCo2: number;
  electricityCo2: number;
  cookingCo2: number;
  solarOffset: number;
  lifestyleBonus: number;
  perCapitaCo2: number;
  zone: Zone;
  discountPercent: number;
  improvementBonusPercent: number;
  durationDiscountPercent: number;
  zoneDiscountPercent: number;
  discountBreakdown: string;
  generatedAt: string;
  recommendations: Recommendation[];
  /** Gemini plain-language summary; optional */
  aiExplanation?: string | null;
}

export interface CarbonScoreResponse extends CarbonScoreDetail {
  improvementPercentage?: number;
}
