/*
 * Decked Out Card Callout mod
 * Copyright (C) 2026 SIRTACOOOOO
 * Based on "Decked Out OBS" by Meza (https://github.com/meza/decked-out-obs).
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License version 3 as published by the Free Software Foundation.
 * It is distributed WITHOUT ANY WARRANTY; see the LICENSE file or <https://www.gnu.org/licenses/>.
 */
package gg.meza.doobs.hud;

import gg.meza.doobs.DeckedOutOBS;
import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** User settings for where and how big the card pops up. Stored in config/decked-out-card-callout.properties. */
public final class CardHudConfig {
    public enum Corner {
        TOP_RIGHT("Top right"), TOP_LEFT("Top left"), BOTTOM_RIGHT("Bottom right"), BOTTOM_LEFT("Bottom left");
        private final String label;
        Corner(String l) { label = l; }
        @Override public String toString() { return label; }
        public boolean right() { return this == TOP_RIGHT || this == BOTTOM_RIGHT; }
        public boolean bottom() { return this == BOTTOM_RIGHT || this == BOTTOM_LEFT; }
    }

    public static boolean enabled = true;
    public static Corner corner = Corner.TOP_RIGHT;
    public static int sizePercent = 30;      // card height as % of the scaled screen height
    public static int marginX = 6;           // gap from the left/right edge, in GUI pixels
    public static int marginY = 6;           // gap from the top/bottom edge, in GUI pixels
    public static int seconds = 9;           // how long a card stays on screen

    private CardHudConfig() {}

    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("decked-out-card-callout.properties"); }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(f)) { p.load(in); }
            enabled = Boolean.parseBoolean(p.getProperty("enabled", "true"));
            try { corner = Corner.valueOf(p.getProperty("corner", "TOP_RIGHT")); } catch (IllegalArgumentException e) { corner = Corner.TOP_RIGHT; }
            sizePercent = clamp(p.getProperty("sizePercent"), 10, 60, 30);
            marginX = clamp(p.getProperty("marginX"), 0, 300, 6);
            marginY = clamp(p.getProperty("marginY"), 0, 300, 6);
            seconds = clamp(p.getProperty("seconds"), 3, 20, 9);
        } catch (Throwable t) {
            DeckedOutOBS.LOGGER.warn("Decked Out Card Callout: could not read config, using defaults", t);
        }
    }

    public static void save() {
        try {
            Properties p = new Properties();
            p.setProperty("enabled", Boolean.toString(enabled));
            p.setProperty("corner", corner.name());
            p.setProperty("sizePercent", Integer.toString(sizePercent));
            p.setProperty("marginX", Integer.toString(marginX));
            p.setProperty("marginY", Integer.toString(marginY));
            p.setProperty("seconds", Integer.toString(seconds));
            try (OutputStream out = Files.newOutputStream(file())) { p.store(out, "Decked Out Card Callout mod settings"); }
        } catch (Throwable t) {
            DeckedOutOBS.LOGGER.warn("Decked Out Card Callout: could not save config", t);
        }
    }

    private static int clamp(String s, int lo, int hi, int def) {
        try { return Math.max(lo, Math.min(hi, Integer.parseInt(s.trim()))); } catch (Exception e) { return def; }
    }
}
