<div align="center">

# Ski

**An AI agent that lives in a bubble on your Android phone.**

[![Release](https://img.shields.io/github/v/release/testZZtest/ski?style=for-the-badge&label=release)](https://github.com/testZZtest/ski/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/testZZtest/ski/total?style=for-the-badge)](https://github.com/testZZtest/ski/releases)
[![CI](https://img.shields.io/github/actions/workflow/status/testZZtest/ski/build.yml?style=for-the-badge&label=build)](https://github.com/testZZtest/ski/actions/workflows/build.yml)
![Android](https://img.shields.io/badge/Android-11%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)

**[Download the APK](https://github.com/testZZtest/ski/releases/latest/download/ski.apk)** · [User guide](docs/GUIDE.md) · [Report an issue](https://github.com/testZZtest/ski/issues)

</div>

---

Type or say what you want — *"reply to Sam that I'm running late"*, *"add a Switch 2 to my Amazon cart"*, *"turn on the bedroom lights"* — and Ski reads the screen, taps, types and scrolls to get it done. It **asks you before anything is sent, posted, bought or deleted**.

Ski runs on your own plan: [CC], Codex, OpenCode, Antigravity or Grok, installed inside the app. No root, no adb, no PC.

Ski began as a fork of [Buddy](https://github.com/ghorbelhamdi/buddy-android) (Apache-2.0) with its own package identity (`ski.agent`), so it installs side by side with Buddy and updates only from this repository. The original design and most of the code are Hamdi Ghorbel's; this fork adds the features listed below.

## Highlights

| | |
|---|---|
| **Works in the background** | The agent can work on a hidden display of its own while you keep using your phone — your screen, keyboard and Back button stay yours. A step that needs you, like a sign-in, comes to your screen. |
| **Asks first** | Approve, Deny or Change before anything is sent, posted, bought or deleted. Money apps, settings and stores always ask. Approvals expire, and an OK for sending a message never covers a Pay or Delete button that comes after. |
| **Bank mode** | Banks refuse to run while any accessibility service is on. Bank mode turns Ski's service off for real, with one tap, and one tap brings it back — the banks then see no accessibility service at all. |
| **Shows you how** | *"Show me how to turn on dark mode"*: it points at each step and waits for your tap. |
| **Long-task card** | Leave the app while the agent works and the chat shrinks to a picture-in-picture card with the run's status; tap it to come back. |
| **Acts on notifications** | *"When a customer asks on WhatsApp, reply with our opening hours"*, with your OK or a countdown you can stop. |
| **Voice** | Talk to it and have replies read aloud. |
| **Background browser** | Pages load in a browser inside Ski without taking over your screen, even while the phone is locked. It marks what you must do (sign in, a captcha) and never types your passwords. |
| **Builds apps** | With Linux tools on, *"make me a calculator app"* builds a real APK on the phone and installs it for you. |
| **Finds by meaning** | `find_and_tap` finds an element by text, description or view id and taps it in one call. Numbered overlays and OCR cover apps that hide their UI. Passwords are never typed by the agent and are masked in every screen read. |
| **Chat apps** | Telegram, WhatsApp, Discord, Slack, Weixin, Lark, QQ and Zulip: open the contact, type, tap Send (with your OK) and read the conversation back. |
| **Phone tools anywhere** | The same tools as a local MCP server for your own agent sessions, or over stdio with [mcp-bridge](mcp-bridge). |
| **Web & memory** | `web_search`, `web_fetch`, `translate`, and facts and preferences remembered across chats. |
| **18 languages** | See [docs/GUIDE.md](docs/GUIDE.md) for the list and how to help. |

## How it works

```
Ski app / bubble ──► Ski's Linux (Alpine via proot): claude, codex, opencode, agy or grok
        ▲                                                     │
        └──────────── streamed replies ◄──────────────────────┤
                                                              ▼
Ski accessibility service ◄──── MCP on 127.0.0.1 (phone tools)
```

Ski is a normal Android app with an accessibility service, the kind screen readers use. The agent runs in a small Linux inside the app and calls Ski's phone tools (read the screen, tap, type, scroll, open apps) over a local MCP server. No adb, no wireless debugging, and it works without Wi-Fi. Background mode is the one exception: it pairs with the phone's own Wireless debugging once, so the agent can work on a hidden display — the setup walk is in the app.

## Install

You need a 64-bit Android 11+ phone, plus an account for the agent you pick (Claude, ChatGPT, Google or X). OpenCode's free models need no account.

1. Download [`ski.apk`](https://github.com/testZZtest/ski/releases/latest/download/ski.apk) and open it.
2. Follow the setup guide that opens on first launch: pick your agent, turn on the accessibility service, install and sign in, then try a first task.

Ski is not on Google Play (the store does not allow apps that use accessibility to carry out tasks), so the APK is installed by hand. If Play Protect blocks it, see [Setup in detail](docs/GUIDE.md#setup-in-detail). You can also let [Obtainium](https://obtainium.imranr.dev) watch this repository ([how](docs/GUIDE.md#updating)).

Because the package is `ski.agent`, this build can coexist with the original Ski app. They don't see each other's chats or settings.

## Updating

Install the new APK over the old one: chats, settings and sign-ins stay (don't uninstall first). You can also ask Ski to *"update yourself"*.

Updates must be signed with the same key as the installed build. Keep your signing key backed up; if it is lost, the only way to update is to uninstall and install again.

## Security

An accessibility service is powerful, so please read this.

- **Local only.** The phone tools listen on `127.0.0.1` with a random per-install token. Every run gets a token of its own that stands for that chat and dies with it.
- **Ski enforces the approvals, not the agent.** Messaging, shopping and money apps start on a hard limit: Ski itself stops on buttons like Send, Pay and Delete until you OK them. An approval expires after five minutes, and one given only for sending never covers a Pay, Buy or Delete button that comes after.
- **After you approve, the tap is re-checked.** If the screen changed while the card was up, the step doesn't land on something else.
- **No passwords.** Ski never types passwords, codes or payment details; it hands those to you, and their text is never kept in the run's step log.
- **Banking apps are out of scope.** Ski never works in a bank app. If your bank refuses to run while Ski is installed, use Bank mode (above): it turns Ski's accessibility service off until you bring it back.
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

### Continuous integration

Every push to `main` (and every pull request) runs two jobs: the **helper tests** (ski-run's translation of each agent's output, checked on recorded runs) and the **APK build** (native libraries from source, then the release APK). The build summary shows the version, the APK's size and whether it's signed.

On `main`, the built APK is published as a plain file to the repository's `dist` branch:

> **https://github.com/testZZtest/ski/raw/dist/ski.apk** — always the newest build, no zip around it.

Pushing a tag such as `v2.6.0` also attaches the APK to a GitHub release, as `ski.apk` (the fixed name the in-app updater and Obtainium use) and `ski-v2.6.0.apk`.

The workflow signs the APK when these repository secrets exist:

| Secret | Value |
|---|---|
| `SKI_KEYSTORE_BASE64` | `base64 -w0 ~/.ski-keys/release.jks` |
| `SKI_KEYSTORE_PASSWORD` | the contents of `~/.ski-keys/password` |

Without them the APK is unsigned: the `dist` branch and artifacts still get it, but a tag's release step is skipped (an unsigned APK can't install over a signed one).

## Project layout

```
app/src/main/java/ski/agent/   the app: UI, accessibility service, tools, MCP servers
app/src/main/res/              layouts, drawables and every string (18 languages)
app/src/main/assets/linux/     the helper scripts installed into Ski's Linux
linux/                         sources of those scripts + tests + the android-app skill
native/                        proot, talloc and the pty helper, built from source
mcp-bridge/                    stdio bridge for MCP clients that start servers as commands
docs/                          user guide and privacy policy
```

## Credits and license

Ski is a fork of [Buddy](https://github.com/ghorbelhamdi/buddy-android) by Hamdi Ghorbel. The original design, the Linux integration and most of the code are his; this fork changes the app identity and adds the improvements listed above.

[Apache-2.0](LICENSE). Geist fonts: SIL Open Font License. Icons: Material Symbols (Apache-2.0). Bundled proot (GPL-2.0), talloc (LGPL-3.0) and libandroid-shmem (BSD-3-Clause): see [third_party](third_party/README.md) for licences and source.
