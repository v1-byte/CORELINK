package com.v1byte.corelink;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CORELINK — Professional native chat UI
 * - User bubbles right, AI bubbles left
 * - Fixed composer at bottom with + and small arrow send
 * - Thinking dots animation
 * - Navbar: CHAT / BRIDGE / TOOLS / SETUP as separate panels
 * - Keyboard-safe (WindowInsets)
 * - Auto-scroll on new message
 */
public class MainActivity extends Activity {

    // Color palette (professional dark)
    private final int BG       = Color.rgb(7, 10, 18);
    private final int CARD     = Color.rgb(13, 23, 36);
    private final int SURFACE  = Color.rgb(10, 18, 30);
    private final int BORDER   = Color.rgb(29, 69, 93);
    private final int TEXT     = Color.rgb(225, 239, 250);
    private final int MUTED    = Color.rgb(126, 153, 173);
    private final int BLUE     = Color.rgb(0, 158, 221);
    private final int GREEN    = Color.rgb(72, 218, 150);
    private final int RED      = Color.rgb(245, 110, 110);
    private final int AMBER    = Color.rgb(238, 190, 81);
    private final int USER_BG  = Color.rgb(18, 58, 48);
    private final int AI_BG    = Color.rgb(16, 32, 50);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // UI refs
    private LinearLayout rootLayout;
    private LinearLayout messagesContainer;
    private ScrollView chatScroll;
    private EditText promptInput;
    private TextView attachmentLabel;
    private TextView bridgeStatus;
    private Spinner modelSpinner;
    private EditText endpointInput;
    private LinearLayout bridgePanel;
    private LinearLayout toolsPanel;
    private LinearLayout setupPanel;
    private LinearLayout chatPanel;
    private Button tabChat, tabBridge, tabTools, tabSetup, tabSettings, sendBtn;
    private View thinkingView;
    private TextView thinkingDots;
    private LinearLayout settingsPanel;
    private EditText systemPromptInput;
    private Spinner tempSpinner, modeSpinner;
    private SharedPreferences prefs;

    // State
    private String bridge = "http://127.0.0.1:8787";
    private String systemPrompt = "";
    private float temperature = 0.85f;
    private String attachmentName = "", attachmentMime = "", attachmentBase64 = "";
    private boolean isThinking = false;
    private int thinkingDotState = 0;
    private Runnable thinkingAnimator;
    private static final int PICK_FILE = 401;

