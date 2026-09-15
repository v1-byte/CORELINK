import React, { useEffect, useState } from "react";
import { Chat, Lock, Unlock, Database } from "lucide-react";
import { toast } from "sonner";

export default function NavChatColumn() {
  const [open, setOpen] = useState(false);
  const [trainingText, setTrainingText] = useState("");
  const [privacyOpen, setPrivacyOpen] = useState(() => (localStorage.getItem("corelink-privacy-open") === "true"));
  const bridgeUrl = localStorage.getItem("corelink-bridge-url") || "http://127.0.0.1:8787";

  useEffect(() => { localStorage.setItem("corelink-privacy-open", String(privacyOpen)); }, [privacyOpen]);

  const sendTraining = async () => {
    if (!trainingText.trim()) return toast.error("Text kosong");
    try {
      const res = await fetch(`${bridgeUrl.replace(/\/$/,"")}/api/ollama/train`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ text: trainingText }) });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      toast.success("Training dikirim ke bridge");
      setTrainingText("");
    } catch (err) {
      console.error(err);
      toast.error("Gagal kirim training (cek bridge)");
    }
  };

  return (
    <div className={`nav-chat-column fixed right-4 top-20 z-40 transition-transform ${open ? "translate-x-0" : "translate-x-40"}`}>
      <div className="w-80 bg-slate-900/80 border border-slate-700 rounded-lg shadow-lg p-3 backdrop-blur">
        <div className="flex items-center justify-between mb-2">
          <div className="flex items-center gap-2"><Chat size={16} /><div className="text-sm font-medium">Chat Trainer</div></div>
          <div className="flex items-center gap-2">
            <button title="Toggle privacy" onClick={() => setPrivacyOpen((s) => !s)} className="p-1 rounded hover:bg-slate-800">{privacyOpen ? <Unlock size={16}/> : <Lock size={16}/>}</button>
            <button title="Close" onClick={() => setOpen(false)} className="p-1 rounded hover:bg-slate-800">✕</button>
          </div>
        </div>

        <div className="text-xs text-slate-400 mb-2">Use this panel to send training examples or manage privacy for local Ollama data. When privacy is closed, training payloads are scrubbed of PII.</div>

        <textarea value={trainingText} onChange={(e) => setTrainingText(e.target.value)} placeholder={privacyOpen ? "Enter private training text (PII preserved)" : "Enter public training text (PII scrubbed)"} className="w-full h-28 p-2 bg-slate-800 rounded border border-slate-700 text-sm text-slate-200" />

        <div className="flex items-center justify-between mt-2">
          <div className="text-xs text-slate-400">Privacy: {privacyOpen ? "Open" : "Closed"}</div>
          <div className="flex items-center gap-2">
            <button onClick={() => { setTrainingText(""); toast.success("Cleared"); }} className="px-2 py-1 bg-slate-800 rounded text-xs">Clear</button>
            <button onClick={sendTraining} className="px-2 py-1 bg-emerald-500 hover:bg-emerald-600 rounded text-xs">Send</button>
          </div>
        </div>

        <div className="mt-3 text-xs text-slate-400 flex items-center gap-2"><Database size={14}/> <span>Local DB: <strong className="text-slate-200">sqlite (local)</strong></span></div>
      </div>

      <style>{`
        .nav-chat-column { transition: transform 220ms ease; }
      `}</style>

      <button onClick={() => setOpen((s) => !s)} className="fixed right-4 top-12 z-50 bg-emerald-500 p-2 rounded-full shadow-lg">{open ? '×' : '💬'}</button>
    </div>
  );
}
