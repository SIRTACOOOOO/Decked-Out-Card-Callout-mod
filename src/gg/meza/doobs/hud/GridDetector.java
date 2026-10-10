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
import gg.meza.doobs.cardProcessing.CardCallouts;
import gg.meza.doobs.data.Location;
import gg.meza.doobs.data.Settings;
import net.minecraft.*;

import java.util.*;

/** Watches the jukebox wall each tick and pushes newly started cards to the HUD; also locates the wall on demand. */
public final class GridDetector {
    private static final Set<String> PROCESSED = new HashSet<>();
    private static long lastPlaying;
    private static boolean failed;
    private static Location cachedLoc;
    private static Map<class_2338, String> cachedCallouts;

    private GridDetector() {}

    public static void tick(Object worldObj) {
        SoundWatcher.tick();
        if (failed) return;
        try {
            Settings s = DeckedOutOBS.settings;
            if (s == null || !s.isDungeonSet()) return;
            Location loc = s.getDungeonPosition();
            if (cachedLoc == null || !cachedLoc.equals(loc)) {
                cachedLoc = loc;
                cachedCallouts = CardCallouts.getCallouts(loc);
            }
            class_638 world = (class_638) worldObj;
            long now = System.currentTimeMillis();
            boolean anyPlaying = false;
            for (Map.Entry<class_2338, String> e : cachedCallouts.entrySet()) {
                class_2586 be = world.method_8321(e.getKey());
                if (!(be instanceof class_2619)) continue;
                Boolean has = (Boolean) be.method_11010().method_11654(class_2387.field_11180);
                if (has == null || !has) continue;
                anyPlaying = true;
                if (PROCESSED.add(e.getValue())) CardHud.push(e.getValue());
            }
            if (anyPlaying) lastPlaying = now;
            else if (!PROCESSED.isEmpty() && now - lastPlaying > 5000) PROCESSED.clear();
        } catch (Throwable t) {
            failed = true;
            DeckedOutOBS.LOGGER.warn("Decked Out Card Callout: card detection stopped after an error", t);
        }
    }

    public static class_2350 parseDirection(String s) {
        switch (s.toLowerCase(Locale.ROOT)) {
            case "north": return class_2350.field_11043;
            case "south": return class_2350.field_11035;
            case "east": return class_2350.field_11034;
            case "west": return class_2350.field_11039;
            default: return null;
        }
    }

    public static String dirName(class_2350 d) {
        if (d == class_2350.field_11043) return "north";
        if (d == class_2350.field_11035) return "south";
        if (d == class_2350.field_11034) return "east";
        return "west";
    }

    /** Result of a wall search. */
    public static final class Match {
        public int score, total; public class_2350 dir; public int refX, refY, refZ, anchorX, anchorY, anchorZ;
    }

    /** Slide the jukebox pattern over every position within radius of (cx,cy,cz), for all 4 facings. */
    public static Match find(class_638 world, int cx, int cy, int cz, int radius) {
        class_2350[] dirs = {class_2350.field_11043, class_2350.field_11035, class_2350.field_11034, class_2350.field_11039};
        Match best = new Match();
        best.score = -1;
        for (class_2350 d : dirs) {
            Map<class_2338, String> offs = CardCallouts.getCallouts(new Location(d, new class_2338(0, 0, 0)));
            int n = offs.size();
            int[] ox = new int[n], oy = new int[n], oz = new int[n];
            int i = 0;
            for (class_2338 p : offs.keySet()) { ox[i] = p.method_10263(); oy[i] = p.method_10264(); oz[i] = p.method_10260(); i++; }
            for (int dx = -radius; dx <= radius; dx++)
                for (int dy = -radius; dy <= radius; dy++)
                    for (int dz = -radius; dz <= radius; dz++) {
                        int rx = cx + dx, ry = cy + dy, rz = cz + dz;     // where pattern entry 0 would sit
                        if (!(world.method_8321(new class_2338(rx, ry, rz)) instanceof class_2619)) continue;
                        int score = 0;
                        for (int k = 0; k < n; k++)
                            if (world.method_8321(new class_2338(rx + ox[k] - ox[0], ry + oy[k] - oy[0], rz + oz[k] - oz[0])) instanceof class_2619) score++;
                        if (score > best.score) {
                            best.score = score; best.total = n; best.dir = d;
                            best.refX = rx; best.refY = ry; best.refZ = rz;
                            best.anchorX = rx - ox[0]; best.anchorY = ry - oy[0]; best.anchorZ = rz - oz[0];
                        }
                    }
        }
        return best;
    }
}
