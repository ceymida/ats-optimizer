import type { HistoryItem } from "@/lib/types";
import { scoreColor } from "./ScoreRing";

export default function HistoryList({ items }: { items: HistoryItem[] }) {
  if (items.length === 0) {
    return (
      <p className="rounded-2xl border border-dashed border-slate-300 bg-white/60 p-6 text-center text-sm text-slate-500">
        Henüz analiz geçmişi yok. İlk analizinizi yaptığınızda burada görünecek.
      </p>
    );
  }

  return (
    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
      {items.map((item) => (
        <div
          key={item.id}
          className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm transition hover:shadow-md"
        >
          <div className="flex items-center justify-between gap-2">
            <span
              className="text-xl font-bold tabular-nums"
              style={{ color: scoreColor(item.overallScore) }}
            >
              {item.overallScore}
            </span>
            <span className="text-[11px] text-slate-400">
              {new Date(item.createdAt).toLocaleDateString("tr-TR", {
                day: "2-digit",
                month: "short",
                hour: "2-digit",
                minute: "2-digit",
              })}
            </span>
          </div>
          <p className="mt-1 truncate text-sm font-medium text-slate-700" title={item.fileName}>
            {item.fileName}
          </p>
          <p className="mt-0.5 text-xs text-slate-400">
            Kelime {item.keywordScore}% · Format {item.formatScore}%
          </p>
        </div>
      ))}
    </div>
  );
}
