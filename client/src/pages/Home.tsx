import { useEffect, useMemo, useRef, useState } from "react";
import {
  Activity, Bot, BrainCircuit, ChevronLeft, ChevronRight, CircleHelp, Clipboard, Code2, Command, Copy, Eye, Github, GitBranch, Globe2, Menu, MessageSquare, MoreHorizontal, PanelRightClose, PanelRightOpen, Plus, Send, Settings2, Sparkles, Terminal, X, Zap
} from "lucide-react";
import { toast } from "sonner";

type Connector = { name: string; icon: React.ElementType; status: string; latency: string; tone: string };
const connectors: Connector[] = [
  { name: "GitHub", icon: Github, status: "Not configured", latency: "—", tone: "muted" },
  { name: "GitLab", icon: GitBranch, status: "Not configured", latency: "—", tone: "muted" },
  { name: "Cloudflared", icon: Globe2, status: "Not configured", latency: "—", tone: "muted" },
  { name: "Docker", icon: Code2, status: "Off", latency: "—", tone: "muted" },
  { name: "Vercel", icon: Zap, status: "Not configured", latency: "—", tone: "muted" },
  { name: "Supabase", icon: Activity, status: "Off", latency: "—", tone: "muted" },
  { name: "Ollama Local", icon: Bot, status: "Manual connect", latency: "—", tone: "amber" },
  { name: "HuggingFace", icon: BrainCircuit, status: "Off", latency: "—", tone: "muted" },
];

const initialMessages = [
  { role: "assistant", text: "Connection established. I’m ready to work across your connected services. What should we build today?", time: "20:18:42" },
  { role: "user", text: "Audit the current connector health and prepare a sync plan.", time: "20:19:08" },
  { role: "assistant", text: "All core services are responding within normal range. GitHub, GitLab, Vercel, and Ollama are online. Cloudflared is negotiating an edge route now.", time: "20:19:12", code: "git fetch --all --prune\nollama list\ncloudflared tunnel --url http://localhost:5173" },
];

