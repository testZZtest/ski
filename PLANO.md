# PLANO — Buddy: agente de IA mobile completo

Este arquivo é o ponto de partida para uma outra IA continuar o trabalho. Ele resume o que foi levantado dos ~15 repos alvo, o que ainda falta integrar no Buddy (repositório local `app/buddy/assistant`, app Android Java/Gradle) e a ordem sugerida de implementação.

---

## 1. O que o Buddy já tem (repositório local)

App Android Java + Gradle (ou `build.sh` no Termux, sem Gradle), Android 11+, 64-bit. Branca: `freebuff/4341bbc4fa6ef2eea7d991ee`.

Arquitetura:
- `MainActivity` — lista de chats, nova conversa, bolha, status.
- `ChatActivity` — conversa com um agente, tool calls agrupados.
- `BuddyService` — serviço de acessibilidade, bolha flutuante, MCP local em `127.0.0.1:8765`, pause/resume, `askConfirm`, `askQuestion`.
- `Tools.java` (~1966 linhas) — dispatch das tools MCP: `read_screen`, `tap`, `long_press`, `type_text`, `scroll`, `swipe`, `press`, `open_app`, `list_apps`, `open_url`, `screenshot`, `wait`, `wait_for`, `do_steps`, `collect`, `find`, `confirm`, `ask`, `point_out`, `notify`, `reply_notification`, `schedule_message`, `list_scheduled`, `cancel_scheduled`, `install_apk`, `update_buddy`, `wait_for_install`, + as de `PhoneTools` e `BrowserTools`.
- `PhoneTools.java` — clipboard, alarm, timer, share, save_to_downloads, device_status, take_photo, notifications, media, calendar_events, add_calendar_event, (location fora do Play).
- `McpServer.java` — servidor MCP local em 127.0.0.1:8765, com token por instalação.
- `Browser.java` / `BrowserTools` — navegador em background (páginas dentro do Buddy, sem tomar a tela).
- `Linux.java` — Linux interno (Alpine via proot): claude, codex, opencode, agy (Antigravity), grok.
- `Providers.java` — chaves de API para serviços externos (DeepSeek, OpenRouter, Anthropic, OpenAI), modelos por serviço.
- `Models.java` — modelos por agente (claude, codex, opencode, agy, grok), níveis de esforço.
- `Sessions.java` — sessões de chat, mensagens, agendamentos.
- `Skills.java` — skills (SKILL.md em ~/.agents/skills), importação de zip/.skill.
- `Triggers.java` — regras de notificação (reagir a notificações de apps).
- `Schedules.java` — mensagens agendadas.
- `Rules.java` — regras de confiança por app.
- `Guard.java` — aprovações, hard/smart/trust modes.
- `Account.java` — contas dos agentes (Claude, Codex, OpenCode, Antigravity, Grok).
- `mcp-bridge/bridge.mjs` — bridge MCP local para clientes que iniciam servidores como comandos (stdio).
- `linux/` — prompts (`system-prompt.md`), perfil (`profile.sh`), AGENTS.md do Linux interno.

Versão: `BuildInfo.java` → VERSION = "2.2.22", VERSION_CODE = 191.

---

## 2. O que os 14 repos alvo trazem (levantado dos READMEs)

### zafiro
- Kotlin + Jetpack Compose (UI moderna, opcional para reescrita futura)
- Shizuku (permissões de sistema sem root)
- Python via Chaquopy (Python on-device)
- Xposed / LSPosed (hooks de sistema)
- Skills / MCP (próprio ecossistema)
- Lembrança (memória de conversas)
- Workspace (vários ambientes de trabalho)

### Verdroid
- Web search / fetch (busca na web)
- Termux bash + Python + Selenium (automação de navegador real)
- Tradução de e-books
- Crypto backtesting
- Modelos GGUF / LiteRT on-device (IA rodando no aparelho, sem nuvem)
- Modelos OpenRouter / Kilo com fallback

