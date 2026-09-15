package com.v1byte.corelink;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final int BG = Color.rgb(7, 10, 18), CARD = Color.rgb(13, 23, 36), BORDER = Color.rgb(29, 69, 93), TEXT = Color.rgb(225, 239, 250), MUTED = Color.rgb(126, 153, 173), BLUE = Color.rgb(0, 158, 221), GREEN = Color.rgb(72, 218, 150), RED = Color.rgb(245, 110, 110), AMBER = Color.rgb(238, 190, 81);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private EditText endpoint, prompt; private Spinner modelSpinner; private TextView bridgeStatus, chat; private Button connect, send;
    private String bridge = "http://127.0.0.1:8787";
    private final String commands = "pkg update -y\npkg install -y nodejs git curl\ngit clone https://github.com/v1-byte/CORELINK.git\ncd CORELINK/bridge\ncp config.template .env\nbash start-termux.sh";

    @Override public void onCreate(Bundle state) { super.onCreate(state); Window w = getWindow(); w.setStatusBarColor(BG); w.setNavigationBarColor(BG); w.getDecorView().setSystemUiVisibility(0); buildNativeScreen(); }
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private TextView text(String value, float size, int color) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color); v.setTypeface(Typeface.create("sans", Typeface.NORMAL)); return v; }
    private GradientDrawable bg(int color, int stroke) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(12)); if (stroke != 0) d.setStroke(dp(1), stroke); return d; }
    private void pad(View v, int h, int vert) { v.setPadding(dp(h), dp(vert), dp(h), dp(vert)); }
    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w < 0 ? w : dp(w), h < 0 ? h : dp(h)); }
    private void section(LinearLayout root, String name) { TextView t = text(name, 11, BLUE); t.setTypeface(Typeface.MONOSPACE, Typeface.BOLD); t.setPadding(0, dp(20), 0, dp(8)); root.addView(t); }

    private void buildNativeScreen() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); pad(root, 18, 16); scroll.addView(root);
        root.setOnApplyWindowInsetsListener((v, insets) -> { v.setPadding(dp(18), insets.getInsets(WindowInsets.Type.statusBars()).top + dp(10), dp(18), insets.getInsets(WindowInsets.Type.navigationBars()).bottom + dp(12)); return insets; });
        LinearLayout header = row(); header.setPadding(0, 0, 0, dp(16));
        ImageView logo = new ImageView(this); logo.setImageResource(com.v1byte.corelink.R.drawable.ai_connector_icon); logo.setScaleType(ImageView.ScaleType.CENTER_CROP); header.addView(logo, lp(46, 46));
        LinearLayout titles = new LinearLayout(this); titles.setOrientation(LinearLayout.VERTICAL); titles.setPadding(dp(11), 0, 0, 0); TextView title = text("AI CONNECTOR", 19, TEXT); title.setTypeface(Typeface.DEFAULT_BOLD); titles.addView(title); TextView sub = text("CORELINK  •  BRIDGE OS", 10, BLUE); sub.setTypeface(Typeface.MONOSPACE); titles.addView(sub); header.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));
        TextView operational = text("● ONLINE", 10, GREEN); operational.setTypeface(Typeface.MONOSPACE); header.addView(operational); root.addView(header);
        TextView welcome = text("Your local AI command center", 23, TEXT); welcome.setTypeface(Typeface.DEFAULT_BOLD); root.addView(welcome); TextView description = text("One native workspace for Ollama, Termux, cloud services, Git, Docker, and AI connectors.", 13, MUTED); description.setPadding(0, dp(6), 0, dp(16)); root.addView(description);

        LinearLayout bridgeCard = new LinearLayout(this); bridgeCard.setOrientation(LinearLayout.VERTICAL); pad(bridgeCard, 14, 14); bridgeCard.setBackground(bg(CARD, BORDER)); TextView bridgeTitle = text("BRIDGE CONNECTION", 11, BLUE); bridgeTitle.setTypeface(Typeface.MONOSPACE); bridgeCard.addView(bridgeTitle); endpoint = new EditText(this); endpoint.setSingleLine(true); endpoint.setText(bridge); endpoint.setTextColor(TEXT); endpoint.setTextSize(13); endpoint.setHintTextColor(MUTED); endpoint.setHint("http://127.0.0.1:8787"); endpoint.setBackground(bg(Color.rgb(8, 15, 24), BORDER)); endpoint.setPadding(dp(12), 0, dp(12), 0); LinearLayout.LayoutParams ep = lp(-1, 48); ep.topMargin = dp(10); bridgeCard.addView(endpoint, ep);
        LinearLayout actions = row(); actions.setPadding(0, dp(10), 0, 0); connect = button("CONNECT BRIDGE", BLUE); actions.addView(connect, new LinearLayout.LayoutParams(0, dp(44), 1)); bridgeStatus = text("Not connected", 11, MUTED); bridgeStatus.setGravity(Gravity.CENTER_VERTICAL); bridgeStatus.setPadding(dp(12), 0, 0, 0); actions.addView(bridgeStatus, new LinearLayout.LayoutParams(0, dp(44), 1)); bridgeCard.addView(actions); root.addView(bridgeCard, lp(-1, -2));

        section(root, "TERMUX QUICK SETUP"); LinearLayout setup = new LinearLayout(this); setup.setOrientation(LinearLayout.VERTICAL); pad(setup, 14, 13); setup.setBackground(bg(CARD, BORDER)); TextView setupInfo = text("1. Copy commands  2. Paste in Termux  3. Run the Bridge", 11, TEXT); setup.addView(setupInfo); TextView commandBox = text(commands, 10, Color.rgb(166, 213, 230)); commandBox.setTypeface(Typeface.MONOSPACE); commandBox.setTextIsSelectable(true); commandBox.setGravity(Gravity.TOP); commandBox.setBackground(bg(Color.rgb(8, 15, 24), BORDER)); pad(commandBox, 10, 10); LinearLayout.LayoutParams cb = lp(-1, 166); cb.topMargin = dp(10); setup.addView(commandBox, cb); Button copyCommands = button("COPY COMMANDS", BLUE); LinearLayout.LayoutParams cp = lp(-1, 42); cp.topMargin = dp(9); setup.addView(copyCommands, cp); TextView setupNote = text("After Bridge starts, run: ollama serve\nThen connect with the endpoint above.", 10, MUTED); setupNote.setTypeface(Typeface.MONOSPACE); setupNote.setPadding(0, dp(9), 0, 0); setup.addView(setupNote); root.addView(setup, lp(-1, -2)); copyCommands.setOnClickListener(v -> copy("CORELINK Termux commands", commands));

        section(root, "CONNECTORS"); LinearLayout connectors = new LinearLayout(this); connectors.setOrientation(LinearLayout.VERTICAL); connectors.setBackground(bg(CARD, BORDER)); String[] names = {"GitHub", "GitLab", "Cloudflared", "Docker", "Vercel", "Supabase", "Ollama Local", "HuggingFace / Meta"}; for (String name : names) connectors.addView(connectorRow(name, name.equals("Ollama Local") ? "Bridge required" : "Configure in Termux")); root.addView(connectors, lp(-1, -2));

        section(root, "OLLAMA MODEL"); modelSpinner = new Spinner(this); modelSpinner.setBackground(bg(Color.rgb(8, 15, 24), BORDER)); ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Connect Bridge to load models"}); modelSpinner.setAdapter(adapter); root.addView(modelSpinner, lp(-1, 48));
        section(root, "AI WORKSPACE"); chat = text("Bridge belum tersambung.\n\nTekan COPY COMMANDS, paste ke Termux, jalankan Bridge, lalu tekan CONNECT BRIDGE.", 13, MUTED); chat.setGravity(Gravity.TOP); pad(chat, 14, 14); chat.setBackground(bg(CARD, BORDER)); root.addView(chat, lp(-1, 180));
        LinearLayout composer = row(); composer.setPadding(0, dp(12), 0, 0); prompt = new EditText(this); prompt.setSingleLine(false); prompt.setHint("Ask Ollama through CORELINK..."); prompt.setHintTextColor(MUTED); prompt.setTextColor(TEXT); prompt.setTextSize(13); prompt.setBackground(bg(Color.rgb(8, 15, 24), BORDER)); prompt.setPadding(dp(12), 0, dp(12), 0); composer.addView(prompt, new LinearLayout.LayoutParams(0, dp(52), 1)); send = button("SEND", BLUE); LinearLayout.LayoutParams sp = lp(82, 52); sp.leftMargin = dp(8); composer.addView(send, sp); root.addView(composer);
        TextView footer = text("Bridge 8787  •  Ollama 11434  •  Native Android", 9, MUTED); footer.setTypeface(Typeface.MONOSPACE); footer.setPadding(0, dp(12), 0, 0); root.addView(footer);
        connect.setOnClickListener(v -> connectBridge()); send.setOnClickListener(v -> sendPrompt()); setContentView(scroll); }
    private View connectorRow(String name, String status) { LinearLayout r = row(); r.setPadding(dp(12), dp(11), dp(12), dp(11)); TextView icon = text("●", 15, name.equals("Ollama Local") ? AMBER : MUTED); r.addView(icon, lp(24, -2)); LinearLayout copy = new LinearLayout(this); copy.setOrientation(LinearLayout.VERTICAL); TextView n = text(name, 12, TEXT); TextView s = text(status, 9, MUTED); s.setTypeface(Typeface.MONOSPACE); copy.addView(n); copy.addView(s); r.addView(copy, new LinearLayout.LayoutParams(0, -2, 1)); TextView arrow = text("›", 22, MUTED); r.addView(arrow); r.setOnClickListener(v -> Toast.makeText(this, name + ": " + status, Toast.LENGTH_SHORT).show()); return r; }
    private Button button(String label, int color) { Button b = new Button(this); b.setText(label); b.setTextSize(10); b.setTextColor(Color.WHITE); b.setTypeface(Typeface.MONOSPACE, Typeface.BOLD); b.setAllCaps(false); b.setBackground(bg(color, 0)); return b; }
    private void copy(String label, String value) { ClipboardManager cm = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE); cm.setPrimaryClip(ClipData.newPlainText(label, value)); Toast.makeText(this, "Copied. Paste in Termux.", Toast.LENGTH_SHORT).show(); }
    private void connectBridge() { bridge = endpoint.getText().toString().trim().replaceAll("/$", ""); bridgeStatus.setText("Connecting..."); executor.execute(() -> { try { request(bridge + "/health", null); String tags = request(bridge + "/api/ollama/tags", null); ArrayList<String> names = new ArrayList<>(); Matcher m = Pattern.compile("\\\"name\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(tags); while (m.find()) names.add(m.group(1)); runOnUiThread(() -> { bridgeStatus.setText("● Connected"); bridgeStatus.setTextColor(GREEN); if (names.isEmpty()) names.add("No Ollama model found"); modelSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, names)); chat.setText("Bridge connected. Ollama siap menerima pesan."); }); } catch (Exception e) { runOnUiThread(() -> { bridgeStatus.setText("Connection failed"); bridgeStatus.setTextColor(RED); Toast.makeText(this, "Bridge gagal. Pastikan start-termux.sh sudah berjalan.", Toast.LENGTH_LONG).show(); }); } }); }
    private void sendPrompt() { String q = prompt.getText().toString().trim(); if (q.isEmpty()) return; String selected = modelSpinner.getSelectedItem() == null ? "qwen2.5-coder:1.5b" : modelSpinner.getSelectedItem().toString(); chat.setText(chat.getText() + "\n\nYOU\n" + q + "\n\nCORELINK AI\nThinking..."); prompt.setText(""); executor.execute(() -> { try { String body = "{\"model\":\"" + selected.replace("\"", "") + "\",\"prompt\":\"" + q.replace("\\", "\\\\").replace("\"", "\\\"") + "\",\"stream\":false}"; String response = request(bridge + "/api/ollama/generate", body); Matcher m = Pattern.compile("\\\"response\\\"\\s*:\\s*\"(.*?)\"").matcher(response); String answer = m.find() ? m.group(1).replace("\\n", "\n") : response; runOnUiThread(() -> chat.setText(chat.getText() + "\n" + answer)); } catch (Exception e) { runOnUiThread(() -> chat.setText(chat.getText() + "\nGagal: " + e.getMessage())); } }); }
    private String request(String url, String body) throws Exception { HttpURLConnection c = (HttpURLConnection)new URL(url).openConnection(); c.setConnectTimeout(8000); c.setReadTimeout(120000); c.setRequestMethod(body == null ? "GET" : "POST"); c.setRequestProperty("Content-Type", "application/json"); if (body != null) { c.setDoOutput(true); try (OutputStream o = c.getOutputStream()) { o.write(body.getBytes("UTF-8")); } } int code = c.getResponseCode(); BufferedReader r = new BufferedReader(new InputStreamReader(code < 400 ? c.getInputStream() : c.getErrorStream())); StringBuilder out = new StringBuilder(); String line; while ((line = r.readLine()) != null) out.append(line); if (code >= 400) throw new Exception("HTTP " + code); return out.toString(); }
    @Override protected void onDestroy() { executor.shutdownNow(); super.onDestroy(); }
}
