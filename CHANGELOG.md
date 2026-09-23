# Changelog

OwnTV Mobile is the phone and tablet app. It is versioned independently of the OwnTV TV app and of
the core library — a version here lines up with neither. Release tags are plain `v1.0.0`, and the
release workflow reads this file to write the release notes, taking the section whose heading matches
the tag.

## Unreleased

### ✨ New features

#### 👄 A/V sync works on every player

The **A/V sync** control in the Audio panel (and the Settings default) now also works on ExoPlayer —
the player live channels usually open on — and on films that play with ExoPlayer. **Remember**
keeps it for that channel or film.

#### ⏮️ Previous channel

Once you have watched two channels, the player bar has a **Previous channel** button that jumps back
to the one before — tap again to flip back. "Previous" on headphones or the media notification does
the same on a live channel (it did nothing there before).

#### 🎞️ Auto frame rate now works

Turned on, full-screen video asks the screen for a refresh rate that suits it — only where the switch
is seamless, so a 90/120 Hz phone can drop to 50 or 60 Hz without flicker. It used to do nothing.

#### 🧹 Reset live player choices, forget learned stream fixes

Playback → Video player has **Reset saved live TV player choices** and **Forget learned stream
fixes**. What the player learns about a provider's catch-up now expires by itself after 14 days.

#### 🕒 Catch-up time zone in quarter hours

The catch-up time zone moves in 15-minute steps, and the per-playlist list offers the real half- and
quarter-hour zones (+05:30, +05:45, −03:30 …).

#### 🎚️ Four more settings per playlist

Each playlist can now keep its own **catch-up time zone** (Settings → Sources & guide, next to
Catch-up), its own **Movies & Series player** and its own **Give up after** time (Settings →
Playback → Video player, under the global rows). A playlist left on "Follow setting" behaves exactly
as before.

#### 🔗 Referer for a playlist that needs one

The Add / Edit playlist form — the first-run wizard included — has an optional **Referer** box under
User-Agent. Whatever you enter is sent with that playlist's channels, films, episodes, catch-up, cast
and the external player. A stream that names its own Referer keeps it.

#### 🗣️ The player remembers your audio and subtitle language

Pick an audio track or a subtitle in the player and that channel, film or whole series opens on the
same language next time — subtitles off is remembered too. Each profile keeps its own choices.

#### ▶️ Catch-up carries on to the next programme

With **Auto-play next** on, a catch-up programme that ends goes on to the next one in the guide — or
back to the live channel once you have caught up with what is on now — exactly as on the TV.

### 🩹 Fixes

#### 🎞️ Deinterlacing is automatic

The Deinterlacing setting is gone: the player smooths interlaced video by itself wherever it draws
the picture itself — the only place that setting could ever work.

#### 🌈 HDR says it is mpv only

The HDR row reads **HDR (mpv only)** and explains that ExoPlayer always passes a stream's HDR
through. The A/V sync and Movies & Series player descriptions no longer say ExoPlayer can't sync.

#### 📱 Settings speak about your phone, not a television

Auto frame rate and its warning, HDR, hardware decoding, both player descriptions, surround sound,
channel numbers, measured stats, animations, the zoom warning and Multiview's "can't decode another
channel" now say device or screen instead of TV, in every language.

#### 🧭 Per-playlist pickers open their second step

Live TV player, Live latency and Pre-buffer per playlist closed the moment a playlist was chosen, so
nothing could be set; Custom latency now opens its seconds slider too.

#### 📐 The add-playlist form clears the status bar

Its first line sat under the clock.

#### 🧰 Player memory moved into the app's database

Channel and film player choices, sound-only marks and audio delays now live in the database, move
over by themselves on the first start, travel in backups, and are deleted with their playlist.

#### 🌐 Older language codes match

A track tagged "ger" or "fre" now counts as German or French, both for your preferred language and
for the remembered one.

#### 📺 Channels you set to ExoPlayer on the TV open on ExoPlayer here too

The phone only ever read the "play on mpv" choices, so a channel switched to ExoPlayer on the
television still opened on mpv on the phone. Both directions are honoured now.

#### 🛠️ Diagnostics, catch-up and restored player settings now work on the phone

Turning on Detailed diagnostics now actually records a log; catch-up programmes that need software
decoding are remembered between runs; and a Live latency or frame-rate choice brought over by a
restore or sync takes effect instead of reading as the default.

#### 🔐 Restoring or syncing from another device keeps this phone's playback setup

Backups now record which device made them. A backup or sync from the television no longer copies
its player engines, hardware decoding, frame-rate matching, HDR, surround and
per-channel player choices onto the phone unless you tick "Hardware settings from the other device".

#### 👥 A restore never hands one person's data to another profile

Data belonging to a profile that is not on this phone is skipped instead of landing on whoever has
the same profile number, and one bad value in a backup no longer stops the whole restore.

#### 📱 Multiview no longer closes when the screen times out

Multiview did not keep the screen on, so the phone's own timeout turned it off mid-match — and
turning the screen off closes the grid. The screen now stays on for as long as the grid is showing.

#### 🧠 Memory pressure reaches every player

When the phone runs short of memory the app now gives some back from every player — the live
channel and Multiview tiles as well as films — instead of from none of them.

#### 🔎 Settings search finds more settings

Hardware decoding, Channel numbers, Measured stream stats, Autoplay next, Detailed
playback logging and every row of Recording and Subtitle appearance can now be found by name.
Volume, audio sync and audio language are also found by words like "lip sync" or "loud".

#### 💾 Backups keep Multiview and recording settings

Multiview on/off, its tile count and your "I understand", and all the recording settings are now in
a backup.

#### 🌐 Custom DNS actually works — and all three presets pass

Custom DNS answers were never read correctly, so the app quietly used the phone's normal DNS. That is
fixed, and the Google, Cloudflare and Quad9 presets now all pass their Test. A proxy or custom DNS is
in force from the very first stream after the app starts.

#### 🔋 The player does less work

The player screen and mini player no longer redraw everything every second, the LIVE dot and skip
arrows animate without rebuilding the screen, the sound-only bars stop when paused, live subtitles
are only set up when switched on, and the notification picture is loaded small.

#### 🔁 Quick channel picks always end on the last one

