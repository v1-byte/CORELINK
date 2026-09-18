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


const quickPrompts = [
  { label: "Health check", prompt: "Audit the current connector health and flag anything that needs attention." },
  { label: "Sync plan", prompt: "Prepare a concise sync plan for my connected services." },
  { label: "Recent activity", prompt: "Summarize the latest activity across my connector network." },
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
  const [model, setModel] = useState(() => { const saved = localStorage.getItem("corelink-ollama-model"); return saved && !/([3-9]b|[1-9][0-9]+b)/i.test(saved) ? saved : "qwen2.5:0.5b"; });
  const abortRef = useRef<AbortController | null>(null);
  const visibleConnectors = useMemo(() => connectors, []);
  const isConnectorRequest = (text: string) => /\b(github|gitlab|vercel|supabase|huggingface|docker|cloudflared|repository|repo|project|deployment|model hub|container)\b/i.test(text);

  const sendMessage = () => {
    const value = input.trim();
    if (!value || thinking) return;
    setMessages((m) => [...m, { role: "user", text: value, time: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" }) }]);
    setInput(""); setThinking(true);
    if (ollamaOnline && isConnectorRequest(value)) {
      const controller = new AbortController(); abortRef.current = controller;
      const timer = window.setTimeout(() => controller.abort(), 15000);
      const answerTime = new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" });
      setMessages((m) => [...m, { role: "assistant", text: "", time: answerTime }]);
      fetch(`${bridgeUrl.replace(/\/$/, "")}/api/agent`, { signal: controller.signal, method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ prompt: value, model }) })
        .then(async (response) => {
          const data = await response.json();
          if (!response.ok) throw new Error(data.error || `Bridge HTTP ${response.status}`);
          const result = data.result || data;
          const text = result.configured === false ? `GitHub belum dikonfigurasi. Tambahkan GITHUB_TOKEN di ~/corelink-bridge/.env lalu restart bridge.` : JSON.stringify(data, null, 2);
          setMessages((m) => { const copy = [...m]; const last = copy.length - 1; if (copy[last]?.role === "assistant") copy[last] = { ...copy[last], text }; return copy; });
        })
        .catch((error) => setMessages((m) => [...m, { role: "assistant", text: error.name === "AbortError" ? "Permintaan connector timeout." : `Bridge error: ${error.message}`, time: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" }) }]))
        .finally(() => { window.clearTimeout(timer); setThinking(false); abortRef.current = null; });
    } else if (ollamaOnline) {
      const controller = new AbortController(); abortRef.current = controller;
      const timer = window.setTimeout(() => controller.abort(), 120000);
      const answerTime = new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" });
      setMessages((m) => [...m, { role: "assistant", text: "", time: answerTime }]);
      fetch(`${bridgeUrl.replace(/\/$/, "")}/api/ollama/generate`, { signal: controller.signal, method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ model, prompt: value, stream: true, options: { num_ctx: 1024, temperature: 0.2 } }) })
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
    try { const health = await fetch(`${normalized}/health`, { signal: AbortSignal.timeout(8000) }); if (!health.ok) throw new Error(); const response = await fetch(`${normalized}/api/ollama/tags`, { signal: AbortSignal.timeout(8000) }); if (!response.ok) throw new Error(); const data = await response.json(); const names = (data.models || []).map((entry: { name: string }) => entry.name); const preferred = names.find((name: string) => name === "qwen2.5:0.5b") || names.find((name: string) => /0\.5b/i.test(name)) || names[0]; setModels(names); if (preferred && (!names.includes(model) || /([3-9]b|[1-9][0-9]+b)/i.test(model))) { setModel(preferred); localStorage.setItem("corelink-ollama-model", preferred); } setOllamaOnline(true); toast.success(`Bridge connected · ${names.length} Ollama model(s)`); }
    catch { setOllamaOnline(false); toast.error("Ollama belum dapat dijangkau"); }
  };
  useEffect(() => { connectOllamaSilently(); }, []);
  const connectOllamaSilently = async () => { try { const health = await fetch(`${bridgeUrl.replace(/\/$/, "")}/health`, { signal: AbortSignal.timeout(4000) }); if (!health.ok) return; const response = await fetch(`${bridgeUrl.replace(/\/$/, "")}/api/ollama/tags`, { signal: AbortSignal.timeout(4000) }); if (!response.ok) return; const data = await response.json(); setModels((data.models || []).map((entry: { name: string }) => entry.name)); setOllamaOnline(true); } catch { setOllamaOnline(false); } };
  const resetChat = () => { setMessages([]); setInput(""); setPreview(null); toast.success("New chat started"); };
  const copy = (text: string) => { navigator.clipboard?.writeText(text); toast.success("Copied to clipboard"); };

  return <div className="app-shell">
    <header className="topbar"><div className="brand"><div className="brand-mark"><img src="/ai-connector-icon.png" alt="AI Connector" /></div><div><div className="brand-name">AI <span>CONNECTOR</span></div><div className="brand-sub">AI CONNECTOR OS <i>v2.4.1</i></div></div></div><div className="top-actions"><button className="icon-btn mobile-menu" onClick={() => setMode(mode === "expanded" ? "hidden" : "expanded")} title="Open connectors"><Menu size={18}/></button><span className="live-dot"><span/> SYSTEM OPERATIONAL</span><button className="icon-btn" onClick={() => setShowSetup(true)} title="Bridge setup"><CircleHelp size={17}/></button><button className="avatar">FB</button></div></header>
    <div className="workspace">
      <aside className={`connectors-panel ${mode}`}>
        <div className="panel-heading"><div><span className="eyebrow">NETWORK</span><h2>Connectors <b>08</b></h2></div><button className="icon-btn" onClick={() => setMode(mode === "expanded" ? "icons" : mode === "icons" ? "hidden" : "expanded")} title="Toggle connector panel">{mode === "expanded" ? <ChevronLeft size={16}/> : mode === "icons" ? <ChevronRight size={16}/> : <PanelRightOpen size={16}/>}</button></div>
        <button className="new-chat-btn" onClick={resetChat}><Plus size={15}/><span>New chat</span><kbd>⌘ K</kbd></button>
        <div className="sidebar-section-label"><span>AVAILABLE CONNECTORS</span><b>{visibleConnectors.length}</b></div>
        <div className="connector-list">{visibleConnectors.map((c) => { const Icon = c.icon; return <button className={`connector-row ${activeConnector === c.name ? "active" : ""}`} key={c.name} onClick={() => setActiveConnector(c.name)} title={c.name}><span className={`connector-icon ${c.tone}`}><Icon size={17}/></span><span className="connector-copy"><strong>{c.name}</strong><small><i className={`status-dot ${c.tone}`}/>{c.status}</small></span><span className="latency">{c.latency}</span></button>})}</div>
        <button className="add-connector" onClick={() => toast.info("Connector marketplace coming soon")}><Plus size={15}/> <span>Add connector</span></button>
        <div className="sidebar-foot"><button><Settings2 size={16}/> <span>Settings</span></button><button><Terminal size={16}/> <span>Terminal</span></button></div>
      </aside>
      <main className="chat-panel">
        <div className="chat-top"><div className="chat-heading-copy"><span className="eyebrow">WORKSPACE / DEFAULT</span><h1>Ask your network</h1><p>Private workspace for your connected services</p></div><div className="chat-top-actions"><span className="session-badge"><i/> Secure session</span>{models.length > 0 && <select className="model-select" value={model} onChange={(e) => { setModel(e.target.value); localStorage.setItem("corelink-ollama-model", e.target.value); }} aria-label="Ollama model">{models.map((name) => <option key={name}>{name}</option>)}</select>}<button className="connection-pill ollama-pill" onClick={connectOllama}><i className={ollamaOnline ? "" : "offline"}/> {ollamaOnline ? "Ollama linked" : "Connect Ollama"}</button><button className="icon-btn"><MoreHorizontal size={18}/></button></div></div>
        <div className="chat-canvas"><div className="chat-context"><div className="context-copy"><span className="context-icon"><Sparkles size={13}/></span><div><strong>Private command session</strong><span>Your workspace is ready for a new request</span></div></div><span className="context-status"><i/> Encrypted</span></div><div className={`brain-stage ${messages.length > 0 ? "compact" : ""} ${thinking ? "thinking" : ""}`}><div className="orbit orbit-one"/><div className="orbit orbit-two"/><div className="neural-visual" role="img" aria-label="Animated colorful neural brain"><div className="brain-halo halo-one"/><div className="brain-halo halo-two"/><div className="brain-layer layer-violet"><BrainCircuit size={messages.length > 0 ? 144 : 184} strokeWidth={1.15}/></div><div className="brain-layer layer-coral"><BrainCircuit size={messages.length > 0 ? 136 : 176} strokeWidth={1.05}/></div><div className="brain-layer layer-cyan"><BrainCircuit size={messages.length > 0 ? 128 : 168} strokeWidth={1.2}/></div><div className="brain-core"><BrainCircuit size={messages.length > 0 ? 116 : 156} strokeWidth={1.35}/></div><span className="neural-node node-a"/><span className="neural-node node-b"/><span className="neural-node node-c"/><span className="neural-node node-d"/><span className="neural-node node-e"/><span className="signal signal-a"/><span className="signal signal-b"/><span className="signal signal-c"/></div><span className="brain-label">{thinking ? "PROCESSING REQUEST" : "NEURAL LINK ACTIVE"}</span></div>
          <div className="messages">{messages.map((m, i) => <div key={i} className={`message ${m.role}`}><div className="message-avatar">{m.role === "assistant" ? <Sparkles size={14}/> : "FB"}</div><div className="message-body"><div className="message-meta"><strong>{m.role === "assistant" ? "CORELINK AI" : "YOU"}</strong><span>{m.time}</span></div><p>{m.text}</p>{m.role === "assistant" && thinking && i === messages.length - 1 && !m.text && <div className="typing-indicator" aria-label="CORELINK AI is typing"><span/><span/><span/></div>}{m.code && <div className="code-block"><div className="code-head"><span><Code2 size={13}/> BASH</span><button onClick={() => copy(m.code!)}><Copy size={13}/> Copy</button></div><pre>{m.code}</pre></div>} {m.role === "assistant" && m.text && <div className="message-tools"><button onClick={() => copy(m.text)}><Clipboard size={13}/> Copy</button><button onClick={() => setPreview(m.text)}><Eye size={13}/> Preview</button></div>}</div></div>)}</div>
        </div>
        <div className="composer"><div className="suggestion-row" aria-label="Suggested prompts">{quickPrompts.map((item) => <button key={item.label} onClick={() => setInput(item.prompt)} disabled={thinking}><Sparkles size={12}/>{item.label}</button>)}</div><div className="composer-inner"><textarea value={input} onChange={(e) => setInput(e.target.value)} onKeyDown={(e) => {if(e.key === "Enter" && !e.shiftKey){e.preventDefault(); sendMessage()}}} placeholder={ollamaOnline ? `Ask ${model} through CORELINK...` : "Ask CORELINK anything..."} rows={1}/><div className="composer-actions"><span><Command size={12}/> K</span>{thinking ? <button onClick={() => abortRef.current?.abort()} className="send-btn stop-btn" aria-label="Stop">■</button> : <button onClick={sendMessage} className="send-btn" aria-label="Send"><Send size={16}/></button>}</div></div><div className="composer-foot"><span><MessageSquare size={12}/> {ollamaOnline ? `Live Bridge · ${bridgeUrl}` : "Preview mode · start CORELINK Bridge to enable connectors"}</span><span>SHIFT + ENTER for new line</span></div></div>
      </main>
      <aside className={`flow-panel ${flowOpen ? "" : "collapsed"}`}><div className="flow-heading"><div><span className="eyebrow">OBSERVABILITY</span><h2>Connection flow</h2></div><button className="icon-btn" onClick={() => setFlowOpen(!flowOpen)}>{flowOpen ? <PanelRightClose size={16}/> : <PanelRightOpen size={16}/>}</button></div>{flowOpen && <><div className="flow-status"><span className="pulse-ring"><i/></span><div><strong>Sync in progress</strong><small>Last event 2s ago</small></div><span className="percent">72%</span></div><div className="flow-steps"><div className="flow-step done"><span>01</span><div><strong>Auth init</strong><small>service-account.json</small></div><b>✓</b></div><div className="flow-line done"/><div className="flow-step done"><span>02</span><div><strong>Git fetch</strong><small>3 repositories indexed</small></div><b>✓</b></div><div className="flow-line active"/><div className="flow-step active"><span>03</span><div><strong>AI indexing</strong><small>embedding code context</small></div><b className="spinner">◌</b></div><div className="flow-line"/><div className="flow-step"><span>04</span><div><strong>Cloudflared</strong><small>waiting for edge route</small></div><b>—</b></div></div><div className="terminal"><div className="terminal-head"><span><i/> LIVE TERMINAL</span><span>● REC</span></div><div className="terminal-body"><p><em>$</em> cloudflared tunnel run</p><p className="success">✓ Connected to edge</p><p className="dim">➜ https://corelink-edge.trycloudflare.com</p><p><em>$</em> ollama serve</p><p className="success">✓ qwen2.5-coder:1.5b loaded</p><span className="cursor"/></div></div><div className="flow-footer"><span><Activity size={13}/> 4 services monitored</span><button onClick={() => toast.success("Flow refreshed")}>Refresh <Zap size={12}/></button></div></>}</aside>
    </div>
    {mode === "hidden" && <button className="reopen-connectors" onClick={() => setMode("expanded")}><Menu size={16}/> Connect</button>}
    {showSetup && <div className="modal-backdrop" onClick={() => setShowSetup(false)}><div className="setup-modal" onClick={(e) => e.stopPropagation()}><div className="preview-head"><span><Terminal size={16}/> CORELINK Bridge setup</span><button className="icon-btn" onClick={() => setShowSetup(false)}><X size={17}/></button></div><div className="setup-content"><img className="setup-logo" src="/ai-connector-logo.jpg" alt="AI Connector — Connecting AI to Your World" /><p className="setup-lead">Pastikan Termux dan CORELINK Bridge berjalan di HP yang sama. Bridge meneruskan chat APK ke Ollama dan menghubungkan connector lain.</p><div className="setup-step"><b>1</b><div><strong>Install dan jalankan Bridge</strong><div className="setup-code">pkg update -y<br/>pkg install -y nodejs git curl<br/>git clone https://github.com/v1-byte/CORELINK.git<br/>cd CORELINK/bridge<br/>cp config.template .env<br/>nano .env<br/>bash start-termux.sh</div><button className="copy-all" onClick={() => copy("pkg update -y\npkg install -y nodejs git curl\ngit clone https://github.com/v1-byte/CORELINK.git\ncd CORELINK/bridge\ncp config.template .env\nnano .env\nbash start-termux.sh")}><Copy size={14}/> Copy commands</button></div></div><div className="setup-step"><b>2</b><div><strong>Jalankan Ollama lalu masukkan endpoint Bridge</strong><p>Di Termux jalankan `ollama serve`. Setelah itu tekan Connect Bridge dan gunakan:</p><div className="endpoint-code">http://127.0.0.1:8787</div><p>Jangan masukkan port Ollama 11434 di APK. APK berbicara ke Bridge port 8787.</p><button className="copy-all" onClick={() => copy("http://127.0.0.1:8787")}><Copy size={14}/> Copy endpoint</button></div></div><div className="setup-step"><b>3</b><div><strong>Isi token connector (opsional)</strong><p>Token disimpan di Termux dalam file <code>.env</code>, bukan di APK. Bridge akan membaca GitHub, GitLab, Vercel, Supabase, dan HuggingFace setelah token diisi.</p></div></div></div></div></div>}
    {preview && <div className="modal-backdrop" onClick={() => setPreview(null)}><div className="preview-modal" onClick={(e) => e.stopPropagation()}><div className="preview-head"><span><Eye size={16}/> Response preview</span><button className="icon-btn" onClick={() => setPreview(null)}><X size={17}/></button></div><div className="preview-content"><div className="preview-chip">CORELINK AI / MARKDOWN</div><p>{preview}</p></div><button className="copy-all" onClick={() => copy(preview)}><Copy size={14}/> Copy all</button></div></div>}
  </div>
}
