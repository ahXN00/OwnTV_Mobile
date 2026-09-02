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

### 📺 Live TV

- **The channel list**, with the categories along the top as chips: every channel shows its number,
  its logo, what is on now and what is next, and how far through the current programme you are. A
  star marks a favourite and a clock marks a channel whose past programmes can still be watched.
- **Channel numbers, hidden channels and your own channel order are the ones you already set** — the
  same settings the TV app reads, not a separate copy for the phone.
- **Press and hold a channel for its menu**: favourite, rename, match it to the right guide, shift
  its guide by a few minutes, catch-up, play in another app, move it, move it to another folder, and
  hide it. They appear in the order you arranged them in Settings, on either app.
- **Renaming, moving and hiding are shared with the TV app.** A channel renamed on the phone is
  renamed on the television, because both write to the same place.
- **Tap a channel to watch it**, with the picture at the top of the screen and, underneath it, what
  is on now in full, what follows it, and the other channels in the same folder to switch between.
- **Catch-up** opens a list of programmes the provider still keeps, newest first; pick one to replay.
- **Pull the list down to refresh it**, which runs the same playlist sync the TV app's manual refresh
  runs.

### ▶️ The player

- **Tap the picture to fill the screen.** The player turns to landscape by itself when you open it,
  then hands the orientation back, so you are never stuck sideways if you would rather hold the phone
  upright. The controls fade away after three seconds and one tap brings them back.
- **Everything is a button as well as a gesture.** Skip back and forward by your own step, play and
  pause, the seek bar, volume with the same boost above 100% the TV app has, brightness, subtitles,
  picture size, speed, the player engine and the stream information table are all in the control bar
  — the gestures are a shortcut, never the only way in.
- **The gestures**: double-tap the left or right side to skip, slide up and down on the left for
  brightness and on the right for volume, drag across the middle to scrub, pinch to fill or fit the
  picture, press and hold to run at double speed, tap with two fingers to mute, swipe up for the
  channel list and swipe down to send the video to the small bar at the bottom.
- **Live television rewinds.** On a channel whose past programmes the provider keeps, the red bar at
  the bottom drags backwards into the archive and a **Go live** pill brings you back to now.
- **A mini player** sits above the tabs while you keep browsing: tap it to go back to full screen,
  swipe it down to close it. A radio station shows its logo instead of a black rectangle.
- **When playback fails, it says why** — in your language, with the same wording the TV app uses, and
  a retry button.

### 📱 It behaves like a phone, not a television

- **A call pauses the film and hanging up resumes it.** Anything that takes the sound away for a
  moment — a call, a navigation prompt, a voice assistant — pauses playback and gives it back
  afterwards. If you paused it yourself while the call was going on, it stays paused: your choice is
  the newer one. The television still ducks the volume instead of pausing, because pausing live
  television loses the live edge.
- **Pulling out headphones stops the sound** instead of throwing it out of the loudspeaker, and
  plugging them back in does not start it again by itself.
- **Playback controls on the lock screen and in the notification shade**, with the channel name, what
  is on, the logo, and play, pause and a seek bar for anything that can be seeked.
- **It keeps playing when you leave the app.** The picture is dropped and the sound carries on, so a
  radio station or a match you are only listening to costs almost no battery; coming back to the app
  brings the picture straight back without restarting the stream. The same thing happens when the
  screen turns off.
- **Press home while watching and the video shrinks into a floating window**, with buttons for pause
  and for the previous and next channel. Tapping the window brings the app back; closing it stops
  playback.
- **The screen stays awake while there is a picture**, and is allowed to sleep when there is not.

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