Tapping one channel and then quickly another now always ends on the second. Channel + / − in the
floating window waits until you stop pressing before it opens a stream, so a busy one-connection
provider does not lock you out. Live TV's channel handling is now the same code the TV app runs.

#### ⏪ Catch-up from the Guide is never overtaken by the live channel

Picking a programme in the Guide could start the channel's live stream on top of the replay.

#### 🚫 A channel that cannot open leaves the screen as it was

When a channel was refused — data saver, a profile's adult filter, or a portal that did not answer —
its name still replaced the one you were watching. Now nothing changes, and you can tap it again.

#### ⏩ Double tap on live uses your Live rewind step

The double tap on a live channel jumped by the films' seek step. It now uses **Settings → Live rewind
step**, like the TV's buttons.

#### 🔗 Portal channels recover when their link expires

A Stalker (MAC portal) channel that dropped in the middle of playing kept retrying its old link. It now
fetches a fresh one and carries on.

#### 🧩 Multiview tiles follow "Prefer HLS"

A Multiview tile now opens the same address full screen would, including a playlist's Prefer HLS choice.

#### ⚙️ The first channel after starting the app uses your settings

A channel opened straight after the app starts could open with default player settings because they
had not been read yet. It now waits for yours.

#### 🎬 Films with subtitles off keep the picture clean

A film played on ExoPlayer with subtitles switched off kept an empty subtitle layer over the picture.
It is only there now while subtitles are on.

## v1.0.2 — 2026-09-21

### 🩹 Fixes

#### 💥 The app would not open after updating from v1.0.0

Updating straight from `v1.0.0` to `v1.0.1` — including through the app's own update sheet — left
the app unable to start: the splash screen appeared and the app closed again, every time, because
the one-off upgrade of the stored guide data could not finish and was retried on every launch.
Nothing was wrong with the data — the upgrade step asked SQLite to begin a save point that was
already open, which the newer storage engine in `v1.0.1` refuses. The upgrade now runs as one piece
and completes on the first launch, with your profiles, playlists, favourites and history untouched.

A phone already running `v1.0.1` is affected only if it never managed to open it. One that opened
`v1.0.1` once has already finished the upgrade and was never at risk.

## v1.0.1 — 2026-09-21

### ✨ Set a new phone up from the device you already have

- **"From another device" on the very first setup screen.** Setting up a new phone no longer means
  finishing setup and then hunting through the menus for Local sync: the first screen now offers it
  beside "New profile" and "Restore backup", and copies your profiles, playlists, favourites and
  history straight over the Wi-Fi from the OwnTV device you already have. Turn Sync mode on over
  there first (More → Local sync); the new phone finds it on the network, or you scan its code, then
  choose what to bring across, see exactly what will change, and confirm.
- **You still choose what comes over.** The direction is not a question — a phone being set up can
  only receive — but the tick list is the same one Backup & Restore uses, so you can take the
  playlists and leave the old device's settings behind.

### 📅 A week of guide, and you choose how much

- **"Guide days to keep" in Settings → EPG Sources.** The app used to store two days of upcoming
  guide and discard the rest, however much your provider sent — so the guide ran dry two days after a
  refresh. It now keeps as many days as you ask for, seven by default, up to a fortnight. One number
  decides how much is downloaded, how much is kept, and how many days the strip above the guide
  offers.
- **The day strip follows it.** It always showed exactly seven days, whatever the setting said.
- **Old programmes are kept only where they can be replayed** — catch-up channels keep their full
  archive, including ones you matched to a guide by hand; everything else keeps six hours.
- **Guide refresh can be set to "every N days",** like a playlist's. Anything already chosen is
  untouched.

### ⏭️ Next and previous episode, from the player itself

- **Two new buttons beside play.** Reaching the next episode used to mean waiting for the card that
  appears in the last thirty seconds, or leaving the picture, finding the series and picking the
  episode by hand — so skipping the end credits cost four taps and a trip out of full screen. The
  player now carries **Previous** and **Next**, exactly where the television has always had them.
  They appear only where they mean something: never on a film, never on a live channel, and no
  "next" on the last episode of a series.

### 🔗 Setting a phone up from another device, and choosing what a backup brings back

- **Pairing with the television did nothing at all.** On the first setup screen, "From another
  device" → pick the television → type its PIN → **Pair** dropped straight back to "Set up OwnTV"
  as though nothing had happened. The pairing had in fact succeeded on the television; the phone
  threw the result away. The cause was the keyboard, which is always open at that moment because the
  PIN has just been typed into it: a sheet that opens while the keyboard is up is told to rise twice
  at once, and the losing animation was mistaken for the sheet being closed by hand — so the panel
  that should have asked "what to receive" was dismissed in the frame it appeared, and setup read
  that as "go back". Hiding the keyboard first used to be the only way through. Every sheet in the
  app was exposed to this; the wizard is where it cost the most.
- **First-run "Restore backup" asks what to restore.** It used to take the whole file, always —
  while Settings → Backup & Restore and the local-sync step have always offered the tick-list. Now
  the same list appears before anything is applied, so a restore can bring the playlists and leave
  the old device's settings behind. Fixed in core (`core-1.0.52`), so the television gained it too.
- **A sync that fails on an unreadable file says so**, rather than "Something went wrong". A failure
  also leaves something in the log now — it previously left nothing at all, which is why this took a
  device to find.

### 📺 Protected and MPEG-DASH channels play

- **Channels delivered as MPEG-DASH now play, protected ones included.** Some providers publish a
  channel at an address that gives no hint of its format and only reveals it once the app asks. The
  app could only ever guess between two formats, so these channels were opened the wrong way and
  stopped with a format error before any picture appeared — and because a protected channel can only
  run on one of the two players, there was nothing left to fall back to. It now recognises the format
  three ways: from what the playlist says about the channel, from what the provider actually sends
  back, and from what other channels on the same provider have already turned out to be. Reported by
  a user running the app on a tablet alongside the television.
- **Stream info said MPEG-TS on a DASH channel.** The Format line only had two possible answers, so
  the third format was labelled as the wrong one — even on a channel that had failed to start, which
  is what people were sending in with their reports. It now shows HLS, DASH or MPEG-TS to match what
  is really being played.
