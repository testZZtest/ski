# Análise dos repositórios de referência (PLANO.md) — 2026-11-17

Levantamento feito via GitHub API + READMEs. Destino: escolher o que implementar no Ski.

## Status dos repositórios

| Repo | Estado | Nota |
|---|---|---|
| 8crsk/openclaw-android | ativo (Kotlin, 20★) | gateway on-device, risk graduado, audit log |
| agentsdroid/agentsdroid | **não existe** | remover do PLANO.md |
| AIOSAI/AIPass | muito ativo (319★) | memória semântica entre agentes |
| aohp-os/MobileClaw | semi-inativo | quase tudo já no Ski |
| bandada-labs/agent-ddp | diário (Rust) | **diff pós-ação compacto** |
| callstack/agent-device | muito ativo (5420★) | snapshots token-eficientes, refs estáveis |
| Core-Mate/OpenGUI | ativo (1812★) | relatório de execução com evidências |
| Drevero/Dots-hermes | ativo | **approvals via notificação** |
| droidrun/mobilerun | muito ativo (10060★) | tools mobile-nativos bem fatorados |
| duOCELOT/pestinha-duOCELOT | ativo | só estética da bolha |
| FoneClaw-AI/foneclaw-android | ativo | catálogo de 120+ ações, i18n |
| getpaseo/paseo | muito ativo (24221★) | orquestração paralela (conceito) |
| Hy4ri/hermes-mobile | ativo (470★) | painel remoto via REST/WS |
| JMAN730/opendroid | ativo (1293★) | **habit detection, re-evaluation** |
| KYRIE66nb/mobile-agent | ativo | **percepção dupla, safety gate, recipes** |
| Mangi-11/Eta | muito ativo (2947★) | **tela virtual + desvio automático** |
| memex-lab/memex | ativo | journal tangencial |
| morluto/rea | muito ativo | REA (reversa) |
| Nido007-cmyk/Nido | alpha | memória SQLCipher + Keystore |
| oroplex/pony | ativo | pareamento QR/relay E2E; nota: sem Shizuku, display oculto só roda apps próprios |
| SimoneAvogadro/android-reverse-engineering-skill | ativo (8739★) | **skill que extrai HTTP APIs de APKs** |
| stablyai/orca | muito ativo (127k★) | frota de agentes (conceito) |
| techjarves/Mobile-Harness | ativo | similar ao Linux do Ski |
| TecnicoBot/mobile-agent | renomeado → TecnicalBot/mobile-agent | corrigir URL |
| wangzan101/Mika | muito ativo (8914★) | **trabalha em tela oculta em background** |
| wangzhishou/OneBox | ativo (1192★) | **robot em PiP**, per-tool timeouts |

## TOP 10 ideias priorizadas

1. **Tela virtual em background com desvio automático** (Eta + Mika + pony) — Fase B do PLANO.md. Android 14+ com Shizuku: VirtualDisplay "trusted"; app em foreground → opera in-place; senão display oculto isolado (foco/teclado/áudio não roubados). Maior ganho de UX e paralelismo. Pré-requisito: Shizuku (já existe no Ski).
2. **Diff pós-ação compacto** (agent-ddp) — tap/type/scroll retornam ~1 linha ("new/gone/changed" + refs estáveis @eN) em vez de árvore/screenshot. Corta tokens e latência do loop. + replay de fluxo sem modelo.
3. **Snapshots de acessibilidade token-eficientes** (agent-device) — read_screen denso (só texto/refs/estados relevantes) + tap_by_ref estável entre chamadas.
4. **Habit & Routine Detection** (opendroid) — minerar sequências de apps (UsageStatsManager) e oferecer rotinas; replanificar passo que falhou; Compound Intent Guard.
5. **Semantic safety gate + audit log** (mobile-agent + FoneClaw + openclaw) — níveis de risco por ferramenta (leitura → livre; escrita externa → confirmar; pagamento → bloquear); auditoria on-device; "sempre permitir/só desta vez" com memória.
6. **Recipes: 1 execução → template → replay sem LLM** (mobile-agent + agent-ddp) — serializar ações bem-sucedidas com seletores semânticos; replay determinístico com fallback para o agente.
7. **Robot em Picture-in-Picture** (OneBox) — tarefa longa + usuário saiu → PiP nativo (zero permissões) com progresso e retorno em 1 toque. Complementa a bolha.
8. ~~**APIs nativas via engenharia reversa**~~ — **DESCARTADA por decisão do usuário**: ele nunca usa o Ski em apps bancários, e a solução escolhida para o bloqueio dos bancos é o Modo Banco (desligar o serviço de acessibilidade de verdade via Shizuku), não contornar a detecção pela API do app.
9. **Personal context via Providers do sistema + read-back** (Eta) — ferramentas somente-leitura: notificações 7d, usage stats, SMS/código (com gate); toda escrita relê o estado real e reporta sucesso/falha.
10. **Memória criptografada + busca semântica** (Nido + AIPass) — vault com chave no Keystore; embedding local leve além de tag/id; Live Updates (Android 16) para status na lockscreen.

## Correções no PLANO.md
- `agentsdroid/agentsdroid` — não existe mais: remover.
- `TecnicoBot/mobile-agent` — correto: `TecnicalBot/mobile-agent`.

## O que já foi portado do upstream Buddy (2.3.6 → 2.3.11)
Veja commits `7636710` e `38b6890`: aprovações por chat, expiração de aprovação,
proteção pós-card, senhas nunca digitadas, 1ª pergunta por app novo, troca de agente
com handover, detecção de helper morto, tokens por run no MCP, mirrors de download,
WorkNotice, helper v25 (AGENT_END/traps/quiet_end), backup endurecido.
