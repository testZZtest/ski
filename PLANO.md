# PLANO E RESUMO DA IMPLEMENTAÇÃO — Buddy (Android AI Agent)

Este documento unifica o **PLANO.md** original e o **RESUMO_IMPLEMENTACAO.md**, servindo como ponto de partida completo e detalhado para qualquer outra IA ou desenvolvedor dar continuidade ao projeto Buddy.

---

## 1. Estado Atual do Repositório e Arquitetura

- **Repositório GitHub:** `https://github.com/testZZtest/ski.git`
- **Branch principal:** `main` (tudo enviado e sincronizado)
- **Versão Atual:** `BuildInfo.java` → `VERSION = "2.5.0"`, `VERSION_CODE = 203`
- **Requisitos de Sistema:** Android 11+ (API 30+), 64-bit ARM/x86. Compilável via `./gradlew assembleDebug` ou `./build.sh` no Termux.

### Estrutura e Componentes Principais (`app/src/main/java/app/buddy/assistant/`)

- `MainActivity` — Lista de chats, nova conversa, controle da bolha flutuante e exibição de status do app.
- `ChatActivity` — Interface de conversa com os agentes de IA, agrupando requisições e respostas de ferramentas (tool calls).
- `BuddyService` — Serviço de acessibilidade principal. Mantém a bolha flutuante (`Bubble.java`), o servidor MCP local (`127.0.0.1:8765`), opções de pause/resume e diálogos de confirmação (`askConfirm`, `askQuestion`).
- `Tools.java` (~2350 linhas) — Registro de schemas (`list()`) e despacho de execução (`call0()`) de todas as ferramentas de automação Android, sistema, memória, navegador e chat.
- `PhoneTools.java` — Implementação das ferramentas do sistema: clipboard, alarme, timer, compartilhamento, downloads, status do dispositivo, câmera, notificações, mídia e calendário.
- `ChatApps.java` (Fase D) — Suporte a 8 aplicativos de mensagem (Telegram, WhatsApp, Discord, Slack, Weixin, Lark, QQ, Zulip) com automação via acessibilidade (`chat_send`, `chat_read`).
- `Memory.java` — Sistema de memória persistente em vault local (`~/.buddy/memory/`), com suporte a busca por tag, id e resumo.
- `Browser.java` / `BrowserTools.java` — Navegador headless em segundo plano para automação web silenciosa.
- `Linux.java` — Ambiente Alpine Linux embarcado via `proot` dentro da pasta interna do app (`c.getFilesDir()/linux/alpine`).
- `Shizuku.java` (Fase J) — Integração com o Shizuku (`moe.shizuku.privileged.API`) para comandos de shell com permissões elevadas sem root.
- `McpServer.java` & `McpHttpServer.java` (Fase E) — Servidores MCP local (portas 8765 stdio/bridge e 8766 HTTP) com autenticação por token de instalação.
- `Providers.java` & `Models.java` — Gerenciamento de chaves de API (DeepSeek, OpenRouter, Anthropic, OpenAI, Groq, Perplexity, Mistral, xAI, Vertex, HuggingFace, Ollama, LocalAI) e agentes (`claude`, `codex`, `opencode`, `agy`, `grok`).
- `Sessions.java`, `Schedules.java`, `Triggers.java`, `Skills.java`, `Rules.java`, `Guard.java`, `Account.java`.
- `mcp-bridge/bridge.mjs` — Bridge MCP em Node.js para clientes stdio.
- `linux/` — Arquivos do ambiente Linux (`system-prompt.md`, `profile.sh`, `AGENTS.md`).

---

## 2. Visão Geral das Ferramentas Disponíveis (Tools)

