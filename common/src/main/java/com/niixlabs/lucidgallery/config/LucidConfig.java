package com.niixlabs.lucidgallery.config;

public class LucidConfig {
    @ConfigOption(comment = "Number of columns in the gallery grid")
    public static int gridColumns = 3;

    @ConfigOption(comment = "Card background color (ARGB)", hex = true)
    public static int cardBackgroundColor = 0xAA000000;

    @ConfigSection("Scaling")
    @ConfigOption(comment = "Custom GUI Scale (0 = Auto, 1-8 = Fixed)")
    public static int customGuiScale = 0;

    @ConfigOption(comment = "Minimum virtual width for auto-scaling")
    public static int scaleMinVirtualWidth = 400;

    @ConfigOption(comment = "Minimum virtual height for auto-scaling")
    public static int scaleMinVirtualHeight = 240;

    @ConfigSection("Scrollbar")
    @ConfigOption(comment = "Scrollbar track color", hex = true)
    public static int screenScrollbarTrackColor = 0xAA1A1A1A;

    @ConfigOption(comment = "Active scrollbar thumb color", hex = true)
    public static int screenScrollbarThumbActive = 0xFF00FFAA;

    @ConfigOption(comment = "Idle scrollbar thumb color", hex = true)
    public static int screenScrollbarThumbIdle = 0xAA00FFAA;

    @ConfigOption(comment = "Minimum scroll thumb height")
    public static int screenMinScrollThumbHeight = 24;

    @ConfigOption(comment = "Scrollbar width")
    public static int screenScrollbarWidth = 3;

    @ConfigOption(comment = "Scrollbar right margin")
    public static int screenScrollbarRightMargin = 12;

    @ConfigSection("Screenshot Upload")
    @ConfigOption(comment = "Enable the 'Upload' button in the screenshot preview (uploads to catbox.moe, no account needed)")
    public static boolean screenshotUploadEnabled = true;

    @ConfigOption(comment = "Minimum seconds between two uploads (anti-spam cooldown)")
    public static int screenshotUploadCooldownSeconds = 20;

    @ConfigSection("Internal")
    @ConfigOption(comment = "Config watcher (Needs Restart)")
    public static boolean useConfigWatcher = false;

    public static void load() {
        ConfigManager.load(LucidConfig.class);
    }

    public static void save() {
        ConfigManager.save(LucidConfig.class);
    }

    public static void updateAndSave(String fieldName, Object value) {
        ConfigManager.updateAndSave(LucidConfig.class, fieldName, value);
    }

    public static void startWatcher() {
        ConfigManager.startWatcher(LucidConfig.class);
    }
}