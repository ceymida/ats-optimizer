import type { AnalysisResponse } from "@/lib/types";
import ScoreRing from "./ScoreRing";
import ScoreBar from "./ScoreBar";

function keywordLabel(term: string, weight: number) {
  return (
    <span
      key={term}
      className="inline-flex items-center gap-1.5 rounded-full border border-amber-200 bg-amber-50 px-3 py-1 text-sm text-amber-900"
      title={`İlandaki önem ağırlığı: ${weight.toFixed(1)}`}
    >
      {term}
      <span className="text-[10px] font-semibold text-amber-500">{weight >= 3 ? "●" : "○"}</span>
    </span>
  );
}

export default function ResultPanel({ result }: { result: AnalysisResponse }) {
  const passedChecks = result.checks.filter((c) => c.passed).length;

  return (
    <div className="space-y-6">
      <div className="grid gap-6 lg:grid-cols-[300px_1fr]">
        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex justify-center">
            <ScoreRing score={result.overallScore} grade={result.grade} />
          </div>
          <div className="mt-6 space-y-4">
            <ScoreBar label="Anahtar kelime uyumu" value={result.keywordScore} />
            <ScoreBar label="Format / yapı" value={result.formatScore} />
            <ScoreBar label="Okunabilirlik" value={result.readabilityScore} />
          </div>
          <p className="mt-5 text-center text-xs text-slate-500">
            {result.wordCount} kelime · {passedChecks}/{result.checks.length} format kontrolü geçti
          </p>
        </div>

        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h2 className="text-lg font-semibold text-slate-900">Genel Değerlendirme</h2>
            <span
              className={`rounded-full px-3 py-1 text-xs font-medium ${
                result.aiEnhanced
                  ? "bg-violet-100 text-violet-700"
                  : "bg-slate-100 text-slate-600"
              }`}
            >
              {result.aiEnhanced ? "AI destekli öneriler" : "Kural tabanlı öneriler"}
            </span>
          </div>
          <p className="mt-3 text-sm leading-6 text-slate-600">{result.summary}</p>

          <h3 className="mt-6 text-sm font-semibold uppercase tracking-wide text-slate-500">
            İyileştirme Önerileri
          </h3>
          <ol className="mt-3 space-y-3">
            {result.suggestions.map((suggestion, index) => (
              <li key={`${index}-${suggestion}`} className="flex gap-3">
                <span className="mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-indigo-50 text-xs font-semibold text-indigo-600">
                  {index + 1}
                </span>
                <span className="text-sm leading-6 text-slate-700">{suggestion}</span>
              </li>
            ))}
          </ol>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-500">
              Eksik Anahtar Kelimeler
            </h3>
            <span className="text-xs text-slate-400">{result.missingKeywords.length} terim</span>
          </div>
          {result.missingKeywords.length === 0 ? (
            <p className="mt-3 text-sm text-emerald-600">
              Harika! İlandaki tüm önemli terimler özgeçmişinizde geçiyor.
            </p>
          ) : (
            <>
              <p className="mt-2 text-xs text-slate-400">
                ● yüksek öncelik · ○ destekleyici terim — bu terimleri ilgili deneyimlerinize ekleyin.
              </p>
              <div className="mt-3 flex max-h-64 flex-wrap gap-2 overflow-y-auto pr-1">
                {result.missingKeywords.map((keyword) =>
                  keywordLabel(keyword.term, keyword.weight)
                )}
              </div>
            </>
          )}

          <h3 className="mt-6 text-sm font-semibold uppercase tracking-wide text-slate-500">
            Eşleşen Kelimeler
          </h3>
          <div className="mt-3 flex max-h-40 flex-wrap gap-2 overflow-y-auto pr-1">
            {result.matchedKeywords.length === 0 ? (
              <p className="text-sm text-slate-500">İlandaki terimlerle eşleşme bulunamadı.</p>
            ) : (
              result.matchedKeywords.map((term) => (
                <span
                  key={term}
                  className="inline-flex rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1 text-sm text-emerald-800"
                >
                  {term}
                </span>
              ))
            )}
          </div>
        </div>

        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-500">
            Format Kontrolleri
          </h3>
          <ul className="mt-3 space-y-2.5">
            {result.checks.map((check) => (
              <li key={check.label} className="flex items-center gap-3">
                <span
                  className={`flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-[11px] font-bold ${
                    check.passed
                      ? "bg-emerald-100 text-emerald-700"
                      : "bg-rose-100 text-rose-600"
                  }`}
                >
                  {check.passed ? "✓" : "✕"}
                </span>
                <span className="flex-1 text-sm text-slate-700">{check.label}</span>
                <span className="text-xs text-slate-400">+{check.points}</span>
              </li>
            ))}
          </ul>
          <p className="mt-4 text-xs text-slate-400">
            Analiz: <span className="font-medium text-slate-500">{result.fileName}</span>
          </p>
        </div>
      </div>
    </div>
  );
}
