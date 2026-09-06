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
- **Building the trending row shows its progress in detail.** That one job can spend minutes on a
  single stage, so the strip at the top names the stage and the numbers moving through it — the only
  sign it is working rather than stuck.

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
- **Find a category by typing.** A playlist with four hundred folders makes the chip strip along the
  top useless on a phone, so the search button beside it opens a list you can filter by name.
- **A channel with no logo still looks like a channel.** Plenty of playlists carry logo links that
  have gone dead, and the app used to leave a blank square where the picture should be — for as long
  as the list was open. Now a channel whose logo is missing *or* simply will not load shows a small
  television symbol instead, everywhere: the channel list, the guide, Home and the player.

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
  download, move it, move it to another folder, delete its downloaded subtitles, film details, play
  the trailer, fetch the details again, set the name used to look them up, remove from history and
  hide — in the order you arranged them in Settings, on either app.
- **Film details in a sheet**: the picture, the year, the rating, the length, the full description
  and the cast, taken from the same place the television takes them and honouring the same choice
  about whether your provider's text or the online text wins.
- **The trailer opens in whatever plays YouTube on your phone**, which is a better player than one
  built into a television app.
- **Press and hold an episode too** — download it, play it in another app, mark it watched, see its
  details, fetch them again, or delete the subtitles you downloaded for it. The television has had
  this; the phone now has the same six.
- **Tap one to open it**, with its picture, year, rating, length and description, and a **Resume**
  button that starts where you stopped — the same place the television stopped, because both apps
  share it.
- **A show opens on the season of the last episode you watched**, with a bar under each episode
  showing how far through it you are.
- **A show tells you where you are in it.** A **Next up** card at the top starts the episode you owe,
  each season chip carries how many of its episodes are finished, a tick marks the ones that are, and
  the one you watched last is labelled.
- **Hide the episodes you have finished**, and **sort seasons and episodes oldest or newest first** —
  the same choice the television makes, stored in the same place, so a show reversed there opens
  reversed here.
- **Films play in the same player as live television**, in the mini bar or full screen, with all the
  same controls and gestures.
- **Hiding and sorting live behind their own button.** They used to sit as a row of chips between
  *Next up* and the first episode, where they read as part of the resume card and pushed the episode
  list off the screen. They are now behind a button beside Download, at the top of the show, with the
  episode list starting where the list should start.

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
- **When the trending row is missing, Settings says why.** Layout → Home names the actual reason —
  metadata turned off, a playlist with no films or shows, a sync that has not run yet, or too few
  matches to be worth a row — and offers to build it again there and then, instead of leaving a blank
  space with nothing to act on. The television and the phone read the same rows, so they can never
  give you two different answers.
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
- **Auto-match EPG fills a guide that arrived empty.** Providers name the same channel differently in
  the playlist and in the guide feed, so the guide comes out blank even though the data is there.
  One press matches them by name: the certain ones are applied straight away, and the rest are
  offered for review with the app's confidence beside each — "BBC One HD → BBC One · 88%" — to accept
  or skip one at a time or all at once. Programmes appear as soon as a match is accepted.
- **Order the guide the way the television does**: A–Z, by provider, live channels, catch-up channels
  or your favourites only.
- **The guide says what it is holding**: how many channels have programmes, how many programmes there
  are, and how many channels offer catch-up.
- **It tells you when the guide belongs to a different provider.** If the feed's channel ids match
  none of yours, the guide says so instead of showing empty rows with no reason.
- **No guide feed at all now offers to add one**, with a button that opens straight onto the EPG
  sources page.
- **Press and hold a channel in the guide** to match it to a guide channel yourself, or to shift that
  one channel's programmes when a provider hangs two time zones off a single guide.

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
- **The controls are their own piece of glass.** The buttons sit on two soft-cornered panes over the
  picture instead of loose on a flat gradient, the play, skip and seek buttons share a capsule, the
  shading behind them is dark where the text is and fades out over the film rather than dimming the
  whole frame, and the channel's logo sits on a small plate at the top. Scope *Player controls* and
  *On-screen messages* into the Glass Effect and both panes gain the lit edge, the highlight and the
  shadow the rest of the app has.
