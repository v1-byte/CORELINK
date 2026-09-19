package com.v1byte.corelink;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.graphics.Color;
import android.view.MenuItem;
import android.widget.PopupMenu;
import android.graphics.Path;
import android.graphics.Paint;
import android.graphics.Canvas;
import android.graphics.Bitmap;
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
import java.io.IOException;
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
    // Light modern theme (screenshot style)
    private final int BG       = Color.rgb(244, 248, 252);
    private final int CARD     = Color.rgb(255, 255, 255);
    private final int SURFACE  = Color.rgb(255, 255, 255);
    private final int BORDER   = Color.rgb(210, 225, 238);
    private final int TEXT     = Color.rgb(28, 40, 55);
    private final int MUTED    = Color.rgb(110, 130, 150);
    private final int BLUE     = Color.rgb(0, 168, 232);
    private final int GREEN    = Color.rgb(16, 185, 129);
    private final int RED      = Color.rgb(239, 68, 68);
    private final int AMBER    = Color.rgb(245, 158, 11);
    private final int USER_BG  = Color.rgb(230, 248, 255);
    private final int AI_BG    = Color.rgb(255, 255, 255);
    private final int ACCENT   = Color.rgb(0, 180, 255);

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
    private Button tabChat, tabBridge, tabTools, tabSetup, tabSettings, tabRemote, sendBtn;
    private View thinkingView;
    private TextView thinkingDots;
    private LinearLayout settingsPanel, remotePanel;
    private EditText systemPromptInput;
    private Spinner tempSpinner, modeSpinner;
    private TextView aConnectTokenView, aConnectStatusView, processLogView, emptyHint;
    private SharedPreferences prefs;
    private String aConnectToken = "";
    private String remoteRole = ""; // user | admin
    private Runnable remotePollRunnable;
    private EditText remoteChatInput;
    private TextView remoteChatLog;
    private android.animation.ObjectAnimator sendGlowAnimator;
    private ImageView robotView;
    private ImageView robotGlowView;
    private LinearLayout robotWrap;
    private boolean robotThinking = false;
    private boolean robotBlinkClosed = false;
    private Runnable robotAnimRunnable;
    private int messageCount = 0;

    // State
    private static final String CLOUD_WORKER = "https://corelink-ai.corelink-ai.workers.dev";
    private String bridge = CLOUD_WORKER;
    private String systemPrompt = "";
    private float temperature = 0.85f;
    private String attachmentName = "", attachmentMime = "", attachmentBase64 = "";
    private boolean isThinking = false;
    private int thinkingDotState = 0;
    private Runnable thinkingAnimator;
    private static final int PICK_FILE = 401;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("corelink", MODE_PRIVATE);
        bridge = prefs.getString("bridge", CLOUD_WORKER);
        systemPrompt = prefs.getString("system_prompt", DEFAULT_SMART_PROMPT);
        temperature = prefs.getFloat("temperature", 0.85f);
        Window w = getWindow();
        w.setStatusBarColor(BG);
        w.setNavigationBarColor(BG);
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            w.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
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
        Button[] tabs = {tabChat, tabBridge, tabTools, tabSetup, tabSettings, tabRemote};
        for (Button t : tabs) {
            if (t == null) continue;
            boolean on = t == active;
            t.setBackground(makeBg(on ? BLUE : CARD, on ? BLUE : BORDER, 20));
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
        if (remotePanel != null)
            remotePanel.setVisibility(panel == remotePanel ? View.VISIBLE : View.GONE);
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
        avatar.setBackground(makeBg(isUser ? Color.rgb(200, 240, 255) : Color.rgb(230, 240, 250), 0, 8));
        LinearLayout.LayoutParams avLp = lp(36, 36);
        if (isUser) avLp.leftMargin = dp(8); else avLp.rightMargin = dp(8);
        avatar.setLayoutParams(avLp);

        // Bubble body
        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        int bgColor = isUser ? USER_BG : AI_BG;
        int stroke = isUser ? Color.rgb(160, 220, 245) : Color.rgb(210, 225, 238);
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
        messageCount++;
        updateRobotSizeForChat();
    }

    // ─── Thinking animation ──────────────────────────────────────────────────

    private void showThinking() {
        if (isThinking) return;
        isThinking = true;
        setRobotThinking(true);

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
        setRobotThinking(false);
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

        // 3-dot menu (screenshot style) — tabs hidden in popup
        tabChat = makeTab("CHAT", true);
        tabBridge = makeTab("LINK", false);
        tabTools = makeTab("TOOLS", false);
        tabSetup = makeTab("SETUP", false);
        tabSettings = makeTab("AI", false);
        tabRemote = makeTab("REMOTE", false);

        TextView menuBtn = makeText("⋮", 22, TEXT);
        menuBtn.setPadding(dp(12), dp(4), dp(8), dp(4));
        menuBtn.setOnClickListener(v -> showNavMenu(v));
        header.addView(menuBtn);
        rootLayout.addView(header);

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

        remotePanel = buildRemotePanel();
        remotePanel.setVisibility(View.GONE);
        content.addView(remotePanel, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        tabChat.setOnClickListener(v -> { setActiveTab(tabChat); showPanel(chatPanel); });
        tabBridge.setOnClickListener(v -> { setActiveTab(tabBridge); showPanel(bridgePanel); });
        tabTools.setOnClickListener(v -> { setActiveTab(tabTools); showPanel(toolsPanel); });
        tabSetup.setOnClickListener(v -> { setActiveTab(tabSetup); showPanel(setupPanel); });
        tabSettings.setOnClickListener(v -> { setActiveTab(tabSettings); showPanel(settingsPanel); });
        tabRemote.setOnClickListener(v -> { setActiveTab(tabRemote); showPanel(remotePanel); });

        setContentView(outer);
        // empty chat — no welcome bubble
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
        b.setBackground(makeBg(active ? BLUE : CARD, active ? BLUE : BORDER, 20));
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

        // Professional 3D mascot (from user asset) + motion
        robotWrap = new LinearLayout(this);
        robotWrap.setOrientation(LinearLayout.VERTICAL);
        robotWrap.setGravity(Gravity.CENTER_HORIZONTAL);
        robotWrap.setPadding(0, dp(6), 0, dp(2));
        // Brain static — only circuit lines glow (overlay)
        FrameLayout brainStage = new FrameLayout(this);
        robotView = new ImageView(this);
        robotView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        robotView.setAdjustViewBounds(true);
        robotGlowView = new ImageView(this);
        robotGlowView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        robotGlowView.setAdjustViewBounds(true);
        robotGlowView.setAlpha(0f);
        try {
            int idle = getResources().getIdentifier("ai_brain_idle", "drawable", getPackageName());
            int glow = getResources().getIdentifier("ai_brain_glow", "drawable", getPackageName());
            if (idle == 0) idle = getResources().getIdentifier("ai_brain_think", "drawable", getPackageName());
            if (glow == 0) glow = idle;
            if (idle != 0) robotView.setImageResource(idle);
            if (glow != 0) robotGlowView.setImageResource(glow);
        } catch (Exception ignored) {}
        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(dp(120), dp(120), Gravity.CENTER);
        brainStage.addView(robotView, flp);
        brainStage.addView(robotGlowView, flp);
        robotWrap.addView(brainStage, new LinearLayout.LayoutParams(dp(120), dp(120)));
        panel.addView(robotWrap);
        startRobotIdleAnim();

        chatScroll = new ScrollView(this);
        chatScroll.setFillViewport(true);
        chatScroll.setVerticalScrollBarEnabled(false);
        messagesContainer = new LinearLayout(this);
        messagesContainer.setOrientation(LinearLayout.VERTICAL);
        messagesContainer.setPadding(dp(14), dp(8), dp(14), dp(16));
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
        inputWrap.setBackground(makeBg(CARD, BORDER, 24));

        promptInput = new EditText(this);
        promptInput.setHint("Message CORELINK…");
        promptInput.setHintTextColor(MUTED);
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
        plusBtn.setBackground(makeBg(Color.rgb(235, 244, 252), 0, 12));
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
        sendBtn.setTextSize(20);
        sendBtn.setTypeface(Typeface.DEFAULT_BOLD);
        sendBtn.setTextColor(Color.WHITE);
        sendBtn.setBackground(makeBg(ACCENT, 0, 22));
        sendBtn.setMinHeight(0);
        sendBtn.setMinWidth(0);
        sendBtn.setPadding(0, 0, 0, 0);
        sendBtn.setElevation(dp(6));
        LinearLayout.LayoutParams sendLp = lp(48, 48);
        sendLp.leftMargin = dp(8);
        inputRow.addView(sendBtn, sendLp);
        sendBtn.setOnClickListener(v -> sendMessage());
        startSendGlow();

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

        TextView heading = makeText("CLOUD CONNECTION", 12, BLUE);
        heading.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        heading.setLetterSpacing(0.1f);
        panel.addView(heading);

        TextView desc = makeText("Terhubung otomatis ke Cloudflare Workers AI.", 12, MUTED);
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

        TextView modelLabel = makeText("CLOUD AI MODEL", 10, MUTED);
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

        TextView logTitle = makeText("PROSES LIVE", 10, MUTED);
        logTitle.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        logTitle.setPadding(0, dp(12), 0, dp(6));
        panel.addView(logTitle);

        processLogView = makeText("› Menunggu aksi…\n› Connect Bridge / refresh connector untuk melihat proses.", 11, TEXT);
        processLogView.setTypeface(Typeface.MONOSPACE);
        processLogView.setBackground(makeBg(CARD, BORDER, 12));
        processLogView.setPadding(dp(12), dp(12), dp(12), dp(12));
        processLogView.setLineSpacing(dp(3), 1.15f);
        panel.addView(processLogView);

        Button refreshConn = new Button(this);
        refreshConn.setText("REFRESH CONNECTORS");
        refreshConn.setTextSize(11);
        refreshConn.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        refreshConn.setAllCaps(false);
        refreshConn.setTextColor(Color.WHITE);
        refreshConn.setBackground(makeBg(BLUE, 0, 10));
        LinearLayout.LayoutParams rlp = lp(-1, 44);
        rlp.topMargin = dp(10);
        rlp.bottomMargin = dp(8);
        panel.addView(refreshConn, rlp);
        refreshConn.setOnClickListener(v -> refreshConnectors());

        TextView desc = makeText("Tools cloud terhubung melalui CORELINK Workers.", 12, MUTED);
        desc.setPadding(0, dp(6), 0, dp(14));
        panel.addView(desc);

        String[][] connectors = {
                {"GitHub", "Not configured"},
                {"GitLab", "Not configured"},
                {"Cloudflared", "Not configured"},
                {"Docker", "Off"},
                {"Vercel", "Not configured"},
                {"Supabase", "Off"},
                {"Cloudflare Workers AI", "Connected via cloud"},
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

            TextView dot = makeText("●", 12, c[0].contains("Cloudflare") ? GREEN : MUTED);
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
                    Toast.makeText(this, c[0] + " tersedia melalui cloud", Toast.LENGTH_SHORT).show());
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

    // ─── Cloud Setup Panel ────────────────────────────────────────────────────

    private LinearLayout buildSetupPanel() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(16), dp(16), dp(28));
        panel.setBackgroundColor(BG);

        TextView heading = makeText("CLOUD AI SETUP", 13, BLUE);
        heading.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        heading.setLetterSpacing(0.08f);
        panel.addView(heading);

        TextView desc = makeText("CORELINK sekarang berjalan melalui Cloudflare Workers AI. Tidak perlu Termux, Ollama, atau server lokal.", 12, MUTED);
        desc.setPadding(0, dp(8), 0, dp(18));
        desc.setLineSpacing(dp(2), 1.2f);
        panel.addView(desc);

        TextView endpoint = makeText("CLOUD ENDPOINT\n" + CLOUD_WORKER, 12, Color.rgb(170, 215, 232));
        endpoint.setTypeface(Typeface.MONOSPACE);
        endpoint.setBackground(makeBg(CARD, BORDER, 12));
        endpoint.setPadding(dp(12), dp(14), dp(12), dp(14));
        panel.addView(endpoint);

        TextView steps = makeText("CARA MENGGUNAKAN\n\n1. Buka tab LINK\n2. Pastikan status Connected\n3. Buka tab CHAT\n4. Kirim pesan\n\nSemua proses AI berjalan di cloud. Koneksi internet diperlukan.", 12, TEXT);
        steps.setPadding(0, dp(20), 0, 0);
        steps.setLineSpacing(dp(3), 1.2f);
        panel.addView(steps);

        scroll.addView(panel);
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        return wrapper;
    }


    // ─── Remote panel: MyBase + A-Connect (UI list first, logic later) ───────

    private LinearLayout buildRemotePanel() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(14), dp(14), dp(14), dp(28));
        panel.setBackgroundColor(BG);

        TextView heading = makeText("REMOTE · IZIN 2 PIHAK", 12, BLUE);
        heading.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        heading.setLetterSpacing(0.06f);
        panel.addView(heading);

        TextView sub = makeText(
                "Alur perbaikan HP / medsos (support):\n" +
                "1. ADMIN share link APK User (Assist)\n" +
                "2. USER install APK\n" +
                "3. USER generate TOKEN → kirim ke admin\n" +
                "4. ADMIN tempel token → USER setuju popup\n" +
                "5. Sesi support jalan (engine bertahap)\n\n" +
                "Tetap di navbar REMOTE. Bukan remote diam-diam.",
                12, TEXT);
        sub.setPadding(0, dp(6), 0, dp(12));
        sub.setLineSpacing(dp(2), 1.2f);
        panel.addView(sub);

        // ── Card 1: MyBase ──
        panel.addView(sectionTitle("①  MYBASE", "Device shield & analysis"));
        LinearLayout myBase = card();
        myBase.addView(bodyText(
                "Modul analisis di perangkat ini (bukan spionase app lain tanpa izin).\n\n" +
                "Rencana fungsi:\n" +
                "• Scan ancaman lokal (permission mencurigakan, APK tidak dikenal)\n" +
                "• Ringkasan kesehatan perangkat\n" +
                "• Deteksi konfigurasi berisiko\n" +
                "• Laporan aman yang bisa dibaca user / admin support\n\n" +
                "Status: daftar fitur — engine belum aktif."));
        LinearLayout mbRow = new LinearLayout(this);
        mbRow.setOrientation(LinearLayout.HORIZONTAL);
        mbRow.setPadding(0, dp(10), 0, 0);
        Button mbScan = smallBtn("SCAN (soon)", Color.rgb(22, 64, 88));
        Button mbReport = smallBtn("LAPORAN (soon)", Color.rgb(22, 64, 88));
        mbRow.addView(mbScan, rowBtnLp());
        mbRow.addView(mbReport, rowBtnLp());
        myBase.addView(mbRow);
        mbScan.setOnClickListener(v ->
                Toast.makeText(this, "MyBase scan engine akan ditambahkan bertahap.", Toast.LENGTH_SHORT).show());
        mbReport.setOnClickListener(v ->
                Toast.makeText(this, "Laporan MyBase belum tersedia.", Toast.LENGTH_SHORT).show());
        panel.addView(myBase);

        // ── Card Assist (user APK share) ──
        panel.addView(sectionTitle("1. ADMIN → SHARE APK USER", "CoreLink Assist · link untuk pihak yang dibantu"));
        LinearLayout assist = card();
        assist.addView(bodyText(
                "Admin butuh bantu perbaikan HP / akun medsos?\n" +
                "Cukup BAGIKAN LINK APK ke user.\n\n" +
                "User: install → buka → Generate Token → kirim ke kamu.\n" +
                "Lalu di bawah (ADMIN Desk) tempel token.\n" +
                "User harus setuju popup sebelum sesi.\n" +
                "STOP kapan saja oleh kedua pihak."));
        LinearLayout asRow = new LinearLayout(this);
        asRow.setOrientation(LinearLayout.HORIZONTAL);
        asRow.setPadding(0, dp(10), 0, 0);
        Button shareApk = smallBtn("SHARE LINK APK USER", BLUE);
        Button copyTerms = smallBtn("SALIN SYARAT IZIN", Color.rgb(22, 64, 88));
        asRow.addView(shareApk, rowBtnLp());
        asRow.addView(copyTerms, rowBtnLp());
        assist.addView(asRow);
        shareApk.setOnClickListener(v -> {
            String link = "https://github.com/v1-byte/CORELINK/releases";
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_TEXT,
                    "Install CoreLink Assist (user remote support).\n" +
                    "Setelah install: buka app → REMOTE → Generate Token → kirim token ke admin.\n" +
                    "Download: " + link);
            startActivity(Intent.createChooser(send, "Bagikan CoreLink Assist"));
        });
        copyTerms.setOnClickListener(v -> {
            String terms =
                    "SYARAT IZIN 2 PIHAK\n" +
                    "1. USER buat token & kirim ke ADMIN.\n" +
                    "2. ADMIN minta sesi dengan token.\n" +
                    "3. USER tekan YA pada popup izinkan.\n" +
                    "4. Salah satu pihak bisa STOP sesi.\n" +
                    "5. Tidak ada remote tanpa kedua izin.";
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("syarat", terms));
            Toast.makeText(this, "Syarat izin 2 pihak disalin.", Toast.LENGTH_SHORT).show();
        });
        panel.addView(assist);

        // ── Card 2: CoreLink Desk (admin) ──
        panel.addView(sectionTitle("2. TOKEN & SESI", "User buat token · Admin hubungkan · izin 2 pihak"));
        LinearLayout aConn = card();
        aConn.addView(bodyText(
                "Remote SUPPORT — wajib izin Admin + User.\n\n" +
                "Nama modul admin: CoreLink Desk (Connector Android/PC)\n" +
                "APK user: CoreLink Assist (bisa di-share)\n\n" +
                "Alur izin 2 pihak:\n" +
                "1. USER install Assist → setuju syarat support\n" +
                "2. USER Generate Token + kirim ke ADMIN\n" +
                "3. ADMIN (CoreLink Desk) tempel token → minta sesi\n" +
                "4. USER konfirmasi POPUP Izinkan support? → YA\n" +
                "5. Sesi aktif; USER atau ADMIN bisa STOP kapan saja\n\n" +
                "Tidak ada remote tanpa token + konfirmasi user."));

        // Role buttons
        LinearLayout roleRow = new LinearLayout(this);
        roleRow.setOrientation(LinearLayout.HORIZONTAL);
        roleRow.setPadding(0, dp(10), 0, 0);
        Button asUser = smallBtn("USER (Assist)", BLUE);
        Button asAdmin = smallBtn("ADMIN (Desk)", Color.rgb(22, 90, 70));
        roleRow.addView(asUser, rowBtnLp());
        roleRow.addView(asAdmin, rowBtnLp());
        aConn.addView(roleRow);

        aConnectStatusView = makeText("Mode: belum dipilih", 11, MUTED);
        aConnectStatusView.setTypeface(Typeface.MONOSPACE);
        aConnectStatusView.setPadding(0, dp(10), 0, 0);
        aConn.addView(aConnectStatusView);

        aConnectTokenView = makeText("Token: —", 13, TEXT);
        aConnectTokenView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        aConnectTokenView.setPadding(0, dp(6), 0, 0);
        aConn.addView(aConnectTokenView);

        LinearLayout tokRow = new LinearLayout(this);
        tokRow.setOrientation(LinearLayout.HORIZONTAL);
        tokRow.setPadding(0, dp(10), 0, 0);
        Button genTok = smallBtn("GENERATE TOKEN", BLUE);
        Button copyTok = smallBtn("SALIN", Color.rgb(22, 64, 88));
        Button stopSes = smallBtn("STOP SESI", Color.rgb(90, 40, 40));
        tokRow.addView(genTok, rowBtnLp());
        tokRow.addView(copyTok, rowBtnLp());
        tokRow.addView(stopSes, rowBtnLp());

        aConn.addView(tokRow);

        Button acceptBtn = smallBtn("USER: IZINKAN SESI", GREEN);
        LinearLayout.LayoutParams accLp = lp(-1, 44);
        accLp.topMargin = dp(8);
        aConn.addView(acceptBtn, accLp);
        acceptBtn.setOnClickListener(v -> remoteAcceptSession());

        Button shareDev = smallBtn("USER: BAGIKAN INFO HP", Color.rgb(22, 64, 88));
        aConn.addView(shareDev, accLp);
        shareDev.setOnClickListener(v -> remoteShareDevice());

        remoteChatLog = makeText("Chat support muncul saat sesi AKTIF.", 11, MUTED);
        remoteChatLog.setTypeface(Typeface.MONOSPACE);
        remoteChatLog.setBackground(makeBg(CARD, BORDER, 10));
        remoteChatLog.setPadding(dp(10), dp(10), dp(10), dp(10));
        remoteChatLog.setMinHeight(dp(80));
        LinearLayout.LayoutParams clp = lp(-1, -2);
        clp.topMargin = dp(10);
        aConn.addView(remoteChatLog, clp);

        remoteChatInput = new EditText(this);
        remoteChatInput.setHint("Pesan support (sesi aktif)");
        remoteChatInput.setHintTextColor(MUTED);
        remoteChatInput.setTextColor(TEXT);
        remoteChatInput.setTextSize(13);
        remoteChatInput.setBackground(makeBg(CARD, BORDER, 10));
        remoteChatInput.setPadding(dp(10), dp(10), dp(10), dp(10));
        aConn.addView(remoteChatInput, clp);

        Button sendRemote = smallBtn("KIRIM PESAN SUPPORT", BLUE);
        aConn.addView(sendRemote, accLp);
        sendRemote.setOnClickListener(v -> remoteSendMessage());


        EditText adminTokenIn = new EditText(this);
        adminTokenIn.setHint("Admin: tempel token user di sini");
        adminTokenIn.setHintTextColor(Color.rgb(90, 120, 145));
        adminTokenIn.setTextColor(TEXT);
        adminTokenIn.setTextSize(12);
        adminTokenIn.setSingleLine(true);
        adminTokenIn.setBackground(makeBg(Color.rgb(8, 15, 24), BORDER, 10));
        adminTokenIn.setPadding(dp(12), dp(10), dp(12), dp(10));
        adminTokenIn.setVisibility(View.GONE);
        LinearLayout.LayoutParams atLp = lp(-1, -2);
        atLp.topMargin = dp(10);
        aConn.addView(adminTokenIn, atLp);

        Button adminConnect = smallBtn("HUBUNGKAN KE USER", Color.rgb(22, 90, 70));
        adminConnect.setVisibility(View.GONE);
        LinearLayout.LayoutParams acLp = lp(-1, 44);
        acLp.topMargin = dp(8);
        aConn.addView(adminConnect, acLp);

        asUser.setOnClickListener(v -> {
            remoteRole = "user"; aConnectStatusView.setText("Mode: USER · menunggu izin 2 pihak");
            aConnectStatusView.setTextColor(GREEN);
            adminTokenIn.setVisibility(View.GONE);
            adminConnect.setVisibility(View.GONE);
            genTok.setVisibility(View.VISIBLE);
        });
        asAdmin.setOnClickListener(v -> {
            remoteRole = "admin"; aConnectStatusView.setText("Mode: ADMIN CoreLink Desk · butuh token + izin user");
            aConnectStatusView.setTextColor(AMBER);
            adminTokenIn.setVisibility(View.VISIBLE);
            adminConnect.setVisibility(View.VISIBLE);
        });
        genTok.setOnClickListener(v -> remoteCreateToken());
        copyTok.setOnClickListener(v -> {
            if (aConnectToken == null || aConnectToken.isEmpty()) {
                aConnectToken = prefs.getString("aconnect_token", "");
            }
            if (aConnectToken.isEmpty()) {
                Toast.makeText(this, "Belum ada token. Generate dulu.", Toast.LENGTH_SHORT).show();
                return;
            }
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("A-Connect token", aConnectToken));
            Toast.makeText(this, "Token disalin.", Toast.LENGTH_SHORT).show();
        });
        stopSes.setOnClickListener(v -> remoteStopSession());
        adminConnect.setOnClickListener(v -> remoteAdminRequest(adminTokenIn.getText().toString().trim()));

        // restore token if any
        aConnectToken = prefs.getString("aconnect_token", "");
        if (aConnectToken != null && !aConnectToken.isEmpty()) {
            aConnectTokenView.setText("Token: " + aConnectToken);
        }

        panel.addView(aConn);

        TextView note = makeText(
                "Izin 2 pihak wajib: token USER + konfirmasi USER + aksi ADMIN. " +
                "CoreLink Desk = admin. CoreLink Assist = user. Engine remote bertahap.",
                10, MUTED);
        note.setPadding(0, dp(14), 0, 0);
        note.setLineSpacing(dp(2), 1.15f);
        panel.addView(note);

        scroll.addView(panel);
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        return wrapper;
    }

    private View sectionTitle(String title, String subtitle) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(10), 0, dp(4));
        TextView t = makeText(title, 11, BLUE);
        t.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        TextView s = makeText(subtitle, 10, MUTED);
        box.addView(t);
        box.addView(s);
        return box;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(makeBg(CARD, BORDER, 14));
        c.setPadding(dp(14), dp(12), dp(14), dp(14));
        LinearLayout.LayoutParams lp = lp(-1, -2);
        lp.bottomMargin = dp(12);
        c.setLayoutParams(lp);
        return c;
    }

    private TextView bodyText(String s) {
        TextView t = makeText(s, 12, TEXT);
        t.setLineSpacing(dp(2), 1.2f);
        return t;
    }

    private Button smallBtn(String label, int color) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(9);
        b.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackground(makeBg(color, 0, 8));
        b.setMinHeight(0);
        b.setMinWidth(0);
        b.setPadding(dp(6), dp(8), dp(6), dp(8));
        return b;
    }

    private LinearLayout.LayoutParams rowBtnLp() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(40), 1);
        p.setMargins(dp(2), 0, dp(2), 0);
        return p;
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
                "Atur gaya Cloud AI. System prompt + temperature dikirim ke Workers setiap chat.",
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
                "Tips: Mode Smart & Obedient membuat Cloud AI lebih mengikuti perintah, " +
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



    private void showNavMenu(View anchor) {
        PopupMenu pm = new PopupMenu(this, anchor);
        pm.getMenu().add(0, 1, 0, "CHAT");
        pm.getMenu().add(0, 2, 1, "LINK");
        pm.getMenu().add(0, 3, 2, "TOOLS");
        pm.getMenu().add(0, 4, 3, "SETUP");
        pm.getMenu().add(0, 5, 4, "AI");
        pm.getMenu().add(0, 6, 5, "REMOTE");
        pm.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) { setActiveTab(tabChat); showPanel(chatPanel); }
            else if (id == 2) { setActiveTab(tabBridge); showPanel(bridgePanel); }
            else if (id == 3) { setActiveTab(tabTools); showPanel(toolsPanel); }
            else if (id == 4) { setActiveTab(tabSetup); showPanel(setupPanel); }
            else if (id == 5) { setActiveTab(tabSettings); showPanel(settingsPanel); }
            else if (id == 6) { setActiveTab(tabRemote); showPanel(remotePanel); }
            return true;
        });
        pm.show();
    }

    private void updateRobotSizeForChat() {
        if (robotView == null || robotWrap == null) return;
        int s = messageCount > 0 ? dp(64) : dp(120);
        if (robotView.getParent() instanceof FrameLayout) {
            FrameLayout stage = (FrameLayout) robotView.getParent();
            LinearLayout.LayoutParams slp = (LinearLayout.LayoutParams) stage.getLayoutParams();
            if (slp != null) { slp.width = s; slp.height = s; stage.setLayoutParams(slp); }
            FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(s, s, Gravity.CENTER);
            robotView.setLayoutParams(flp);
            if (robotGlowView != null) robotGlowView.setLayoutParams(flp);
        }
        robotWrap.setPadding(0, messageCount > 0 ? dp(2) : dp(8), 0, dp(2));
    }

    private void setRobotThinking(boolean thinking) {
        robotThinking = thinking;
        if (robotView == null) return;
        robotView.animate().cancel();
        if (robotGlowView != null) robotGlowView.animate().cancel();
        // Brain stays STILL — only circuit-line glow changes
        robotView.setRotation(0f);
        robotView.setTranslationY(0f);
        robotView.setScaleX(1f);
        robotView.setScaleY(1f);
        robotView.setAlpha(1f);
        if (robotGlowView != null) {
            robotGlowView.setRotation(0f);
            robotGlowView.setTranslationY(0f);
            robotGlowView.setScaleX(1f);
            robotGlowView.setScaleY(1f);
        }
        if (thinking) {
            runThinkingMotion();
        } else if (robotGlowView != null) {
            robotGlowView.animate().alpha(0.15f).setDuration(400).start();
        }
    }

    /** Thinking: only lines glow (overlay alpha pulse), brain body does not move */
    private void runThinkingMotion() {
        if (robotGlowView == null || !robotThinking) return;
        robotGlowView.animate()
                .alpha(1f)
                .setDuration(480)
                .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    if (robotGlowView == null || !robotThinking) return;
                    robotGlowView.animate()
                            .alpha(0.25f)
                            .setDuration(480)
                            .withEndAction(this::runThinkingMotion)
                            .start();
                }).start();
    }

    private void startRobotIdleAnim() {
        if (robotAnimRunnable != null) mainHandler.removeCallbacks(robotAnimRunnable);
        // Idle: brain fully still; circuit lines soft glow pulse only
        robotAnimRunnable = new Runnable() {
            int tick = 0;
            @Override public void run() {
                if (robotGlowView == null) return;
                tick++;
                if (!robotThinking) {
                    float al = (tick % 2 == 0) ? 0.35f : 0.08f;
                    robotGlowView.animate().alpha(al).setDuration(1400)
                            .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                            .start();
                }
                mainHandler.postDelayed(this, 1500);
            }
        };
        mainHandler.post(robotAnimRunnable);
    }

    private void refreshRobotFrame() {
        // static brain + glow overlay — no frame swap needed
    }

    /** Simple mascot: idle arms down + blink; thinking hand on head */
    private Bitmap drawRobotBitmap(int size, boolean thinking, boolean blink) {
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float cx = size / 2f;
        float cy = size / 2f;
        float headR = size * 0.32f;

        // soft shadow
        p.setColor(Color.argb(30, 0, 120, 180));
        c.drawCircle(cx, cy + size * 0.08f, headR * 1.15f, p);

        // body
        p.setColor(Color.rgb(0, 170, 230));
        c.drawRoundRect(cx - headR * 0.7f, cy + headR * 0.35f, cx + headR * 0.7f, cy + headR * 1.35f, headR * 0.3f, headR * 0.3f, p);

        // head
        p.setColor(Color.rgb(245, 250, 255));
        c.drawCircle(cx, cy - headR * 0.1f, headR, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(size * 0.02f);
        p.setColor(Color.rgb(0, 170, 230));
        c.drawCircle(cx, cy - headR * 0.1f, headR, p);
        p.setStyle(Paint.Style.FILL);

        // antenna
        p.setColor(Color.rgb(0, 170, 230));
        c.drawRect(cx - size * 0.02f, cy - headR * 1.35f, cx + size * 0.02f, cy - headR * 0.9f, p);
        c.drawCircle(cx, cy - headR * 1.4f, size * 0.045f, p);

        // face plate
        p.setColor(Color.rgb(20, 35, 55));
        c.drawRoundRect(cx - headR * 0.55f, cy - headR * 0.35f, cx + headR * 0.55f, cy + headR * 0.25f, headR * 0.25f, headR * 0.25f, p);

        // eyes
        float eyeY = cy - headR * 0.08f;
        float eyeR = size * 0.045f;
        if (blink) {
            p.setColor(Color.rgb(0, 220, 200));
            p.setStrokeWidth(size * 0.025f);
            p.setStyle(Paint.Style.STROKE);
            c.drawLine(cx - headR * 0.28f - eyeR, eyeY, cx - headR * 0.28f + eyeR, eyeY, p);
            c.drawLine(cx + headR * 0.28f - eyeR, eyeY, cx + headR * 0.28f + eyeR, eyeY, p);
            p.setStyle(Paint.Style.FILL);
        } else {
            p.setColor(Color.rgb(0, 230, 210));
            c.drawCircle(cx - headR * 0.28f, eyeY, eyeR, p);
            c.drawCircle(cx + headR * 0.28f, eyeY, eyeR, p);
            p.setColor(Color.rgb(10, 20, 30));
            c.drawCircle(cx - headR * 0.28f, eyeY, eyeR * 0.45f, p);
            c.drawCircle(cx + headR * 0.28f, eyeY, eyeR * 0.45f, p);
        }

        // smile
        p.setColor(Color.rgb(0, 220, 200));
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(size * 0.02f);
        p.setStrokeCap(Paint.Cap.ROUND);
        Path smile = new Path();
        smile.moveTo(cx - headR * 0.2f, cy + headR * 0.12f);
        smile.quadTo(cx, cy + headR * 0.22f, cx + headR * 0.2f, cy + headR * 0.12f);
        c.drawPath(smile, p);
        p.setStyle(Paint.Style.FILL);

        // arms
        p.setColor(Color.rgb(0, 170, 230));
        p.setStrokeWidth(size * 0.07f);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStyle(Paint.Style.STROKE);
        if (thinking) {
            // left arm up to head (thinking pose)
            c.drawLine(cx - headR * 0.75f, cy + headR * 0.55f, cx - headR * 0.85f, cy - headR * 0.35f, p);
            c.drawLine(cx - headR * 0.85f, cy - headR * 0.35f, cx - headR * 0.45f, cy - headR * 0.55f, p);
            // right arm normal
            c.drawLine(cx + headR * 0.75f, cy + headR * 0.55f, cx + headR * 1.05f, cy + headR * 1.0f, p);
            // hand near head
            p.setStyle(Paint.Style.FILL);
            c.drawCircle(cx - headR * 0.42f, cy - headR * 0.55f, size * 0.05f, p);
        } else {
            c.drawLine(cx - headR * 0.75f, cy + headR * 0.55f, cx - headR * 1.05f, cy + headR * 1.0f, p);
            c.drawLine(cx + headR * 0.75f, cy + headR * 0.55f, cx + headR * 1.05f, cy + headR * 1.0f, p);
            p.setStyle(Paint.Style.FILL);
            c.drawCircle(cx - headR * 1.05f, cy + headR * 1.0f, size * 0.045f, p);
            c.drawCircle(cx + headR * 1.05f, cy + headR * 1.0f, size * 0.045f, p);
        }

        // chest badge
        p.setColor(Color.rgb(0, 220, 180));
        c.drawCircle(cx, cy + headR * 0.75f, size * 0.06f, p);
        p.setColor(Color.WHITE);
        p.setStrokeWidth(size * 0.015f);
        p.setStyle(Paint.Style.STROKE);
        Path check = new Path();
        check.moveTo(cx - size * 0.03f, cy + headR * 0.75f);
        check.lineTo(cx - size * 0.005f, cy + headR * 0.75f + size * 0.025f);
        check.lineTo(cx + size * 0.035f, cy + headR * 0.75f - size * 0.025f);
        c.drawPath(check, p);

        return bmp;
    }

    private void startSendGlow() {
        if (sendBtn == null) return;
        if (sendGlowAnimator != null) sendGlowAnimator.cancel();
        sendGlowAnimator = android.animation.ObjectAnimator.ofFloat(sendBtn, "alpha", 1f, 0.75f, 1f);
        sendGlowAnimator.setDuration(1300);
        sendGlowAnimator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        sendGlowAnimator.start();
        pulseSendLoop();
    }

    private void pulseSendLoop() {
        if (sendBtn == null || !sendBtn.isAttachedToWindow()) return;
        sendBtn.animate().scaleX(1.1f).scaleY(1.1f).setDuration(600)
                .withEndAction(() -> {
                    if (sendBtn == null) return;
                    sendBtn.animate().scaleX(1f).scaleY(1f).setDuration(600)
                            .withEndAction(this::pulseSendLoop).start();
                }).start();
    }

    private void appendProcess(String line) {
        runOnUiThread(() -> {
            if (processLogView == null) return;
            String prev = processLogView.getText() == null ? "" : processLogView.getText().toString();
            String next = (prev.isEmpty() ? "" : prev + "\n") + "› " + line;
            String[] lines = next.split("\n");
            if (lines.length > 28) {
                StringBuilder sb = new StringBuilder();
                for (int i = lines.length - 28; i < lines.length; i++) {
                    if (sb.length() > 0) sb.append('\n');
                    sb.append(lines[i]);
                }
                next = sb.toString();
            }
            processLogView.setText(next);
            processLogView.setVisibility(View.VISIBLE);
        });
    }



    // ─── Remote support API (Bridge, two-party) ─────────────────────────────

    private void remoteCreateToken() {
        remoteRole = "user";
        appendProcess("Remote: create token…");
        executor.execute(() -> {
            try {
                String body = "{\"note\":\"user support token\",\"device\":{\"model\":\"" +
                        android.os.Build.MODEL.replace("\"", "") + "\",\"android\":\"" +
                        android.os.Build.VERSION.RELEASE + "\"}}";
                String res = request(bridge + "/api/remote/create", body);
                String tok = extractJsonString(res, "token");
                if (tok.isEmpty()) tok = extractNestedToken(res);
                final String token = tok;
                runOnUiThread(() -> {
                    if (token.isEmpty()) {
                        Toast.makeText(this, "Gagal buat token. Bridge jalan?", Toast.LENGTH_LONG).show();
                        appendProcess("Remote create gagal: " + res);
                        return;
                    }
                    aConnectToken = token;
                    prefs.edit().putString("aconnect_token", token).apply();
                    aConnectTokenView.setText("Token: " + token);
                    aConnectStatusView.setText("USER · token siap · kirim ke admin");
                    aConnectStatusView.setTextColor(GREEN);
                    appendProcess("Token: " + token);
                    Toast.makeText(this, "Token dibuat via Bridge. Kirim ke admin.", Toast.LENGTH_LONG).show();
                    startRemotePoll();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Bridge error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    appendProcess("Remote error: " + e.getMessage());
                });
            }
        });
    }

    private void remoteAdminRequest(String tokenIn) {
        remoteRole = "admin";
        if (tokenIn == null || tokenIn.trim().isEmpty()) {
            Toast.makeText(this, "Tempel token user dulu.", Toast.LENGTH_SHORT).show();
            return;
        }
        final String token = tokenIn.trim().toUpperCase();
        aConnectToken = token;
        appendProcess("Admin request sesi: " + token);
        executor.execute(() -> {
            try {
                String body = "{\"token\":\"" + token.replace("\"", "") + "\",\"note\":\"support perbaikan\"}";
                String res = request(bridge + "/api/remote/request", body);
                runOnUiThread(() -> {
                    aConnectTokenView.setText("Token: " + token);
                    aConnectStatusView.setText("ADMIN · menunggu USER izinkan sesi");
                    aConnectStatusView.setTextColor(AMBER);
                    appendProcess("Request terkirim");
                    Toast.makeText(this, "Menunggu user menekan IZINKAN SESI", Toast.LENGTH_LONG).show();
                    startRemotePoll();
                    applyRemoteSessionJson(res);
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Gagal: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void remoteAcceptSession() {
        if (aConnectToken == null || aConnectToken.isEmpty()) {
            aConnectToken = prefs.getString("aconnect_token", "");
        }
        if (aConnectToken.isEmpty()) {
            Toast.makeText(this, "Belum ada token.", Toast.LENGTH_SHORT).show();
            return;
        }
        new android.app.AlertDialog.Builder(this)
                .setTitle("Izinkan support remote?")
                .setMessage("Admin meminta sesi support.\n\nHanya lanjut jika kamu percaya admin ini.\nKamu bisa STOP kapan saja.")
                .setPositiveButton("YA, IZINKAN", (d, w) -> {
                    appendProcess("User mengizinkan sesi");
                    executor.execute(() -> {
                        try {
                            String body = "{\"token\":\"" + aConnectToken.replace("\"", "") +
                                    "\",\"device\":{\"model\":\"" + android.os.Build.MODEL.replace("\"", "") +
                                    "\",\"android\":\"" + android.os.Build.VERSION.RELEASE + "\"}}";
                            String res = request(bridge + "/api/remote/accept", body);
                            runOnUiThread(() -> {
                                applyRemoteSessionJson(res);
                                Toast.makeText(this, "Sesi AKTIF", Toast.LENGTH_SHORT).show();
                                startRemotePoll();
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    });
                })
                .setNegativeButton("TIDAK", null)
                .show();
    }

    private void remoteStopSession() {
        if (aConnectToken == null || aConnectToken.isEmpty()) {
            aConnectToken = prefs.getString("aconnect_token", "");
        }
        stopRemotePoll();
        if (aConnectToken.isEmpty()) {
            aConnectTokenView.setText("Token: —");
            aConnectStatusView.setText("Tidak ada sesi");
            return;
        }
        final String tok = aConnectToken;
        final String by = "admin".equals(remoteRole) ? "admin" : "user";
        executor.execute(() -> {
            try {
                request(bridge + "/api/remote/stop",
                        "{\"token\":\"" + tok.replace("\"", "") + "\",\"by\":\"" + by + "\"}");
            } catch (Exception ignored) {}
            runOnUiThread(() -> {
                aConnectToken = "";
                prefs.edit().remove("aconnect_token").apply();
                aConnectTokenView.setText("Token: —");
                aConnectStatusView.setText("Sesi dihentikan");
                aConnectStatusView.setTextColor(MUTED);
                if (remoteChatLog != null) remoteChatLog.setText("Sesi berhenti.");
                appendProcess("Sesi stop oleh " + by);
                Toast.makeText(this, "Sesi dihentikan", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void remoteShareDevice() {
        if (aConnectToken == null || aConnectToken.isEmpty()) {
            Toast.makeText(this, "Sesi/token belum ada.", Toast.LENGTH_SHORT).show();
            return;
        }
        executor.execute(() -> {
            try {
                String body = "{\"token\":\"" + aConnectToken.replace("\"", "") +
                        "\",\"model\":\"" + android.os.Build.MODEL.replace("\"", "") +
                        "\",\"android\":\"" + android.os.Build.VERSION.RELEASE +
                        "\",\"app\":\"CORELINK\"}";
                String res = request(bridge + "/api/remote/device", body);
                runOnUiThread(() -> {
                    applyRemoteSessionJson(res);
                    Toast.makeText(this, "Info HP dibagikan ke sesi", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void remoteSendMessage() {
        if (remoteChatInput == null) return;
        String msg = remoteChatInput.getText().toString().trim();
        if (msg.isEmpty() || aConnectToken == null || aConnectToken.isEmpty()) {
            Toast.makeText(this, "Sesi aktif + isi pesan dulu.", Toast.LENGTH_SHORT).show();
            return;
        }
        final String from = "admin".equals(remoteRole) ? "admin" : "user";
        executor.execute(() -> {
            try {
                String body = "{\"token\":\"" + aConnectToken.replace("\"", "") +
                        "\",\"from\":\"" + from + "\",\"text\":\"" +
                        msg.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
                String res = request(bridge + "/api/remote/message", body);
                runOnUiThread(() -> {
                    remoteChatInput.setText("");
                    applyRemoteSessionJson(res);
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void startRemotePoll() {
        stopRemotePoll();
        remotePollRunnable = new Runnable() {
            @Override public void run() {
                if (aConnectToken == null || aConnectToken.isEmpty()) return;
                executor.execute(() -> {
                    try {
                        String res = request(bridge + "/api/remote/status/" +
                                java.net.URLEncoder.encode(aConnectToken, "UTF-8"), null);
                        runOnUiThread(() -> applyRemoteSessionJson(res));
                    } catch (Exception ignored) {}
                });
                mainHandler.postDelayed(this, 3000);
            }
        };
        mainHandler.postDelayed(remotePollRunnable, 1500);
    }

    private void stopRemotePoll() {
        if (remotePollRunnable != null) {
            mainHandler.removeCallbacks(remotePollRunnable);
            remotePollRunnable = null;
        }
    }

    private void applyRemoteSessionJson(String res) {
        if (res == null) return;
        String status = extractJsonString(res, "status");
        if (status.isEmpty()) {
            // try nested session.status
            int i = res.indexOf("\"status\"");
            if (i >= 0) {
                int q1 = res.indexOf('"', i + 8);
                int q2 = res.indexOf('"', q1 + 1);
                if (q1 >= 0 && q2 > q1) status = res.substring(q1 + 1, q2);
            }
        }
        if (!status.isEmpty() && aConnectStatusView != null) {
            aConnectStatusView.setText("Sesi: " + status);
            if ("active".equals(status)) aConnectStatusView.setTextColor(GREEN);
            else if ("stopped".equals(status)) aConnectStatusView.setTextColor(MUTED);
            else aConnectStatusView.setTextColor(AMBER);
        }
        // render messages simply
        if (remoteChatLog != null && res.contains("\"messages\"")) {
            StringBuilder sb = new StringBuilder();
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("\"from\"\\s*:\\s*\"([^\"]+)\".*?\"text\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
                    .matcher(res);
            while (m.find()) {
                sb.append(m.group(1)).append(": ").append(m.group(2).replace("\\\"", "\"")).append("\n");
            }
            if (sb.length() > 0) remoteChatLog.setText(sb.toString().trim());
        }
        // user side: if pending_user_confirm, optional toast once
        if ("pending_user_confirm".equals(status) && "user".equals(remoteRole)) {
            appendProcess("Admin meminta sesi — tekan IZINKAN SESI");
        }
    }

    private String extractJsonString(String json, String key) {
        if (json == null) return "";
        String pat = "\"" + key + "\"";
        int i = json.indexOf(pat);
        if (i < 0) return "";
        int colon = json.indexOf(':', i + pat.length());
        if (colon < 0) return "";
        int q1 = json.indexOf('"', colon + 1);
        if (q1 < 0) return "";
        int q2 = q1 + 1;
        while (q2 < json.length()) {
            char c = json.charAt(q2);
            if (c == '"' && json.charAt(q2 - 1) != '\\') break;
            q2++;
        }
        if (q2 >= json.length()) return "";
        return json.substring(q1 + 1, q2);
    }

    private String extractNestedToken(String res) {
        // "session":{"token":"AC-..."
        int i = res.indexOf("\"token\"");
        if (i < 0) return "";
        int q1 = res.indexOf('"', i + 7);
        int q2 = res.indexOf('"', q1 + 1);
        if (q1 < 0 || q2 < 0) return "";
        return res.substring(q1 + 1, q2);
    }

    private void refreshConnectors() {
        appendProcess("Refresh connectors…");
        appendProcess("GET /api/connectors");
        executor.execute(() -> {
            try {
                String json = request(bridge + "/api/connectors", null);
                appendProcess("Respons diterima (" + json.length() + " chars)");
                // parse simple online flags
                String lower = json.toLowerCase();
                if (lower.contains("workers-ai") || lower.contains("cloudflare")) appendProcess("Cloud AI: online");
                if (lower.contains("github")) appendProcess("GitHub token: " + (lower.contains("\"configured\":true") ? "terkonfigurasi / cek" : "belum"));
                if (lower.contains("cloudflared")) appendProcess("Cloudflared: dicek");
                if (lower.contains("docker")) appendProcess("Docker: dicek");
                appendProcess("Selesai refresh");
                runOnUiThread(() -> Toast.makeText(this, "Connector di-refresh", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                appendProcess("Error: " + e.getMessage());
            }
        });
    }

    private void connectBridge() {
        bridge = endpointInput.getText().toString().trim().replaceAll("/$", ""); prefs.edit().putString("bridge", bridge).apply();
        bridgeStatus.setText("  Connecting…");
        bridgeStatus.setTextColor(AMBER);
        appendProcess("Mulai koneksi Bridge → " + bridge);
        appendProcess("Cek /health …");

        executor.execute(() -> {
            try {
                request(bridge + "/health", null);
                appendProcess("Health OK");
                appendProcess("Ambil model Cloud AI …");
                String tags = request(bridge + "/api/ollama/tags", null);
                ArrayList<String> names = new ArrayList<>();
                Matcher m = Pattern.compile("\\\"name\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(tags);
                while (m.find()) names.add(m.group(1));
                names.sort((a, b) -> {
                    boolean aLight = a.equalsIgnoreCase("qwen2.5:0.5b") || a.matches("(?i).*0[.]5b.*");
                    boolean bLight = b.equalsIgnoreCase("qwen2.5:0.5b") || b.matches("(?i).*0[.]5b.*");
                    return Boolean.compare(!aLight, !bLight);
                });

                runOnUiThread(() -> {
                    bridgeStatus.setText("  ● Connected");
                    bridgeStatus.setTextColor(GREEN);
                    if (names.isEmpty()) names.add("No model found");
                    modelSpinner.setAdapter(new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_dropdown_item, names));
                    addMessage("assistant", "Cloud AI connected. Siap menerima pesan.");
                    setActiveTab(tabChat);
                    showPanel(chatPanel);
                    Toast.makeText(this, "Cloud AI connected", Toast.LENGTH_SHORT).show();
                    appendProcess("Terhubung · " + names.size() + " model: " + names);
                    appendProcess("Siap chat");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    bridgeStatus.setText("  Connection failed");
                    bridgeStatus.setTextColor(RED);
                    Toast.makeText(this, "Cloud AI belum terhubung. Periksa koneksi internet.", Toast.LENGTH_LONG).show();
                    appendProcess("GAGAL: " + (e.getMessage() == null ? "error" : e.getMessage()));
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
        appendProcess("Kirim pesan ke agent…");
        mainHandler.postDelayed(this::showThinking, 160);

        String selected = modelSpinner.getSelectedItem() == null
                ? "qwen2.5:0.5b"
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
                        ",\"num_ctx\":1024" + attachmentJson + "}";

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
        Exception last = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                return requestOnce(url, body);
            } catch (Exception e) {
                last = e;
                if (attempt < 2) Thread.sleep(350L * (attempt + 1));
            }
        }
        throw last == null ? new IOException("Koneksi cloud gagal") : last;
    }

    private String requestOnce(String url, String body) throws Exception {
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
        try {
            InputStream stream = code < 400 ? c.getInputStream() : c.getErrorStream();
            if (stream == null) throw new IOException("Bridge tidak mengirim respons");
            BufferedReader r = new BufferedReader(new InputStreamReader(stream));
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) out.append(line);
            if (code >= 400) throw new Exception("HTTP " + code);
            return out.toString();
        } finally {
            c.disconnect();
        }
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
        stopRemotePoll();
        isThinking = false;
        if (thinkingAnimator != null) mainHandler.removeCallbacks(thinkingAnimator);
        executor.shutdownNow();
        super.onDestroy();
    }
}
