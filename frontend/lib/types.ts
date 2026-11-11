export type KeywordDetail = {
  term: string;
  weight: number;
};

export type CheckItem = {
  label: string;
  passed: boolean;
  points: number;
};

export type AnalysisResponse = {
  id: number;
  overallScore: number;
  keywordScore: number;
  formatScore: number;
  readabilityScore: number;
  grade: string;
  summary: string;
  wordCount: number;
  matchedKeywords: string[];
  missingKeywords: KeywordDetail[];
  suggestions: string[];
  checks: CheckItem[];
  aiEnhanced: boolean;
  fileName: string;
  createdAt: string;
};

export type HistoryItem = {
  id: number;
  fileName: string;
  overallScore: number;
  keywordScore: number;
  formatScore: number;
  createdAt: string;
};
