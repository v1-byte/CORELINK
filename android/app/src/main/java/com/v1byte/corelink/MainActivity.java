package com.v1byte.corelink;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
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
    private final int BG = Color.rgb(7, 10, 18), CARD = Color.rgb(13, 23, 36), BORDER = Color.rgb(29, 69, 93), TEXT = Color.rgb(225, 239, 250), MUTED = Color.rgb(126, 153, 173), BLUE = Color.rgb(0, 158, 221), GREEN = Color.rgb(72, 218, 150);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private EditText endpoint, prompt; private Spinner modelSpinner; private TextView bridgeStatus, chat; private Button connect, send;
    private String bridge = "http://127.0.0.1:8787";

    @Override public void onCreate(Bundle state) { super.onCreate(state); Window w = getWindow(); w.setStatusBarColor(BG); w.setNavigationBarColor(BG); buildNativeScreen(); }
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private TextView text(String value, float size, int color) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color); v.setTypeface(Typeface.create("sans", Typeface.NORMAL)); return v; }
    private GradientDrawable bg(int color, int stroke) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(12)); if (stroke != 0) d.setStroke(dp(1), stroke); return d; }
    private void pad(View v, int h, int vert) { v.setPadding(dp(h), dp(vert), dp(h), dp(vert)); }
    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w < 0 ? w : dp(w), h < 0 ? h : dp(h)); }

    private void buildNativeScreen() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); pad(root, 18, 18); scroll.addView(root);
        LinearLayout header = row(); header.setPadding(0, 0, 0, dp(18));
        ImageView logo = new ImageView(this); logo.setImageResource(com.v1byte.corelink.R.drawable.ai_connector_icon); logo.setScaleType(ImageView.ScaleType.CENTER_CROP); header.addView(logo, lp(48, 48));
        LinearLayout titles = new LinearLayout(this); titles.setOrientation(LinearLayout.VERTICAL); titles.setPadding(dp(12), 0, 0, 0); TextView title = text("AI CONNECTOR", 20, TEXT); title.setTypeface(Typeface.DEFAULT_BOLD); titles.addView(title); TextView sub = text("CORELINK  •  BRIDGE OS", 10, BLUE); sub.setTypeface(Typeface.MONOSPACE); titles.addView(sub); header.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));
        TextView operational = text("● ONLINE", 10, GREEN); operational.setTypeface(Typeface.MONOSPACE); header.addView(operational); root.addView(header);

        TextView welcome = text("Your local AI command center", 24, TEXT); welcome.setTypeface(Typeface.DEFAULT_BOLD); root.addView(welcome); TextView description = text("Connect Termux Bridge to use Ollama and manage your services from one native Android workspace.", 13, MUTED); description.setPadding(0, dp(6), 0, dp(18)); root.addView(description);

        LinearLayout bridgeCard = new LinearLayout(this); bridgeCard.setOrientation(LinearLayout.VERTICAL); pad(bridgeCard, 14, 14); bridgeCard.setBackground(bg(CARD, BORDER)); TextView bridgeTitle = text("BRIDGE CONNECTION", 11, BLUE); bridgeTitle.setTypeface(Typeface.MONOSPACE); bridgeCard.addView(bridgeTitle); endpoint = new EditText(this); endpoint.setSingleLine(true); endpoint.setText(bridge); endpoint.setTextColor(TEXT); endpoint.setTextSize(13); endpoint.setHintTextColor(MUTED); endpoint.setHint("http://127.0.0.1:8787"); endpoint.setBackground(bg(Color.rgb(8, 15, 24), BORDER)); endpoint.setPadding(dp(12), 0, dp(12), 0); LinearLayout.LayoutParams ep = lp(-1, 48); ep.topMargin = dp(10); bridgeCard.addView(endpoint, ep);
        LinearLayout actions = row(); actions.setPadding(0, dp(10), 0, 0); connect = button("CONNECT BRIDGE", BLUE); actions.addView(connect, new LinearLayout.LayoutParams(0, dp(44), 1)); bridgeStatus = text("Not connected", 11, MUTED); bridgeStatus.setGravity(Gravity.CENTER_VERTICAL); bridgeStatus.setPadding(dp(12), 0, 0, 0); actions.addView(bridgeStatus, new LinearLayout.LayoutParams(0, dp(44), 1)); bridgeCard.addView(actions); root.addView(bridgeCard, lp(-1, -2));

        LinearLayout modelCard = new LinearLayout(this); modelCard.setOrientation(LinearLayout.VERTICAL); pad(modelCard, 14, 14); TextView modelTitle = text("OLLAMA MODEL", 11, BLUE); modelTitle.setTypeface(Typeface.MONOSPACE); modelCard.addView(modelTitle); modelSpinner = new Spinner(this); modelSpinner.setBackground(bg(Color.rgb(8, 15, 24), BORDER)); ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Connect Bridge to load models"}); modelSpinner.setAdapter(adapter); LinearLayout.LayoutParams mp = lp(-1, 48); mp.topMargin = dp(8); modelCard.addView(modelSpinner, mp); root.addView(modelCard, lp(-1, -2));

        TextView chatTitle = text("AI WORKSPACE", 11, BLUE); chatTitle.setTypeface(Typeface.MONOSPACE); chatTitle.setPadding(0, dp(22), 0, dp(8)); root.addView(chatTitle); chat = text("Bridge belum tersambung.\n\n1. Buka Termux.\n2. Jalankan: bash start-termux.sh\n3. Pastikan Ollama berjalan: ollama serve\n4. Tekan CONNECT BRIDGE di atas.", 13, MUTED); chat.setGravity(Gravity.TOP); pad(chat, 14, 14); chat.setBackground(bg(CARD, BORDER)); root.addView(chat, lp(-1, 190));
        LinearLayout composer = row(); composer.setPadding(0, dp(12), 0, 0); prompt = new EditText(this); prompt.setSingleLine(false); prompt.setHint("Ask Ollama through CORELINK..."); prompt.setHintTextColor(MUTED); prompt.setTextColor(TEXT); prompt.setTextSize(13); prompt.setBackground(bg(Color.rgb(8, 15, 24), BORDER)); prompt.setPadding(dp(12), 0, dp(12), 0); composer.addView(prompt, new LinearLayout.LayoutParams(0, dp(52), 1)); send = button("SEND", BLUE); LinearLayout.LayoutParams sp = lp(82, 52); sp.leftMargin = dp(8); composer.addView(send, sp); root.addView(composer);
        TextView footer = text("Bridge: 127.0.0.1:8787  •  Ollama: 127.0.0.1:11434", 9, MUTED); footer.setTypeface(Typeface.MONOSPACE); footer.setPadding(0, dp(14), 0, dp(8)); root.addView(footer);
        connect.setOnClickListener(v -> connectBridge()); send.setOnClickListener(v -> sendPrompt()); setContentView(scroll); }
    private Button button(String label, int color) { Button b = new Button(this); b.setText(label); b.setTextSize(10); b.setTextColor(Color.WHITE); b.setTypeface(Typeface.MONOSPACE, Typeface.BOLD); b.setAllCaps(false); b.setBackground(bg(color, 0)); return b; }
    private void connectBridge() { bridge = endpoint.getText().toString().trim().replaceAll("/$", ""); bridgeStatus.setText("Connecting..."); executor.execute(() -> { try { String health = request(bridge + "/health", null); String tags = request(bridge + "/api/ollama/tags", null); ArrayList<String> names = new ArrayList<>(); Matcher m = Pattern.compile("\\\"name\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(tags); while (m.find()) names.add(m.group(1)); runOnUiThread(() -> { bridgeStatus.setText("● Connected"); bridgeStatus.setTextColor(GREEN); if (names.isEmpty()) names.add("No Ollama model found"); ArrayAdapter<String> a = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, names); modelSpinner.setAdapter(a); chat.setText("Bridge connected. Ollama siap menerima pesan."); }); } catch (Exception e) { runOnUiThread(() -> { bridgeStatus.setText("Connection failed"); bridgeStatus.setTextColor(Color.rgb(245, 110, 110)); Toast.makeText(this, "Bridge tidak dapat dijangkau. Jalankan start-termux.sh.", Toast.LENGTH_LONG).show(); }); } }); }
    private void sendPrompt() { String q = prompt.getText().toString().trim(); if (q.isEmpty()) return; String selected = modelSpinner.getSelectedItem() == null ? "qwen2.5-coder:1.5b" : modelSpinner.getSelectedItem().toString(); chat.setText(chat.getText() + "\n\nYOU\n" + q + "\n\nCORELINK AI\nThinking..."); prompt.setText(""); executor.execute(() -> { try { String body = "{\"model\":\"" + selected.replace("\"", "") + "\",\"prompt\":\"" + q.replace("\\", "\\\\").replace("\"", "\\\"") + "\",\"stream\":false}"; String response = request(bridge + "/api/ollama/generate", body); Matcher m = Pattern.compile("\\\"response\\\"\\s*:\\s*\\\"(.*?)\\\"").matcher(response); String answer = m.find() ? m.group(1).replace("\\n", "\n") : response; runOnUiThread(() -> chat.setText(chat.getText() + "\n" + answer)); } catch (Exception e) { runOnUiThread(() -> chat.setText(chat.getText() + "\nGagal: " + e.getMessage())); } }); }
    private String request(String url, String body) throws Exception { HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection(); c.setConnectTimeout(8000); c.setReadTimeout(120000); c.setRequestMethod(body == null ? "GET" : "POST"); c.setRequestProperty("Content-Type", "application/json"); if (body != null) { c.setDoOutput(true); try (OutputStream o = c.getOutputStream()) { o.write(body.getBytes("UTF-8")); } } int code = c.getResponseCode(); BufferedReader r = new BufferedReader(new InputStreamReader(code < 400 ? c.getInputStream() : c.getErrorStream())); StringBuilder out = new StringBuilder(); String line; while ((line = r.readLine()) != null) out.append(line); if (code >= 400) throw new Exception("HTTP " + code); return out.toString(); }
    @Override protected void onDestroy() { executor.shutdownNow(); super.onDestroy(); }
}