- **Hold a control to be told what it is.** The tool buttons are square and unlabelled so more of the
  film shows; press and hold one and it grows sideways into its name, tap it to use it. Subtitles,
  picture size and sound-only stay labelled, because those three have no gesture of their own.
- **The seek bar is readable in the light theme too.** Anything drawn over the picture now uses the
  accent as it looks on black, so a light theme no longer put a dark seek bar on a dark scene.
- **A seek bar made for a thumb.** The bar is thick enough to grab and swells while you hold it, the
  part already downloaded is shaded ahead of the playhead, and a bubble rides above your finger with
  the time you are about to land on and how far that is from where you were — so a long drag is a
  decision rather than a guess. Dragging across the picture feeds the same bubble, because the
  gesture and the bar are one instrument.
- **Live television has an instrument panel.** A channel on a provider with an archive now shows the
  clock and how far back you are watching, a *Now* card with a progress line — and, held sideways,
  what is on next — a red dot that pulses at the live edge and turns amber when you are behind it,
  and a timeline marked with where each programme starts. Scrub it and the bubble names the
  programme you are scrubbing to, not just a time.
- **Every gesture draws itself.** Volume and brightness are a rising bar on the side of the screen
  you are touching, zoom is a frame showing what is being cropped or fitted, double speed is a run of
  travelling arrows, and a skip is a ripple where you tapped with the seconds beside it. The phone
  taps back when a gesture crosses a step — a volume notch, a brightness notch, a skip firing — so
  you can feel the change without looking at the number.
- **The small-window button is the app's own small player.** It used to hand the picture to the
  system's floating window and drop you on the home screen, which is not what a button inside the app
  should do. It now moves the picture into the app's own bar or floating box, per the *Mini-player*
  setting — and that setting no longer offers **Off**, because the button has to open something.
- **Back means finished.** Going back from a channel's page stops the stream instead of leaving it
  running as a bar over the list you just returned to. The small player now appears only when you ask
  for it.
- **Sound only is one tap, not a mode you get stuck in.** Choosing it drops the picture for what you
  are watching now; the next channel or film comes back with its picture, as it always should have.
  In the bar, where the picture would be, a moving equaliser sits over the channel logo, so a
  sound-only stream reads as playing rather than stopped — and those bars keep moving even with
  **Reduce animations** on, because on a screen with no picture they are the only thing saying the
  sound is still coming.
- **Full screen always has a picture.** Sound only lives in the bar at the bottom, which is where its
  button sends it; there is no full-screen sound-only mode to end up in by accident. Coming back to
  full screen from the quick panel, from a closed floating window or from an expanded bar turns the
  picture back on rather than showing a black rectangle with sound.
- **Sound only and the small-window button always land somewhere you can see.** Pressing either from
  a channel's own page used to leave you back on that page with no picture, no bar and no window,
  because the app hides the small player wherever the stream is already on screen. Both now step back
  past the channel to a screen where the small player is visible.
- **Closing the floating window stops everything.** Leaving the app with the picture full screen puts
  it in the system's floating window (with **Picture-in-Picture** on); close that window and the sound
  stops with the picture, rather than carrying on invisibly. The controls stay in the quick panel, so
  pressing play there picks the stream back up as sound only — and opening the app from those controls
  brings the picture with it.
- **The little window is the shape of what you are watching.** It takes the picture's own proportions
  the way the system's floating window does, so a wide film fills a wide window instead of sitting in
  black bands, and it always shows the whole frame rather than inheriting the picture size you chose
  for full screen. It carries more buttons too: ten seconds back, play and pause, ten seconds forward
  and close on a film or episode — and on a live channel, where there is nothing to skip through, a
  button back to full screen.
- **The player's tool bar is now the television's, complete.** Favourite what you are watching
  without leaving it — a channel, a film, or, for an episode, the show it belongs to — jump into
  catch-up, drop the picture, and report a stream that is broken.
