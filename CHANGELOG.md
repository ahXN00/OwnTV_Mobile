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

### 🔌 Getting your own playlist in

- **The app asks for a playlist the first time it opens**, and offers two ways in: add a playlist, or
  restore a backup you made on the television.
- **All three kinds of playlist**: an Xtream account, an M3U address, and a MAG / Stalker portal with
  its MAC address and the device presets that go with it.
- **An M3U file already on the phone** can be picked with the phone's own file picker instead of
  typing an address.
- **A backup from the TV app restores everything** — playlists, favourites, what you have watched,
  your channel order and your settings. An encrypted backup asks for its password and lets you try
  again if it is wrong.
- **The import shows what it is doing** while it runs, counts what it found, and cleans up after
  itself if it fails, so a failed attempt never leaves half a playlist behind.

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

### 🎬 Films and shows

- **Movies and Series**, on a phone as two tabs of one screen and on a tablet as two entries in the
  side rail, with the categories along the top as chips.
- **Your whole catalogue scrolls**, however large it is — it is loaded a page at a time as you reach
  it, so opening the app is no slower with a hundred thousand films than with a hundred.
- **Sort it your way** — by name, by date added, by year or by rating — and switch between a poster
  grid and a list.
- **Pinch to make the posters bigger or smaller**, or use the size slider; the app remembers the size
  you chose.
- **Press and hold a film or a show for its menu**: favourite, play in another app, mark as watched,
  download, remove from history and hide, in the order you arranged them in Settings.
- **Tap one to open it**, with its picture, year, rating, length and description, and a **Resume**
  button that starts where you stopped — the same place the television stopped, because both apps
  share it.
- **A show opens on the season of the last episode you watched**, with a bar under each episode
  showing how far through it you are.
- **Films play in the same player as live television**, in the mini bar or full screen, with all the
  same controls and gestures.

### 🏠 Home

- **The same Home as your television.** The rows you arranged there appear here in the same order,
  and the ones you hid stay hidden — it is one setting, kept with your profile, not two.
- **A card at the top for what you were last watching**, with a bar showing how far in you were and a
  button that carries straight on. Swipe sideways for the ones before it. A live channel gets the
  same card and takes you to the channel.
- **Rows for films and shows you have started**, each poster carrying its own progress bar.
- **Your favourite and recently watched channels**, either as logos to pick from or as a list of what
  is on each of them right now — tap the switch above the row to change your mind, on either device.
- **What is trending**, but only the titles your own playlist actually has.
- **The weather at the top**, in °C or °F, exactly as you set it on the television.
- **Nothing yet?** Home says so and offers to add a playlist.

### 📅 The guide

- **Three guides, and the app remembers which one you like.** *On now* is a list of every channel
  with what is playing, a bar showing how far through it is, and what is on next — it is what a
  phone held upright can actually read. *Grid* is the television's guide: the channel names stay
  pinned down the left while the programmes slide sideways, all the rows moving together. *Timeline*
  is one channel's whole evening read top to bottom.
- **A day at a time**, with the days along the top; today opens at the half hour you are in rather
  than at this morning.
- **A button that jumps the grid back to now**, however far you have scrolled.
- **Search the channels and filter them by category**, with your favourites as their own chip.
- **Tap a programme** for its description, and from there watch the channel, replay the programme
  from the start when your provider keeps an archive, or favourite the channel.
- **A size slider** makes the grid show more of the evening at once, or less of it larger.
- **It stays quick on a big playlist**, because the guide is read one row at a time as that row
  reaches the screen instead of loading the whole lineup.

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

### 🔍 Search

- **One field searches everything.** Channels, films and shows at the same time, from the magnifying
  glass in the top bar of whichever screen you are on — and back returns you to exactly that screen.
  Results are grouped by kind, each group saying how many it found.
- **It searches as you type**, waiting a moment for you to stop rather than firing on every letter,
  and it is the same search the TV app runs, so the same word finds the same things on both.
- **The provider is on every row** when you have more than one playlist loaded, so two channels with
  the same name are told apart.
- **Press and hold a result** for its full menu — favourite, download, hide and the rest — without
  opening it first.
- **It remembers what you searched**, and offers those terms again the moment you open the field.
- **When the field is empty it offers somewhere to start**: what you were watching, favourites you
  have not watched yet, and your channels.

### ⬇️ Downloads

- **A downloads screen with Active, Completed and Failed**, a progress bar on every item, and pause,
  resume, retry and delete on each one.
- **How much room is left and how fast the queue is moving**, at the top, so a long download over a
  slow connection is not a mystery.
- **Download to the SD card.** You pick the storage, not a folder, so the app needs no file
  permission at all — nothing to grant, and nothing left behind on the card if you uninstall.
- **Watch a finished download with the network off**, in the same player everything else uses, from
  where you stopped.
- **Save a copy anywhere** through the phone's own save dialog, once a download has finished — into
  Downloads, onto a memory card, or into cloud storage.

### ⚙️ Settings

- **Every setting the TV app has, in ten groups**, each its own page rather than one endless list:
  Profile, Sources & guide, Appearance, Layout, Content & metadata, Playback, Network, Data and App.
- **A search box at the top of Settings.** Type "subtitle", "buffer" or "backup" and the matching
  settings appear with the page they live on, so nothing is lost behind two taps of drill-down.
- **Quick toggles at the top**, for the handful of switches worth reaching in one tap.
- **Choices open as a sheet from the bottom of the screen**, not as a dialog in the middle — the
  theme, the accent colour, the text size, the player for live television, and every other
  "one of these" setting.
- **The appearance page shows what it will look like while you choose**, so a theme, an accent and a
  text size are picked by looking rather than by guessing and going back.
- **Settings for a phone, that the television has no use for**: keep playing in the background,
  Picture-in-Picture, how far a swipe travels, and a data saver that stops streaming on mobile data
  until you say otherwise.
- **Downloads over Wi-Fi only**, on by default, so a queue left running cannot spend a data
  allowance while you are out.
- **Per-playlist playback overrides.** One provider that only works on the other player, or needs a
  bigger buffer, can have its own setting without changing the rest.
- **Forget what the player remembered**, in one place: pinned players, saved zoom, saved volume and
  saved audio delay, each with a count of how many titles it applies to and a confirmation.
- **Check that your film details are actually working**, by looking a title up from the settings
  page and seeing what comes back; the shared service also shows how much of its allowance is left.
- **Everything is stored where the TV app stores it**, so a setting changed on the phone and backed
  up arrives on the television when you restore it there.

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
