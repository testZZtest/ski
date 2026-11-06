# RESUMO DA IMPLEMENTAÇÃO — Buddy (Android AI Agent)

Este documento resume tudo o que foi implementado, o que ainda falta, os repositórios de referência e como continuar o trabalho. Destinado a ser entregue para outra IA dar continuidade.

---

## 1. Estado atual do repositório

- **Repositório:** https://github.com/testZZtest/ski.git
- **Branch de trabalho:** `freebuff/4341bbc4fa6ef2eea7d991ee`
- **Branch main (remoto):** `main` — já tem a Fase D (ChatApps.java) no commit `552721b`
- **Commit atual da branch de trabalho:** `4a5aed9` ("Personalização v2: skills, provedores, app_status, velocidade máxima")
- **Worktree:** limpo, sem alterações não comitadas
- **SDK/Java:** não há JDK no ambiente Cloud — compilação não testada, apenas revisão manual de código
- **Não foi enviado para o GitHub ainda** (o resumo e novos arquivos ainda não existem localmente)

---

## 2. O que já foi implementado (versão 2.5.0 / VERSION_CODE 203)

### Arquitetura e app
- **MainActivity** — lista de chats, nova conversa, bolha, status
- **ChatActivity** — conversa com um agente, tool calls agrupados
- **BuddyService** — serviço de acessibilidade, bolha flutuante, MCP local em `127.0.0.1:8765`, pause/resume, `askConfirm`, `askQuestion`
- **Account.java** — contas dos agentes (Claude, Codex, OpenCode, Antigravity, Grok)
- **Sessions.java** — sessões de chat, mensagens, agendamentos
- **Rules.java** — regras de confiança por app
- **Guard.java** — aprovações, hard/smart/trust modes, detecção de botões arriscados
- **Schedules.java** — mensagens agendadas
- **Triggers.java** — regras de notificação (reagir a notificações de apps)
- **Skills.java** — skills (SKILL.md em ~/.agents/skills), importação de zip/.skill, 5 skills padrão
- **Providers.java** — chaves de API para serviços externos (DeepSeek, OpenRouter, Anthropic, OpenAI, Groq, Perplexity, Mistral, xAI, Google Vertex, HuggingFace, Ollama, LocalAI), modelos por serviço
- **Models.java** — modelos por agente (claude, codex, opencode, agy, grok), níveis de esforço
- **BuildInfo.java** — VERSION = "2.5.0", VERSION_CODE = 203

### Phone tools (Tools.java / PhoneTools.java)
- read_screen, tap, long_press, type_text, scroll, swipe, press, open_app, list_apps, open_url
- screenshot, read_screen_with_numbers, tap_by_number, ocr_screen
- wait, wait_for, do_steps, collect, find
- confirm, ask, point_out, notify, reply_notification
- schedule_message, list_scheduled, cancel_scheduled
- install_apk, update_buddy, wait_for_install
- clipboard_read, clipboard_write, set_alarm, set_timer, share, save_to_downloads
- device_status (bateria, rede, armazenamento, memória RAM, hora local, modo silencioso)
- take_photo, location (fora do build Play)
- notifications, media, calendar_events, add_calendar_event
- web_search, web_fetch, translate
- shizuku_bind, shizuku_run, shizuku_status, shizuku_check_permission
- memory_save, memory_read, memory_list, memory_search, memory_delete, memory_summary
- reasoning_mode, remote_status
- trace_start, trace_end, trace_status
- local_model, local_model_status
- workflow_template
- app_customize (action_delay_ms, bubble_opacity, bubble_autohide_seconds, bubble_show_status_pill)
- app_status

### Linux interno
- **Linux.java** — Alpine Linux via proot dentro do app (rootfs em c.getFilesDir()/linux/alpine)
- Execução de comandos de shell dentro do Alpine (proot + bind mounts)
- Download, verificação SHA256 e desempacotamento do rootfs Alpine

### Shizuku (permissões elevadas sem root)
- **Shizuku.java** — vincula ao serviço moe.shizuku.privileged.API
- shizuku_bind, shizuku_run, shizuku_status, shizuku_check_permission
- Integrado em PhoneTools (disponível fora do build Play)

### Memória persistente
- **Memory.java** — vault em ~/.buddy/memory/, entradas JSON com id, kind, text, tags, created, updated, source
- memory_save, memory_read, memory_list, memory_search, memory_delete, memory_summary

### Navegador em background
- **Browser.java / BrowserTools.java** — páginas dentro do Buddy, sem tomar a tela
- browser_open, browser_read, browser_click, browser_type, browser_scroll, browser_back, browser_screenshot