- **A channel that will not open can now fall back to the provider's own address.** Where a playlist
  supplies one, it is tried once as the very last resort — after every other recovery step, at the
  point where the alternative is an error screen. Channels that work today are unaffected: this
  address is never used first, because providers frequently publish one that only works inside their
  own network.
- **Recording a copy-protected channel is refused straight away, and says why.** It used to try, save
  a file that could not play, and reconnect to the provider for the entire length of the programme —
  holding one of your allowed connections the whole time. The recording now stops immediately and
  explains that the protection comes from the provider rather than from OwnTV.

### 📡 The guide keeps downloading when the screen goes off

- **A large guide can finish.** The phone froze OwnTV about thirty seconds after the display dimmed
  and cut its network with it, so the download died part-way — and the next time the app was opened
  it started again from the beginning, forever. On a big feed with a short screen timeout it could
  never finish. The guide sync now runs as a proper foreground job with an ongoing notification,
  which is what downloads and recordings have always done. Fixed in core (`core-1.0.52`). If
  notifications are denied the sync still runs; only the notification is missing.

### 🩹 The player's panels stopped hiding their own lower half

- **The frame rate reported for a live channel is no longer a notch too low.** A channel sending 25
  pictures a second could be reported as 24, because the reading was taken over a single second —
  short enough that one frame of slack tipped the answer onto the wrong standard rate. It now
  measures over a longer stretch and insists on the same answer twice before accepting it. Fixed in
  core (`core-1.0.47`). Only the number was ever wrong; the picture was always correct.
- **Audio and subtitle panels scroll.** Held sideways — which is how the player is always held — a
  panel taller than the screen did not scroll and did not grow. It simply stopped drawing, and
  everything below the fold vanished without a trace: the **A/V sync** nudge at the foot of the
  audio panel, and **subtitle timing**, **Search subtitles** and **Select local subtitle** at the
  foot of the subtitle one. All of it had been built and none of it could be reached on a film with
  more than a few tracks. The same fault was quietly clipping four other panels, so **Speed** lost
  its 2.0×, **Aspect ratio** lost Force 4:3, and the sleep timer, catch-up and subtitle-search lists
  were each given half a landscape screen to work with instead of the proper share.
- **The sound button is always on the bar.** It used to hide itself whenever a film carried only one
  soundtrack, which also hid the A/V sync control behind it — and a single-soundtrack film is
  precisely the one whose voices need dragging back into line with the mouths. It now stays put like
  the subtitles button beside it and like the television's, and the panel says for itself what is in
  there.
- **No spinner over a paused picture.** Pressing pause put the loading ring on screen and left it
  turning, because "paused" and "still loading" were being treated as the same thing. The ring now
  means what it says.
- **The status bar stays out of full screen.** The clock, the battery and the navigation bar came
  back over the picture at the first stray swipe near an edge and then stayed there for the rest of
  the film. They are now transient, as they should be — a swipe still summons them, and they leave
  again on their own. Anything that puts them back, the keyboard included, is answered the same way.
- **The fast-forward badge always disappears.** Double-tapping forward several times in a row could
  leave the "+0:30" marker stranded on the picture, still there while the film played normally, and
  only a rewind or a pause would clear it.

### ▶️ "Resume playback" is a setting again

- **The phone asks whether to resume or start over.** Settings has offered **Resume playback** —
  Always, Ask, Never — since the settings screens were built, and nothing in the app ever read it:
  every route into a part-watched film or episode silently jumped to where you left off, so **Ask**
  and **Never** did nothing at all. All three now work, from Home's Continue watching rows, from the
  hero card and from an episode in a series, with the same wording and the same ten-second threshold
  the television uses. The **Resume at 12:34** and **Play** buttons on a film's own page are
  unchanged — they are already the answer to the question, so they do not ask it again.

### 🔎 Search keeps going past the first forty results

- **Search no longer stops at 40 of each kind.** A search asked for the first forty channels, forty
  films and forty shows and then stopped, with no sign that anything had been left out — so a
  provider carrying two hundred CNN feeds looked like it carried forty, and reaching "US: CNN" meant
  guessing at a longer query. Scrolling to the bottom of the results now loads the next forty of
  whatever you are looking at, and keeps doing so until there is nothing left to find. The rows still
  come from the same shared search as the television's, so hidden items stay hidden and renamed
  channels keep their new names.

### 🩹 Empty categories fill, catch-up plays, and Settings search finds everything

- **A category your provider lists no longer arrives empty.** A playlist could offer a category and
  hand over none of its channels, leaving it on screen with nothing inside — an adult category most
  often, because providers filter those out of the "give me everything" request while still listing
  them. The app now notices a category the provider skipped and asks for it directly. On one portal
  playlist that recovered 174 channels that had never been delivered. Fixed in core (`core-1.0.45`),
  and it applies to live channels, movies and series on both portal and Xtream playlists. Kids
  profiles hide those categories automatically, exactly as before.
- **Stalker catch-up plays.** Picking a past programme on a portal playlist did nothing at all — no
  picture, no message. The archive request carried a reference the provider could not resolve, so it
  answered with an empty response and the app gave up in silence. Fixed in core (`core-1.0.43`) and
  confirmed against a real portal. The lookup is faster too: finding the programme used to take up to
  eight requests to the provider and got slower the later in the day it aired; four at the worst now.
- **Catch-up says when it cannot play.** Where "Watch from start" or "Go back to…" cannot reach the
  archive — a provider with no recording behind the channel, most often — the app now says so instead
  of returning to the live picture without a word.
- **A channel starts while a portal playlist is still filling itself in.** A portal playlist hands
  its catalogue over in thousands of tiny pages, so the app keeps fetching them in the background
  long after setup has finished. On a provider that allows only one connection at a time, that
  background fetching was still running when you pressed a channel, and the channel lost the race —
  a spinner, then nothing. The app now tells the engine which playlist is on screen the moment
  playback begins, so the background fetching stands down until you stop watching and picks up
  exactly where it left off. It had been telling it from the wrong place, which worked when a film
  was opened from its own page and never fired at all for a channel started from the Live TV list —
  the common case. Portal playlists only; Xtream and M3U playlists finish in one pass and have
  nothing running in the background.
