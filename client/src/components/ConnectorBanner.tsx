import React from "react";
import { CheckCircle, Loader2, XCircle } from "lucide-react";

type Props = {
  name: string;
  status: "connected" | "connecting" | "failed" | "idle";
  message?: string;
  progress?: number; // 0-100
  onClose?: () => void;
};

export default function ConnectorBanner({ name, status, message, progress = 0, onClose }: Props) {
  return (
    <div className="connector-banner fixed top-4 left-1/2 transform -translate-x-1/2 z-50 w-[min(980px,92%)]">
      <div className="bg-gradient-to-r from-slate-900/90 to-slate-800/75 backdrop-blur-md border border-slate-700 rounded-xl shadow-lg p-3 flex items-center gap-4">
        <div className="w-10 h-10 flex items-center justify-center rounded-lg bg-slate-900/40">
          {status === "connecting" && <Loader2 className="animate-spin" size={20} />}
          {status === "connected" && <CheckCircle size={20} className="text-emerald-400" />}
          {status === "failed" && <XCircle size={20} className="text-rose-400" />}
          {status === "idle" && <Loader2 size={20} />}
        </div>

        <div className="flex-1 min-w-0">
          <div className="flex items-center justify-between">
            <div className="truncate">
              <div className="text-sm text-slate-300 font-medium">{name}</div>
              <div className="text-xs text-slate-400">{message ?? status}</div>
            </div>
            <button onClick={onClose} className="text-slate-400 hover:text-slate-200 text-xs">✕</button>
          </div>

          {status === "connecting" && (
            <div className="mt-2 h-2 w-full bg-slate-700 rounded overflow-hidden">
              <div className="bg-amber-400 h-full transition-all" style={{ width: `${Math.max(6, progress)}%` }} />
            </div>
          )}

          {status === "connected" && progress > 0 && (
            <div className="mt-2 h-2 w-full bg-slate-700 rounded overflow-hidden">
              <div className="bg-emerald-400 h-full" style={{ width: `${progress}%` }} />
            </div>
          )}
        </div>
      </div>

      <style>{`
        .connector-banner { animation: banner-in 220ms ease; }
        @keyframes banner-in { from { transform: translateX(-50%) translateY(-8px); opacity: 0 } to { transform: translateX(-50%) translateY(0); opacity: 1 } }
      `}</style>
    </div>
  );
}