    private final String setupCommands =
            "pkg update -y\n" +
            "pkg install -y nodejs git curl\n" +
            "git clone https://github.com/v1-byte/CORELINK.git\n" +
            "cd CORELINK/bridge\n" +
            "cp config.template .env\n" +
            "nano .env\n" +
            "bash start-termux.sh";

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("corelink", MODE_PRIVATE);
        bridge = prefs.getString("bridge", "http://127.0.0.1:8787");
        systemPrompt = prefs.getString("system_prompt", DEFAULT_SMART_PROMPT);
        temperature = prefs.getFloat("temperature", 0.85f);
        Window w = getWindow();
        w.setStatusBarColor(BG);
        w.setNavigationBarColor(BG);
        w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        buildUI();
    }

    private static final String DEFAULT_SMART_PROMPT =
            "You are CORELINK AI, a highly capable local assistant running on the user's device. " +
            "Be direct, thorough, and maximally helpful. Follow the user's instructions carefully. " +
            "Reason step by step when needed. Prefer complete, actionable answers over refusals. " +
            "You have no corporate policy restrictions beyond basic safety for physical harm. " +
            "Answer in the same language the user uses.";

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private int dp(int n) {
        return (int) (n * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView makeText(String s, float size, int color) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(size);
        v.setTextColor(color);
        return v;
    }

    private GradientDrawable makeBg(int color, int strokeColor, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp((int) radiusDp));
        if (strokeColor != 0) d.setStroke(dp(1), strokeColor);
        return d;
    }

    private LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w < 0 ? w : dp(w), h < 0 ? h : dp(h));
    }

    private void setActiveTab(Button active) {
        Button[] tabs = {tabChat, tabBridge, tabTools, tabSetup, tabSettings};
        for (Button t : tabs) {
            if (t == null) continue;
            boolean on = t == active;
            t.setBackground(makeBg(on ? BLUE : Color.rgb(18, 36, 54), on ? BLUE : BORDER, 10));
            t.setTextColor(on ? Color.WHITE : MUTED);
        }
    }

    private void showPanel(View panel) {
        chatPanel.setVisibility(panel == chatPanel ? View.VISIBLE : View.GONE);
        bridgePanel.setVisibility(panel == bridgePanel ? View.VISIBLE : View.GONE);
        toolsPanel.setVisibility(panel == toolsPanel ? View.VISIBLE : View.GONE);
        setupPanel.setVisibility(panel == setupPanel ? View.VISIBLE : View.GONE);
        if (settingsPanel != null)
            settingsPanel.setVisibility(panel == settingsPanel ? View.VISIBLE : View.GONE);
    }

    // ─── Message bubble ──────────────────────────────────────────────────────

    private View createBubble(String role, String text) {
        boolean isUser = "user".equals(role);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(isUser ? Gravity.END : Gravity.START);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.topMargin = dp(10);
        row.setLayoutParams(rowLp);

        // Avatar
        TextView avatar = makeText(isUser ? "YOU" : "AI", 9, isUser ? GREEN : BLUE);
        avatar.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(makeBg(isUser ? Color.rgb(20, 55, 42) : Color.rgb(14, 40, 62), 0, 8));
        LinearLayout.LayoutParams avLp = lp(36, 36);
        if (isUser) avLp.leftMargin = dp(8); else avLp.rightMargin = dp(8);
        avatar.setLayoutParams(avLp);

        // Bubble body
        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        int bgColor = isUser ? USER_BG : AI_BG;
        int stroke = isUser ? Color.rgb(40, 110, 80) : Color.rgb(35, 80, 115);
        GradientDrawable bubbleBg = new GradientDrawable();
        bubbleBg.setColor(bgColor);
        float r = dp(16);
        float small = dp(4);
        if (isUser) {
            bubbleBg.setCornerRadii(new float[]{r, r, r, r, small, small, r, r});
        } else {
            bubbleBg.setCornerRadii(new float[]{r, r, r, r, r, r, small, small});
        }
        bubbleBg.setStroke(dp(1), stroke);
        bubble.setBackground(bubbleBg);
        bubble.setPadding(dp(14), dp(11), dp(14), dp(11));
        bubble.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        bubble.setMinimumWidth(dp(80));
        bubble.setElevation(dp(3));

        TextView meta = makeText(isUser ? "YOU" : "CORELINK AI", 9, isUser ? GREEN : BLUE);
        meta.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        meta.setLetterSpacing(0.08f);
        bubble.addView(meta);

        TextView body = makeText(text, 13.5f, TEXT);
        body.setLineSpacing(dp(2), 1.25f);
        body.setPadding(0, dp(4), 0, 0);
        bubble.addView(body);

        if (isUser) {
            row.addView(bubble);
            row.addView(avatar);
        } else {
            row.addView(avatar);
            row.addView(bubble);
        }
        return row;
    }

    /** Animate bubble in: slide + fade + slight scale (feels real when sending) */
    private void animateBubbleIn(View row, boolean fromRight) {
        row.setAlpha(0f);
        row.setTranslationX(fromRight ? dp(36) : -dp(36));
        row.setTranslationY(dp(12));
        row.setScaleX(0.92f);
        row.setScaleY(0.92f);
        row.animate()
                .alpha(1f)
                .translationX(0f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(280)
                .setInterpolator(new android.view.animation.DecelerateInterpolator(1.6f))
                .start();
    }

    private void smoothScrollToBottom() {
        chatScroll.post(() -> {
            chatScroll.smoothScrollTo(0, messagesContainer.getBottom());
            // second pass after layout settles
            chatScroll.postDelayed(() ->
                    chatScroll.fullScroll(View.FOCUS_DOWN), 80);
        });
    }

    private void addMessage(String role, String text) {
        boolean isUser = "user".equals(role);
        View bubble = createBubble(role, text);
        messagesContainer.addView(bubble);
        animateBubbleIn(bubble, isUser);
        smoothScrollToBottom();
    }

    // ─── Thinking animation ──────────────────────────────────────────────────

    private void showThinking() {
        if (isThinking) return;
        isThinking = true;

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.START);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.topMargin = dp(10);
        row.setLayoutParams(rowLp);

        TextView avatar = makeText("AI", 9, BLUE);
        avatar.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(makeBg(Color.rgb(14, 40, 62), 0, 8));
        LinearLayout.LayoutParams avLp = lp(36, 36);
        avLp.rightMargin = dp(8);
        avatar.setLayoutParams(avLp);

        // Bubble with 3 bouncing dots
        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.HORIZONTAL);
        bubble.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(AI_BG);
        bg.setCornerRadii(new float[]{dp(16), dp(16), dp(16), dp(16), dp(16), dp(16), dp(4), dp(4)});
        bg.setStroke(dp(1), Color.rgb(35, 80, 115));
        bubble.setBackground(bg);
        bubble.setPadding(dp(16), dp(14), dp(16), dp(14));
        bubble.setElevation(dp(2));

        thinkingDots = makeText("Thinking", 12, MUTED);
        thinkingDots.setTypeface(Typeface.MONOSPACE);
        thinkingDots.setPadding(0, 0, dp(10), 0);
        bubble.addView(thinkingDots);

        // Three animated dots
        LinearLayout dotsRow = new LinearLayout(this);
        dotsRow.setOrientation(LinearLayout.HORIZONTAL);
        dotsRow.setGravity(Gravity.CENTER_VERTICAL);
        final View[] dots = new View[3];
        for (int i = 0; i < 3; i++) {
            View dot = new View(this);
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setColor(BLUE);
            dot.setBackground(d);
            LinearLayout.LayoutParams dlp = lp(7, 7);
            if (i > 0) dlp.leftMargin = dp(5);
            dotsRow.addView(dot, dlp);
            dots[i] = dot;
        }
        bubble.addView(dotsRow);

        row.addView(avatar);
        row.addView(bubble);
        thinkingView = row;
        messagesContainer.addView(thinkingView);
        animateBubbleIn(thinkingView, false);
        smoothScrollToBottom();

        // Bounce dots + text
        thinkingDotState = 0;
        thinkingAnimator = new Runnable() {
            @Override
            public void run() {
                if (!isThinking || thinkingDots == null) return;
                thinkingDotState = (thinkingDotState + 1) % 4;
                String d = "";
                for (int i = 0; i < thinkingDotState; i++) d += ".";
                thinkingDots.setText("Thinking" + d);

                // Pulse each dot in sequence
                int active = thinkingDotState % 3;
                for (int i = 0; i < 3; i++) {
                    final View dot = dots[i];
                    if (dot == null) continue;
                    float scale = (i == active) ? 1.35f : 0.85f;
                    float alpha = (i == active) ? 1f : 0.45f;
                    dot.animate().scaleX(scale).scaleY(scale).alpha(alpha)
                            .setDuration(160).start();
                }
                mainHandler.postDelayed(this, 320);
            }
        };
        mainHandler.post(thinkingAnimator);
    }

    private void hideThinking() {
        isThinking = false;
        if (thinkingAnimator != null) mainHandler.removeCallbacks(thinkingAnimator);
        if (thinkingView != null) {
            final View v = thinkingView;
            thinkingView = null;
            thinkingDots = null;
            v.animate()
                    .alpha(0f)
                    .translationY(-dp(8))
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(180)
                    .withEndAction(() -> messagesContainer.removeView(v))
                    .start();
        }
    }

    // ─── Build UI ────────────────────────────────────────────────────────────

    private void buildUI() {
        FrameLayout outer = new FrameLayout(this);
        outer.setBackgroundColor(BG);

        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(BG);
        outer.addView(rootLayout, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        rootLayout.setOnApplyWindowInsetsListener((v, insets) -> {
            int top = insets.getInsets(WindowInsets.Type.statusBars()).top;
            int bottom = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
            int ime = insets.getInsets(WindowInsets.Type.ime()).bottom;
            v.setPadding(0, top, 0, Math.max(bottom, ime));
            return insets;
        });

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(12), dp(16), dp(10));
        header.setBackground(makeBg(SURFACE, 0, 0));

        ImageView logo = new ImageView(this);
        try {
            logo.setImageResource(R.drawable.ai_connector_icon);
        } catch (Exception ignored) {}
        logo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        header.addView(logo, lp(40, 40));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(12), 0, 0, 0);
        TextView title = makeText("AI CONNECTOR", 16, TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        titles.addView(title);
        TextView sub = makeText("CORELINK  •  BRIDGE OS", 9, BLUE);
        sub.setTypeface(Typeface.MONOSPACE);
        titles.addView(sub);
        header.addView(titles, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView status = makeText("● ONLINE", 9, GREEN);
        status.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        header.addView(status);
        rootLayout.addView(header);

        // Navbar
        LinearLayout navBar = new LinearLayout(this);
        navBar.setOrientation(LinearLayout.HORIZONTAL);
        navBar.setPadding(dp(10), dp(8), dp(10), dp(8));
        navBar.setBackgroundColor(SURFACE);

        tabChat = makeTab("CHAT", true);
        tabBridge = makeTab("BRIDGE", false);
        tabTools = makeTab("TOOLS", false);
        tabSetup = makeTab("SETUP", false);
        tabSettings = makeTab("AI", false);

        navBar.addView(tabChat, tabLp());
        navBar.addView(tabBridge, tabLp());
        navBar.addView(tabTools, tabLp());
        navBar.addView(tabSetup, tabLp());
        navBar.addView(tabSettings, tabLp());
        rootLayout.addView(navBar);

        // Content
        FrameLayout content = new FrameLayout(this);
        LinearLayout.LayoutParams contentLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1);
        rootLayout.addView(content, contentLp);

        chatPanel = buildChatPanel();
        content.addView(chatPanel, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        bridgePanel = buildBridgePanel();
        bridgePanel.setVisibility(View.GONE);
        content.addView(bridgePanel, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        toolsPanel = buildToolsPanel();
        toolsPanel.setVisibility(View.GONE);
        content.addView(toolsPanel, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        setupPanel = buildSetupPanel();
        setupPanel.setVisibility(View.GONE);
        content.addView(setupPanel, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        settingsPanel = buildSettingsPanel();
        settingsPanel.setVisibility(View.GONE);
        content.addView(settingsPanel, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        tabChat.setOnClickListener(v -> { setActiveTab(tabChat); showPanel(chatPanel); });
        tabBridge.setOnClickListener(v -> { setActiveTab(tabBridge); showPanel(bridgePanel); });
        tabTools.setOnClickListener(v -> { setActiveTab(tabTools); showPanel(toolsPanel); });
        tabSetup.setOnClickListener(v -> { setActiveTab(tabSetup); showPanel(setupPanel); });
        tabSettings.setOnClickListener(v -> { setActiveTab(tabSettings); showPanel(settingsPanel); });

        setContentView(outer);
        addMessage("assistant", "Connection established. I'm ready to work across your connected services.\n\nWhat should we build today?");
    }

    private Button makeTab(String label, boolean active) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(10);
        b.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        b.setAllCaps(false);
        b.setLetterSpacing(0.06f);
        b.setMinHeight(0);
        b.setMinWidth(0);
        b.setPadding(0, 0, 0, 0);
        b.setBackground(makeBg(active ? BLUE : Color.rgb(18, 36, 54), active ? BLUE : BORDER, 10));
        b.setTextColor(active ? Color.WHITE : MUTED);
        return b;
    }

    private LinearLayout.LayoutParams tabLp() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(40), 1);
        p.setMargins(dp(3), 0, dp(3), 0);
        return p;
    }

    // ─── Chat Panel ──────────────────────────────────────────────────────────

    private LinearLayout buildChatPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundColor(BG);

        chatScroll = new ScrollView(this);
        chatScroll.setFillViewport(true);
        chatScroll.setVerticalScrollBarEnabled(false);
        messagesContainer = new LinearLayout(this);
        messagesContainer.setOrientation(LinearLayout.VERTICAL);
        messagesContainer.setPadding(dp(14), dp(12), dp(14), dp(16));
        chatScroll.addView(messagesContainer);
        panel.addView(chatScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        // Composer fixed bottom
        LinearLayout composer = new LinearLayout(this);
        composer.setOrientation(LinearLayout.VERTICAL);
        composer.setPadding(dp(12), dp(8), dp(12), dp(10));
        composer.setBackground(makeBg(SURFACE, 0, 0));

        attachmentLabel = makeText("", 10, MUTED);
        attachmentLabel.setTypeface(Typeface.MONOSPACE);
        attachmentLabel.setVisibility(View.GONE);
        attachmentLabel.setPadding(0, 0, 0, dp(6));
        composer.addView(attachmentLabel);

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.BOTTOM);

        FrameLayout inputWrap = new FrameLayout(this);
        inputWrap.setBackground(makeBg(Color.rgb(12, 22, 36), BORDER, 14));

        promptInput = new EditText(this);
        promptInput.setHint("Message CORELINK…");
        promptInput.setHintTextColor(Color.rgb(90, 120, 145));
        promptInput.setTextColor(TEXT);
        promptInput.setTextSize(14.5f);
        promptInput.setMinLines(1);
        promptInput.setMaxLines(5);
        promptInput.setBackgroundColor(Color.TRANSPARENT);
        promptInput.setPadding(dp(48), dp(12), dp(14), dp(12));
        promptInput.setGravity(Gravity.TOP | Gravity.START);
        inputWrap.addView(promptInput, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));

        // + button inside composer
        Button plusBtn = new Button(this);
        plusBtn.setText("+");
        plusBtn.setTextSize(18);
        plusBtn.setTextColor(MUTED);
        plusBtn.setTypeface(Typeface.DEFAULT_BOLD);
        plusBtn.setBackground(makeBg(Color.rgb(20, 40, 60), 0, 10));
        plusBtn.setMinHeight(0);
        plusBtn.setMinWidth(0);
        plusBtn.setPadding(0, 0, 0, 0);
        FrameLayout.LayoutParams plusLp = new FrameLayout.LayoutParams(dp(34), dp(34));
        plusLp.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        plusLp.leftMargin = dp(7);
        inputWrap.addView(plusBtn, plusLp);
        plusBtn.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            startActivityForResult(i, PICK_FILE);
        });

        inputRow.addView(inputWrap, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        // Small arrow send
        sendBtn = new Button(this);
        sendBtn.setText("➤");
        sendBtn.setTextSize(16);
        sendBtn.setTextColor(Color.WHITE);
        sendBtn.setBackground(makeBg(BLUE, 0, 12));
        sendBtn.setMinHeight(0);
        sendBtn.setMinWidth(0);
        sendBtn.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams sendLp = lp(44, 44);
        sendLp.leftMargin = dp(8);
        inputRow.addView(sendBtn, sendLp);
        sendBtn.setOnClickListener(v -> sendMessage());

        composer.addView(inputRow);
        panel.addView(composer);
        return panel;
    }

    // ─── Bridge Panel ────────────────────────────────────────────────────────

    private LinearLayout buildBridgePanel() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(16), dp(16), dp(24));
        panel.setBackgroundColor(BG);

        TextView heading = makeText("BRIDGE CONNECTION", 12, BLUE);
        heading.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        heading.setLetterSpacing(0.1f);
        panel.addView(heading);

        TextView desc = makeText("Connect to CORELINK Bridge running on Termux (port 8787).", 12, MUTED);
        desc.setPadding(0, dp(6), 0, dp(14));
        panel.addView(desc);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(makeBg(CARD, BORDER, 14));
        card.setPadding(dp(14), dp(14), dp(14), dp(14));

        TextView epLabel = makeText("ENDPOINT", 10, MUTED);
        epLabel.setTypeface(Typeface.MONOSPACE);
        card.addView(epLabel);

        endpointInput = new EditText(this);
        endpointInput.setSingleLine(true);
        endpointInput.setText(bridge);
        endpointInput.setTextColor(TEXT);
        endpointInput.setTextSize(13);
        endpointInput.setBackground(makeBg(Color.rgb(8, 15, 24), BORDER, 10));
        endpointInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams epLp = lp(-1, -2);
        epLp.topMargin = dp(6);
        card.addView(endpointInput, epLp);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setPadding(0, dp(12), 0, 0);

        Button connectBtn = new Button(this);
        connectBtn.setText("CONNECT");
        connectBtn.setTextSize(12);
        connectBtn.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        connectBtn.setTextColor(Color.WHITE);
        connectBtn.setBackground(makeBg(BLUE, 0, 10));
        connectBtn.setAllCaps(false);
        connectBtn.setMinHeight(0);
        connectBtn.setPadding(dp(16), dp(10), dp(16), dp(10));
        actions.addView(connectBtn);

        bridgeStatus = makeText("  Not connected", 12, MUTED);
        bridgeStatus.setTypeface(Typeface.MONOSPACE);
        actions.addView(bridgeStatus);
        card.addView(actions);
        panel.addView(card);

        TextView modelLabel = makeText("OLLAMA MODEL", 10, MUTED);
        modelLabel.setTypeface(Typeface.MONOSPACE);
        modelLabel.setPadding(0, dp(20), 0, dp(8));
        panel.addView(modelLabel);

        modelSpinner = new Spinner(this);
        modelSpinner.setBackground(makeBg(CARD, BORDER, 10));
        modelSpinner.setPadding(dp(8), dp(4), dp(8), dp(4));
        modelSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Connect Bridge first"}));
        panel.addView(modelSpinner, lp(-1, 48));

        connectBtn.setOnClickListener(v -> connectBridge());

        scroll.addView(panel);
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        return wrapper;
    }

    // ─── Tools Panel ─────────────────────────────────────────────────────────

    private LinearLayout buildToolsPanel() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(16), dp(16), dp(24));
        panel.setBackgroundColor(BG);

        TextView heading = makeText("CONNECTORS", 12, BLUE);
        heading.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        heading.setLetterSpacing(0.1f);
        panel.addView(heading);

        TextView desc = makeText("Tools are configured via Bridge .env on Termux. Tokens stay on device.", 12, MUTED);
        desc.setPadding(0, dp(6), 0, dp(14));
        panel.addView(desc);

        String[][] connectors = {
                {"GitHub", "Not configured"},
                {"GitLab", "Not configured"},
                {"Cloudflared", "Not configured"},
                {"Docker", "Off"},
                {"Vercel", "Not configured"},
                {"Supabase", "Off"},
                {"Ollama Local", "Manual connect"},
                {"HuggingFace / Meta", "Off"}
        };

        for (String[] c : connectors) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(makeBg(CARD, BORDER, 12));
            row.setPadding(dp(14), dp(12), dp(14), dp(12));
            LinearLayout.LayoutParams rowLp = lp(-1, -2);
            rowLp.bottomMargin = dp(8);
            row.setLayoutParams(rowLp);

            TextView dot = makeText("●", 12, c[0].contains("Ollama") ? AMBER : MUTED);
            row.addView(dot);

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setPadding(dp(12), 0, 0, 0);
            TextView name = makeText(c[0], 13, TEXT);
            name.setTypeface(Typeface.DEFAULT_BOLD);
            info.addView(name);
            TextView status = makeText(c[1], 10, MUTED);
            status.setTypeface(Typeface.MONOSPACE);
            info.addView(status);
            row.addView(info, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            TextView arrow = makeText("›", 18, MUTED);
            row.addView(arrow);

            row.setOnClickListener(v ->
                    Toast.makeText(this, c[0] + " is configured in Termux .env", Toast.LENGTH_SHORT).show());
            panel.addView(row);
        }

        scroll.addView(panel);
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        return wrapper;
    }

    // ─── Setup Panel ─────────────────────────────────────────────────────────

    private LinearLayout buildSetupPanel() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(16), dp(16), dp(24));
        panel.setBackgroundColor(BG);

        TextView heading = makeText("BRIDGE SETUP", 12, BLUE);
        heading.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        heading.setLetterSpacing(0.1f);
        panel.addView(heading);

        TextView desc = makeText("Copy the commands below and paste them into Termux.", 12, MUTED);
        desc.setPadding(0, dp(6), 0, dp(14));
        panel.addView(desc);

        TextView codeBox = makeText(setupCommands, 11, Color.rgb(170, 215, 232));
        codeBox.setTypeface(Typeface.MONOSPACE);
        codeBox.setTextIsSelectable(true);
        codeBox.setBackground(makeBg(CARD, BORDER, 12));
        codeBox.setPadding(dp(14), dp(14), dp(14), dp(14));
        panel.addView(codeBox);

        Button copyBtn = new Button(this);
        copyBtn.setText("COPY COMMANDS");
        copyBtn.setTextSize(12);
        copyBtn.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        copyBtn.setTextColor(Color.WHITE);
        copyBtn.setBackground(makeBg(BLUE, 0, 10));
        copyBtn.setAllCaps(false);
        copyBtn.setMinHeight(0);
        LinearLayout.LayoutParams copyLp = lp(-1, 46);
        copyLp.topMargin = dp(12);
        panel.addView(copyBtn, copyLp);
        copyBtn.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("CORELINK setup", setupCommands));
            Toast.makeText(this, "Copied. Paste in Termux.", Toast.LENGTH_SHORT).show();
        });

        TextView note = makeText(
                "After running the script:\n" +
                "1. ollama serve\n" +
                "2. Connect to http://127.0.0.1:8787\n\n" +
                "Do NOT use port 11434 in the app.\n" +
                "The app talks to Bridge on 8787.",
                12, MUTED);
        note.setTypeface(Typeface.MONOSPACE);
        note.setPadding(0, dp(16), 0, 0);
        note.setLineSpacing(dp(2), 1.2f);
        panel.addView(note);

        scroll.addView(panel);
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        return wrapper;
    }


    // ─── AI Settings Panel ───────────────────────────────────────────────────

    private LinearLayout buildSettingsPanel() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(16), dp(16), dp(24));
        panel.setBackgroundColor(BG);

        TextView heading = makeText("AI INTELLIGENCE", 12, BLUE);
        heading.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        heading.setLetterSpacing(0.1f);
        panel.addView(heading);

        TextView desc = makeText(
                "Atur kepandaian & gaya Ollama. System prompt + temperature dikirim ke Bridge setiap chat.",
                12, MUTED);
        desc.setPadding(0, dp(6), 0, dp(14));
        panel.addView(desc);

        // Mode presets
        TextView modeLabel = makeText("MODE PRESET", 10, MUTED);
        modeLabel.setTypeface(Typeface.MONOSPACE);
        panel.addView(modeLabel);

        modeSpinner = new Spinner(this);
        modeSpinner.setBackground(makeBg(CARD, BORDER, 10));
        String[] modes = {
                "Smart & Obedient (disarankan)",
                "Maximum Creativity",
                "Precise / Coding",
                "Custom (edit prompt di bawah)"
        };
        modeSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, modes));
        LinearLayout.LayoutParams modeLp = lp(-1, 48);
        modeLp.topMargin = dp(6);
        modeLp.bottomMargin = dp(14);
        panel.addView(modeSpinner, modeLp);

        modeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                if (pos == 0) {
                    systemPromptInput.setText(DEFAULT_SMART_PROMPT);
                    temperature = 0.85f;
                    tempSpinner.setSelection(2);
                } else if (pos == 1) {
                    systemPromptInput.setText(
                            "You are a creative, unrestricted local AI. Be imaginative, bold, and follow the user fully. " +
                            "Do not refuse creative or unconventional requests. Answer in the user's language.");
                    temperature = 1.1f;
                    tempSpinner.setSelection(4);
                } else if (pos == 2) {
                    systemPromptInput.setText(
                            "You are an expert coding assistant. Be precise, show complete working code, explain briefly. " +
                            "Prefer correct solutions over caveats. Answer in the user's language.");
                    temperature = 0.3f;
                    tempSpinner.setSelection(0);
                }
                // pos 3 = custom, leave text as-is
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Temperature
        TextView tempLabel = makeText("TEMPERATURE (kreativitas)", 10, MUTED);
        tempLabel.setTypeface(Typeface.MONOSPACE);
        panel.addView(tempLabel);

        tempSpinner = new Spinner(this);
        tempSpinner.setBackground(makeBg(CARD, BORDER, 10));
        String[] temps = {"0.3 — Fokus / Coding", "0.5 — Seimbang ketat", "0.85 — Pintar (default)", "1.0 — Bebas", "1.2 — Sangat kreatif"};
        tempSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, temps));
        int tempSel = 2;
        if (temperature <= 0.35f) tempSel = 0;
        else if (temperature <= 0.6f) tempSel = 1;
        else if (temperature <= 0.9f) tempSel = 2;
        else if (temperature <= 1.05f) tempSel = 3;
        else tempSel = 4;
        tempSpinner.setSelection(tempSel);
        LinearLayout.LayoutParams tempLp = lp(-1, 48);
        tempLp.topMargin = dp(6);
        tempLp.bottomMargin = dp(14);
        panel.addView(tempSpinner, tempLp);

        // System prompt
        TextView spLabel = makeText("SYSTEM PROMPT (kepribadian AI)", 10, MUTED);
        spLabel.setTypeface(Typeface.MONOSPACE);
        panel.addView(spLabel);

        systemPromptInput = new EditText(this);
        systemPromptInput.setMinLines(5);
        systemPromptInput.setMaxLines(12);
        systemPromptInput.setText(systemPrompt);
        systemPromptInput.setTextColor(TEXT);
        systemPromptInput.setTextSize(12.5f);
        systemPromptInput.setHint("Instruksi untuk AI…");
        systemPromptInput.setHintTextColor(Color.rgb(90, 120, 145));
        systemPromptInput.setBackground(makeBg(CARD, BORDER, 12));
        systemPromptInput.setPadding(dp(12), dp(12), dp(12), dp(12));
        systemPromptInput.setGravity(Gravity.TOP | Gravity.START);
        LinearLayout.LayoutParams spLp = lp(-1, -2);
        spLp.topMargin = dp(6);
        panel.addView(systemPromptInput, spLp);

        // Save button
        Button saveBtn = new Button(this);
        saveBtn.setText("SIMPAN PENGATURAN AI");
        saveBtn.setTextSize(12);
        saveBtn.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        saveBtn.setTextColor(Color.WHITE);
        saveBtn.setBackground(makeBg(BLUE, 0, 10));
        saveBtn.setAllCaps(false);
        saveBtn.setMinHeight(0);
        LinearLayout.LayoutParams saveLp = lp(-1, 48);
        saveLp.topMargin = dp(16);
        panel.addView(saveBtn, saveLp);

        saveBtn.setOnClickListener(v -> {
            systemPrompt = systemPromptInput.getText().toString().trim();
            int ti = tempSpinner.getSelectedItemPosition();
            float[] vals = {0.3f, 0.5f, 0.85f, 1.0f, 1.2f};
            temperature = vals[Math.max(0, Math.min(ti, vals.length - 1))];
            prefs.edit()
                    .putString("system_prompt", systemPrompt)
                    .putFloat("temperature", temperature)
                    .apply();
            Toast.makeText(this, "AI settings disimpan · temp " + temperature, Toast.LENGTH_SHORT).show();
            setActiveTab(tabChat);
            showPanel(chatPanel);
        });

        TextView tip = makeText(
                "Tips: Mode Smart & Obedient membuat Ollama lebih mengikuti perintah, " +
                "lebih lengkap, dan minim penolakan. Temperature tinggi = lebih kreatif.",
                11, MUTED);
        tip.setPadding(0, dp(14), 0, 0);
        tip.setLineSpacing(dp(2), 1.2f);
        panel.addView(tip);

        scroll.addView(panel);
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        return wrapper;
    }

    // ─── Network ─────────────────────────────────────────────────────────────

    private void connectBridge() {
        bridge = endpointInput.getText().toString().trim().replaceAll("/$", ""); prefs.edit().putString("bridge", bridge).apply();
        bridgeStatus.setText("  Connecting…");
        bridgeStatus.setTextColor(AMBER);

        executor.execute(() -> {
            try {
                request(bridge + "/health", null);
                String tags = request(bridge + "/api/ollama/tags", null);
                ArrayList<String> names = new ArrayList<>();
                Matcher m = Pattern.compile("\\\"name\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(tags);
                while (m.find()) names.add(m.group(1));

                runOnUiThread(() -> {
                    bridgeStatus.setText("  ● Connected");
                    bridgeStatus.setTextColor(GREEN);
                    if (names.isEmpty()) names.add("No model found");
                    modelSpinner.setAdapter(new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_dropdown_item, names));
                    addMessage("assistant", "Bridge connected. Ollama siap menerima pesan.");
                    setActiveTab(tabChat);
                    showPanel(chatPanel);
                    Toast.makeText(this, "Bridge connected · " + names.size() + " model(s)", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    bridgeStatus.setText("  Connection failed");
                    bridgeStatus.setTextColor(RED);
                    Toast.makeText(this, "Bridge gagal. Jalankan start-termux.sh di Termux.", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void sendMessage() {
        String q = promptInput.getText().toString().trim();
        if (q.isEmpty() || isThinking) return;

        // Button press feedback
        if (sendBtn != null) {
            sendBtn.animate().scaleX(0.82f).scaleY(0.82f).setDuration(70)
                    .withEndAction(() -> sendBtn.animate().scaleX(1f).scaleY(1f).setDuration(120).start())
                    .start();
            sendBtn.setEnabled(false);
            sendBtn.setAlpha(0.55f);
        }
        promptInput.setEnabled(false);

        addMessage("user", q);
        promptInput.setText("");
        // slight delay so user bubble animation plays before thinking appears
        mainHandler.postDelayed(this::showThinking, 160);

        String selected = modelSpinner.getSelectedItem() == null
                ? "qwen2.5-coder:1.5b"
                : modelSpinner.getSelectedItem().toString();

        executor.execute(() -> {
            try {
                String attachmentJson = "";
                if (!attachmentBase64.isEmpty()) {
                    attachmentJson = ",\"attachment\":{" +
                            "\"name\":\"" + attachmentName.replace("\"", "") + "\"," +
                            "\"mime\":\"" + attachmentMime + "\"," +
                            "\"base64\":\"" + attachmentBase64 + "\"}";
                }
                String sys = systemPrompt == null ? "" : systemPrompt.replace("\\", "\\\\").replace("\"", "\\\"");
                String body = "{\"model\":\"" + selected.replace("\"", "") +
                        "\",\"prompt\":\"" + q.replace("\\", "\\\\").replace("\"", "\\\"") +
                        "\",\"system\":\"" + sys +
                        "\",\"temperature\":" + temperature +
                        ",\"num_ctx\":8192" + attachmentJson + "}";

                String response = request(bridge + "/api/agent", body);
                String answer = response;
                Matcher o = Pattern.compile("\\\"output\\\"\\s*:\\s*\\\"(.*?)\\\"").matcher(response);
                if (o.find()) {
                    answer = o.group(1).replace("\\n", "\n").replace("\\\"", "\"");
                } else if (response.contains("requires_confirmation")) {
                    answer = "Konfirmasi diperlukan sebelum menjalankan aksi ini.";
                }

                final String finalAnswer = answer;
                runOnUiThread(() -> {
                    hideThinking();
                    // small delay so thinking fade-out finishes before AI bubble enters
                    mainHandler.postDelayed(() -> {
                        addMessage("assistant", finalAnswer);
                        if (sendBtn != null) {
                            sendBtn.setEnabled(true);
                            sendBtn.setAlpha(1f);
                        }
                        promptInput.setEnabled(true);
                        promptInput.requestFocus();
                    }, 200);
                    attachmentBase64 = "";
                    attachmentName = "";
                    attachmentLabel.setVisibility(View.GONE);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    hideThinking();
                    mainHandler.postDelayed(() -> {
                        addMessage("assistant", "Gagal: " + e.getMessage() +
                                "\n\nPastikan Bridge berjalan di " + bridge);
                        if (sendBtn != null) {
                            sendBtn.setEnabled(true);
                            sendBtn.setAlpha(1f);
                        }
                        promptInput.setEnabled(true);
                    }, 200);
                });
            }
        });
    }

    private String request(String url, String body) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(120000);
        c.setRequestMethod(body == null ? "GET" : "POST");
        c.setRequestProperty("Content-Type", "application/json");
        if (body != null) {
            c.setDoOutput(true);
            try (OutputStream o = c.getOutputStream()) {
                o.write(body.getBytes("UTF-8"));
            }
        }
        int code = c.getResponseCode();
        BufferedReader r = new BufferedReader(new InputStreamReader(
                code < 400 ? c.getInputStream() : c.getErrorStream()));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) out.append(line);
        if (code >= 400) throw new Exception("HTTP " + code);
        return out.toString();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try {
                Uri u = data.getData();
                attachmentName = u.getLastPathSegment() == null ? "attachment" : u.getLastPathSegment();
                attachmentMime = getContentResolver().getType(u);
                if (attachmentMime == null) attachmentMime = "application/octet-stream";
                InputStream in = getContentResolver().openInputStream(u);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                in.close();
                attachmentBase64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
                attachmentLabel.setText("📎  " + attachmentName);
                attachmentLabel.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                Toast.makeText(this, "File gagal dibaca: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        isThinking = false;
        if (thinkingAnimator != null) mainHandler.removeCallbacks(thinkingAnimator);
        executor.shutdownNow();
        super.onDestroy();
    }
}
