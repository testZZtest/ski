---
name: app-api
description: Find an app's own HTTP API when its screens can't be automated (a banking app that blocks accessibility, an app that detects overlays). Decompile the APK, extract the endpoints (Retrofit/OkHttp/Ktor), and call the service over curl instead. Use when the phone tools are blocked or refuse to act in a specific app.
---

# Using an app's own API when its screen can't be automated

Some apps — banking apps especially — refuse to work while an accessibility service is on, or detect and block
automation. The app's screen is one door; most such apps talk to a server over HTTPS, and that door stays open.
This skill finds that door and uses it, with the user's OK at every step that moves money or data.

Everything runs in Ski's built-in Linux. `SKILL_DIR` means this skill's folder:
`$HOME/.ski/work/.claude/skills/app-api`.

## 1. First: ask, and only look at what the user asked for

- Tell the user what you'll do: "This app blocks automation on its screen. I can look inside the app for its own
  internet interface and use that instead. OK?"
- Never do this for an app the user didn't name. Never read, store or print their credentials for it.
- The user signs in themselves, always (see §5). You never type their password, token or PIN anywhere.

## 2. Find the APK

```sh
pm path <package>        # through phone_shell (background mode on): the APK's path(es)
# or, without background mode: ask the user for the app's name and try
find /data/app -maxdepth 2 -name "base.apk" 2>/dev/null | grep -i <name>
```

`pm path` needs the shell user (background mode set up). Copy the APK somewhere readable:
`cp <path> ~/work/<pkg>/base.apk`.

Split APKs: several paths come back (`base.apk`, `split_config.*.apk`). The base has the code.

## 3. Decompile and find the endpoints

The toolchain script installs what's needed (about 60 MB the first time):

```sh
bash $SKILL_DIR/setup-tools.sh
```

Then:

```sh
# strings first — cheap and often enough:
strings base.apk | grep -E 'https?://[a-z0-9.-]+' | sort -u | head -50
```

That shows the domains. For the real endpoints (paths, methods, headers):

```sh
d2j-dex2jar base.apk -o app.jar 2>/dev/null || jadx -d out/ base.apk
grep -rE '@(GET|POST|PUT|DELETE)\(' out/sources/ | head -40      # Retrofit annotations
grep -rEo '"(/[a-z0-9_/.-]{3,60})"' out/sources/ | sort -u | head -60
```

Retrofit interfaces name the endpoints (`@GET("accounts/v2/balance")` on a base URL like
`https://api.bank.example/`); OkHttp callers build them by hand. Ktor/Flutter apps keep routes in strings or
assets (`grep -rE '"/api/' out/resources/assets/`).

Read the file(s) you found the annotations in — not a fishing expedition. Note: base URL, endpoint paths,
methods, required headers (`Authorization`, `User-Agent`, `X-App-Version`…), and whether bodies are JSON.

## 4. Try one read-only call

Pick the most harmless endpoint (a version/config/health one) and try it:

```sh
curl -sS -m 15 -H 'User-Agent: <the app's own UA>' '<base><path>' | head -c 2000
```

- 200 with JSON: the API talks without signing — note the shape and go on to §5.
- 401/403: it needs the user's token (§5) or the app signs its requests (HMAC, dated headers). Look for the
  signing code (`grep -rE 'HmacSHA|HMAC|signature' out/sources/ | head`). If signing needs a secret baked into
  the app, extracting it is between the user and the app's terms: report what you found and let them decide.
- Certificate pinning blocks curl only when the app itself runs; curl talks to the server directly, so pinning
  doesn't apply. If the server demands mTLS, stop and say so — that's not usable from here.

## 5. Sign in — the user's hands, always

The user signs in on the real app (or a web login you point_out on their screen). Then the session's token is
usually in the app's private storage, which Ski can't read (and shouldn't). Practical routes, in order:

1. **The user captures the token themselves**: in the app's web version, devtools → the Authorization header.
   They paste it to you (or into a file `~/work/<pkg>/token`, which is wiped when you're done — say so).
2. **A refresh token in a file the user exported.**
3. Nothing readable: report that this app needs the screen after all, or a manual export each time.

Never ask the user to paste a password. Token only, into a file, and delete it when the task ends
(`shred -u ~/work/<pkg>/token`).

## 6. Do the task, confirm the moving parts

- Read-only calls (balance, statements, listings) run with the token, one at a time, printing only what the user
  asked for. Mask anything sensitive in what you show (accounts, documents).
- Anything that moves money or data (a transfer, a payment, changing a limit) goes through `confirm` with the
  full request body in the card — the user sees exactly what would be sent — and runs only after APPROVED.
- Verify each write: read the state back after the call and report what the server said, not what you expected.
- Log the least possible: no tokens, no full account numbers, in the chat or in files.

## 7. Clean up

```sh
shred -u ~/work/<pkg>/token 2>/dev/null
rm -rf ~/work/<pkg>/out    # the decompiled sources (big, and not needed again)
```

Keep the notes file (`~/work/<pkg>/API.md`: base URL, endpoints, quirks) so the next conversation doesn't
decompile the app again.
