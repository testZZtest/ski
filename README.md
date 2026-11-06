# Ski

**An AI agent that lives in a bubble on your Android phone.**

Type or say what you want — *"reply to Sam that I'm running late"*, *"add a Switch 2 to my Amazon cart"*, *"turn on the bedroom lights"* — and Ski reads the screen, taps, types and scrolls to get it done. It asks you before anything is sent, posted, bought or deleted.

Ski is a fork of [Buddy](https://github.com/ghorbelhamdi/buddy-android) (Apache-2.0) with its own package identity (`ski.agent`), so it installs side by side with the original app and updates only from this repository.

- **Android 11+, 64-bit**
- **No root, no adb, no PC**
- **Runs on your own plan**: Claude Code, Codex, OpenCode, Antigravity or Grok, installed inside the app

## What it can do

| | |
|---|---|
| **Uses your apps** | Messages, shopping, smart home, settings: anything on screen. You can add instructions while it works. |
| **Asks first** | Approve, Deny or Change before anything is sent, posted, bought or deleted. |
| **Shows you how** | *"Show me how to turn on dark mode"*: it points at each step and waits for your tap. |
| **Acts on notifications** | *"When a customer asks on WhatsApp, reply with our opening hours"*, with your OK or a countdown you can stop. |
| **Voice** | Talk to it and have replies read aloud. |
| **Background browser** | Pages load in a browser inside Ski without taking over your screen, even while the phone is locked. It marks what you must do (sign in, a captcha) and never types your passwords. |
| **Builds apps** | With Linux tools on, *"make me a calculator app"* builds a real APK on the phone; the GitHub build installs it for you. |
| **Find by meaning** | `find_and_tap` finds an element by text, content description or view id, scrolls a list until it appears and taps it in one call (`tap=false` only locates it). Password values are masked in every screen read. |
| **Numbered legend** | `read_screen_with_numbers` overlays 1, 2, 3… on every element and `tap_by_number N` taps it, refusing a number from an older screen. `ocr_screen` covers apps that hide their UI. |
| **Chat apps** | `chat_send` and `chat_read` for Telegram, WhatsApp, Discord, Slack, Weixin, Lark, QQ and Zulip: open the contact, type, tap Send (with your OK) and read the conversation back. |
| **Phone tools anywhere** | The same tools as a local MCP server for your own agent sessions, or over stdio with [mcp-bridge](mcp-bridge). |
| **Remote control** | `McpHttpServer` exposes the phone tools over HTTP on `127.0.0.1:8766`, off by default. |
| **Web & memory** | `web_search`, `web_fetch`, `translate`, and facts and preferences remembered across chats. |
| **Status** | Battery, network, storage, memory, local time, ringer: what an agent needs to know about the phone. |
| **Shizuku** | `shizuku_run` and friends for an elevated shell without root, after you grant Ski permission. |
| **14 languages** | Strings in `res/values-<language>/strings.xml`; community skills and plugin APKs via Skills. |

## How it works

```
Ski app / bubble ──► Ski's Linux (Alpine via proot): claude, codex, opencode, agy or grok
        ▲                                                     │
        └──────────── streamed replies ◄──────────────────────┤
                                                              ▼
Ski accessibility service ◄──── MCP on 127.0.0.1 (phone tools)
```

Ski is a normal Android app with an accessibility service, the kind screen readers use. The agent runs in a small Linux inside the app and calls Ski's phone tools (read the screen, tap, type, scroll, open apps) over a local MCP server. No adb, no wireless debugging, and it works without Wi-Fi.

## Install

You need a 64-bit Android 11+ phone, plus an account for the agent you pick (Claude, ChatGPT, Google or X). OpenCode's free models need no account.

1. Download the latest `ski.apk` from [Releases](https://github.com/testZZtest/ski/releases/latest) and open it.
2. Follow the setup guide that opens on first launch: pick your agent, turn on the accessibility service, install and sign in, then try a first task.

Ski is not on Google Play (the store does not allow apps that use accessibility to carry out tasks), so the APK is installed by hand. If Play Protect blocks it, see [Setup in detail](docs/GUIDE.md#setup-in-detail).

Because the package is `ski.agent`, this build can coexist with the original Buddy app. It does not see Buddy's chats or settings, and Buddy does not see Ski's.

## Updating

Install the new APK over the old one: chats, settings and sign-ins stay (don't uninstall first). You can also ask Ski to *"update yourself"*, or let [Obtainium](https://obtainium.imranr.dev) watch this repository ([how](docs/GUIDE.md#updating)).

Updates must be signed with the same key as the installed build. Keep your signing key backed up; if it is lost, the only way to update is to uninstall and install again.

## Security

An accessibility service is powerful, so please read this.

- **Local only.** The phone tools listen on `127.0.0.1` with a random per-install token.
- **Ski enforces the approvals, not the agent.** Messaging, shopping and money apps start on a hard limit: Ski itself stops on buttons like Send, Pay and Delete until you OK them.
- **Prompt injection is the main risk.** A message or web page could try to give the agent orders. On-screen text is treated as content, and the approval card is the backstop, so read it.
- **No passwords.** Ski never types passwords, codes or payment details; it hands those to you.
- **What leaves the phone:** only what the agent sends to its provider to answer you.

More in [Security in detail](docs/GUIDE.md#security-in-detail). Found a problem? Please use [private vulnerability reporting](https://github.com/testZZtest/ski/security/advisories/new) instead of a public issue.

## Building

### On the phone, in Termux (no Gradle)

```bash
pkg install openjdk-17 aapt2 d8 apksigner python
# put android.jar from platform-36 at ~/android-sdk/android-36/android.jar
./build.sh                     # → build/ski-release.apk
```

`build.sh` compiles resources with `aapt2`, Java with `javac`, dexes with `d8`, packs the native libraries, aligns and signs with `apksigner`. On first run it creates a release key at `~/.ski-keys/release.jks` and its password next to it: back both up, since updates must use the same key.

### On a computer (Android Studio or Gradle)

Open the project in Android Studio, or from a terminal with JDK 21 and the Android SDK 36:

```bash
./gradlew assembleGithubRelease      # app/build/outputs/apk/github/release/
./gradlew bundlePlayRelease          # Google Play bundle, no self-update
```

Release signing is read from the environment: `SKI_KEYSTORE`, `SKI_KEYSTORE_PASSWORD`, and optionally `SKI_KEY_ALIAS` (default `ski`) and `SKI_KEY_PASSWORD`. Without them the APK is unsigned and Android will not install it.

### On GitHub Actions

Every push and pull request builds the app and runs the helper tests. Pushing a tag such as `v2.5.0` builds the release APK and attaches it to a GitHub release, both as `ski-v2.5.0.apk` and as the fixed `ski.apk` link that the in-app updater and Obtainium use.

The workflow signs the APK when these repository secrets exist:

| Secret | Value |
|---|---|
| `SKI_KEYSTORE_BASE64` | `base64 -w0 ~/.ski-keys/release.jks` |
| `SKI_KEYSTORE_PASSWORD` | the contents of `~/.ski-keys/password` |

Without them the APK is unsigned, so the release step is skipped and the build is only kept as a workflow artifact.

## Project layout

```
app/src/main/java/ski/agent/   the app: UI, accessibility service, tools, MCP servers
app/src/main/res/              layouts, drawables and every string (14 languages)
app/src/main/assets/linux/     the helper scripts installed into Ski's Linux
linux/                         sources of those scripts + tests
native/                        proot, talloc and the pty helper, built from source
mcp-bridge/                    stdio bridge for MCP clients that start servers as commands
docs/                          user guide and privacy policy
```

## Credits and license

Ski is a fork of [Buddy](https://github.com/ghorbelhamdi/buddy-android) by Hamdi Ghorbel. The original design, the Linux integration and most of the code are his; this fork changes the app identity and adds the improvements listed above.

[Apache-2.0](LICENSE). Geist fonts: SIL Open Font License. Icons: Material Symbols (Apache-2.0). Bundled proot (GPL-2.0), talloc (LGPL-3.0) and libandroid-shmem (BSD-3-Clause): see [third_party](third_party/README.md) for licences and source.
