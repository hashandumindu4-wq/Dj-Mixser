# DJ Mixer V2 - Complete Android Project

Phone-buildable Android Studio/AndroidIDE-style Gradle project.

Features:
- Landscape DJ mixer UI
- Deck A and Deck B
- Offline local audio picker
- Play/Pause/CUE/seek
- Pitch/speed control
- Crossfader and master volume
- Bass/Mid/Treble controls (UI)
- Theme switcher
- Echo/Loop UI controls

Important:
This package includes the missing Gradle launcher files that were absent from the earlier ZIP.
The launcher first uses the Gradle executable supplied by the phone IDE. If the IDE has its own
Gradle distribution, it can build the project without needing a PC.

Phone build:
1. Open this project folder in AndroidIDE.
2. Allow the project to finish indexing/syncing.
3. Press Build/Run.
4. If AndroidIDE asks for JDK, use JDK 17.
5. The first build may download Android/Gradle dependencies, so Wi-Fi is recommended.

The source is intentionally offline/local-audio focused and does not require a server.
