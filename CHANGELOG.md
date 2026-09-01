# Changelog

OwnTV Mobile is the phone and tablet app. It is versioned independently of the OwnTV TV app and of
the core library — a `v0.x` here lines up with neither. Release tags are plain `v0.1.0`, and the
release workflow reads this file to write the release notes, taking the section whose heading matches
the tag.

## v0.1.0 — unreleased

The first build of the mobile app. There is no user-facing app yet: this release exists to prove the
pipeline that will ship one — the repository, the build, the signing key, the translations and the
release automation.

### 📦 The app builds and installs

- **A Material 3 phone and tablet app**, built on the same core library as the TV app, so the
  database, playlist sync, EPG, backup, profiles and the playback engine are shared code rather than
  a second implementation.
- **Two ABI builds**, `standard` for phones (arm64-v8a and armeabi-v7a) and `x86_64` for the
  emulator, signed with the same key as the TV app so both apps come from one certificate.
- **A development harness screen** stands in for the real app while it is being built. It is not
  translated and will be removed.
- **The app shell**: a bottom bar of five tabs on a phone and a navigation rail on a tablet, which
  swap over as the screen turns. Tabs for content you do not have are hidden, by the same rule the
  TV app uses, and each tab keeps its own place in its list when you leave it and come back.

### 🌍 It speaks 26 languages on day one

- **Every user-visible string comes from the core library**, already translated, so the mobile app
  inherits all 26 packaged languages without carrying a single string file of its own.
- **Four translation checks run in CI** — an inventory that refuses new hardcoded text, a check that
  numbers and dates are formatted for the reader's language, a check that no label is long enough to
  be cut off, and a check that the built APK really contains every language it claims.

### 🤖 Releases are automated

- **Pushing a version tag builds, signs and publishes the release**, with the notes taken from this
  file.
- **A new core library version opens a pull request here automatically** and merges itself once the
  build is green, so the two apps never drift onto different core versions.