- **Go back to an earlier time, from the player.** On a channel whose provider keeps an archive, the
  catch-up button offers the last few hours as plain clock times; **Choose exact time…** opens a day
  strip and a clock face for anything further back. Pick a moment outside what the provider actually
  keeps and it is pulled to the nearest one it does, rather than failing silently. The same list is
  at the foot of a channel's own page, under **Go back to…**.
- **Subtitles, all the way.** Search OpenSubtitles for the film or episode you are watching and
  download one in a tap; or pick a subtitle file off the phone. Subtitles that are pictures rather
  than text are named as such instead of appearing as a blank entry. If they run early or late,
  nudge them in half- and tenth-of-a-second steps, and do the same for sound that drifts from the
  picture — with **Remember this delay**, so the same channel opens correct next time.
- **The next episode counts itself in.** Thirty seconds before an episode ends, a card offers **Play
  now** or **Cancel**, exactly as on the television.
- **Tune straight to a channel number.** Type it into the channel list and go. If nothing has that
  number, if more than one channel does, or if it will not open, it says which.
- **When playback fails, the panel says what it was trying to play** — the format, the size and
  whether it was being decoded by the chip or by software — so a failure is diagnosable instead of
  just red.
- **Stream information scrolls.** It is the longest table in the app, and half a screen never held
  it; the bottom rows, the stream address among them, were simply unreachable.
- **A fling inside any pop-up sheet no longer crashes the app.** Flicking a long list — the catch-up
  times were the first long enough to flick — could close the app outright.

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
- **The big settings open as pages of their own.** Content & metadata is three rows — Customize,
  Metadata and OpenSubtitles — instead of one page with everything on it, and Playback splits the
  same way, with the video player's own settings grouped the way the television groups them.
- **Tidy up your channel and film lists, the way the television can.** Customize now opens a folder
  to show what is inside it, and every folder and every item can be renamed, hidden, moved up, down,
  to the top or to the bottom, or moved into another folder. Sort a section A–Z or by provider, show
  only what is hidden, make a folder of your own, decide whether folders a new playlist brings in
  arrive hidden or visible, and lock the whole page behind a PIN so nobody undoes it.
- **Fix a whole block at once by pressing and holding.** Press and hold a row, choose to select a
  span, then tap the row at the other end: everything in between is hidden, shown, moved or renamed
  together.
- **Rename hundreds of films in one pass.** Build rules — remove or add text, before, after or
  anywhere — or use Auto cleanup to strip country and provider tags, quality and codec tags and
  emoji in one press. A review list shows every old name beside its new one, and you can accept or
  reject them one at a time or all at once; a name that would come out blank or duplicated is
  refused, and Restore originals puts everything back.
- **Clear the film-details key and server** in one press to go back to the shared service, and the
  app tells you once when the day's share of that shared service has run out, instead of letting
  posters quietly stop appearing.
- **Sign in to OpenSubtitles from the phone**, see how many downloads are left today and when the
  count resets, restrict searches to one language, and delete the subtitle files already on the
  phone — one of them, all the films', all the shows', or all of them.
- **The video player page now carries every row the television has**, in the television's own order:
  Engine & picture, Live TV, Sound, Subtitles, Episodes, Diagnostics. Hand a stream to another app,
  show or hide channel numbers, and jump into subtitle appearance without going back a level.
- **The switches there can be pinned to Quick under the same names the television uses**, so a
  pinned list taken from a television lands on the same rows on the phone.
- **Auto frame rate asks before it is turned on** below Android 12, where the display cannot be
  asked which refresh rates it reaches without blanking the picture — keep it off, or turn it on
  anyway.
- **Shrinking the interface past the safe point asks first**, because a screen holding many more
  items at once can run a small-memory device out of memory.
- **Any subtitle colour, not just the five presets**: type six hex digits and the app checks them
  before using them, and one press puts size, colour, position and background back to default.
- **Search reaches the settings inside those pages too**, and tells you the whole path to a result,
  so "Live latency" is found as Playback › Video player › Live latency and one tap lands on it.