- **Settings search finds every setting.** Twenty-two settings on the Video player page could not be
  found by name — Multiview, both player engine pickers, the seek and rewind steps, the volume and
  zoom defaults, the language preferences, the per-playlist overrides — and neither could custom DNS,
  the nav bar customization, Keep watching or the start-on-a-channel option.
- **The user guide now describes first-run setup exactly as it happens**, including the "Set up
  OwnTV" screen it used to fold into the profile step. It also says plainly that the phone never
  offers the TV guide after an import — the television does, the phone does not, and a guide that
  described them as identical left people waiting for a prompt that was never coming. Two additions:
  how to use your phone to fill in a playlist on the television, and how to take channel logos from
  an XMLTV feed when the playlist carries none.

### 🗓️ Guide and EPG matching

- **"Match EPG" lists guide channels again.** On a channel with no guide the picker could come up
  empty while the guide itself drew programmes for that same channel: it was filtering by which
  playlist delivered the guide, and nothing else was. It now offers every guide channel the app
  holds, including feeds that list programmes without naming their channels.
- **Searching the picker understands names.** Typing `bbc1` now finds "BBC One"; a lowercase Cyrillic
  or Greek search now reaches an uppercase name, which it never could before.
- **Your manual EPG matches survive deleting and re-adding a playlist.** Each match was remembered
  against the playlist's internal id, so re-importing the same playlist quietly orphaned all of them.
- **Auto-match no longer reports success it cannot deliver.** It skipped channels whose guide id
  existed but held no programmes, and silently applied matches onto empty guide channels; those now
  go to the review list.
- **Duplicate programmes are removed when the guide is downloaded** rather than hidden on every read.
- **The guide keeps less in memory.** It remembered every row it had ever drawn, so scrolling a large
  guide grew without limit; it now keeps a few hundred and forgets the rest.

## v1.0.0 — 2026-09-14

Initial release.

## v0.1.0 — pre-release

The first build of the mobile app. There is no user-facing app yet: this release exists to prove the
pipeline that will ship one — the repository, the build, the signing key, the translations and the
release automation.

### 📺 Live TV behaves like the television's, everywhere the app talks about playback

Live channels moved onto the second player engine shortly before this release. The rest of the app
did not move with them: about twenty-five places still asked the *first* engine what was happening,
and that engine is stopped while a live channel plays, so every one of them was told "nothing is
playing". Each of the following is that same mistake wearing a different coat.

- **The screen no longer goes to sleep during Live TV.** The app keeps the display awake while there
  is a picture — it simply never believed there was one on a live channel, so the phone dimmed and
  locked mid-programme exactly as it would while reading a page. Films and downloads were always
  right, because those genuinely play on the first engine.
- **The notification, the lock screen, the small player and the floating window drive a live
  channel.** Play and pause did nothing at all on one, the notification showed the play symbol over
  a moving picture, and it carried no channel name.
- **Picture-in-Picture opens for a live channel.** Pressing Home while watching one did nothing.
- **Sound only works on a live channel**, and so does dropping the picture when the screen goes off,
  and "Sound only on mobile data", and a channel's own remembered sound-only choice. All four were
  silently skipped, which is the single largest battery and data saving the app has.
- **Rewinding a live channel no longer starts a second stream.** Dragging the rewind bar back — or
  picking a programme from Catch-up while a channel was playing — opened the archive on one engine
  without stopping the other. Two sounds at once, and two of the playlist's connections spent on one
  thing being watched. Casting a live channel had the same fault: the phone went on playing the
  channel beside the television.
- **Re-opening the channel you are already watching no longer restarts it**, and a channel's
  remembered zoom and volume are now filed under one name rather than one per engine.
- **Multiview stops when you leave the app.** Four tiles are four decoders and four of the playlist's
  connections; pressing Home left all of them running for a window nobody could see.

### 🪜 A live channel that will not play now walks the television's full fallback ladder

The phone had half of one: the second engine was watched and its failures went to the first, and the
first was a terminus. So a channel that engine could not open simply sat there, and neither engine
ever retried a channel on its other stream format.

- **Four rungs, each tried at most once** — this engine's other format, then the other engine, then
  *its* other format. That finiteness is the safety property: without it, a handover in each
  direction would bounce a channel between engines for ever. The ordering is core's own
  `LiveLadder`, which the television has used for months and which is unit-tested there.
- **Settings → Video player → Live TV player finally does something**, globally and per playlist.
  "mpv first" now really starts there *and* falls back; "ExoPlayer only" stops after that engine's
  two formats instead of paying for a handover the user has said will not help.
- **Settings → "Give up after" does something.** The phone drew the slider and read it nowhere else.
  It is now a budget for the whole tune, with an alarm that fires *during* an attempt — so thirty
  seconds means thirty seconds to the person watching, rather than thirty seconds before anyone next
  asks the time. When it runs out the app says so instead of showing a spinner for ever.
- **"Prefer HLS", per playlist, does something.** It is what makes an HLS rung differ from a TS one.
- **A panel already caught refusing its own segment URLs is routed straight past the engine it
  refuses**, rather than paying two dead requests and the wait on every further channel. The lesson
  was already being learned by the engine; nothing on the phone had ever read it.
- **Every ladder decision is written to the playback error log**, which you can read, as well as to
  `adb logcat -s LiveEngine`, which you cannot.

### ⚙️ Three settings that were displayed but never reached the player

Each was stored, shown, backed up and synced to the television, and had no effect here: **Live TV
player per playlist**, **Live latency per playlist** and **Pre-buffer per playlist**. All three now
reach both engines, and Multiview's tiles too — a tile is one of that playlist's streams like any
other, which is what the television does.

### 🔤 A subtitle font, and other small repairs

- **Settings → Subtitle appearance has a Subtitle font row.** All six font files were already in the
  app; nothing had ever told the player to use them, so a font chosen on the television and synced
  here was ignored. The row costs no translation — the label already existed in every language.
- **A small thumb slide no longer closes the player.** Any downward drag past about three
  millimetres counted as "shrink me"; a swipe now has to travel a seventh of the screen.
- **The Guide's "On now" keeps meaning now.** Its progress bars were drawn once when the screen
  opened and then stood still, as were the programme ticks on the player's live bar.
