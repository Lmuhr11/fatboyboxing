# SMP Minigames - Boxing

A Paper plugin for your Apex Hosting server (built for 1.21.x, Java 21). Adds a self-queue
boxing minigame that works fine for both Java and Bedrock (Geyser) players, since it's all
plain server-side logic - nothing client-specific.

## What it does

- `/box join` - queue up. Once 2 players are queued, a match starts automatically.
- `/box leave` - leave the queue before a match starts.
- Fighters get their inventory (items, armor, offhand) wiped and stashed safely, then get
  full health, full hunger, and are teleported to their corner - fists only, nothing to pick up.
- Nobody can land a hit inside the arena world unless they're one of the two active fighters
  standing inside the ring, or an op. Nobody but ops can break/place blocks in that world either.
- Loser respawns at your configured exit point, never at their bed.
- Winner gets their original inventory back and is teleported to the same exit point.
- Whoever quits mid-match automatically forfeits (their items are safely restored before they
  disconnect, so nothing gets lost).
- Winner announcements are broadcast server-wide so you know who to hand a prize to - the
  plugin doesn't give prizes automatically, that part's on you as designed.

## Building the jar

This sandbox can't reach Maven's package repositories, so the jar isn't included - but building
it takes about a minute and needs **no software installed on your computer**, using GitHub Actions:

1. Go to https://github.com/new and create a new repository (public or private, doesn't matter).
2. Open your new repo, click **Add file > Upload files**, and drag the *entire contents* of this
   folder in (including the hidden `.github` folder - if your browser hides it, use `git` instead,
   or a tool like GitHub Desktop, or just create the `.github/workflows/build.yml` file manually
   through GitHub's web editor with the content from this project).
3. Commit the files to the `main` branch.
4. Click the **Actions** tab - a "Build Plugin" run should already be in progress. Wait about a
   minute for it to finish (green check).
5. Click into the finished run, scroll to **Artifacts**, and download `smp-minigames-plugin.zip`.
6. Unzip it - inside is `smp-minigames.jar`. That's your plugin.

If you'd rather build it locally and have Java 21 + Maven installed, it's just `mvn package` from
this folder, and the jar shows up in `target/smp-minigames.jar`.

## Installing on Apex Hosting

1. Upload `smp-minigames.jar` to your server's `/plugins` folder (Apex's file manager, or FTP).
2. Restart the server.
3. You should see `SMP Minigames enabled - boxing is ready.` in the console log.

## Setting up the arena

The plugin doesn't build anything for you - you build the ring, it just enforces the rules.
Recommended: put it in its own world so it's naturally walled off from the rest of the server,
using Multiverse-Core (free):

1. Install Multiverse-Core the same way you installed this plugin (Apex's plugin installer, or
   drop the jar in `/plugins` and restart).
2. In-game, as an op: `/mv create minigames flat` - creates a new flat world called "minigames".
3. Build (or `/mv tp minigames` and build) your boxing ring there - two facing platforms with a
   barrier between them and the rest of the world, however you want it to look.
4. Configure the plugin by standing in the right spots and running these (op only):
   - `/box setring1` - stand at one corner of the legal fighting area, run this.
   - `/box setring2` - stand at the opposite corner, run this. Together these two points define
     a box - anyone landing a hit outside it (except ops) gets blocked automatically.
   - `/box setspawn1` - stand where fighter 1 should start, **facing fighter 2's spot**, run this.
   - `/box setspawn2` - stand where fighter 2 should start, facing fighter 1's spot, run this.
   - `/box setexit` - stand wherever fighters should end up after the match (a spectator area,
     back near the queue spot, wherever). This can be in a different world entirely if you want
     fighters returned to your main spawn after their match - whatever world you're standing in
     when you run this command is what gets saved.
5. Done - `/box join` from two accounts to test it.

Since the whole "minigames" world is protected from block break/place and stray PvP for non-ops,
you've got room in there later for the needle-in-the-haystack and paintball arenas too, without
needing a second world or extra protection plugin.

## Notes for later

- Only one boxing match runs at a time; a third player queuing just waits their turn.
- Permission nodes: `minigames.box.play` (default: everyone) and `minigames.box.admin`
  (default: op only) - hook them into LuckPerms later if you want non-op staff running setup.
- This was written and checked by hand-compiling against stub classes matching the real Paper
  API method-for-method (no network access to Maven here to compile against the real thing) -
  it's syntactically and logically sound, but give it one real test match before trusting it
  in front of your players.
