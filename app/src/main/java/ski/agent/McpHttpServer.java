package ski.agent;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Servidor MCP HTTP opcional (Fase E — controle remoto).
 * Expõe as tools do Ski pelo telefone na porta 8766, para que um cliente
 * externo (desktop, outro app, ou navegador) chame read_screen, tap, type_text, etc.
 * sem precisar do MCP stdio local em 127.0.0.1:8765.
 *
 * Ligado apenas quando o usuário ativa em Settings → Remote Control. Cada requisição
 * carrega "Authorization: Bearer <token>" (o mesmo token do MCP local).
 *
 * Não é o relay emparelhado do pony (WebSocket): é HTTP simples, request/response.
 * Para o modo "MCP host" do pony, o cliente conecta aqui e fala MCP sobre HTTP.
 */
final class McpHttpServer {
    static final int PORT = 8766;
    private static final String TAG = "BuddyMcpHttp";

    private final SkiService svc;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private volatile ServerSocket socket;
    private volatile boolean running;

    McpHttpServer(SkiService svc) {
        this.svc = svc;
    }

    /** Start listening if remote control is on. No-op when already running or off. */
    void startIfEnabled() {
        if (!Prefs.remoteControl(svc) || running) return;
        running = true;
        Thread t = new Thread(this::acceptLoop, "buddy-mcp-http");
        t.setDaemon(true);
        t.start();
        Log.d(TAG, "McpHttpServer listening on :" + PORT);
    }

    /** Whether the server is currently listening. */
    boolean isRunning() {
        return running;
    }

    /** Stop listening. Called from Settings when remote control is turned off, and from onDestroy. */
    void stop() {
        if (!running) return;
        running = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
        pool.shutdownNow();
        Log.d(TAG, "McpHttpServer stopped");
    }

