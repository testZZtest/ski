package ski.agent;

import android.content.Context;
import android.content.SharedPreferences;

import java.security.SecureRandom;

final class Prefs {
    private Prefs() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("ski", Context.MODE_PRIVATE);
    }

    /** Shared secret for the local MCP server; generated once per install. */
    static synchronized String token(Context c) {
        String t = sp(c).getString("token", null);
        if (t == null) {
            byte[] b = new byte[24];
            new SecureRandom().nextBytes(b);
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            t = sb.toString();
            sp(c).edit().putString("token", t).apply();
        }
        return t;
    }

    static int[] bubblePos(Context c) {
        return new int[]{sp(c).getInt("bx", -1), sp(c).getInt("by", 400)};
    }

    /** Background mode: Ski's adb is paired with this phone's Wireless debugging (see Background). */
    static boolean backgroundPaired(Context c) {
        return sp(c).getBoolean("bg_paired", false);
    }

    static void setBackgroundPaired(Context c, boolean paired) {
        sp(c).edit().putBoolean("bg_paired", paired).apply();
    }

    /** The secret the background helper is started with and asks for; generated once per install. */
    static synchronized String backgroundToken(Context c) {
        String t = sp(c).getString("bg_token", null);
        if (t == null) {
            byte[] b = new byte[24];
            new SecureRandom().nextBytes(b);
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            t = sb.toString();
            sp(c).edit().putString("bg_token", t).apply();
        }
        return t;
    }

    /** New chats start in background mode (each chat has its own switch in Chat options). */
    static boolean backgroundDefault(Context c) {
        return sp(c).getBoolean("bg_default", false);
    }

    static void setBackgroundDefault(Context c, boolean on) {
        sp(c).edit().putBoolean("bg_default", on).apply();
    }

    /** Phone commands: agents in chats with Linux tools may run commands on the phone itself. Off unless the user turns it on. */
    static boolean backgroundShell(Context c) {
        return sp(c).getBoolean("bg_shell", false);
    }

    static void setBackgroundShell(Context c, boolean on) {
        sp(c).edit().putBoolean("bg_shell", on).apply();
    }

    /** An approval card shows each phone command first. On unless the user turns it off. */
    static boolean backgroundShellAsk(Context c) {
        return sp(c).getBoolean("bg_shell_ask", true);
    }

    static void setBackgroundShellAsk(Context c, boolean on) {
        sp(c).edit().putBoolean("bg_shell_ask", on).apply();
    }

    /** The port Wireless debugging had when Ski last reached it (tried first next time). */
    static int backgroundAdbPort(Context c) {
        return sp(c).getInt("bg_adb_port", 0);
    }

    static void setBackgroundAdbPort(Context c, int port) {
        sp(c).edit().putInt("bg_adb_port", port).apply();
    }

    /** Agents installed in Ski's Linux from the last helper ping ("claude,codex", "none"), or null if never answered. */
    static String helperStatus(Context c) {
        return sp(c).getString("helper", null);
    }

    static long helperCheckedAt(Context c) {
        return sp(c).getLong("helper_at", 0);
    }

    static void setHelperStatus(Context c, String status) {
        sp(c).edit().putString("helper", status).putLong("helper_at", System.currentTimeMillis()).apply();
    }

    /** Which agent answers the bubble: "claude" (default), "codex", "opencode", "agy" or "grok". */
    static String backend(Context c) {
        return sp(c).getString("backend", "claude");
    }

    static void setBackend(Context c, String backend) {
        sp(c).edit().putString("backend", backend).apply();
    }

    /**
     * Agents (any MCP client, e.g. Claude Code testing Ski) may tap and type in Ski's own screens, except the
     * bubble, approval cards, App permissions and the developer switches. Off by default.
     */
    static boolean agentsTapSki(Context c) {
        return sp(c).getBoolean("agents_tap_ski", false);
    }

    static void setAgentsTapSki(Context c, boolean on) {
        sp(c).edit().putBoolean("agents_tap_ski", on).apply();
    }

    /**
     * The first time an agent acts in an app the user has set no mode for, Ski asks which it gets: Trust or Hard
     * (see Tools.firstUse). On for a new install, off for one set up before that existed, until the user turns it on.
     */
    static boolean askNewApps(Context c) {
        return sp(c).getBoolean("ask_new_apps", false);
    }

    static void setAskNewApps(Context c, boolean on) {
        sp(c).edit().putBoolean("ask_new_apps", on).apply();
    }

    /** Bank mode: Ski's accessibility service is off for the phone's banks (see BankMode). */
    static boolean bankMode(Context c) {
        return sp(c).getBoolean("bank_mode", false);
    }

    static void setBankMode(Context c, boolean on) {
        sp(c).edit().putBoolean("bank_mode", on).apply();
    }

    /** In-app language choice: "sys" (follow the phone) or a locale tag like "pt", "zh-rCN". */
    static String appLanguage(Context c) {
        return sp(c).getString("app_language", "sys");
    }

    static void setAppLanguage(Context c, String tag) {
        sp(c).edit().putString("app_language", tag).apply();
    }

    /** Notify when a new Ski release is out (checked about once a day). */
    static boolean updateNotices(Context c) {
        return !Flavor.PLAY && sp(c).getBoolean("update_notices", true);
    }

    static void setUpdateNotices(Context c, boolean on) {
        sp(c).edit().putBoolean("update_notices", on).apply();
    }

    static long updateCheckedAt(Context c) {
        return sp(c).getLong("update_checked_at", 0);
    }

    static void setUpdateCheckedAt(Context c, long t) {
        sp(c).edit().putLong("update_checked_at", t).apply();
    }

    /** The last version an update notice was shown for (one notice per version). */
    static String updateNotified(Context c) {
        return sp(c).getString("update_notified", "");
    }

    static void setUpdateNotified(Context c, String v) {
        sp(c).edit().putString("update_notified", v).apply();
    }

    /** First-setup choice: apps the user hasn't set (and that aren't on Hard by default) start on Trust, else Hard. */
    static boolean autonomousDefault(Context c) {
        return sp(c).getBoolean("autonomous_default", false);
    }

    static void setAutonomousDefault(Context c, boolean on) {
        sp(c).edit().putBoolean("autonomous_default", on).apply();
    }

    /** Every app the user set a mode for: package → "trust", "hard" or "smart". */
    static java.util.Map<String, String> appSafetyAll(Context c) {
        java.util.Map<String, String> out = new java.util.TreeMap<>();
        for (java.util.Map.Entry<String, ?> e : sp(c).getAll().entrySet()) {
            if (e.getKey().startsWith("app_safety_") && e.getValue() instanceof String) {
                out.put(e.getKey().substring("app_safety_".length()), (String) e.getValue());
            }
        }
        return out;
    }

    /**
     * Linux tools in new chats (was developer mode, for every chat): what a new chat starts with (each chat then has
     * its own, Sessions.Session.dev), and whether the user's own sessions outside Ski's chats may install apps.
     */
    static boolean devMode(Context c) {
        return sp(c).getBoolean("dev", false);
    }

    static void setDevMode(Context c, boolean on) {
        sp(c).edit().putBoolean("dev", on).apply();
    }

    /** The user agreed to the accessibility disclosure (shown before Android's accessibility settings). */
    static boolean a11yConsent(Context c) {
        return sp(c).getBoolean("a11y_consent_v3", false);
    }

    static void setA11yConsent(Context c) {
        sp(c).edit().putBoolean("a11y_consent_v3", true).apply();
    }

    static int helperVersion(Context c) {
        return sp(c).getInt("helper_version", 1);
    }

    static void setHelperVersion(Context c, int v) {
        sp(c).edit().putInt("helper_version", v).apply();
    }

    /** Whether the floating bubble is shown. When off, it still appears for approval cards. */
    static boolean bubbleEnabled(Context c) {
        return sp(c).getBoolean("bubble", true);
    }

    static void setBubbleEnabled(Context c, boolean on) {
        sp(c).edit().putBoolean("bubble", on).apply();
    }

    /**
     * Floating button size: "small" (40dp face), "medium" (60dp, default) or "large" (80dp face).
     * The ring and box scale with it.
     */
    static String bubbleSize(Context c) {
        String s = sp(c).getString("bubble_size", "medium");
        return "small".equals(s) || "large".equals(s) ? s : "medium";
    }

    static void setBubbleSize(Context c, String size) {
        sp(c).edit().putString("bubble_size", size).apply();
    }

    /**
     * Whether the floating status text (the pill) next to the bubble is shown.
     */
    static boolean bubbleShowText(Context c) {
        return sp(c).getBoolean("bubble_show_text", true);
    }

    static void setBubbleShowText(Context c, boolean on) {
        sp(c).edit().putBoolean("bubble_show_text", on).apply();
    }

    /**
     * What a tap on the bubble does: "panel" (open the chat panel), "talk" (start the microphone when no task runs)
     * or "app" (open the chat in the app). Before this was a choice, a switch turned "talk" on.
     */
    static String tapAction(Context c) {
        String a = sp(c).getString("tap_action", null);
        return a != null ? a : sp(c).getBoolean("tap_to_talk", false) ? "talk" : "panel";
    }

    static void setTapAction(Context c, String action) {
        sp(c).edit().putString("tap_action", action).apply();
    }

    /** The chat panel: "full" (the screen, clear of the edge gestures) or "compact" (smaller, more room at the edges). */
    static String panelSize(Context c) {
        return sp(c).getString("panel_size", "full");
    }

    static void setPanelSize(Context c, String size) {
        sp(c).edit().putString("panel_size", size).apply();
    }

    /** Ski's face on the bubble and in the chats: "ski" (the drawn one) or "picture" (one the user picked). */
    static String face(Context c) {
        return sp(c).getString("face", "ski");
    }

    static void setFace(Context c, String face) {
        sp(c).edit().putString("face", face).apply();
    }

    /** Read replies aloud with text-to-speech. */
    static boolean speakReplies(Context c) {
        return sp(c).getBoolean("speak", false);
    }

    static void setSpeakReplies(Context c, boolean on) {
        sp(c).edit().putBoolean("speak", on).apply();
    }

    static boolean hadReply(Context c) {
        return sp(c).getBoolean("had_reply", false);
    }

    /** The "New: talk live with Codex" hint was shown (once, in a Codex chat; see CodexLive). */
    static boolean liveHintShown(Context c) {
        return sp(c).getBoolean("live_hint_shown", false);
    }

    static void setLiveHintShown(Context c) {
        sp(c).edit().putBoolean("live_hint_shown", true).apply();
    }

        static void setHadReply(Context c) {
        if (!hadReply(c)) sp(c).edit().putBoolean("had_reply", true).apply();
    }

    /** Appearance: "system", "light" or "dark". */
    static String themeMode(Context c) {
        return sp(c).getString("theme_mode", "system");
    }

    static void setThemeMode(Context c, String m) {
        sp(c).edit().putString("theme_mode", m).apply();
    }

    /** Accent colour (opaque ARGB). */
    static int accent(Context c) {
        if (android.os.Build.VERSION.SDK_INT >= 31 && accentFromWallpaper(c)) { // (the check here too: for lint)
            return c.getColor(android.R.color.system_accent1_500) | 0xFF000000;
        }
        return sp(c).getInt("accent", 0xFFD97757);
    }

    static void setAccent(Context c, int color) {
        sp(c).edit().putInt("accent", color | 0xFF000000).putBoolean("accent_wallpaper", false).apply();
    }

    /** The accent is the phone's own, taken from the wallpaper (Android 12+), and changes with it. */
    static boolean accentFromWallpaper(Context c) {
        return android.os.Build.VERSION.SDK_INT >= 31 && sp(c).getBoolean("accent_wallpaper", false);
    }

    static void setAccentFromWallpaper(Context c) {
        sp(c).edit().putBoolean("accent_wallpaper", true).apply();
    }

    /** The download server for Ski's Linux (see Mirrors): "" is Alpine's and npm's own. */
    static String mirror(Context c) {
        return sp(c).getString("mirror", "");
    }

    static void setMirror(Context c, String id) {
        sp(c).edit().putString("mirror", id).apply();
    }

    /** The user's own download server (Mirrors.CUSTOM): its Alpine address, or (npm) its npm registry; "" if none. */
    static String mirrorCustom(Context c, boolean npm) {
        return sp(c).getString(npm ? "mirror_custom_npm" : "mirror_custom", "");
    }

    static void setMirrorCustom(Context c, String alpine, String npm) {
        sp(c).edit().putString("mirror_custom", alpine).putString("mirror_custom_npm", npm).apply();
    }

    /** The registry line Mirrors last wrote into Ski's Linux (~/.npmrc), to take just that one out again. */
    static String mirrorNpm(Context c) {
        return sp(c).getString("mirror_npm", "");
    }

    static void setMirrorNpm(Context c, String line) {
        sp(c).edit().putString("mirror_npm", line).apply();
    }

    /** The DNS list Mirrors last wrote into Ski's Linux, to know it as its own later. */
    static String mirrorDns(Context c) {
        return sp(c).getString("mirror_dns", "");
    }

    static void setMirrorDns(Context c, String list) {
        sp(c).edit().putString("mirror_dns", list).apply();
    }

    /** Surface style: "warm", "neutral" or "black" (black applies in dark mode only). */
    static String surface(Context c) {
        return sp(c).getString("surface", "warm");
    }

    static void setSurface(Context c, String s) {
        sp(c).edit().putString("surface", s).apply();
    }

    /** Agents that are signed in, as reported by the helper ("claude,codex", "none"), or null. */
    static String signedAgents(Context c) {
        return sp(c).getString("signed", null);
    }

    static void setSignedAgents(Context c, String s) {
        sp(c).edit().putString("signed", s).apply();
    }

    /** Codex's model list from its cache in Ski's Linux, as JSON [[slug, name], …]. */
    static String codexModels(Context c) {
        return sp(c).getString("codex_models", "[]");
    }

    static void setCodexModels(Context c, String json) {
        sp(c).edit().putString("codex_models", json).apply();
    }

    /** OpenCode's model list (`opencode models`), as JSON [[id, name], …]. */
    static String opencodeModels(Context c) {
        return sp(c).getString("opencode_models", "[]");
    }

    static void setOpencodeModels(Context c, String json) {
        sp(c).edit().putString("opencode_models", json).apply();
    }

    /** Antigravity's model list (`agy models`), as JSON [[id, name], …]. */
    static String agyModels(Context c) {
        return sp(c).getString("agy_models", "[]");
    }

    static void setAgyModels(Context c, String json) {
        sp(c).edit().putString("agy_models", json).apply();
    }

    /** Grok's model list (`grok models`), as JSON [[id, name], …]. */
    static String grokModels(Context c) {
        return sp(c).getString("grok_models", "[]");
    }

    static void setGrokModels(Context c, String json) {
        sp(c).edit().putString("grok_models", json).apply();
    }

    /**
     * The first-run guide's "Local model" choice: OpenCode with a model server of the user's own. The guide then asks
     * for the server too, and the server becomes what OpenCode's chats use by default.
     */
    static boolean localSetup(Context c) {
        return sp(c).getBoolean("local_setup", false);
    }

    static void setLocalSetup(Context c, boolean on) {
        sp(c).edit().putBoolean("local_setup", on).apply();
    }

    /** First-run guide finished (or skipped). */
    static boolean onboarded(Context c) {
        return sp(c).getBoolean("onboarded", false);
    }

    static void setOnboarded(Context c, boolean on) {
        // a new install asks about each new app (see askNewApps); one set up before that existed keeps working as it did
        if (on && !onboarded(c) && !sp(c).contains("ask_new_apps")) setAskNewApps(c, true);
        sp(c).edit().putBoolean("onboarded", on).apply();
    }

    /** Numbering of onboard_step: 1 = Termux-era steps (1.x), 2 = since 2.0. */
    static int onboardVersion(Context c) {
        return sp(c).getInt("onboard_v", sp(c).contains("onboard_step") ? 1 : 2);
    }

    static void setOnboardVersion(Context c, int v) {
        sp(c).edit().putInt("onboard_v", v).apply();
    }

    static int onboardStep(Context c) {
        return sp(c).getInt("onboard_step", 0);
    }

    static void setOnboardStep(Context c, int step) {
        sp(c).edit().putInt("onboard_step", step).apply();
    }

    /** Which agents the user wants: "claude", "codex", "opencode", "agy", "grok" or "both" (Claude Code and Codex). */
    static String agents(Context c) {
        return sp(c).getString("agents", "claude");
    }

    static void setAgents(Context c, String a) {
        sp(c).edit().putString("agents", a).apply();
    }

    /** Unused since Ski has its own Linux (was: Termux setup watch). */
    static long setupWatchUntil(Context c) {
        return sp(c).getLong("watch_until", 0);
    }

    static void setSetupWatchUntil(Context c, long t) {
        sp(c).edit().putLong("watch_until", t).apply();
    }

    /** Last known state of an agent in Ski's Linux: NEED_INSTALL, SIGNED_OUT or SIGNED_IN (null = never checked). */
    static String agentState(Context c, String agent) {
        return sp(c).getString("agent_" + agent, null);
    }

    static void setAgentState(Context c, String agent, String state) {
        sp(c).edit().putString("agent_" + agent, state).apply();
    }

    /** Chats whose agent is working right now: one still here when Ski starts again was cut off (see ChatHub). */
    static java.util.Set<String> busyChats(Context c) {
        return new java.util.HashSet<>(sp(c).getStringSet("busy_chats", new java.util.HashSet<>()));
    }

    static void setBusy(Context c, String chat, boolean busy) {
        java.util.Set<String> s = busyChats(c);
        if (busy ? s.add(chat) : s.remove(chat)) sp(c).edit().putStringSet("busy_chats", s).commit();
    }

    /** The API key services (see Providers), as JSON, without their keys. */
    static String providers(Context c) {
        return sp(c).getString("providers", "[]");
    }

    static void setProviders(Context c, String json) {
        sp(c).edit().putString("providers", json).apply();
    }

    /** What a chat on Default uses with this agent: "" (the agent's own default, on its plan) or "@service/model". */
    static String agentDefault(Context c, String agent) {
        return sp(c).getString("agent_default_" + agent, "");
    }

    static void setAgentDefault(Context c, String agent, String ref) {
        sp(c).edit().putString("agent_default_" + agent, ref).apply();
    }

    /** Earlier builds' model list for an agent's API key: only cleared now (see Providers.migrate). */
    static void setKeyModels(Context c, String agent, String json) {
        sp(c).edit().putString("key_models_" + agent, json).apply();
    }

    /** The base URL of another service an agent's API key is for ("": Anthropic's or OpenAI's API). */
    static String apiBase(Context c, String agent) {
        return sp(c).getString("api_base_" + agent, "");
    }

    static void setApiBase(Context c, String agent, String base) {
        sp(c).edit().putString("api_base_" + agent, base).apply();
    }

    /** The agent is signed in with its plan (not only an API key): plan-only features (voice…) need it. */
    static boolean agentPlan(Context c, String agent) {
        return sp(c).getBoolean("agent_plan_" + agent, true);
    }

    static void setAgentPlan(Context c, String agent, boolean plan) {
        sp(c).edit().putBoolean("agent_plan_" + agent, plan).apply();
    }

    /** Keep a Claude Code session with Remote Control running inside Ski's Linux. */
    static boolean remoteSession(Context c) {
        return sp(c).getBoolean("remote_session", false);
    }

    static void setRemoteSession(Context c, boolean on) {
        sp(c).edit().putBoolean("remote_session", on).apply();
    }

    /** Its name in the Claude app. */
    static String remoteName(Context c) {
        return sp(c).getString("remote_name", "Ski");
    }

    /** Keep a Codex session with remote control running inside Ski's Linux, for the ChatGPT app (CodexRemote). */
    static boolean codexRemote(Context c) {
        return sp(c).getBoolean("codex_remote", false);
    }

    static void setCodexRemote(Context c, boolean on) {
        sp(c).edit().putBoolean("codex_remote", on).apply();
    }

    /** Keep an SSH server running in Ski's Linux, for Termux (SshServer). */
    static boolean sshServer(Context c) {
        return sp(c).getBoolean("ssh_server", false);
    }

    static void setSshServer(Context c, boolean on) {
        sp(c).edit().putBoolean("ssh_server", on).apply();
    }

    /**
     * Remote control: expose Ski's phone tools over HTTP on 127.0.0.1:8766 (McpHttpServer, Fase E).
     * Off by default; the user turns it on in Settings to control Ski from a desktop or another app.
     */
    static boolean remoteControl(Context c) {
        return !Flavor.PLAY && sp(c).getBoolean("remote_control", false);
    }

    static void setRemoteControl(Context c, boolean on) {
        sp(c).edit().putBoolean("remote_control", on).apply();
    }

    /**
     * The background browser (Browser): off until the user turns it on. Never in the Play build for now: it downloads
     * and runs a whole browser (Chromium from Alpine), which Play's policies on downloaded executable code may not allow.
     */
    static boolean browser(Context c) {
        return sp(c).getBoolean("browser", false);
    }

    static void setBrowser(Context c, boolean on) {
        sp(c).edit().putBoolean("browser", on).apply();
    }

    /** Terminal text size in sp (pinch to zoom). */
    static float termFont(Context c) {
        return sp(c).getFloat("term_font", 13f);
    }

    static void setTermFont(Context c, float sp) {
        sp(c).edit().putFloat("term_font", sp).apply();
    }

    /** Keep the screen on while the Terminal is open. */
    static boolean termAwake(Context c) {
        return sp(c).getBoolean("term_awake", false);
    }

    static void setTermAwake(Context c, boolean on) {
        sp(c).edit().putBoolean("term_awake", on).apply();
    }

    /** Voice input engine: "phone" (Android's recognizer), "claude" (Claude Code's dictation) or "chatgpt". */
    static String voiceEngine(Context c) {
        return sp(c).getString("voice_engine", "phone");
    }

    static void setVoiceEngine(Context c, String e) {
        sp(c).edit().putString("voice_engine", e).apply();
    }

    /** The user's choice for an app: "trust" or "hard" (hard limit), or null for the default (see Guard.mode). */
    static String appSafety(Context c, String pkg) {
        return sp(c).getString("app_safety_" + pkg, null);
    }

    static void setAppSafety(Context c, String pkg, String mode) {
        sp(c).edit().putString("app_safety_" + pkg, mode).apply();
    }

    /** Guard lists as JSON: "allow", "ask", "seen". */
    static String guardList(Context c, String list) {
        return sp(c).getString("guard_" + list, "[]");
    }

    static void setGuardList(Context c, String list, String json) {
        sp(c).edit().putString("guard_" + list, json).apply();
    }

    /** The built-in risky words (Send, Pay, Delete… in several languages) count as "always ask". */
    static boolean guardBuiltin(Context c) {
        return sp(c).getBoolean("guard_builtin", true);
    }

    static void setGuardBuiltin(Context c, boolean on) {
        sp(c).edit().putBoolean("guard_builtin", on).apply();
    }

    /** The smart reviewer's harness: "same" (as the chat's agent), "claude" or "codex". */
    static String reviewer(Context c) {
        return sp(c).getString("reviewer", "same");
    }

    static void setReviewer(Context c, String r) {
        sp(c).edit().putString("reviewer", r).apply();
    }

    /** The model the reviewer uses on a harness (claude or codex); "" for the quick default (Haiku, or a small Codex model). */
    static String reviewerModel(Context c, String harness) {
        return sp(c).getString("reviewer_model_" + harness, "");
    }

    static void setReviewerModel(Context c, String harness, String model) {
        sp(c).edit().putString("reviewer_model_" + harness, model == null ? "" : model.trim()).apply();
    }

    /** The user has opened the bubble's hold menu once (no more hint in the status pill). */
    static boolean menuHinted(Context c) {
        return sp(c).getBoolean("menu_hinted", false);
    }

    static void setMenuHinted(Context c) {
        sp(c).edit().putBoolean("menu_hinted", true).apply();
    }

    /** How many times the hold menu peeked out / the "hold the bubble" tip was shown (each stops after 3). */
    static int peekCount(Context c) {
        return sp(c).getInt("menu_peeks", 0);
    }

    static void addPeek(Context c) {
        sp(c).edit().putInt("menu_peeks", peekCount(c) + 1).apply();
    }

    static int tipCount(Context c) {
        return sp(c).getInt("menu_tips", 0);
    }

    static void addTip(Context c) {
        sp(c).edit().putInt("menu_tips", tipCount(c) + 1).apply();
    }

    /** Reasoning mode: before acting, the agent writes what it sees, what it plans, and why, then asks for OK.
     *  Off / on / always (on for risky actions too). Default: off.
     */
    static String reasoningMode(Context c) {
        return sp(c).getString("reasoning_mode", "off");
    }

    static void setReasoningMode(Context c, String mode) {
        sp(c).edit().putString("reasoning_mode", mode).apply();
    }

    /** Who reads replies aloud: "phone" (Android's voices) or "chatgpt" (ChatGPT's voice through Codex). */
    static String speechEngine(Context c) {
        return sp(c).getString("speech_engine", "phone");
    }

    static void setSpeechEngine(Context c, String e) {
        sp(c).edit().putString("speech_engine", e).apply();
    }

    /** Which ChatGPT voice reads replies. */
    static String speechVoice(Context c) {
        return sp(c).getString("speech_voice", "juniper");
    }

    static void setSpeechVoice(Context c, String v) {
        sp(c).edit().putString("speech_voice", v).apply();
    }

    /** The user's voice languages, comma-separated BCP-47 tags (first = default). */
    static String voiceLangs(Context c) {
        String d = java.util.Locale.getDefault().toLanguageTag();
        return sp(c).getString("voice_langs", d);
    }

    static void setVoiceLangs(Context c, String csv) {
        sp(c).edit().putString("voice_langs", csv).apply();
    }

    /**
     * Tracing enabled (Fase G — observabilidade): sends OTLP spans to the endpoint below.
     * Off by default; the user turns it on in Settings to profile agent runs.
     */
    static boolean tracingEnabled(Context c) {
        return sp(c).getBoolean("tracing_enabled", false);
    }

    static void setTracingEnabled(Context c, boolean on) {
        sp(c).edit().putBoolean("tracing_enabled", on).apply();
    }

    /** OTLP traces endpoint, e.g. https://collector.example/v1/traces. Empty = tracing off even if enabled. */
    static String tracingEndpoint(Context c) {
        return sp(c).getString("tracing_endpoint", "");
    }

    static void setTracingEndpoint(Context c, String ep) {
        sp(c).edit().putString("tracing_endpoint", ep == null ? "" : ep).apply();
    }

    /**
     * On-device model enabled (Fase F — IA on-device): run GGUF/LiteRT locally instead of
     * sending every prompt to a cloud agent. Off by default.
     */
    static boolean localModelEnabled(Context c) {
        return sp(c).getBoolean("local_model_enabled", false);
    }

    static void setLocalModelEnabled(Context c, boolean on) {
        sp(c).edit().putBoolean("local_model_enabled", on).apply();
    }

    /** Path of the local model file inside Ski's Linux, e.g. /root/.ski/models/llama-3.2-1b.Q4_K_M.gguf. */
    static String localModelPath(Context c) {
        return sp(c).getString("local_model_path", "");
    }

    static void setLocalModelPath(Context c, String path) {
        sp(c).edit().putString("local_model_path", path == null ? "" : path).apply();
    }

    /** CPU threads for the local model (0 = auto). */
    static int localModelThreads(Context c) {
        return sp(c).getInt("local_model_threads", 0);
    }

    static void setLocalModelThreads(Context c, int n) {
        sp(c).edit().putInt("local_model_threads", Math.max(0, n)).apply();
    }

    /** Context size for the local model (tokens). */
    static int localModelCtxSize(Context c) {
        return sp(c).getInt("local_model_ctx", 2048);
    }

    static void setLocalModelCtxSize(Context c, int n) {
        sp(c).edit().putInt("local_model_ctx", Math.max(128, n)).apply();
    }

    /**
     * Delay entre ações do agent (ms). Deixa o app menos apressado para apps lentos.
     * 0 = sem delay (máxima velocidade). Padrão: 300 ms.
     */
    static int actionDelay(Context c) {
        return sp(c).getInt("action_delay", 0);
    }

    static void setActionDelay(Context c, int ms) {
        sp(c).edit().putInt("action_delay", Math.max(0, Math.min(5000, ms))).apply();
    }

    /** Opacidade da bolha flutuante: 10..100 (100 = opaca). Padrão: 100. */
    static int bubbleOpacity(Context c) {
        return sp(c).getInt("bubble_opacity", 100);
    }

    static void setBubbleOpacity(Context c, int pct) {
        sp(c).edit().putInt("bubble_opacity", Math.max(10, Math.min(100, pct))).apply();
    }

    /** Auto-esconder a bolha após X segundos sem atividade (0 = nunca). Padrão: 0. */
    static int bubbleAutoHide(Context c) {
        return sp(c).getInt("bubble_autohide", 0);
    }

    static void setBubbleAutoHide(Context c, int seconds) {
        sp(c).edit().putInt("bubble_autohide", Math.max(0, seconds)).apply();
    }

    /** Mostrar o pill de status ao lado da bolha. Padrão: true. */
    static boolean bubbleShowStatusPill(Context c) {
        return sp(c).getBoolean("bubble_show_status_pill", true);
    }

    static void setBubbleShowStatusPill(Context c, boolean show) {
        sp(c).edit().putBoolean("bubble_show_status_pill", show).apply();
    }

    /** The language to listen for next (one of voiceLangs). */
    static String voiceLang(Context c) {
        String all = voiceLangs(c);
        String cur = sp(c).getString("voice_lang", null);
        if (cur != null && ("," + all + ",").contains("," + cur + ",")) return cur;
        return all.split(",")[0];
    }

    static void setVoiceLang(Context c, String tag) {
        sp(c).edit().putString("voice_lang", tag).apply();
    }

    static void saveBubblePos(Context c, int x, int y) {
        sp(c).edit().putInt("bx", x).putInt("by", y).apply();
    }

    /** Notification rules as JSON (see Rules). */
    static String rules(Context c) {
        return sp(c).getString("rules", "[]");
    }

    static void setRules(Context c, String json) {
        sp(c).edit().putString("rules", json).apply();
    }

    /** The rules screen's main switch: off pauses every rule. */
    static boolean rulesOn(Context c) {
        return sp(c).getBoolean("rules_on", true);
    }

    static void setRulesOn(Context c, boolean on) {
        sp(c).edit().putBoolean("rules_on", on).apply();
    }

    /** The user agreed to what notification access is for (Google Play's prominent disclosure). */
    static boolean noticeConsent(Context c) {
        return sp(c).getBoolean("notice_consent", false);
    }

    static void setNoticeConsent(Context c) {
        sp(c).edit().putBoolean("notice_consent", true).apply();
    }

    /** What the rules did lately, as JSON (see Triggers.log). */
    static String rulesLog(Context c) {
        return sp(c).getString("rules_log", "[]");
    }

    static void setRulesLog(Context c, String json) {
        sp(c).edit().putString("rules_log", json).apply();
    }

    /** The chat each rule conversation continues in, as JSON {key: chat id}. */
    static String ruleChats(Context c) {
        return sp(c).getString("rule_chats", "{}");
    }

    static void setRuleChats(Context c, String json) {
        sp(c).edit().putString("rule_chats", json).apply();
    }

    /** The rules' replies waiting for the user's OK, as JSON (see Waiting). */
    static String waiting(Context c) {
        return sp(c).getString("waiting", "[]");
    }

    static void setWaiting(Context c, String json) {
        sp(c).edit().putString("waiting", json).apply();
    }
}