### KYRIE66nb/mobile-agent
- Dupla percepção: acessibilidade + visual (screenshot + OCR)
- DecisionGate (gate de decisão antes de ações)
- Root virtual display (display oculto para automação)
- Bridge loopback MCP para desktop (controlar o desktop pelo celular)
- Recipes (receitas de automação)
- Triggers, ad guard, substituição de voz

### droid-mcp
- 160+ ferramentas MCP, 53 módulos
- Servidor HTTP no telefone (MCP sobre HTTP, não só stdio/local)
- Confirmação de chamadas arriscadas
- Recursos e prompts MCP

### TecnicalBot/mobile-agent
- On-device (IA no aparelho)
- MCP, skills, memória persistente
- Multimodal, voz

### AutoCua
- Computer use desktop + mobile via USB/Wi-Fi
- Compressão de contexto
- Memória vault (memória persistente estruturada)
- Controle remoto via Telegram

### ghorbelhamdi/buddy-android (base do Buddy local)
- Floating bubble, aprovação antes de ações, Linux tools, build APK

### droidrun/mobilerun
- CLI + Python API
- Android + iOS
- Portal app
- Visão, reasoning mode
- Tracing (Arize / Langfuse)

### aohp-os/MobileClaw
- Agente GUI humano-like via visão
- Memory e skills como .md
- Comunicação via apps de chat: Telegram, Weixin, Lark, QQ, Zulip, Discord, WhatsApp, Slack
- config.yaml
- Dois modelos: control + computer use

### bandada-labs/agent-ddp
- Protocolo estilo DevTools para native iOS/Android debug builds
- Element tree, animações, checks
- MCP server, ddp CLI

### FoneClaw-AI/foneclaw-android
- 120+ ações de voz em 16 categorias
- Aprovação com classificação de risco (risk-graded approval)
- Community skills, workflow templates
- Plugin APKs
- 14 idiomas

### oroplex/pony
- WebSocket relay emparelhado (conexão remota segura)
- Screenshots + UI tree
- tap / swipe / type
- Hidden display
- Chave no Android Keystore
- 3 modos de conexão: própria chave, MCP host, Grok Bot

### 8crsk/openclaw-android
- On-device, OpenClaw gateway Node.js dentro do app (libnode.so)
- AccessibilityService
- Numbered legend UI automation (legendas numeradas na UI)
- BYOK (NVIDIA / OpenAI / Anthropic / Gemini)
- Heartbeat
- 300+ app integrations via Composio
- Shizuku opcional

---

## 3. Plano de integração (ordem sugerida)

A ideia é acrescentar ferramentas ao `Tools.java`/`PhoneTools.java`, novas capabilities ao `BuddyService`, e expor tudo via MCP (local + HTTP opcional), sem quebrar o que já funciona.

### Fase A — Ferramentas de telefone novas (baixo risco, alto impacto) — **IMPLEMENTADA**

Adicionado a `PhoneTools.java` e exposto em `Tools.list`:
1. **Web search / fetch** (Verdroid): `web_search(query)`, `web_fetch(url)` — buscar na web e ler páginas via DuckDuckGo HTML (sem API key).
2. **Tradução** (Verdroid): `translate(text, to)` — traduzir texto via LibreTranslate (best effort, sem API key).
3. **Device status detalhado** (droid-mcp / openclaw): `device_status` agora inclui temperatura da bateria, memória RAM (livre/total/uso %) além de bateria, rede, armazenamento, hora local e modo silencioso.

### Fase B — Automação de UI avançada (médio risco) — **IMPLEMENTADA (parcial)**