    private void acceptLoop() {
        while (running) {
            try {
                ServerSocket ss = new ServerSocket();
                ss.setReuseAddress(true);
                ss.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), PORT));
                socket = ss;
                while (running) {
                    final Socket s = ss.accept();
                    pool.execute(() -> handle(s));
                }
            } catch (IOException e) {
                if (!running) return;
                Log.w(TAG, "server error, retrying", e);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    return;
                }
            }
        }
    }

    private void handle(Socket s) {
        try {
            s.setSoTimeout(30000);
            InputStream in = new BufferedInputStream(s.getInputStream());
            OutputStream out = s.getOutputStream();

            String requestLine = readLine(in);
            if (requestLine == null || requestLine.isEmpty()) return;
            String[] parts = requestLine.split(" ");
            if (parts.length < 2) return;
            String method = parts[0];
            String path = parts[1];
            int q = path.indexOf('?');
            if (q >= 0) path = path.substring(0, q);

            Map<String, String> headers = new HashMap<>();
            String line;
            while ((line = readLine(in)) != null && !line.isEmpty()) {
                int c = line.indexOf(':');
                if (c > 0) {
                    headers.put(line.substring(0, c).trim().toLowerCase(Locale.ROOT), line.substring(c + 1).trim());
                }
            }
            byte[] body = readBody(in, headers);

            String auth = headers.get("authorization");
            if (!authorized(auth)) {
                send(out, 401, "application/json", "{\"error\":\"unauthorized\"}");
                return;
            }

            if ("/mcp".equals(path) && "POST".equals(method)) {
                String text = new String(body, StandardCharsets.UTF_8).trim();
                if (text.startsWith("[")) {
                    JSONArray batch = new JSONArray(text);
                    JSONArray replies = new JSONArray();
                    for (int i = 0; i < batch.length(); i++) {
                        JSONObject r = rpc(batch.getJSONObject(i));
                        if (r != null) replies.put(r);
                    }
                    if (replies.length() == 0) send(out, 202, null, null);
                    else send(out, 200, "application/json", replies.toString());
                } else {
                    JSONObject r = rpc(new JSONObject(text));
                    if (r == null) send(out, 202, null, null);
                    else send(out, 200, "application/json", r.toString());
                }
            } else if ("/mcp".equals(path) && "DELETE".equals(method)) {
                send(out, 200, null, null);
            } else if ("/mcp".equals(path)) {
                send(out, 405, null, null);
            } else if ("/health".equals(path)) {
                send(out, 200, "application/json", "{\"ok\":true,\"port\":" + PORT + "}");
            } else if ("/tools".equals(path) && "GET".equals(method)) {
                // lightweight list of tool names, for a client that just wants to know what's available
                try {
                    JSONArray names = new JSONArray();
                    JSONArray t = Tools.listNames();
                    for (int i = 0; i < t.length(); i++) names.put(t.optJSONObject(i).optString("name"));
                    send(out, 200, "application/json", new JSONObject().put("tools", names).toString());
                } catch (JSONException e) {
                    send(out, 500, "application/json", "{\"error\":\"" + e.getMessage() + "\"}");
                }
            } else if ("/devtools".equals(path) && "GET".equals(method)) {
                // Fase G — DevTools: the current accessibility UI tree, for remote inspection
                try {
                    JSONObject tree = new JSONObject();
                    tree.put("windows", new JSONArray());
                    tree.put("scale", svc.getResources().getDisplayMetrics().density);
                    for (android.view.accessibility.AccessibilityWindowInfo w : svc.getWindows()) {
                        if (w.getType() != android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION
                                && w.getType() != android.view.accessibility.AccessibilityWindowInfo.TYPE_SYSTEM) continue;
                        android.view.accessibility.AccessibilityNodeInfo root = w.getRoot();
                        if (root == null) continue;
                        JSONObject win = new JSONObject();
                        win.put("id", w.getId());
                        win.put("type", w.getType());
                        win.put("title", w.getTitle() == null ? "" : w.getTitle().toString());
                        win.put("package", root.getPackageName() == null ? "" : root.getPackageName().toString());
                        win.put("active", w.isActive());
                        android.graphics.Rect b = new android.graphics.Rect();
                        w.getBoundsInScreen(b);
                        win.put("bounds", new JSONObject()
                                .put("left", b.left).put("top", b.top)
                                .put("right", b.right).put("bottom", b.bottom));
                        win.put("nodes", nodeTree(root));
                        tree.getJSONArray("windows").put(win);
                    }
                    send(out, 200, "application/json", tree.toString());
                } catch (Exception e) {
                    send(out, 500, "application/json", "{\"error\":\"" + e.getMessage() + "\"}");
                }
            } else {
                send(out, 404, null, null);
            }
        } catch (Exception e) {
            Log.w(TAG, "request failed", e);
        } finally {
            try {
                s.close();
            } catch (IOException ignored) {
            }
        }
    }

    /** JSON-RPC 2.0 over HTTP: initialize, ping, tools/list, tools/call. Delegates to the local Tools. */
    private JSONObject rpc(JSONObject req) throws JSONException {
        if (!req.has("id")) return null;
        Object id = req.get("id");
        String method = req.optString("method");
        JSONObject params = req.optJSONObject("params");
        if (params == null) params = new JSONObject();

        JSONObject res = new JSONObject().put("jsonrpc", "2.0").put("id", id);
        try {
            switch (method) {
                case "initialize": {
                    JSONObject result = new JSONObject()
                            .put("protocolVersion", params.optString("protocolVersion", "2025-06-18"))
                            .put("capabilities", new JSONObject().put("tools", new JSONObject()))
                            .put("serverInfo", new JSONObject().put("name", "buddy-phone-http").put("version", BuildInfo.VERSION))
                            .put("instructions", "Ski's phone tools over HTTP. Call tools/list, then tools/call.");
                    return res.put("result", result);
                }
                case "ping":
                    return res.put("result", new JSONObject());
                case "tools/list": {
                    JSONArray t = Tools.list();
                    JSONArray names = new JSONArray();
                    for (int i = 0; i < t.length(); i++) names.put(t.optJSONObject(i).optString("name"));
                    return res.put("result", new JSONObject().put("tools", names));
                }
                case "tools/call": {
                    String name = params.optString("name");
                    JSONObject args = params.optJSONObject("arguments");
                    if (args == null) args = new JSONObject();
                    // run through Ski's own Tools (same as the local MCP server), so approvals and guards apply
                    JSONObject result;
                    try {
                        result = new Tools(svc).call(name, args);
                    } catch (Exception e) {
                        result = new JSONObject()
                                .put("content", new JSONArray().put(new JSONObject()
                                        .put("type", "text").put("text", "Error: " + e.getMessage())))
                                .put("isError", true);
                    }
                    return res.put("result", result);
                }
                default:
                    return res.put("error", new JSONObject().put("code", -32601).put("message", "Method not found: " + method));
            }
        } catch (Exception e) {
            return res.put("error", new JSONObject().put("code", -32603).put("message", String.valueOf(e.getMessage())));
        }
    }

    private boolean authorized(String header) {
        if (header == null) return false;
        return java.security.MessageDigest.isEqual(("Bearer " + Prefs.token(svc)).getBytes(StandardCharsets.UTF_8),
                header.getBytes(StandardCharsets.UTF_8));
    }

    private static void send(OutputStream out, int code, String type, String body) throws IOException {
        byte[] b = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        StringBuilder h = new StringBuilder();
        h.append("HTTP/1.1 ").append(code).append(' ').append(reason(code)).append("\r\n");
        if (type != null) h.append("Content-Type: ").append(type).append("; charset=utf-8\r\n");
        h.append("Content-Length: ").append(b.length).append("\r\n");
        h.append("Connection: close\r\n\r\n");
        out.write(h.toString().getBytes(StandardCharsets.US_ASCII));
        out.write(b);
        out.flush();
    }

    private static String reason(int code) {
        switch (code) {
            case 200: return "OK";
            case 202: return "Accepted";
            case 204: return "No Content";
            case 401: return "Unauthorized";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            case 500: return "Internal Server Error";
            default: return "Status";
        }
    }

    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') break;
            if (b != '\r') buf.write(b);
            if (buf.size() > 65536) throw new IOException("line too long");
        }
        if (b == -1 && buf.size() == 0) return null;
        return buf.toString("UTF-8");
    }

    /** Converts one AccessibilityNodeInfo and its children into a DevTools-style JSON subtree. */
    private static JSONObject nodeTree(android.view.accessibility.AccessibilityNodeInfo n) throws org.json.JSONException {
        JSONObject j = new JSONObject();
        j.put("class", n.getClassName() == null ? "" : n.getClassName().toString());
        j.put("text", n.getText() == null ? "" : n.getText().toString());
        j.put("desc", n.getContentDescription() == null ? "" : n.getContentDescription().toString());
        j.put("clickable", n.isClickable());
        j.put("editable", n.isEditable());
        j.put("checked", n.isChecked());
        j.put("enabled", n.isEnabled());
        j.put("focused", n.isFocused());
        j.put("password", n.isPassword());
        android.graphics.Rect r = new android.graphics.Rect();
        n.getBoundsInScreen(r);
        j.put("bounds", new org.json.JSONObject()
                .put("left", r.left).put("top", r.top)
                .put("right", r.right).put("bottom", r.bottom));
        JSONArray children = new JSONArray();
        for (int i = 0; i < n.getChildCount(); i++) {
            android.view.accessibility.AccessibilityNodeInfo child = n.getChild(i);
            if (child != null) children.put(nodeTree(child));
        }
        j.put("children", children);
        return j;
    }

    private static byte[] readBody(InputStream in, Map<String, String> headers) throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        String cl = headers.get("content-length");
        if (cl != null) {
            int n = Integer.parseInt(cl.trim());
            byte[] buf = new byte[8192];
            while (n > 0) {
                int r = in.read(buf, 0, Math.min(buf.length, n));
                if (r == -1) break;
                body.write(buf, 0, r);
                n -= r;
            }
        }
        return body.toByteArray();
    }
}
