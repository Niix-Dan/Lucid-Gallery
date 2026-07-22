<div align="center">

# Lucid Gallery

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21%2B-blue?style=for-the-badge)
![Side](https://img.shields.io/badge/Side-Client-green?style=for-the-badge)

![NeoForge](https://img.shields.io/badge/NeoForge-red?style=for-the-badge)
![Forge](https://img.shields.io/badge/Forge-orange?style=for-the-badge)
![Fabric](https://img.shields.io/badge/Fabric-gray?style=for-the-badge)

**Browse, preview, and share your screenshots without ever leaving the game.**

</div>

***

**Lucid Gallery** is a lightweight, client-side mod that turns your `screenshots` folder into a clean, in-game gallery. Instead of alt-tabbing to your file explorer every time you want to check a screenshot, open a sleek grid view directly from the pause menu or a dedicated keybind, preview any shot in full size, and even upload it to the web to grab a shareable link - all without touching your desktop.

***

## SCREENSHOT BROWSING

No more digging through folders. Lucid Gallery gives your screenshots a proper home:

*   **Grid Gallery View:** All your screenshots are displayed as clean, uniform cards in a configurable, scrollable grid - newest first.
*   **Instant Access:** Open the gallery anytime with a dedicated keybind (`G` by default) or from a new button added straight to the pause menu.
*   **Full Preview Modal:** Click any card to open a large preview, showing the screenshot alongside its **date taken, file size, and resolution**.
*   **Quick Actions:** Open a screenshot in your system's default image viewer or delete it permanently, right from the card or the preview modal - no confirmation dialogs slowing you down.
*   **Smooth Scrolling:** A custom, fully draggable scrollbar keeps large screenshot collections easy to navigate.

## ONE-CLICK CLOUD SHARING

Want to send a screenshot to a friend without leaving Minecraft? Lucid Gallery has you covered:

*   **Instant Upload:** Upload any screenshot straight from the preview modal - no account, login, or API key required.
*   **Powered by Litterbox (catbox.moe):** Screenshots are uploaded as temporary files that automatically expire after 24 hours, so nothing you share sticks around forever.
*   **Copy Link Button:** Once uploaded, the link is cached and copied to your clipboard with a single click, ready to paste anywhere.
*   **Smart Anti-Spam Cooldown:** A configurable cooldown between uploads prevents accidental spam, with clear on-screen feedback (`Uploading...`, `Cooldown`, `Retry`, etc.).
*   **Resilient Networking:** Failed uploads are automatically retried with exponential backoff before showing an error, so a flaky connection won't ruin your upload.

## DYNAMIC GUI SCALE

*   **Adaptive Scaling:** Lucid Gallery automatically fits the interface to your window size, capping the scale so cards never get too small or too large.
*   **Manual Override:** A dedicated dropdown in the top-right corner lets you lock the gallery scale to `Auto` or a fixed value from `1` to `8`, independent of your normal Minecraft GUI scale.

## QUALITY OF LIFE & CONFIGURATION

*   **Hot-Reloadable Configuration:** Every option lives in a simple `config/lucidgallery.properties` file, with an optional live file-watcher so changes apply without restarting the game.
*   **Full Visual Customization:** Adjust the number of grid columns, card background color, scrollbar colors, scrollbar width/margin, and minimum thumb size to match your taste.
*   **Texture Caching:** Screenshot thumbnails are loaded and cached on demand, and automatically released when scrolled out of view, keeping memory usage low even with huge screenshot folders.
*   **Safe & Client-Side:** Lucid Gallery only touches your local `screenshots` folder and interface. It's perfectly safe to drop into any modpack or join any server, singleplayer or multiplayer.

***

## Bug Reports & Feedback

If you run into a bug, have a feature request, or want to suggest an improvement, please open an issue on our **Issue Tracker** using the provided templates.

<br>
<div align="center">

[![Discord Community](https://discordapp.com/api/guilds/1519007858778177749/widget.png?style=banner2)](https://discord.gg/vChttNgJ4v)

</div>