export default function Home() {
  const [mode, setMode] = useState<"expanded" | "icons" | "hidden">("expanded");
  const [messages, setMessages] = useState(initialMessages);
  const [input, setInput] = useState("");
  const [thinking, setThinking] = useState(false);
  const [preview, setPreview] = useState<string | null>(null);
  const [showSetup, setShowSetup] = useState(false);
  const [flowOpen, setFlowOpen] = useState(true);
  const [activeConnector, setActiveConnector] = useState("GitHub");
  const [bridgeUrl, setBridgeUrl] = useState(() => localStorage.getItem("corelink-bridge-url") || "http://127.0.0.1:8787");
  const [ollamaOnline, setOllamaOnline] = useState(false);
  const [models, setModels] = useState<string[]>([]);
  const [model, setModel] = useState(() => localStorage.getItem("corelink-ollama-model") || "qwen2.5-coder:1.5b");
  const abortRef = useRef<AbortController | null>(null);
  const visibleConnectors = useMemo(() => connectors, []);

  const sendMessage = () => {
    const value = input.trim();
    if (!value || thinking) return;
    setMessages((m) => [...m, { role: "user", text: value, time: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" }) }]);
    setInput(""); setThinking(true);
    if (ollamaOnline) {
      const controller = new AbortController(); abortRef.current = controller;
      const timer = window.setTimeout(() => controller.abort(), 120000);
      const answerTime = new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" });
      setMessages((m) => [...m, { role: "assistant", text: "", time: answerTime }]);
      fetch(`${bridgeUrl.replace(/\/$/, "")}/api/ollama/generate`, { signal: controller.signal, method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ model, prompt: value, stream: true, options: { num_ctx: 2048 } }) })
        .then(async (response) => {
          if (!response.ok || !response.body) throw new Error(`Ollama HTTP ${response.status}`);
          const reader = response.body.getReader(); const decoder = new TextDecoder(); let buffer = "";
          while (true) { const { value: chunk, done } = await reader.read(); if (done) break; buffer += decoder.decode(chunk, { stream: true });
            const lines = buffer.split("\n"); buffer = lines.pop() || "";
            for (const line of lines) { if (!line.trim()) continue; const data = JSON.parse(line); if (data.response) setMessages((m) => { const copy = [...m]; const last = copy.length - 1; if (copy[last]?.role === "assistant") copy[last] = { ...copy[last], text: copy[last].text + data.response }; return copy; }); }
          }
        })
        .catch((error) => { setOllamaOnline(false); setMessages((m) => [...m, { role: "assistant", text: error.name === "AbortError" ? "Request Ollama dihentikan karena timeout 120 detik." : "Ollama tidak dapat dijangkau. Periksa ollama serve, alamat endpoint, model, dan OLLAMA_ORIGINS.", time: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" }) }]); })
        .finally(() => { window.clearTimeout(timer); setThinking(false); abortRef.current = null; });
    } else {
      window.setTimeout(() => {
        setMessages((m) => [...m, { role: "assistant", text: `Mode preview aktif. Jalankan CORELINK Bridge di ${bridgeUrl} untuk mengaktifkan connector Termux.`, time: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" }) }]);
        setThinking(false);
      }, 950);
    }
  };
  const connectOllama = async () => {
    const value = window.prompt("CORELINK Bridge endpoint", bridgeUrl)?.trim();
    if (!value) return;
    const normalized = value.replace(/\/$/, "");
    setBridgeUrl(normalized); localStorage.setItem("corelink-bridge-url", normalized);
    try { const health = await fetch(`${normalized}/health`, { signal: AbortSignal.timeout(8000) }); if (!health.ok) throw new Error(); const response = await fetch(`${normalized}/api/ollama/tags`, { signal: AbortSignal.timeout(8000) }); if (!response.ok) throw new Error(); const data = await response.json(); const names = (data.models || []).map((entry: { name: string }) => entry.name); setModels(names); if (names.length && !names.includes(model)) { setModel(names[0]); localStorage.setItem("corelink-ollama-model", names[0]); } setOllamaOnline(true); toast.success(`Bridge connected · ${names.length} Ollama model(s)`); }
    catch { setOllamaOnline(false); toast.error("Ollama belum dapat dijangkau"); }
  };
  useEffect(() => { connectOllamaSilently(); }, []);
  const connectOllamaSilently = async () => { try { const health = await fetch(`${bridgeUrl.replace(/\/$/, "")}/health`, { signal: AbortSignal.timeout(4000) }); if (!health.ok) return; const response = await fetch(`${bridgeUrl.replace(/\/$/, "")}/api/ollama/tags`, { signal: AbortSignal.timeout(4000) }); if (!response.ok) return; const data = await response.json(); setModels((data.models || []).map((entry: { name: string }) => entry.name)); setOllamaOnline(true); } catch { setOllamaOnline(false); } };
  const copy = (text: string) => { navigator.clipboard?.writeText(text); toast.success("Copied to clipboard"); };

  return <div className="app-shell">
    <header className="topbar"><div className="brand"><div className="brand-mark"><img src="/ai-connector-icon.png" alt="AI Connector" /></div><div><div className="brand-name">AI <span>CONNECTOR</span></div><div className="brand-sub">AI CONNECTOR OS <i>v2.4.1</i></div></div></div><div className="top-actions"><span className="live-dot"><span/> SYSTEM OPERATIONAL</span><button className="icon-btn" onClick={() => setShowSetup(true)} title="Bridge setup"><CircleHelp size={17}/></button><button className="avatar">FB</button></div></header>
    <div className="workspace">
      <aside className={`connectors-panel ${mode}`}>
        <div className="panel-heading"><div><span className="eyebrow">NETWORK</span><h2>Connectors <b>08</b></h2></div><button className="icon-btn" onClick={() => setMode(mode === "expanded" ? "icons" : mode === "icons" ? "hidden" : "expanded")} title="Toggle connector panel">{mode === "expanded" ? <ChevronLeft size={16}/> : mode === "icons" ? <ChevronRight size={16}/> : <PanelRightOpen size={16}/>}</button></div>
        <div className="connector-list">{visibleConnectors.map((c) => { const Icon = c.icon; return <button className={`connector-row ${activeConnector === c.name ? "active" : ""}`} key={c.name} onClick={() => setActiveConnector(c.name)} title={c.name}><span className={`connector-icon ${c.tone}`}><Icon size={17}/></span><span className="connector-copy"><strong>{c.name}</strong><small><i className={`status-dot ${c.tone}`}/>{c.status}</small></span><span className="latency">{c.latency}</span></button>})}</div>
        <button className="add-connector" onClick={() => toast.info("Connector marketplace coming soon")}><Plus size={15}/> <span>Add connector</span></button>
        <div className="sidebar-foot"><button><Settings2 size={16}/> <span>Settings</span></button><button><Terminal size={16}/> <span>Terminal</span></button></div>
      </aside>
      <main className="chat-panel">
        <div className="chat-top"><div><span className="eyebrow">WORKSPACE / DEFAULT</span><h1>Ask your network</h1></div><div className="chat-top-actions">{models.length > 0 && <select className="model-select" value={model} onChange={(e) => { setModel(e.target.value); localStorage.setItem("corelink-ollama-model", e.target.value); }} aria-label="Ollama model">{models.map((name) => <option key={name}>{name}</option>)}</select>}<button className="connection-pill ollama-pill" onClick={connectOllama}><i className={ollamaOnline ? "" : "offline"}/> {ollamaOnline ? "Ollama linked" : "Connect Ollama"}</button><button className="icon-btn"><MoreHorizontal size={18}/></button></div></div>
        <div className="chat-canvas"><div className={`brain-stage ${messages.length > 0 ? "compact" : ""} ${thinking ? "thinking" : ""}`}><div className="orbit orbit-one"/><div className="orbit orbit-two"/><div className="brain-glow"><BrainCircuit size={messages.length > 0 ? 58 : 112}/></div><span className="brain-label">{thinking ? "PROCESSING REQUEST" : "NEURAL LINK ACTIVE"}</span></div>
          <div className="messages">{messages.map((m, i) => <div key={i} className={`message ${m.role}`}><div className="message-avatar">{m.role === "assistant" ? <Sparkles size={14}/> : "FB"}</div><div className="message-body"><div className="message-meta"><strong>{m.role === "assistant" ? "CORELINK AI" : "YOU"}</strong><span>{m.time}</span></div><p>{m.text}</p>{m.code && <div className="code-block"><div className="code-head"><span><Code2 size={13}/> BASH</span><button onClick={() => copy(m.code!)}><Copy size={13}/> Copy</button></div><pre>{m.code}</pre></div>} {m.role === "assistant" && <div className="message-tools"><button onClick={() => copy(m.text)}><Clipboard size={13}/> Copy</button><button onClick={() => setPreview(m.text)}><Eye size={13}/> Preview</button></div>}</div></div>)}</div>
        </div>
        <div className="composer"><div className="composer-inner"><textarea value={input} onChange={(e) => setInput(e.target.value)} onKeyDown={(e) => {if(e.key === "Enter" && !e.shiftKey){e.preventDefault(); sendMessage()}}} placeholder={ollamaOnline ? `Ask ${model} through CORELINK...` : "Ask CORELINK anything..."} rows={1}/><div className="composer-actions"><span><Command size={12}/> K</span>{thinking ? <button onClick={() => abortRef.current?.abort()} className="send-btn stop-btn" aria-label="Stop">■</button> : <button onClick={sendMessage} className="send-btn" aria-label="Send"><Send size={16}/></button>}</div></div><div className="composer-foot"><span><MessageSquare size={12}/> {ollamaOnline ? `Live Bridge · ${bridgeUrl}` : "Preview mode · start CORELINK Bridge to enable connectors"}</span><span>SHIFT + ENTER for new line</span></div></div>
      </main>
      <aside className={`flow-panel ${flowOpen ? "" : "collapsed"}`}><div className="flow-heading"><div><span className="eyebrow">OBSERVABILITY</span><h2>Connection flow</h2></div><button className="icon-btn" onClick={() => setFlowOpen(!flowOpen)}>{flowOpen ? <PanelRightClose size={16}/> : <PanelRightOpen size={16}/>}</button></div>{flowOpen && <><div className="flow-status"><span className="pulse-ring"><i/></span><div><strong>Sync in progress</strong><small>Last event 2s ago</small></div><span className="percent">72%</span></div><div className="flow-steps"><div className="flow-step done"><span>01</span><div><strong>Auth init</strong><small>service-account.json</small></div><b>✓</b></div><div className="flow-line done"/><div className="flow-step done"><span>02</span><div><strong>Git fetch</strong><small>3 repositories indexed</small></div><b>✓</b></div><div className="flow-line active"/><div className="flow-step active"><span>03</span><div><strong>AI indexing</strong><small>embedding code context</small></div><b className="spinner">◌</b></div><div className="flow-line"/><div className="flow-step"><span>04</span><div><strong>Cloudflared</strong><small>waiting for edge route</small></div><b>—</b></div></div><div className="terminal"><div className="terminal-head"><span><i/> LIVE TERMINAL</span><span>● REC</span></div><div className="terminal-body"><p><em>$</em> cloudflared tunnel run</p><p className="success">✓ Connected to edge</p><p className="dim">➜ https://corelink-edge.trycloudflare.com</p><p><em>$</em> ollama serve</p><p className="success">✓ qwen2.5-coder:1.5b loaded</p><span className="cursor"/></div></div><div className="flow-footer"><span><Activity size={13}/> 4 services monitored</span><button onClick={() => toast.success("Flow refreshed")}>Refresh <Zap size={12}/></button></div></>}</aside>
    </div>
    {mode === "hidden" && <button className="reopen-connectors" onClick={() => setMode("expanded")}><Menu size={16}/> Connect</button>}
    {showSetup && <div className="modal-backdrop" onClick={() => setShowSetup(false)}><div className="setup-modal" onClick={(e) => e.stopPropagation()}><div className="preview-head"><span><Terminal size={16}/> CORELINK Bridge setup</span><button className="icon-btn" onClick={() => setShowSetup(false)}><X size={17}/></button></div><div className="setup-content"><img className="setup-logo" src="/ai-connector-logo.jpg" alt="AI Connector — Connecting AI to Your World" /><p className="setup-lead">Jalankan Bridge di Termux agar APK terhubung ke Ollama, Docker, Cloudflared, dan connector cloud.</p><div className="setup-step"><b>1</b><div><strong>Install dan jalankan Bridge</strong><div className="setup-code">git clone https://github.com/v1-byte/CORELINK.git<br/>cd CORELINK/bridge<br/>cp config.template .env<br/>nano .env<br/>bash start-termux.sh</div><button className="copy-all" onClick={() => copy("git clone https://github.com/v1-byte/CORELINK.git\ncd CORELINK/bridge\ncp config.template .env\nnano .env\nbash start-termux.sh")}><Copy size={14}/> Copy commands</button></div></div><div className="setup-step"><b>2</b><div><strong>Masukkan endpoint Bridge</strong><p>Tekan Connect Ollama lalu gunakan:</p><div className="endpoint-code">http://127.0.0.1:8787</div><button className="copy-all" onClick={() => copy("http://127.0.0.1:8787")}><Copy size={14}/> Copy endpoint</button></div></div><div className="setup-step"><b>3</b><div><strong>Isi token connector (opsional)</strong><p>Token disimpan di Termux dalam file <code>.env</code>, bukan di APK. Bridge akan membaca GitHub, GitLab, Vercel, Supabase, dan HuggingFace setelah token diisi.</p></div></div></div></div></div>}
    {preview && <div className="modal-backdrop" onClick={() => setPreview(null)}><div className="preview-modal" onClick={(e) => e.stopPropagation()}><div className="preview-head"><span><Eye size={16}/> Response preview</span><button className="icon-btn" onClick={() => setPreview(null)}><X size={17}/></button></div><div className="preview-content"><div className="preview-chip">CORELINK AI / MARKDOWN</div><p>{preview}</p></div><button className="copy-all" onClick={() => copy(preview)}><Copy size={14}/> Copy all</button></div></div>}
  </div>
}
