# ObscureNicks

A dynamic profile personalization plugin built for modern Paper/Minecraft servers (1.21+). It bridges cosmetic text styling with visual immersion by allowing players to seamlessly change both their in-game text identity (chat and tab display names) and their physical character skin simultaneously through a single command.

---

## 🚀 Current Project Status
The foundational core of the plugin is fully operational, package-synchronized, and stable.

* **Database Management:** Powered by a local SQLite engine that cleanly caches user data (`uuid`, `nickname`, `skin_textures`, `rank`) across server restarts.
* **Dual API Skin Fetcher:** Asynchronously speaks to Mojang’s REST endpoints to validate premium player handles, pulling the matching `.png` skin values and cryptographic signatures safely without halting server ticks.
* **Network Packet Player Refreshing:** Implements an advanced, multi-tick network tracking loop (`hidePlayer` and `showPlayer` toggles coupled with async client teleports) to force clients to instantly dump skin rendering caches and show visual changes immediately.
* **Chat & Tab Name Overhauls:** Dynamically hooks into Paper’s modern `AsyncChatEvent` to enforce rich `Component`-based nicknames in global text channels, while updating the Tab List player profile layouts.
* **API Interoperability:** Functions as an active **API Hook Provider** by exposing its state to **PlaceholderAPI** via the `%lanick_name%` placeholder string, making it compatible with scoreboards, holograms, and chat formatters.
* **Command Management:** Features a built-in user handler via `/nick <name>` and `/changenick <name>`, featuring an instant cleanup toggle (`/nick off`) to safely roll profile states back to vanilla defaults.

---

## 🗺️ Feature Roadmap
To elevate the utility of **ObscureNicks** from a pure utility plugin to a full-featured server suite, the following features are slated for upcoming updates:

[Current Core Engine] ➔ [1. Group Rank Sync] ➔ [2. GUI Selection Menu] ➔ [3. Smart Text Filtering]

### 1. Database-to-Group Rank Synchronization
* **Objective:** Fully hook into the database's existing but unused `rank` column.
* **Details:** Implement integrations with permission engines (like LuckPerms). When a player alters their nickname, the system will read their designated rank layer to append prefix color components, matching permissions, and group weights in the chat and tab lists automatically.

### 2. Visual GUI Profile Customization Menu
* **Objective:** Eliminate the text-only requirement for cosmetic choices.
* **Details:** Introduce a rich internal inventory interface where players can view their active cosmetic properties, pick from a history of past favorite nicknames, and choose pre-approved historical skin profiles directly from interactive menus.

### 3. Smart Moderation & Filter Checkpoints
* **Objective:** Protect the server's chat environment from abusive usage.
* **Details:** Build a customization system featuring blacklisted strings, regex validations, character length clamps, and unique alphanumeric checks to prevent players from mimicking server staff or selecting offensive names.
