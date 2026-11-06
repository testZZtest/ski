package app.buddy.assistant;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Fase G — Observabilidade (droidrun/mobilerun: Arize / Langfuse).
 *
 * Envia spans OTLP (OpenTelemetry) sobre HTTP para um coletor configurado
 * (Arize, Langfuse, ou qualquer backend OTLP-compatível). Cada span tem:
 * name, start/end timestamps, atributos (chat, tool, agent, duração, ok/erro)
 * e um evento opcional com o resultado.
 *
 * Desligado por padrão; o usuário ativa em Settings e informa o endpoint OTLP.
 * Sem dependências externas: usa apenas HttpURLConnection.
 */
final class Tracing {
    private static final String TAG = "BuddyTracing";
    private static final int MAX_SPANS = 200; // não deixa crescer sem limite na RAM do telefone

    private final Context c;
    private volatile boolean enabled;
    private volatile String endpoint; // ex. https://otel-collector.example/v1/traces
    private final List<Span> spans = new CopyOnWriteArrayList<>();

    Tracing(Context c) {
        this.c = c;
        this.enabled = Prefs.tracingEnabled(c);
        this.endpoint = Prefs.tracingEndpoint(c);
    }

    static JSONObject listPrefs() throws JSONException {
        return new JSONObject()
                .put("tracing_enabled", Prefs.tracingEnabled(null))
                .put("tracing_endpoint", Prefs.tracingEndpoint(null));
    }

    static void setPrefs(Context c, boolean enabled, String endpoint) {
        Prefs.setTracingEnabled(c, enabled);
        Prefs.setTracingEndpoint(c, endpoint == null ? "" : endpoint.trim());
    }

    boolean enabled() {
        return enabled;
    }

    String endpoint() {
        return endpoint;
    }

    void setEnabled(boolean on) {
        enabled = on;
    }

    void setEndpoint(String ep) {
        endpoint = ep == null ? "" : ep.trim();
    }

    /** Inicia um span e retorna seu id para ser fechado com end(). */
    String start(String name, String chat, String tool, String agent) {
        if (!enabled || endpoint.isEmpty()) return null;
        Span s = new Span(name, chat, tool, agent);
        spans.add(s);
        trim();
        return s.id;
    }

    /** Fecha um span, anexando um evento com o resultado e enviando ao coletor. */
    void end(String id, String result, boolean error) {
        if (!enabled || endpoint.isEmpty()) return;
        Span s = find(id);
        if (s == null) return;
        s.endMs = System.currentTimeMillis();
        s.result = result;
        s.error = error;
        send(s);
    }

    /** Falha rápida: encerra o span com erro sem aguardar envio síncrono. */
    void fail(String id, String reason) {
        end(id, reason, true);
    }

    private Span find(String id) {
        for (Span s : spans) if (s.id.equals(id)) return s;
        return null;
    }

    private void trim() {
        while (spans.size() > MAX_SPANS) spans.remove(0);
    }

    private void send(Span s) {
        if (endpoint.isEmpty()) return;
        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject()
                        .put("resourceSpans", new JSONArray().put(new JSONObject()
                                .put("resource", new JSONObject().put("attributes", new JSONArray().put(
                                        new JSONObject().put("key", "service.name").put("value",
                                                new JSONObject().put("stringValue", "buddy-phone")))))
                                .put("scopeSpans", new JSONArray().put(new JSONObject()
                                        .put("spans", new JSONArray().put(s.toJson()))))));
                URL url = new URL(endpoint);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json");
                con.setConnectTimeout(5000);
                con.setReadTimeout(5000);
                con.setDoOutput(true);
                byte[] b = payload.toString().getBytes(StandardCharsets.UTF_8);
                con.setRequestProperty("Content-Length", String.valueOf(b.length));
                try (OutputStream out = con.getOutputStream()) {
                    out.write(b);
                }
                int code = con.getResponseCode();
                if (code >= 400) Log.w(TAG, "tracing: collector returned " + code);
                con.disconnect();
            } catch (Exception e) {
                Log.w(TAG, "tracing: send failed", e);
            }
        }, "buddy-tracing").start();
    }

    /** Um span OTLP mínimo, serializado direto no formato do coletor. */
    private static class Span {
        final String id;
        final String name;
        final String chat;
        final String tool;
        final String agent;
        final long startMs;
        long endMs;
        String result;
        boolean error;

        Span(String name, String chat, String tool, String agent) {
            this.id = java.util.UUID.randomUUID().toString();
            this.name = name;
            this.chat = chat;
            this.tool = tool;
            this.agent = agent;
            this.startMs = System.currentTimeMillis();
        }

        JSONObject toJson() throws JSONException {
            JSONObject j = new JSONObject()
                    .put("traceId", "00000000000000000000000000000001")
                    .put("spanId", id.substring(0, 16))
                    .put("name", name)
                    .put("kind", 1) // internal
                    .put("startTimeUnixNano", String.valueOf(startMs * 1_000_000L))
                    .put("endTimeUnixNano", String.valueOf((endMs > 0 ? endMs : System.currentTimeMillis()) * 1_000_000L));
            JSONArray attrs = new JSONArray();
            if (chat != null && !chat.isEmpty())
                attrs.put(new JSONObject().put("key", "buddy.chat").put("value", new JSONObject().put("stringValue", chat)));
            if (tool != null && !tool.isEmpty())
                attrs.put(new JSONObject().put("key", "buddy.tool").put("value", new JSONObject().put("stringValue", tool)));
            if (agent != null && !agent.isEmpty())
                attrs.put(new JSONObject().put("key", "buddy.agent").put("value", new JSONObject().put("stringValue", agent)));
            attrs.put(new JSONObject().put("key", "buddy.error").put("value", new JSONObject().put("booleanValue", error)));
            if (result != null && !result.isEmpty())
                attrs.put(new JSONObject().put("key", "buddy.result").put("value", new JSONObject().put("stringValue", result.length() > 500 ? result.substring(0, 500) + "…" : result)));
            j.put("attributes", attrs);
            return j;
        }
    }
}
