import { scoreColor } from "./ScoreRing";

type Props = {
  label: string;
  value: number;
  hint?: string;
};

export default function ScoreBar({ label, value, hint }: Props) {
  return (
    <div>
      <div className="mb-1.5 flex items-baseline justify-between gap-2">
        <span className="text-sm font-medium text-slate-700">{label}</span>
        <span className="text-sm font-semibold tabular-nums text-slate-900">{value}%</span>
      </div>
      <div className="h-2 w-full overflow-hidden rounded-full bg-slate-200">
        <div
          className="h-full rounded-full transition-all duration-700"
          style={{ width: `${Math.min(Math.max(value, 0), 100)}%`, backgroundColor: scoreColor(value) }}
        />
      </div>
      {hint ? <p className="mt-1 text-xs text-slate-400">{hint}</p> : null}
    </div>
  );
}