- **Interface & Acessibilidade:** `read_screen`, `tap`, `long_press`, `type_text`, `scroll`, `swipe`, `press`, `open_app`, `list_apps`, `open_url`, `screenshot`, `read_screen_with_numbers`, `tap_by_number`, `find_and_tap` (texto/descrição/view id, com scroll), `ocr_screen`.
- **Controle & Fluxo:** `wait`, `wait_for`, `do_steps`, `collect`, `find`, `confirm`, `ask`, `point_out`, `notify`, `reply_notification`.
- **Agendamento & Instalação:** `schedule_message`, `list_scheduled`, `cancel_scheduled`, `install_apk`, `update_buddy`, `wait_for_install`.
- **Utilitários do Dispositivo:** `clipboard_read`, `clipboard_write`, `set_alarm`, `set_timer`, `share`, `save_to_downloads`, `device_status`, `take_photo`, `location`, `notifications`, `media`, `calendar_events`, `add_calendar_event`.
- **Web & Tradução:** `web_search`, `web_fetch`, `translate`.
- **Permissões Elevadas (Shizuku):** `shizuku_bind`, `shizuku_run`, `shizuku_status`, `shizuku_check_permission`.
- **Memória Persistente:** `memory_save`, `memory_read`, `memory_list`, `memory_search`, `memory_delete`, `memory_summary`.
- **Navegador em Background:** `browser_open`, `browser_read`, `browser_click`, `browser_type`, `browser_scroll`, `browser_back`, `browser_screenshot`.
- **Mensagens & Chat (Fase D):** `chat_send`, `chat_read`.
- **Observabilidade & Configuração:** `reasoning_mode`, `remote_status`, `trace_start`, `trace_end`, `trace_status`, `local_model`, `local_model_status`, `workflow_template`, `app_customize` (delay=0ms padrão, opacidade, autohide, status pill), `app_status`.

---

## 3. Status da Integração por Fases

| Fase | Funcionalidade / Origem | Status Atual |
|------|------------------------|--------------|
| **Fase A** | Web search/fetch (Verdroid), Tradução (Verdroid), Device status detalhado | **Concluída** |
| **Fase B** | Numbered legend UI (`read_screen_with_numbers`), OCR (`ocr_screen`), seletores semânticos (`find_and_tap`) e senhas mascaradas | **Concluída** |
| **Fase B** | Display virtual/oculto separado (KYRIE66nb, pony) | Pendente |
| **Fase C** | Memória persistente vault (`Memory.java`), Skills como .md | **Concluída** |
| **Fase C** | Lembrança e resumo automático de conversas antigas (zafiro) | Pendente |
| **Fase D** | Comunicação via apps de chat Telegram, WhatsApp, Discord, Slack, etc. (`ChatApps.java`) | **Concluída** |
| **Fase E** | Servidor MCP HTTP no telefone (127.0.0.1:8766) (`McpHttpServer.java`) | **Concluída** |
| **Fase E** | Controle remoto via Bot do Telegram (AutoCua) | Pendente |
| **Fase F** | Execução de modelos GGUF/LiteRT on-device com dependências nativas | Esqueleto pronto (`OnDeviceModel.java`) |
| **Fase G** | Reasoning mode (`Prefs.reasoningMode`) | **Concluída** |
| **Fase G** | Tracing OTLP com Arize/Langfuse e protocolo DevTools (`agent-ddp`) | Estrutura parcial (`Tracing.java`) |
| **Fase H** | Suporte a 14 idiomas em `strings.xml` e Workflow Templates (`workflow_template`) | **Concluída** |
| **Fase J** | Suporte a Shizuku para permissões e comandos de shell (`Shizuku.java`) | **Concluída** |
| **Fase K** | Personalização do botão flutuante e ajuste de delay para 0ms (`app_customize`) | **Concluída** |

### Próximas Funcionalidades Planejadas (Novas Demandas)

1. **SystemTools.java** — Auditoria do aparelho (bateria, memória RAM, armazenamento, processos, pacotes), debloat (listar e desabilitar apps não utilizados), execução de scripts via Shizuku/Linux e otimização do sistema.
2. **NeuralMemory.java** — Aprendizado contínuo com falhas (registrando erros em `memory_save` com `kind=fact/error`), identificação de padrões e sugestões automáticas de evolução.
3. **GameLogic.java** — Automação e raciocínio para jogos (xadrez, quebra-cabeças, jogos de memória) combinando leitura de tela via OCR/screenshot e ações de toque.

---

## 4. Todos os 32 Repositórios de Referência

