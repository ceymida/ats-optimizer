import type { AnalysisResponse, HistoryItem } from "./types";

const DEFAULT_API_URL = "http://localhost:8080";

function resolveApiUrl(): string {
  const configured = process.env.NEXT_PUBLIC_API_URL?.trim();
  if (!configured) {
    if (process.env.NODE_ENV === "production") {
      console.warn(
        "[ats-optimizer] NEXT_PUBLIC_API_URL tanımlı değil. Vercel üzerinde bu değişkeni " +
          "backend adresinizle ayarlayın; şimdilik varsayılan adres kullanılıyor."
      );
    }
    return DEFAULT_API_URL;
  }
  return configured.replace(/\/+$/, "");
}

export const API_URL = resolveApiUrl();

async function parseError(response: Response): Promise<string> {
  try {
    const body = await response.json();
    if (body && typeof body.message === "string") {
      return body.message;
    }
  } catch {
    // ignore
  }
  return `İstek başarısız oldu (HTTP ${response.status}).`;
}

export async function analyzeResume(params: {
  file: File | null;
  resumeText: string;
  jobDescription: string;
}): Promise<AnalysisResponse> {
  const formData = new FormData();
  if (params.file) {
    formData.append("file", params.file);
  }
  if (params.resumeText.trim()) {
    formData.append("resumeText", params.resumeText);
  }
  formData.append("jobDescription", params.jobDescription);

  const response = await fetch(`${API_URL}/api/analyze`, {
    method: "POST",
    body: formData,
  });

  if (!response.ok) {
    throw new Error(await parseError(response));
  }
  return response.json();
}

export async function fetchHistory(limit = 8): Promise<HistoryItem[]> {
  const response = await fetch(`${API_URL}/api/analyses?limit=${limit}`, {
    cache: "no-store",
  });
  if (!response.ok) {
    throw new Error(await parseError(response));
  }
  return response.json();
}
