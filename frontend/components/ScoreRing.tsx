type Props = {
  score: number;
  grade: string;
  size?: number;
};

export function scoreColor(score: number) {
  if (score >= 85) return "#10b981";
  if (score >= 70) return "#22c55e";
  if (score >= 50) return "#f59e0b";
  return "#f43f5e";
}

export default function ScoreRing({ score, grade, size = 180 }: Props) {
  const stroke = 14;
  const radius = (size - stroke) / 2;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference - (Math.min(Math.max(score, 0), 100) / 100) * circumference;
  const color = scoreColor(score);

  return (
    <div className="relative flex items-center justify-center" style={{ width: size, height: size }}>
      <svg width={size} height={size} className="-rotate-90">
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke="#e5e7eb"
          strokeWidth={stroke}
        />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke={color}
          strokeWidth={stroke}
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          style={{ transition: "stroke-dashoffset 900ms ease-out" }}
        />
      </svg>
      <div className="absolute flex flex-col items-center">
        <span className="text-4xl font-bold tabular-nums" style={{ color }}>
          {score}
        </span>
        <span className="mt-1 text-xs font-medium uppercase tracking-wider text-slate-500">
          {grade}
        </span>
      </div>
    </div>
  );
}