### Repositórios Originais do Plano
1. **ghorbelhamdi/buddy-android** — https://github.com/ghorbelhamdi/buddy-android (Base do Buddy: floating bubble, aprovação de ações, Linux tools, build APK).
2. **zafiro** — https://github.com/zafiro (Kotlin, Jetpack Compose, Shizuku, Python via Chaquopy, Xposed/LSPosed, skills/MCP, memória, workspace).
3. **Verdroid** — https://github.com/Verdroid (Web search/fetch, Termux bash, Selenium, tradução de e-books, GGUF/LiteRT local, fallback OpenRouter).
4. **KYRIE66nb/mobile-agent** — https://github.com/KYRIE66nb/mobile-agent (Dupla percepção acessibilidade+visual, DecisionGate, virtual display, bridge MCP, recipes).
5. **droid-mcp** — https://github.com/droid-mcp (160+ ferramentas MCP, 53 módulos, servidor HTTP Android).
6. **TecnicoBot/mobile-agent** — https://github.com/TecnicoBot/mobile-agent (On-device IA, MCP, skills, memória persistente, voz/multimodal).
7. **AutoCua** — https://github.com/AutoCua (Computer use desktop + mobile via USB/Wi-Fi, memória vault, controle via Telegram).
8. **droidrun/mobilerun** — https://github.com/droidrun/mobilerun (CLI + Python API, Android + iOS, portal app, visão, reasoning mode, tracing Arize/Langfuse).
9. **aohp-os/MobileClaw** — https://github.com/aohp-os/MobileClaw (Agente GUI humano-like, skills .md, integração com 8 apps de chat).
10. **bandada-labs/agent-ddp** — https://github.com/bandada-labs/agent-ddp (Protocolo estilo DevTools para iOS/Android, element tree, MCP server).
11. **FoneClaw-AI/foneclaw-android** — https://github.com/FoneClaw-AI/foneclaw-android (120+ ações de voz em 16 categorias, aprovação por nível de risco, workflow templates).
12. **oroplex/pony** — https://github.com/oroplex/pony (WebSocket relay emparelhado, hidden display, Android Keystore).
13. **8crsk/openclaw-android** — https://github.com/8crsk/openclaw-android (Node.js on-device via `libnode.so`, AccessibilityService, legibilidade numerada).

### Novos Repositórios Adicionados
14. **Nido007-cmyk/Nido** — https://github.com/Nido007-cmyk/Nido/
15. **AIOSAI/AIPass** — https://github.com/AIOSAI/AIPass
16. **Hy4ri/hermes-mobile** — https://github.com/Hy4ri/hermes-mobile
17. **Drevero/Dots-hermes** — https://github.com/Drevero/Dots-hermes
18. **getpaseo/paseo** — https://github.com/getpaseo/paseo
19. **agentsdroid/agentsdroid** — https://github.com/agentsdroid/agentsdroid
20. **droidrun/mobilerun** — https://github.com/droidrun/mobilerun
21. **stablyai/orca** — https://github.com/stablyai/orca
22. **callstack/agent-device** — https://github.com/callstack/agent-device
23. **wangzan101/Mika** — https://github.com/wangzan101/Mika
24. **morluto/rea** — https://github.com/morluto/rea
25. **Core-Mate/OpenGUI** — https://github.com/Core-Mate/OpenGUI
26. **Mangi-11/Eta** — https://github.com/Mangi-11/Eta
27. **JMAN730/opendroid** — https://github.com/JMAN730/opendroid
28. **techjarves/Mobile-Harness** — https://github.com/techjarves/Mobile-Harness
29. **memex-lab/memex** — https://github.com/memex-lab/memex
30. **wangzhishou/OneBox** — https://github.com/wangzhishou/OneBox
31. **SimoneAvogadro/android-reverse-engineering-skill** — https://github.com/SimoneAvogadro/android-reverse-engineering-skill
32. **duOCELOT/pestinha-duOCELOT** — https://github.com/duOCELOT/pestinha-duOCELOT

---

## 5. Instruções para a Próxima IA / Desenvolvedor

1. **Leitura prévia do código:**
   - `Tools.java` — para registrar novas ferramentas no schema e no dispatch (`call0()`).
   - `Prefs.java` — para adicionar novas preferências do usuário.
   - `Shizuku.java` / `Linux.java` — para executar comandos de sistema e shell de forma segura.
   - `BuildInfo.java` — atualizar a versão sempre que adicionar novas capacidades relevantes.

2. **Criação dos novos módulos:**
   - Implementar `SystemTools.java` (auditoria, debloat, permissões).
   - Implementar `NeuralMemory.java` (aprendizado com falhas e refinamento de contexto).
   - Implementar `GameLogic.java` (jogos e lógica visual).

3. **Compilação e validação:**
   - Testar build com `./gradlew assembleDebug` (ou `./build.sh` no Termux).
   - Em ambientes sem JDK/Android SDK (como a nuvem), realizar validação estática minuciosa de sintaxe Java e integridade de chamadas.

4. **Sincronização:**
   - Commitar as alterações e realizar push para o branch `main`.

---
*Última atualização: novembro 2026*