1. **Numbered legend UI automation** (openclaw): `read_screen_with_numbers` sobrepõe números nos elementos da tela; `tap_by_number N` toca pelo número. Implementado em `Tools.java`.
2. **Visual perception + OCR** (KYRIE66nb, TecnicoBot): `ocr_screen` lê todo o texto visível na tela como linhas simples com posições, para apps que escondem a UI do accessibility (jogos, canvas). Implementado em `Tools.java`.
3. **Hidden / virtual display** (KYRIE66nb, pony): pendente — requer display virtual separado, maior complexidade.

### Fase C — Memória, skills e lembrança (médio risco) — **IMPLEMENTADA (memória base)**

1. **Memória persistente estruturada / vault** (TecnicoBot, AutoCua, zafiro): novo arquivo `Memory.java` com `memory_save`, `memory_read`, `memory_list`, `memory_search`, `memory_delete`, `memory_summary`. Entradas salvas em `~/.buddy/memory/` como JSON com id, kind, text, tags, created, updated.
2. **Skills como .md** (MobileClaw, zafiro): já tem Skills (SKILL.md); permitir importar skills de apps de chat — ver Fase D.
3. **Lembrança / resumo de conversas** (zafiro): resumir chats antigos — pendente.

### Fase D — Comunicação via apps de chat (médio-alto risco) — pendente

Enviar/receber mensagens por apps de chat externos (MobileClaw): Telegram, WhatsApp, Discord, Slack, Weixin, Lark, QQ, Zulip.
- Implementar como tools `chat_send(app, contact, text)` e `chat_read(app, contact, since)`.
- Usar accessibility + open_url (deep links) quando não houver API pública.

### Fase E — Controle remoto e relay (alto impacto) — **IMPLEMENTADA (parcial)**

1. **Servidor MCP HTTP no telefone** (droid-mcp, pony): novo arquivo `McpHttpServer.java` expõe as tools do Buddy via HTTP em `127.0.0.1:8766`, com o mesmo token do MCP local. Ativado em Settings via `Prefs.remoteControl`. Integrado ao `BuddyService` (inicia em `onServiceConnected`, para em `onDestroy`).
2. **Bridge loopback MCP para desktop** (KYRIE66nb): o `McpHttpServer` serve como endpoint HTTP para clientes externos; o mcp-bridge local continua em stdio.
3. **Controle remoto via Telegram** (AutoCua): pendente — requer bot do Telegram e polling.

### Fase F — IA on-device (alto impacto, maior complexidade) — pendente

1. **Modelos GGUF / LiteRT on-device** (Verdroid, TecnicoBot): rodar modelos pequenos localmente. Requer dependências nativas.
2. **Fallback de modelos** (Verdroid).

### Fase G — Observabilidade e dev tools (baixo risco) — **IMPLEMENTADA (parcial)**

1. **Reasoning mode** (droidrun): preferência `Prefs.reasoningMode` (`off`/`on`/`always`). Antes de cada ação de telefone, o agent descreve o que vê, planeja e por que. Implementado como preference + schema em `Tools.listNames()`.
2. **Tracing** (droidrun: Arize / Langfuse): pendente — requer integração com serviço externo.
3. **Protocolo estilo DevTools** (agent-ddp): pendente — o `/tools` endpoint do `McpHttpServer` já lista as tool names.

### Fase H — Internacionalização e plugins (baixo risco) — **IMPLEMENTADA (parcial)**

1. **14 idiomas** (FoneClaw): strings novas adicionadas a `res/values/strings.xml`; arquivos `res/values-<language>/strings.xml` podem ser criados por tradutores (mecanismo já existe, comentado no topo do arquivo).
2. **Community skills / plugin APKs** (FoneClaw): já existe `Skills.java` (importação de .skill/.zip).
3. **Workflow templates** (FoneClaw): pendente — templates de `do_steps` pré-definidos.

### Fase I — iOS e portal (fora do escopo imediato) — pendente

- `droidrun/mobilerun` traz Android + iOS e portal app. Futuro, não neste repositório.

### Fase J — Shizuku (permissões elevadas sem root) — **IMPLEMENTADA**