- **The floating Picture-in-Picture window takes the picture's shape** instead of a fixed 16:9, and
  grows out of the picture rather than cross-fading into the corner.
- **Unmuting a live channel shows the volume you are actually hearing.**

### 🚀 The app opens faster from cold

A baseline profile now ships inside the APK. It is a list of the classes and methods the app reaches
on the way to its first screen, recorded by touring a real, set-up app: Home and its scrolling rows,
each destination on the bottom bar, and opening one item and coming back. Android reads that list at
install time and compiles those paths ahead of time, so the Compose runtime, Room's query machinery
and Koin's graph resolution no longer run interpreted while somebody waits at a blank screen.

It costs nothing at runtime and changes no behaviour — the same code runs, prepared rather than
worked out on the spot. **One recording serves both ABI builds**, because a profile names code, not
machine instructions.

**It is not known yet how much this helps here.** No release-build start-up measurement has been
taken on a phone, so the profile ships on the general grounds that it cannot hurt, not on a measured
figure. **Re-record it whenever the start-up path changes materially** — a stale profile quietly
stops helping rather than failing a build.

### ⬆️ The app tells you when there is a new version

This app is sideloaded, so until now nothing was ever going to tell anyone a new version existed —
the Releases page only says so to whoever thinks to look at it. The television has had an in-app
updater since long before this app existed, and this is that updater, unchanged: core's
`UpdateManager` state machine, core's strings, core's release-notes rendering.

- **Settings → App → Check for updates** looks now and says what it found.
- **Check updates on startup** (on by default, and the same preference the television stores) checks
  five seconds after the app opens.
- An update offers **What's new** — the release body, which is this repository's `CHANGELOG_APP.md`
  — then downloads and hands the APK to the system installer.

**Where it differs from the television, deliberately.** The television posts a corner toast for
every outcome, including "checking…" and "you are up to date". Here the startup check is silent
unless it actually finds something: those are answers to a question nobody asked, and on a phone
they would land on top of whatever the user opened the app to do. Ask on the Settings row and it
answers every time, including its failures.

**It needs `REQUEST_INSTALL_PACKAGES`**, which this app's manifest previously listed among
permissions it would never declare — the reasoning being that Google Play scrutinises it on a media
player. That reasoning still holds and Play is still out of scope; the permission is declared for
the updater and nothing else, and the manifest says so. If this app ever goes to Play, the
permission and these rows come out together.

### 🔎 Set how big everything is during setup, not after it

A request on the television's tracker (#179) pointed out something true of both apps: the settings
that make the interface bigger could only be found *after* setup, on screens the user had already
struggled to read.

- **The first run now asks on its second screen**, before the disclaimer, which is the first screen
  that is mostly words.
- **Two sliders — UI Zoom and Font size — with a sample sentence beneath them that resizes as you
  drag**, so the size is judged by reading it rather than by picking a number.
- These are the same settings as **Settings → Appearance** and **Settings → Fonts**, so a choice made
  here is simply the app's from then on and can be changed again at any time.
- Zoom offers its whole 50–150% range, and crossing below 85% raises the same low-memory
  confirmation the Appearance page raises — asked once, not on every further drag.

### 🎨 A real colour picker, everywhere a colour is chosen

Every colour setting offered a short list of named colours and a box for a six-character code. That is
fine if you already know the code and useless if you do not — and the selection highlight did not even
have the box, so it was eight fixed colours or nothing.

All three colour settings — **the accent colour**, **the selection highlight** and **the subtitle text
colour** — now open the same picker the television has: a rainbow strip, a saturation-and-brightness
square, a live preview of what you have, and the hex code, which fills itself in as you drag. The
television works its picker with a remote, so its bar and square are enter-to-edit controls; here they
are dragged directly.

The picker sits inside the setting it belongs to, under that setting's presets, so Accent and Selection
highlight are reached the same way. The Accent row also now reports the colour actually in use — a
custom colour overrides the preset, and the row used to keep naming the preset regardless.

### 🏷️ Hide or move a category without leaving the screen

Long-press a category on **Live TV**, in the **Library** or in the **Guide** for Hide and for Move — to
the top, up, down, or to the bottom. Reordering follows the same menu Settings → Customize uses rather
than introducing a second way to do it.

Both routes write the same thing, so a change made here shows up in Settings → Customize and the other
way round. Hiding the category you are currently looking at returns the list to All. The television
gained this from community pull request #131 → #146; underneath, both apps now order their categories
through one shared implementation instead of two that had to agree.

### ♿ The app says what it is doing, not just shows it

- **A screen reader now announces which row or chip is chosen.** A tick and an accent colour say
  "selected" to an eye and to nothing else. Thirteen places were affected — the language list, profiles,
  storage folders, the category picker, every choice sheet, the Customize panes and the subtitle
  colours.
- **The line between list rows starts from the right in Arabic.** It was always drawn from the left, so
  in a right-to-left layout it ran under the icon and stopped short of the text it underlines.
- **Three controls no longer clip their text at a large font setting** — the glass preview, the player's
  speed and engine buttons, and the buttons on the Home hero. They grow instead.

### ⏺️ Record Live TV

Live TV can be recorded, and the recordings live in **Downloads → Live TV**, beside Movies and Series.

- **From the guide**, **from the channel list**, or **from the player while you are watching** — the
  Record button appears on live channels once the setting is on. A recording keeps going when you
  change channel or leave the player.
- **Record every showing** of a programme on a channel, as a standing rule.
- **Catch-up as a recording** — a programme already broadcast, saved from your provider's archive.
- **It costs one of your provider's connections**, and says so before it starts rather than failing
  into a spinner.
- The status pill appears **over the player** while one is running, so a recording is not invisible for
  most of its life.

### 🔲 Multiview — several channels at once

Watch live channels side by side, driven by taps. **Landscape is the real mode**; portrait stacks them
rather than drawing four postage stamps.

- Open it from the player's **Multiview** button, or mark channels with **Add to Multiview** from the
  channel list and then tap any channel — tapping plays straight away while channels are waiting,
  instead of opening the channel page first.
- **The Settings number is a ceiling, not a size.** The grid opens with two and grows only when you
  ask, up to your maximum.