### Controle remoto (Fase E parcial)
- **McpHttpServer.java** — servidor MCP HTTP em 127.0.0.1:8766, token por instalação
- remote_control preference, exposto via remote_status tool

### Observabilidade e IA on-device (Fase G/F parciais)
- **Tracing.java** — OTLP spans, trace_start/end/status tools
- **OnDeviceModel.java** — esqueleto GGUF/LiteRT local, local_model/local_model_status tools

### Workflow templates e personalização (Fase H/K)
- workflow_templates.json (6 templates), workflow_template tool
- app_customize tool com prefs: action_delay (padrão 0 = máxima velocidade), bubble_opacity, bubble_autohide, bubble_show_status_pill
- bubble size (small/medium/large), bubble show text
- app_status tool

### Internacionalização
- 14 idiomas com pastas res/values-<language>/strings.xml

---

## 3. O que ainda falta (fases pendentes do PLANO.md)

| Fase | Item | Status |
|------|------|--------|
| **Fase B** | Hidden / virtual display (KYRIE66nb, pony) | Pendente — requer display virtual separado |
| **Fase C** | Lembrança/resumo de chats antigos | Pendente |
| **Fase D** | Comunicação via apps de chat (Telegram, WhatsApp, Discord, Slack, etc.) | Em outro branch (ChatApps.java no main), não nesta worktree |
| **Fase E** | Controle remoto via Telegram (AutoCua) | Pendente — requer bot do Telegram e polling |
| **Fase F** | Modelos GGUF/LiteRT on-device com dependências nativas | Pendente — OnDeviceModel.java é só esqueleto |
| **Fase G** | Tracing com coletor OTLP real (Arize/Langfuse) | Pendente — só a preferência e o endpoint estão prontos |
| **Fase G** | Protocolo estilo DevTools (agent-ddp) | Pendente |
| **Fase I** | iOS e portal app (droidrun/mobilerun) | Fora do escopo imediato |

### Novas funcionalidades solicitadas pelo usuário (não iniciadas nesta sessão)
1. **SystemTools.java** — auditoria completa do celular: bateria, armazenamento, memória, rede, processos, pacotes, cache, debloat (listar/desabilitar apps não usados), execução de comandos via Shizuku/Linux, auditoria de permissões, otimização de apps
2. **NeuralMemory.java** — aprendizado com erros (failed tool calls → memory_save com kind=fact/error), armazenamento de padrões, sugestões de evolução
3. **GameLogic.java** — xadrez, quebra-cabeças, jogos de memória, jogos de raciocínio lógico usando leitura de tela + ferramentas de tap
4. Integração das três novas tools em Tools.java (schema/list/call0) e Prefs.java (novas preferências)
5. Bump de versão em BuildInfo.java (2.5.x / VERSION_CODE 20x)
6. Atualização de PLANO.md e README.md

---

## 4. Repositórios de referência (todos os fornecidos)

### Repositórios já no PLANO.md original
1. **ghorbelhamdi/buddy-android** — base do Buddy (floating bubble, aprovação antes de ações, Linux tools, build APK)
2. **zafiro** — Kotlin + Jetpack Compose, Shizuku, Python via Chaquopy, Xposed/LSPosed, skills/MCP, lembrança, workspace
3. **Verdroid** — web search/fetch, Termux bash + Python + Selenium, tradução de e-books, crypto backtesting, modelos GGUF/LiteRT on-device, OpenRouter/Kilo fallback
4. **KYRIE66nb/mobile-agent** — dupla percepção (acessibilidade + visual), DecisionGate, display oculto virtual, bridge loopback MCP, recipes, triggers, ad guard, substituição de voz
5. **droid-mcp** — 160+ ferramentas MCP, 53 módulos, servidor HTTP no telefone, confirmação de chamadas arriscadas
6. **TecnicoBot/mobile-agent** — on-device, MCP, skills, memória persistente, multimodal, voz
7. **AutoCua** — computer use desktop + mobile via USB/Wi-Fi, compressão de contexto, memória vault, controle remoto via Telegram
8. **droidrun/mobilerun** — CLI + Python API, Android + iOS, portal app, visão, reasoning mode, tracing (Arize/Langfuse)
9. **aohp-os/MobileClaw** — agente GUI humano-like via visão, memory e skills como .md, comunicação via apps de chat (Telegram, Weixin, Lark, QQ, Zulip, Discord, WhatsApp, Slack), config.yaml, dois modelos (control + computer use)
10. **bandada-labs/agent-ddp** — protocolo estilo DevTools para debug builds iOS/Android, element tree, animações, checks, MCP server, ddp CLI
11. **FoneClaw-AI/foneclaw-android** — 120+ ações de voz em 16 categorias, aprovação com classificação de risco, community skills, workflow templates, plugin APKs, 14 idiomas
12. **oroplex/pony** — WebSocket relay emparelhado, screenshots + UI tree, tap/swipe/type, hidden display, chave no Android Keystore, 3 modos de conexão
13. **8crsk/openclaw-android** — on-device, OpenClaw gateway Node.js dentro do app, AccessibilityService, numbered legend UI automation, BYOK, heartbeat, 300+ app integrations via Composio, Shizuku opcional