- **Reorder your Quick toggles.** Press and hold one and move it up or down; the order you set on the
  phone is the order the television shows.
- **Your playlists have a page of their own.** Every playlist with its type, which one is the
  default, when it expires and how often it refreshes — and on each: edit it, test that it still
  connects, refresh it, refresh it and drop titles the provider no longer has, or delete it after a
  warning that says what goes with it. A refresh in progress shows its counts as it runs and can be
  stopped.
- **Guide feeds have a page of their own too.** Add or edit an XMLTV address — or fill it in from a
  playlist that carries one — give it a user agent if the provider needs one, choose how often it
  refreshes, and say whether its channel logos should be used. Each feed shows how many channels,
  programmes and catch-up channels it brought in, or what went wrong.
- **Catch-up settings**: follow the phone's clock or set the provider's offset by hand, and choose
  whether a catch-up programme asks, plays in OwnTV, or opens in another app.
- **A frosted glass look, made for the phone.** Set a background picture and the app's panels, bars,
  cards and sheets become panes of frosted glass over it: the picture is genuinely blurred behind
  them, each pane has a lit edge and a highlight along its top, and the panes pick up a little of the
  picture's own colour so they belong to it instead of sitting on it. A new **Aurora** look is the
  one to try first. Devices older than Android 12 cannot blur behind a panel, so there the glass is
  see-through and edge-lit without the frost.
- **Glass Effect has a page of its own**, with a live sample at the top that changes as you drag.
  Six looks to pick from, a switch for each part of the app you want it on — bars, panels, cards,
  sheets, the mini player, the player controls and the messages that flash over the picture —
  sliders for see-through, blur and edge light, shadows on or off, a background picture you choose
  with the phone's own picker, and one press back to Balanced.
- **The glass has depth now, not one recipe everywhere.** Things that float above the app — dialogs
  and the messages that flash over the picture — are the lightest and cast the deepest shadow; the
  bars, the mini player and the player controls are a firmer, quieter pane; panels and the preview
  sit further back; and a poster or a row inside a panel is only a whisper of glass, because glass
  inside glass on top of glass is mud. Layers nested inside each other lighten as they go in, so a
  sheet on a panel on a background is still three readable steps and not one grey block.
- **Glass reacts to your finger.** Press a card, a row or a chip and its pane brightens and settles
  under the touch instead of only changing colour, and lets go when you do. With **Reduce
  animations** on, the change is instant rather than removed, so nothing becomes invisible.
- **The app draws its own icons.** Every icon in the app is drawn by OwnTV rather than taken from
  Google's set, on the same grid as the television's, so the two apps look like one product — and
  the app carries no icon library at all, which is a smaller download. Arrows that should turn round
  in a right-to-left language do; the play, skip and rewind buttons do not, because a timeline runs
  the same way in every language.
- **Things arrive instead of appearing.** Panes now settle in with a soft band of light travelling
  across them once, the background picture drifts a fraction against the pane in front of it, and a
  poster you tap grows out of the grid into the top of the film's page instead of the two pictures
  crossfading. Screens change with a short fade rather than the long one the system does by itself.
  With **Reduce animations** on, all of it is genuinely absent — nothing moves, nothing fades and
  nothing slides, including menus closing, jumping a long list back to the top, and the switches and
  rows that used to animate whatever the setting said.
- **Menus and pickers are real frosted glass at last.** Every press-and-hold menu and every picker
  now opens inside the app's own window, so it blurs the same background picture the rest of the app
  blurs — before, they were a separate window and could only be flat. They can be dragged to half
  height or full height, they settle where you throw them, the back gesture shrinks and fades the
  sheet under your finger, and the keyboard no longer covers a rename field. The dialogs that are
  still true dialogs blur what is behind them on Android 12 and newer, and stop doing it while
  battery saver is on.
- **Anything you type into rises above the keyboard.** Every sheet with a text field — adding a
  playlist, adding an EPG address, renaming, searching for subtitles — now lifts itself and scrolls
  the field you are in into view when the keyboard opens, so you can see what you are typing instead
  of typing underneath it. A sheet opened at full height also stops short of the status bar rather
  than running under the clock.