1. **Suporte a Shizuku** (zafiro, openclaw): novo arquivo `Shizuku.java` que vincula ao serviço `moe.shizuku.privileged.API` do Shizuku.
   - `shizuku_bind()` — conecta ao serviço Shizuku.
   - `shizuku_run(cmd)` — executa comandos de shell com privilégios elevados.
   - `shizuku_status()` — verifica se está instalado e vinculado.
   - `shizuku_check_permission(permission)` — verifica se uma permissão foi concedida.
   - Integrado ao `BuddyService` (campo `shizuku`, vinculado em `onServiceConnected`, desvinculado em `onDestroy`).
   - Exposto via `PhoneTools.java` (tools `shizuku_bind`, `shizuku_run`, `shizuku_status`, `shizuku_check_permission`, disponíveis fora do build Play).

### Fase K — Personalização do botão flutuante — **IMPLEMENTADA**

1. **Tamanho do botão flutuante** (`Prefs.bubbleSize`): `small` (40dp face), `medium` (60dp, default), `large` (80dp face). O ring e box escalam proporcionalmente.
2. **Mostrar/esconder o texto flutuante** (`Prefs.bubbleShowText`): controla a visibilidade do pill (status text) ao lado da bolha.
   - Novo arquivo `Shizuku.java` + preferências em `Prefs.java`.
   - `Bubble.java` lê `faceSize`, `boxSize` e `showPill` no construtor e usa em `buildCollapsed()`.

---

## 4. Versão e versionamento

- Atual: `BuildInfo.java` → `VERSION = "2.4.0"`, `VERSION_CODE = 201`.
- Ao integrar mais funcionalidades, subir para `2.4.1`, `2.5.0` etc.
- Atualizar `README.md` com a nova lista de capabilities.

---

## 5. Como continuar (para a próxima IA)

1. Leia este PLANO.md e o `README.md` atual.
2. Escolha a próxima fase (sugestão: **Fase B** — automação de UI avançada, ou **Fase D** — apps de chat).
3. Leia o arquivo relevante em `app/src/main/java/app/buddy/assistant/` (`Tools.java`, `PhoneTools.java`, `BuddyService.java`, `McpServer.java`, `Providers.java`, `Models.java`).
4. O novo `Memory.java` e `Shizuku.java` já estão integrados: as tools `memory_*` e `shizuku_*` estão expostas via MCP automaticamente.
5. Adicione novas tools em `Tools.list()` e implemente em `Tools.call0()` / `PhoneTools.call()`.
6. Exponha novas tools via `McpServer` (já expõe tudo de `Tools.list` automaticamente).
7. Adicione strings em `res/values/strings.xml` para novas UI.
8. Teste compilação com `./gradlew assembleDebug` (ou `./build.sh` no Termux — sem Java no ambiente Cloud, revise o código manualmente).
9. Atualize `README.md` e este `PLANO.md` à medida que avança.
10. Ao terminar uma fase, suba a versão em `BuildInfo.java` e commite.

---

## 6. Repositórios de referência (para copiar código/lógica)

Todos listados no histórico da conversa; URLs no GitHub:
- `ghorbelhamdi/buddy-android` (base)
- `zafiro`, `Verdroid`, `KYRIE66nb/mobile-agent`, `droid-mcp`, `TecnicoBot/mobile-agent`, `AutoCua`, `droidrun/mobilerun`, `aohp-os/MobileClaw`, `bandada-labs/agent-ddp`, `FoneClaw-AI/foneclaw-android`, `oroplex/pony`, `8crsk/openclaw-android`

---

**Última atualização do plano:** Fase A (web_search, web_fetch, translate, device_status detalhado), Fase C (memória persistente via Memory.java), Fase J (Shizuku) e Fase K (personalização do botão flutuante: tamanho + mostrar/esconder texto) implementadas. Versão 2.3.0. Próximo: Fase B ou D.