### Novos repositórios fornecidos pelo usuário (esta sessão)
14. **https://github.com/Nido007-cmyk/Nido/**
15. **https://github.com/AIOSAI/AIPass**
16. **https://github.com/Hy4ri/hermes-mobile**
17. **https://github.com/Drevero/Dots-hermes**
18. **https://github.com/getpaseo/paseo**
19. **https://github.com/agentsdroid/agentsdroid**
20. **https://github.com/droidrun/mobilerun** (já listado acima)
21. **https://github.com/stablyai/orca**
22. **https://github.com/callstack/agent-device**
23. **https://github.com/wangzan101/Mika**
24. **https://github.com/morluto/rea**
25. **https://github.com/Core-Mate/OpenGUI**
26. **https://github.com/Mangi-11/Eta**
27. **https://github.com/JMAN730/opendroid**
28. **https://github.com/techjarves/Mobile-Harness**
29. **https://github.com/memex-lab/memex**
30. **https://github.com/wangzhishou/OneBox**
31. **https://github.com/SimoneAvogadro/android-reverse-engineering-skill**

---

## 5. Como continuar (para a próxima IA)

### Passo 1 — Leia os arquivos existentes
- `PLANO.md` — plano de fases e ordem sugerida
- `README.md` — capabilities atuais e instruções de build
- `app/src/main/java/app/buddy/assistant/Tools.java` — schema (`list()`) e dispatch (`call0()` switch)
- `app/src/main/java/app/buddy/assistant/Prefs.java` — shared preferences (padrão a seguir para novas prefs)
- `app/src/main/java/app/buddy/assistant/Memory.java` — padrão para vault de memória persistente
- `app/src/main/java/app/buddy/assistant/BuildInfo.java` — versão (atual: 2.5.0 / 203)
- `app/src/main/java/app/buddy/assistant/Linux.java` — execução de comandos no Alpine via proot
- `app/src/main/java/app/buddy/assistant/Shizuku.java` — comandos elevados via Shizuku
- `app/src/main/java/app/buddy/assistant/PhoneTools.java` — device_status, shizuku commands, etc.
- `app/src/main/java/app/buddy/assistant/Skills.java` — padrão para criação de skills
- `app/src/main/java/app/buddy/assistant/Guard.java` — sistema de aprovação e safety

### Passo 2 — Novos arquivos a criar
- `SystemTools.java` — auditoria completa do celular, debloat, shell commands, permission audit, app optimization
- `NeuralMemory.java` — aprendizado com erros, pattern storage, evolution suggestions
- `GameLogic.java` — xadrez, quebra-cabeças, jogos de memória, raciocínio lógico

### Passo 3 — Integração
- Adicionar novas tools em `Tools.list()` e implementar em `Tools.call0()`
- Adicionar novos nomes em `Tools.listNames()`
- Adicionar novas preferências em `Prefs.java`
- Bump de versão em `BuildInfo.java`
- Atualizar `PLANO.md` e `README.md`

### Passo 4 — Build e teste
- `./gradlew assembleDebug` (Android Studio / Gradle)
- `./build.sh` (no phone, Termux, sem Gradle → build/buddy-release.apk)
- Sem JDK no ambiente Cloud: revisar código manualmente

### Passo 5 — Commit e push
- Commitar todas as mudanças
- Push via `cloud_git` (ou `git push`) para branch `freebuff/4341bbc4fa6ef2eea7d991ee` ou `main`

---

## 6. Observações importantes

- **Não foi compilado nem executado** (sem JDK/Android SDK no sandbox) — todas as mudanças são revisão manual de código
- O `main` remoto (commit `552721b`) já contém a Fase D (`ChatApps.java`) mas a branch de trabalho (`freebuff/4341bbc4fa6ef2eea7d991ee`, commit `4a5aed9`) não
- Todos os links de repositórios acima devem ser revisados pela próxima IA para extrair código/lógica reutilizável
- A action_delay padrão é 0 (máxima velocidade), conforme decisão do usuário

---

*Última atualização: novembro 2026*