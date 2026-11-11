"use client";

import { useEffect, useRef, useState } from "react";
import { analyzeResume, fetchHistory } from "@/lib/api";
import type { AnalysisResponse, HistoryItem } from "@/lib/types";
import ResultPanel from "@/components/ResultPanel";
import HistoryList from "@/components/HistoryList";

const MAX_FILE_MB = 10;

function formatBytes(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export default function Home() {
  const [mode, setMode] = useState<"file" | "text">("file");
  const [file, setFile] = useState<File | null>(null);
  const [resumeText, setResumeText] = useState("");
  const [jobDescription, setJobDescription] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<AnalysisResponse | null>(null);
  const [history, setHistory] = useState<HistoryItem[]>([]);
  const [dragging, setDragging] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    fetchHistory()
      .then(setHistory)
      .catch(() => setHistory([]));
  }, []);

  function pickFile(selected: File | null) {
    setError(null);
    if (!selected) {
      setFile(null);
      return;
    }
    const name = selected.name.toLowerCase();
    const allowed =
      name.endsWith(".pdf") ||
      name.endsWith(".json") ||
      name.endsWith(".txt") ||
      name.endsWith(".md");
    if (!allowed) {
      setError("Yalnızca PDF, JSON veya TXT dosyaları destekleniyor.");
      return;
    }
    if (selected.size > MAX_FILE_MB * 1024 * 1024) {
      setError(`Dosya boyutu en fazla ${MAX_FILE_MB} MB olabilir.`);
      return;
    }
    setFile(selected);
  }

  async function handleAnalyze() {
    setError(null);
    if (mode === "file" && !file) {
      setError("Lütfen bir özgeçmiş dosyası seçin.");
      return;
    }
    if (mode === "text" && !resumeText.trim()) {
      setError("Lütfen özgeçmiş metnini yapıştırın.");
      return;
    }
    if (!jobDescription.trim()) {
      setError("Lütfen hedef iş ilanını yapıştırın.");
      return;
    }

    setLoading(true);
    try {
      const response = await analyzeResume({
        file: mode === "file" ? file : null,
        resumeText: mode === "text" ? resumeText : "",
        jobDescription,
      });
      setResult(response);
      fetchHistory().then(setHistory).catch(() => undefined);
      setTimeout(() => {
        document.getElementById("sonuclar")?.scrollIntoView({ behavior: "smooth", block: "start" });
      }, 100);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Beklenmeyen bir hata oluştu.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-[radial-gradient(1200px_600px_at_50%_-100px,#e0e7ff_0%,transparent_60%)]">
      <div className="mx-auto w-full max-w-6xl px-5 py-10 sm:px-8">
        <header className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-indigo-600 text-lg font-bold text-white shadow-lg shadow-indigo-200">
              ATS
            </div>
            <div>
              <h1 className="text-xl font-semibold tracking-tight text-slate-900">
                Smart Resume &amp; ATS Optimizer
              </h1>
              <p className="text-sm text-slate-500">
                Özgeçmişinizi hedef ilana göre puanlayın, eksikleri görün.
              </p>
            </div>
          </div>
          <a
            href="http://localhost:8080/api/health"
            target="_blank"
            rel="noreferrer"
            className="hidden rounded-full border border-slate-200 bg-white px-4 py-2 text-xs font-medium text-slate-500 shadow-sm transition hover:text-slate-700 sm:block"
          >
            API: localhost:8080
          </a>
        </header>

        <main className="mt-8 space-y-10">
          <section className="grid gap-6 lg:grid-cols-2">
            <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
              <div className="flex items-center justify-between">
                <h2 className="text-base font-semibold text-slate-900">1. Özgeçmiş</h2>
                <div className="flex rounded-lg bg-slate-100 p-1 text-xs font-medium">
                  <button
                    type="button"
                    onClick={() => setMode("file")}
                    className={`rounded-md px-3 py-1.5 transition ${
                      mode === "file" ? "bg-white text-slate-900 shadow-sm" : "text-slate-500"
                    }`}
                  >
                    Dosya
                  </button>
                  <button
                    type="button"
                    onClick={() => setMode("text")}
                    className={`rounded-md px-3 py-1.5 transition ${
                      mode === "text" ? "bg-white text-slate-900 shadow-sm" : "text-slate-500"
                    }`}
                  >
                    Metin
                  </button>
                </div>
              </div>

              {mode === "file" ? (
                <div
                  onDragOver={(e) => {
                    e.preventDefault();
                    setDragging(true);
                  }}
                  onDragLeave={() => setDragging(false)}
                  onDrop={(e) => {
                    e.preventDefault();
                    setDragging(false);
                    pickFile(e.dataTransfer.files?.[0] ?? null);
                  }}
                  onClick={() => inputRef.current?.click()}
                  className={`mt-4 flex cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed px-6 py-10 text-center transition ${
                    dragging
                      ? "border-indigo-400 bg-indigo-50"
                      : "border-slate-200 bg-slate-50 hover:border-indigo-300 hover:bg-indigo-50/50"
                  }`}
                >
                  <input
                    ref={inputRef}
                    type="file"
                    accept=".pdf,.json,.txt,.md"
                    className="hidden"
                    onChange={(e) => pickFile(e.target.files?.[0] ?? null)}
                  />
                  <span className="text-3xl">📄</span>
                  {file ? (
                    <>
                      <p className="mt-2 max-w-full truncate text-sm font-medium text-slate-800">
                        {file.name}
                      </p>
                      <p className="text-xs text-slate-400">{formatBytes(file.size)} · değiştirmek için tıklayın</p>
                    </>
                  ) : (
                    <>
                      <p className="mt-2 text-sm font-medium text-slate-700">
                        PDF, JSON veya TXT dosyasını sürükleyin
                      </p>
                      <p className="text-xs text-slate-400">veya seçmek için tıklayın · en fazla {MAX_FILE_MB} MB</p>
                    </>
                  )}
                </div>
              ) : (
                <textarea
                  value={resumeText}
                  onChange={(e) => setResumeText(e.target.value)}
                  placeholder="Özgeçmiş metninizi buraya yapıştırın..."
                  className="mt-4 h-56 w-full resize-y rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm leading-6 text-slate-800 outline-none transition focus:border-indigo-400 focus:bg-white focus:ring-2 focus:ring-indigo-100"
                />
              )}
            </div>

            <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
              <h2 className="text-base font-semibold text-slate-900">2. Hedef İş İlanı</h2>
              <textarea
                value={jobDescription}
                onChange={(e) => setJobDescription(e.target.value)}
                placeholder="İş ilanındaki gereksinimleri buraya yapıştırın..."
                className="mt-4 h-56 w-full resize-y rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm leading-6 text-slate-800 outline-none transition focus:border-indigo-400 focus:bg-white focus:ring-2 focus:ring-indigo-100 lg:h-[252px]"
              />
            </div>
          </section>

          <div className="flex flex-col items-center gap-3">
            {error ? (
              <p className="w-full rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-center text-sm text-rose-700">
                {error}
              </p>
            ) : null}
            <button
              type="button"
              onClick={handleAnalyze}
              disabled={loading}
              className="inline-flex items-center gap-2 rounded-xl bg-indigo-600 px-8 py-3 text-sm font-semibold text-white shadow-lg shadow-indigo-200 transition hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {loading ? (
                <>
                  <span className="h-4 w-4 animate-spin rounded-full border-2 border-white/40 border-t-white" />
                  Analiz ediliyor...
                </>
              ) : (
                <>ATS Uyumunu Analiz Et</>
              )}
            </button>
            <p className="text-xs text-slate-400">
              Skor anahtar kelime uyumu, format ve okunabilirlik üzerinden hesaplanır.
            </p>
          </div>

          {result ? (
            <section id="sonuclar" className="scroll-mt-6 space-y-4">
              <div className="flex items-center gap-3">
                <h2 className="text-lg font-semibold text-slate-900">Analiz Sonucu</h2>
                <span className="rounded-full bg-indigo-50 px-2.5 py-1 text-xs font-medium text-indigo-600">
                  #{result.id}
                </span>
              </div>
              <ResultPanel result={result} />
            </section>
          ) : null}

          <section className="space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold text-slate-900">Son Analizler</h2>
              <span className="text-xs text-slate-400">H2 in-memory · oturum boyunca saklanır</span>
            </div>
            <HistoryList items={history} />
          </section>
        </main>

        <footer className="mt-12 border-t border-slate-200 pt-6 text-center text-xs text-slate-400">
          Spring Boot · Apache PDFBox · Next.js · Tailwind CSS
        </footer>
      </div>
    </div>
  );
}
