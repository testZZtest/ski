# Ski phone bridge (stdio)

**This needs the free Ski app on your Android phone: [download the APK](https://github.com/testZZtest/ski/releases/latest/download/ski.apk) or see [Ski on GitHub](https://github.com/testZZtest/ski).** Ski lets AI agents read your screen, tap and type, and asks before anything risky. No root, no adb, no PC.

Ski's phone tools (read the screen, tap, type, scroll, open apps, screenshots, notifications, the background browser…) are a local MCP server inside the Ski app on your Android phone: `http://127.0.0.1:8765/mcp`. Most agents connect to it directly; Ski's **Settings → Phone tools in your own sessions** gives the command for Claude Code, Codex, OpenCode, Antigravity and Grok.

This bridge is for MCP clients that can only start a server as a command. It speaks MCP over stdio and passes every request on to Ski. It has no dependencies (Node 18+).

```bash
BUDDY_TOKEN=<token from Ski's Settings> npx -y buddy-phone-bridge
```

Or from a copy of this folder: `BUDDY_TOKEN=<token> node bridge.mjs`.

MCP client config:

```json
{
  "mcpServers": {
    "phone": {
      "command": "npx",
      "args": ["-y", "buddy-phone-bridge"],
      "env": { "BUDDY_TOKEN": "<token from Ski's Settings>" }
    }
  }
}
```

- `BUDDY_TOKEN`: the token Ski shows (with or without `Bearer `).
- `BUDDY_URL`: Ski's address, if not `http://127.0.0.1:8765/mcp`.

Run it on the phone itself (Termux, for example): Ski only listens on `127.0.0.1`. When Ski can't be reached, the bridge still lists the tools (from `tools.json`, a copy of Ski's list) and each call says why it couldn't run; that's also what happens in the Dockerfile's container, which has no phone.

Ski is an independent project, not made by or affiliated with Anthropic, OpenAI, Google or xAI.