- **Tap** a tile to give it the sound, **double-tap** for fullscreen, **long-press** for its menu:
  change channel, add a tile, sound only, remove.
- **Filling a tile starts at the categories**, across every playlist, with a search box — a flat list
  of every channel is tens of thousands of rows on a phone.
- **A tile never sits blank**: it says whether the provider had no connection spare or this phone could
  not decode another channel.
- **Leaving the grid stops everything**, rather than leaving one channel playing and one connection
  spent.

**Multiview has settings on the phone at last** — the on/off switch and the tile ceiling, under
*Settings → Playback*. Without them the feature could not be switched on here at all.

### 🔌 How many channels your provider allows

Most providers never say. OwnTV now finds out by trying, once, when a playlist is added — before any
channels are saved, so nothing you are watching is interrupted — and warns you *before* refusing a
tile or a recording instead of after.

- A playlist's **Test** is now **Info**; **Re-test** sits inside it, behind a warning that playback
  will stop and that it can take up to two minutes, with **Skip**.

### 📂 Files where you want them

- **Downloads and recordings can be saved to a folder you pick**, through Android's own folder picker
  — the only kind of folder a phone from Google Play is allowed to reach.
- **Export moves a recording or download** into a folder of your own, and keeps working next week
  rather than losing permission with the app's process.
- The storage header shows the **folder's name**, not a raw `content://…` address.
- Downloads tabs are **Live TV / Movies / Series**.

### ▶️ Live TV on the ExoPlayer engine

The phone can now play live on the second engine, with the same watchdogs the television uses — a
picture that never arrives while the audio plays, segment URLs a provider refuses, a stream that opens
and delivers nothing. The engine button in the player HUD applies to live channels, and a channel can
be **pinned to compatibility mode** on its own.

### 📱 Fixes and smaller things

- **"Add to Multiview" appears in the channel menu.** It was written but never shown: the menu only
  draws actions listed in the shared action file, and this one was missing from it — the same omission
  that cost the phone its Record row.
- **Two channels could play their sound at once.** Four tiles starting together each asked "is any tile
  filled yet?" before any had registered itself, so several decided they were first and each turned its
  own sound on. Which tile has the sound is now settled before any stream opens.
- **The channel you tapped kept playing underneath the grid.** It was started full screen and then
  stopped a fraction of a second later — a race the stop lost. Nothing is started that is about to be
  stopped.
- **A tile that could not be played no longer plays its sound.** The picture failing does not stop the
  audio track.
- **A menu or picker opened before its data arrived stayed empty for ever.** A sheet is drawn by a host
  that kept the version it was first given, so a picker opened while its categories were still loading
  showed a search box and nothing else until the screen was rotated.
- **A picker in landscape showed only its search box** — the list was given half a landscape phone's
  height, which is barely one row.
- **The player's channel button goes through the categories**, so a channel in another category or
  another playlist can be reached from the player.
- **An episode download appeared in no list at all.**
- **File sizes say MB again.**
- **The landscape player HUD has its two clusters and the gap between them**, like the television's.

### 📺 Live TV, episodes and profiles say more

- **Upcoming programmes carry their synopsis**, not just a time and a title, on a channel's page.
- **Episodes show the day they first aired** — on the list rows and in the details sheet. The
  provider's own date wins where it sends one, with TMDB's as the fallback.
- **A profile picture of your own**, chosen with the phone's photo picker. It is copied into the app,
  cropped square and scaled down, and it travels inside your backup — restore on the TV and the
  picture arrives with it, on the right person.
- The ten drawn avatars were redrawn with a lit gradient and a soft sheen, matching the television.

### 🗓️ Stalker portals: a guide, and catch-up

- **A portal that publishes no XMLTV feed now has a guide** — the portal's own. It appears in
  Settings → EPG as **"Guide from the portal"**, and **Add EPG source → Fill from playlist** offers
  it and adds it in one tap, since there is nothing to type.
- **Catch-up now appears on portal channels that have it.** The app had been looking for a field name
  that Xtream uses and a MAG portal does not send, so every portal channel looked archive-less.

### 🔎 The Guide stops blaming a missing playlist

