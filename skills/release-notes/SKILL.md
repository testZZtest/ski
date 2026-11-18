---
name: release-notes
description: Draft release notes from the commits since the last tag.
---

# Release notes

1. Run `git log --oneline $(git describe --tags --abbrev=0)..HEAD` to list the changes.
2. Group them: new features, fixes, internal changes.
3. Write one line per change, in plain language a user understands.
4. End with a one-sentence summary suitable for a store listing.