- **"Fill from playlist" fills the address in.** Adding an EPG source and picking one of your
  playlists used to close the picker and do nothing at all; it now puts that playlist's guide address
  into the field, ready to save.
- **One press-and-hold menu, the same everywhere.** A film on Home, a show in the library, a channel
  in Live TV and a result in Search all open the same menu with the same actions in the same order,
  instead of four menus that each knew about a few of them.
- **Settings rows sit on plates now.** Rows that belong together share one rounded panel with the
  heading above it, so a long page reads as a handful of groups rather than one endless list, and
  switches and sliders are the app's own rather than three different shapes on three pages.
- **Full screen really is full screen.** The picture now fills the whole panel in both directions,
  under the notch and behind the bars, so there is no strip of the background picture along the top
  or the side when you turn the phone sideways.
- **Sideways, the top bar lines up with the tab rail.** Held horizontally the bar used to start a
  little further left than the rail and the page below it, so the three left edges did not agree.
- **Sound only is a bar, not a page.** Dropping the picture no longer takes over the screen: what is
  playing collapses into a slim bar with a moving wave where the video was, so you can keep browsing
  with the sound running and bring the picture back with one tap.
- **Fonts have a page of their own.** The interface font and size, and — new on the phone — the font
  and size used inside menus and sheets, with a sample that changes as you choose.
- **Weather has a page of its own, and can use where the phone actually is.** Turn it on and the
  forecast follows the phone instead of guessing from the network, which on a phone that travels is
  the difference between your weather and your provider's. It asks only for approximate location,
  only when you turn it on, and typing a city by hand still works.
- **Any accent colour, not just the presets**: type six hex digits and the app checks them as you
  type.
- **Everything is stored where the TV app stores it**, so a setting changed on the phone and backed
  up arrives on the television when you restore it there.
- **The language picker is a page you can search.** All 26 languages, each in its own script with the
  language's own name beside the English one, how complete its translation is, and a search field —
  type "de", "Deutsch" or "German" and you land on the same row. Following the system language is
  still the top choice.
- **Help translate, from inside the app.** The language page offers a link to the translation site,
  a QR code for anyone with a second device, a way to copy the address, and a way to ask for a
  language that is not there yet.
- **About tells you what the app is**, with the version, what it does and does not do, the licence,
  who has contributed, a link to the source code and a link to the Telegram group with a QR code
  drawn from the address itself, so the code and the link can never disagree.
- **Clearing history asks what to clear** — live, films, shows, or all of it — instead of taking the
  lot, and uses the same wording as the television.

### 📱 It behaves like a phone, not a television

- **The settings that a phone does not have say so.** The Playback page ends with one line
  explaining that the live preview and the remote-control shortcuts are television features, so a
  setting you remember from the TV app is accounted for rather than just absent.
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
  screen turns off, and both are settings you can turn off.
- **Press home while watching and the video shrinks into a floating window**, with buttons for pause
  and for jumping ten seconds back or forward. Tapping the window brings the app back; closing it
  stops playback. **Back can do the same thing** if you turn it on — otherwise Back leaves the player
  and drops what you were watching into the app's own little window instead.
- **A little window you drag around the app.** What is playing follows you while you browse, in a
  window you put wherever it suits you: drag it and it settles against the nearest edge, tap it for
  pause and close, double-tap to go full screen, pinch to make it small, medium or large, swipe it
  down to stop, and hold it for favourite, sound only, a sleep timer, full screen and close. If you
  prefer the old bar above the tabs, or nothing at all, both are settings.
- **A screen for listening without watching.** Turning the picture off leaves the artwork, what is
  playing, the volume up to 150 % and a sleep timer that stops the stream in fifteen minutes to an
  an hour and a half — or at the end of the programme. One button brings the picture back. Channels you
  listen to rather than watch are remembered, and there is a setting that turns the picture off by
  itself on mobile data.
- **Ten seconds back and forward from the notification**, and a ♪ button there that drops the picture
  without opening the app.
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