- **A playlist that offers two TV guides now sets up both of them (TV #171).** Some playlists name
  more than one guide in their header, separated by a comma — one per country a provider covers. Both
  addresses were taken as a single one and glued together, so the request could only fail and no
  programmes arrived. Each guide is now its own entry, syncing and re-syncing on its own, and **Fill
  from playlist** offers each of them separately.

- **An empty guide now says which of the two things is true.** "Add a playlist to see the guide" was
  shown even with three playlists and eighteen thousand programmes loaded, when the real reason was a
  **Catch-up** or **Favourites** filter matching nothing. It now says the filter is hiding everything.
- **Catch-up is only offered when you have channels to rewind**, the way the television already did
  it — and if it was already your choice on a playlist with no archive, it simply is not applied, so
  you are never left filtered with no way back.

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
- **What is trending**, but only the titles your own playlist actually has — and now as the same full
  card the television shows: the artwork behind it, the poster with its position in the chart, the
  year, rating and quality, the description, the badges saying which version of the title you have,
  and a panel that explains **why** it is there if you tap it open. Play it, open its episodes, watch
  the trailer, read the full details or search for every other version you own. It moves to the next
  title every ten seconds; hold your finger on the card to stop it, swipe to change it yourself.
- **Or keep the simple row of posters.** Layout → Home has a *Trending layout* line — *Detailed card*
  or *Posters only* — so trending can be the big card or just another poster row like the ones under
  it. The detailed card is what you get unless you change it, and the line only appears while
  trending is switched on. **The television has the same choice now**, and the two apps share the
  setting, so picking posters on one picks posters on the other.
- **When the trending row is missing, Settings says why.** Layout → Home names the actual reason —
  metadata turned off, a playlist with no films or shows, a sync that has not run yet, or too few
  matches to be worth a row — and offers to build it again there and then, instead of leaving a blank
  space with nothing to act on. The television and the phone read the same rows, so they can never
  give you two different answers.
- **The weather at the top**, in °C or °F, exactly as you set it on the television.
- **Nothing yet?** Home says so and offers to add a playlist.

### 📅 The guide

- **The guide refreshes itself after a guide feed changes.** Adding, re-syncing or deleting an EPG
  source updates the guide straight away, instead of showing the old programmes until the app is
  restarted.
- **Leaving the guide no longer kills the app.** Opening the guide and then going anywhere else
  could run the phone out of memory and close the app outright; the guide's list is now measured
  properly and it does not happen.

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

### 📡 Casting to a Chromecast

- **The cast button is live**, in the app's top bar and over the picture in the player. It appears
  only when there is something to cast to, and it is the phone's own standard button, so the list of
  devices and the way to stop casting are the ones every other app uses.
- **Casting takes over from wherever you are.** Pick a television mid-film and the phone stops
  playing and the television picks it up at the same second. End the cast and the film comes back to
  the phone at the point the television had reached. A live channel comes back at the live edge,
  which is where a live channel always is.
- **The player screen becomes the cast screen** while the television has the stream: the artwork,
  what is playing, which television it is playing on, and controls that drive that television —
  play and pause, ten seconds back and forward, the scrub bar for a film, and its volume.
- **The notification and the lock screen follow it too.** They say "Playing on <your television>"
  and their buttons control the television, so you are never left tapping a phone that is not
  playing anything.
- **A stream a Chromecast cannot play says so, in your language.** A Chromecast decodes the stream
  itself, and it cannot decode everything an IPTV playlist contains — so instead of a black screen
  or a spinner that never ends, you get a plain message. Three cases are known before anything is
  sent: a channel that needs custom headers, a raw MPEG-TS stream, and a protected film.
- **Casting does not hijack the phone's sound.** Unplugging your headphones, or a notification
  arriving, no longer pauses something playing in another room.
- **No floating window while casting**, and no picture-in-picture: the picture is on the television,
  so the phone shows the docked bar instead of an empty black square that follows you around.

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
- **The shine is yours to keep or drop.** A glass panel arrives with a narrow band of light
  travelling across it once. Under *Behavior* on the Glass Effect page there is now a switch for it,
  so a screen that should simply appear can. It is on to begin with, and **Reduce animations** still
  removes it whatever the switch says.
- **The background picture belongs to the Glass Effect.** Turning the Glass Effect off now takes the
  picture with it and gives you the plain theme back, instead of leaving a wallpaper behind panels
  that no longer have anything to do with it. Turning glass back on brings the picture back — it is
  never deleted, only hidden.
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
- **Monospace really is monospace.** The app now carries its own typewriter font instead of asking
  the phone for one. Some phones — particularly with a custom system font pack installed — answer
  that request with the same face they use for ordinary text, which left two entries in the font
  list looking identical. A font inside the app cannot be substituted away.
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

### 👤 Profiles

- **The app asks who is watching before it shows anything.** When more than one profile exists, or a
  profile is locked, a "Who's watching?" screen comes first, with each profile's picture, its name
  and a tag for a kids profile or a locked one. Nothing of anybody's library is drawn until a
  profile has been chosen and, if it is locked, unlocked.
- **Every prompt here rises from the bottom of the screen**, frosted like the rest of the app, so it is
  where your thumb is and the keyboard does not cover it — the PIN, the profile editor, and every
  choice on the way into and out of a backup.
- **A locked profile asks for its PIN**, and a wrong PIN says so and clears the field rather than
  letting you through.
- **Add, rename, re-picture, lock, unlock and delete a profile**, from the chooser or from the
  Profile page in Settings. A profile can be marked as a kids profile, and the last remaining
  profile cannot be deleted.
- **Unlocking lasts for as long as the app is open**, so rotating the phone or coming back from
  another app does not ask again — but closing the app does.
- **Switching profile is a tap on the Profile settings page**, and a locked one asks for its PIN
  before it switches.
- **Profiles is reachable from More.** The row was there from the beginning and did nothing when
  tapped; it now opens the profile manager, so switching or editing who is watching no longer means
  finding it inside Settings.

### 💾 Backup and restore, from the phone

- **A Backup & Restore page of its own**, under Data in Settings, doing everything the television's
  does.
- **You choose whose data goes in.** Every profile is listed, the one you are using is ticked, and a
  locked profile that is not yours has to be unlocked with its PIN before it can be included.
- **You choose what goes in** — playlists, your Customize layout, favourites, history, resume
  points, your manual ordering and your settings — and the same choice again when restoring, out of
  what the file actually holds.
- **A password encrypts the whole file**, so nothing in it — not your playlist addresses, not your
  history, not even the list of what is inside — can be read without it. Without a password the file
  is still written, but your playlist passwords are simply left out and you re-enter them after a
  restore.
- **The backup goes wherever you keep files** — the downloads folder, a cloud drive, an SD card —
  through the phone's own file picker, so the app never asks for access to all your storage.
- **Restoring adds to what is there rather than replacing it**, and tells you how much came back.

### 🔄 Sync with your television over your own Wi-Fi

- **Local sync**, under Data in Settings. It pairs this phone with the OwnTV app on your television —
  or with another phone or tablet — over your home Wi-Fi. No account, no cloud, nothing leaving the
  house.
- **Turn Sync mode on at both ends, tap Connect, type the PIN once.** A badge at the top right shows
  while sync mode is on, and it switches itself off the moment you leave the screen, so nothing is
  listening the rest of the time. The other device is found on the network, or by scanning the QR
  code it shows, or by typing its address.
- **Either device can start it, and the direction is always named** — *Send to*, *Receive from* or
  *Merge with*. There is no bare "sync" button whose meaning you have to guess.
- **You choose what travels**: the same tick-list Backup & Restore uses — playlists, favourites, watch
  history, resume points, your customisations, your manual ordering and your settings.
- **You are never asked for a password, and your playlist logins travel anyway.** The two devices
  agree a key between themselves when they pair, and everything that crosses the network is locked
  with it. There used to be a password box here, and leaving it empty — which is what anyone would do
  syncing to their own television — quietly left the playlist logins behind.
- **You see what will change before it does.** A summary counts what would be added and what would be
  removed, and nothing is written until you confirm.
- **A deletion now stays deleted.** Unfavourite something here, sync, and the television does not
  hand it back on the next sync.
- **The newer of the two always wins.** Finish an episode on the television and the phone takes that
  over, not the other way round. Watch history and resume points used to be decided by whichever
  device happened to sync last, so a sync could quietly move you backwards in a show.
- **A device you have already paired says so**, instead of asking for its PIN a second time — tap it
  and you go straight to Send, Receive or Merge.
- **Pairing the same device again updates it** instead of adding a second copy to the list. Pair your
  phone three times and it was three identical rows.
- **Two of the same phone are told apart** by a short code after the name, shown only when two paired
  devices would otherwise read the same.
- **As many devices as you like.** Each one you pair is listed with when you last synced with it.

### ⏳ A large playlist can finish in the background

- **"Run in background" while a playlist is importing.** A provider with a hundred thousand films
  takes a while; the button leaves the sync running and lets you use the app while it finishes,
  instead of holding you on the progress screen.

### 📱 It behaves like a phone, not a television

- **Switch playlist from the top bar, as on the television.** A chip in the bar names the playlist
  you are browsing; tap it to pick another, or *All playlists* for the merged view. The choice
  applies everywhere — Live TV, Movies, Series, the Guide and Home — and is remembered, because it
  is the same "default playlist" the TV app and Settings have always used. With only one playlist
  the chip simply names it. It stands where the profile picture used to: that only ever opened the
  More tab, which the bottom bar already reaches, while switching playlist had no door at all.
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

### 📐 Tablets and big screens

- **Two panes side by side where there is room for them.** Past 840dp of window width — a tablet in
  landscape, or a phone-sized foldable opened flat — Live TV shows the channel list on the left and
  the channel playing on the right, Movies and Series show the grid beside the film or show you
  tapped, and Settings shows the nine groups beside the group you are in. A group's own pages open
  in that same right-hand pane, so the list never disappears out from under you, and Back steps back
  through it one page at a time.
- **Live TV starts empty rather than tuning something by itself.** Opening the tab is not a request
  to watch, so the pane says *Select a channel to preview it here* until you pick one. Library is
  the other way round: the pane follows the first title in the grid, because a grid's preview pane
  with nothing in it is half a screen of nothing.
- **The navigation rail is the same five places as the phone's bottom bar**, with Library split into
  Movies and Series because a rail has the room for both. Downloads and Settings are not on it —
  they live under More, on every screen size. The rail sits centred, and scrolls when a large display
  size makes it taller than the screen.
- **The poster grid counts its columns from the space it actually has**, not from how wide the screen
  is, so it stays sensible next to the rail and next to a detail pane instead of squeezing the
  posters to fit a number.
- **Rotating, unfolding or splitting the screen keeps your place** — where you had scrolled to, which
  channel or title was open, and even half-typed text in the settings search.
- **The Home hero card stops growing** instead of stretching to a tablet's full width and pushing
  every other row off the bottom of the screen.

### ⋯ The More tab is where everything that is not a setting now lives

- **Favourites, in one place, for the first time.** More → Favourites is one screen with three chips
  — Live TV, Movies, Series — showing everything you have starred. Each chip is the list that type
  already has: the same channel row with what is on it now, the same poster grid at your own column
  count, the same press-and-hold menu. Un-starring something removes it from the list there and then,
  and the Library agrees the moment you go back to it. A chip with nothing under it is still shown,
  so a Movies count of zero reads as "you have not starred a film" rather than as a broken screen.
- **Watch history, the same way**, newest first, with the resume bar on the posters that have one.
  Tapping a film picks it up where you left it, and a show opens on the episode you were on.
- **Clear history moved onto that screen.** It used to be three levels down inside Settings, which is
  not where you are when you decide to throw your history away. Same four choices — everything, Live
  TV, Movies or Series — and the same confirmation before anything goes.
- **Profiles works from More.** The row has been there since the app's second phase and did nothing;
  it now opens the profile manager, where you rename, set a PIN, turn kids mode on or delete.
- **Backup & Restore, Local sync, the error log and About left Settings.** None of them is a
  preference — two are places, one is a log and one is a page of facts — and they were only in
  Settings because Settings used to be the only door. All four are More rows now, opening exactly the
  same pages.
- **Settings holds settings and nothing else.** Nine groups became seven: *Data* is gone, and so is
  *Profile*. Searching Settings for "backup", "sync" or "history" now finds nothing, because none of
  those is in Settings any more.
- **The two download preferences moved to the Downloads screen**, behind a gear in the bar — where
  the downloads are saved, and whether they wait for Wi-Fi. They are the screen you are looking at
  when you care about either one.
- **Three rows left More.** *Add playlist*, *Restore backup* and *Sync now* were second doors to
  things that already have one.

### 🚀 The first time you open it

- **The same welcome as the television, step for step.** A greeting with the app's name and your
  choice of language, the *Before you start* notice, then start-fresh-or-restore, your profile, and
  finally where your content comes from — a new playlist, one another profile already has, or a
  backup file. *Skip for now* is there too, if you would rather set a playlist up later.
- **It does not ask who is watching when there is only one answer.** Straight after typing your own
  profile's name, being asked to pick a profile was the app forgetting what it had just been told.
  The chooser now appears only when it is genuinely needed: more than one profile, or one with a PIN.
- **A playlist left unnamed is named for you** — *My IPTV*, *My Playlist* or *My Portal*, exactly as
  the television has always done. An unnamed one used to leave the playlist chip in the top bar as a
  blank pill, and a playlist added before this fix now shows its proper name too.

### 🛠️ Fixes

- **Fixed: the app closed itself after adding a playlist.** Pressing OK on *All set!* ended the setup
  from a background thread, which Android does not allow to change screens, and the whole app went
  down with it.
- **Fixed: Home said "Add a playlist first" when you already had one.** On a brand-new playlist every
  row on Home is legitimately empty — nothing watched, nothing favourited yet — and the screen
  offered to fix a problem you did not have. It now says what the television says: *Start watching to
  see your activity here*, and never offers to add a playlist once you have one.
- **Fixed: the little floating window opened on its own.** Tapping a channel started a preview, and
  walking off to another tab put a window over whatever you went to look at. The window now appears
  only when you ask for it in the full screen player, and leaving a channel stops the stream instead
  of letting it play on where you cannot see it.

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
