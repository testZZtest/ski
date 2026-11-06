package app.buddy.assistant;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Fase F — IA on-device (Verdroid, TecnicoBot: modelos GGUF / LiteRT locais).
 *
 * Estrutura para rodar um modelo de linguagem dentro do telefone, sem nuvem.
 * O modelo fica num arquivo (.gguf via llama.cpp, ou .tflite via LiteRT) dentro
 * do Linux do Buddy (/root/.buddy/models/...). A inferência roda por shell com
 * o binário do modelo, passando o prompt via stdin e lendo a resposta pelo stdout.
 *
 * Sem dependências nativas no build: o usuário baixa o modelo e o binário de
 * inferência (ex. llama.cpp compilado) para dentro do Linux do Buddy, e aponta
 * o caminho em Settings → IA on-device.
 *
 * Desligado por padrão. Quando off, o agent continua usando seu plano na nuvem.
 */
final class OnDeviceModel {
    private static final String TAG = "BuddyOnDevice";
    private static final String WORK_ROOT = "/root/.buddy";

    private final Context c;
    private volatile boolean enabled;
    private volatile String modelPath;
    private volatile int threads;
    private volatile int ctxSize;

    OnDeviceModel(Context c) {
        this.c = c;
        this.enabled = Prefs.localModelEnabled(c);
        this.modelPath = Prefs.localModelPath(c);
        this.threads = Prefs.localModelThreads(c);
        this.ctxSize = Prefs.localModelCtxSize(c);
    }

    boolean enabled() {
        return enabled;
    }

    void setEnabled(boolean on) {
        enabled = on;
    }

    void setModelPath(String path) {
        modelPath = path;
    }

    void setThreads(int n) {
        threads = Math.max(0, n);
    }

    void setCtxSize(int n) {
        ctxSize = Math.max(128, n);
    }

    /**
     * Runs the local model on a prompt and returns the answer text.
     * Returns an error message when the model file or the inference binary is missing.
     */
    String run(String model, String prompt, int threads, int ctxSize) throws Exception {
        if (!enabled) {
            return "On-device model is off. Turn it on in Settings and point to a model file.";
        }
        String m = model == null || model.isEmpty() ? this.modelPath : model;
        if (m == null || m.isEmpty()) {
            return "No model file set. Put a .gguf or .tflite in Buddy's Linux and set its path in Settings.";
        }
        File f = new File(m);
        if (!f.isFile()) {
            return "Model file not found: " + m;
        }
        // Try llama.cpp-style binary first, then a generic tflite runner.
        String bin = inferBinary();
        if (bin == null) {
            return "No inference binary found. Install llama.cpp (llama-cli) or a TFLite runner in Buddy's Linux.";
        }
        ProcessBuilder pb = new ProcessBuilder(
                bin,
                "-m", m,
                "-p", prompt,
                "-n", "256",
                "-t", String.valueOf(threads > 0 ? threads : Runtime.getRuntime().availableProcessors()),
                "-c", String.valueOf(ctxSize > 0 ? ctxSize : 2048)
        );
        pb.directory(new File(WORK_ROOT));
        pb.redirectErrorStream(true);
        Process p = pb.start();
        StringBuilder out = new StringBuilder();
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) out.append(line).append('\n');
        }
        int code = p.waitFor();
        if (code != 0) {
            return "Model exited with code " + code + ": " + clip(out.toString());
        }
        return clip(out.toString()).isEmpty() ? "(no output from the model)" : clip(out.toString());
    }

    /** Guesses the inference binary: llama-cli first, then anything named tflite/run_model. */
    private String inferBinary() {
        String[] candidates = {
                WORK_ROOT + "/bin/llama-cli",
                WORK_ROOT + "/bin/llama.cpp",
                WORK_ROOT + "/bin/tflite",
                WORK_ROOT + "/bin/run_model",
                "/system/bin/llama-cli",
                "/data/local/tmp/llama-cli",
        };
        for (String b : candidates) {
            if (new File(b).canExecute()) return b;
        }
        return null;
    }

    private static String clip(String s) {
        if (s == null) return "";
        s = s.trim();
        return s.length() > 3000 ? s.substring(0, 2997) + "…" : s;
    }

    static JSONObject status() throws JSONException {
        return new JSONObject()
                .put("enabled", Prefs.localModelEnabled(null))
                .put("model_path", Prefs.localModelPath(null))
                .put("threads", Prefs.localModelThreads(null))
                .put("ctx_size", Prefs.localModelCtxSize(null));
    }
}
