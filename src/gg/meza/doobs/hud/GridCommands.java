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

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import gg.meza.doobs.DeckedOutOBS;
import gg.meza.doobs.cardProcessing.CardCallouts;
import gg.meza.doobs.data.Location;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.*;

import java.util.Map;

public final class GridCommands {
    private GridCommands() {}

    public static void register(CommandDispatcher<FabricClientCommandSource> d) {
        d.register(ClientCommandManager.literal("deckedout")
            .then(ClientCommandManager.literal("dungeon")
                .then(ClientCommandManager.literal("find")
                    .executes(c -> find(c, 24))
                    .then(ClientCommandManager.argument("radius", IntegerArgumentType.integer(4, 80))
                        .executes(c -> find(c, IntegerArgumentType.getInteger(c, "radius")))))
                .then(ClientCommandManager.literal("setgrid")
                    .then(ClientCommandManager.argument("x", IntegerArgumentType.integer())
                    .then(ClientCommandManager.argument("y", IntegerArgumentType.integer())
                    .then(ClientCommandManager.argument("z", IntegerArgumentType.integer())
                    .then(ClientCommandManager.argument("direction", StringArgumentType.word())
                        .executes(c -> setGrid(c))))))) 
                .then(ClientCommandManager.literal("setanchor")
                    .then(ClientCommandManager.argument("x", IntegerArgumentType.integer())
                    .then(ClientCommandManager.argument("y", IntegerArgumentType.integer())
                    .then(ClientCommandManager.argument("z", IntegerArgumentType.integer())
                    .then(ClientCommandManager.argument("direction", StringArgumentType.word())
                        .executes(c -> setAnchor(c))))))) 
                .then(ClientCommandManager.literal("listen")
                    .executes(c -> listen(c, 1))
                    .then(ClientCommandManager.literal("all").executes(c -> listen(c, 2)))
                    .then(ClientCommandManager.literal("off").executes(c -> listen(c, 0))))
                .then(ClientCommandManager.literal("probe").executes(c -> probe(c)))
                .then(ClientCommandManager.literal("test")
                    .executes(c -> test(c, "payToWin"))
                    .then(ClientCommandManager.argument("card", StringArgumentType.word())
                        .executes(c -> test(c, StringArgumentType.getString(c, "card")))))));
    }

    private static void say(CommandContext<FabricClientCommandSource> c, String msg) {
        try { c.getSource().sendFeedback(class_2561.method_43470(msg)); }
        catch (Throwable t) { DeckedOutOBS.LOGGER.info("Decked Out Card Callout: " + msg); }
    }

    private static int test(CommandContext<FabricClientCommandSource> c, String card) {
        CardHud.push(card);
        say(c, "Decked Out Card Callout: showing card '" + card + "' in the top right.");
        return 1;
    }

    private static int find(CommandContext<FabricClientCommandSource> c, int radius) {
        if (DeckedOutOBS.settings == null) { say(c, "Join a world or server first."); return 0; }
        class_746 p = c.getSource().getPlayer();
        class_638 world = c.getSource().getWorld();
        int cx = (int) Math.floor(p.method_23317()), cy = (int) Math.floor(p.method_23318()), cz = (int) Math.floor(p.method_23321());
        GridDetector.Match m = GridDetector.find(world, cx, cy, cz, radius);
        if (m.score < 0) { say(c, "No jukeboxes found within " + radius + " blocks. Fly closer to the card wall (it must be loaded) and try again."); return 0; }
        String where = "jukebox #1 at " + m.refX + " " + m.refY + " " + m.refZ + ", facing " + GridDetector.dirName(m.dir);
        if (m.score * 10 < m.total * 7) {
            say(c, "Best match was only " + m.score + "/" + m.total + " jukeboxes (" + where + "), so I did not save it. Get closer to the card wall, or use /deckedout dungeon setgrid x y z direction.");
            return 0;
        }
        DeckedOutOBS.settings.setDungeonPosition(new class_2338(m.anchorX, m.anchorY, m.anchorZ), m.dir);
        say(c, "Found the card wall: matched " + m.score + "/" + m.total + " jukeboxes (" + where + "). Saved - cards will now pop up top right.");
        return 1;
    }

    /** Sets the reference ("key barrel") block directly; the card wall is then derived from it. */
    private static int setAnchor(CommandContext<FabricClientCommandSource> c) {
        if (DeckedOutOBS.settings == null) { say(c, "Join a world or server first."); return 0; }
        int x = IntegerArgumentType.getInteger(c, "x"), y = IntegerArgumentType.getInteger(c, "y"), z = IntegerArgumentType.getInteger(c, "z");
        class_2350 dir = GridDetector.parseDirection(StringArgumentType.getString(c, "direction"));
        if (dir == null) { say(c, "Direction must be north, east, south or west."); return 0; }
        class_2338 anchor = new class_2338(x, y, z);
        DeckedOutOBS.settings.setDungeonPosition(anchor, dir);
        Map<class_2338, String> abs = CardCallouts.getCallouts(new Location(dir, anchor));
        class_638 world = c.getSource().getWorld();
        int hit = 0; int sx = 0, sy = 0, sz = 0;
        for (class_2338 pos : abs.keySet()) {
            sx = pos.method_10263(); sy = pos.method_10264(); sz = pos.method_10260();
            if (world.method_8321(pos) instanceof class_2619) hit++;
        }
        say(c, "Saved. The card wall is around " + sx + " " + sy + " " + sz + " (" + hit + "/" + abs.size() + " jukeboxes currently loaded there; fly closer if that's 0).");
        return 1;
    }

    private static int listen(CommandContext<FabricClientCommandSource> c, int mode) {
        SoundWatcher.setListen(mode);
        if (mode == 0) { say(c, "Sound listening off."); return 1; }
        say(c, (mode == 1 ? "Listening for sounds from the server's resource pack/mods" : "Listening for ALL sounds") + " - new sound names will be printed here. ("
                + SoundWatcher.received() + " sounds heard so far)");
        return 1;
    }

    /** Reports which block types sit at the 48 card positions. */
    private static int probe(CommandContext<FabricClientCommandSource> c) {
        if (DeckedOutOBS.settings == null || !DeckedOutOBS.settings.isDungeonSet()) { say(c, "Set the dungeon first (setanchor), then run probe near the wall."); return 0; }
        Map<class_2338, String> abs = CardCallouts.getCallouts(DeckedOutOBS.settings.getDungeonPosition());
        class_638 world = c.getSource().getWorld();
        Map<String, Integer> counts = new java.util.TreeMap<>();
        for (class_2338 pos : abs.keySet()) {
            String s = String.valueOf(world.method_8320(pos));
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("Block\\{([^}]*)\\}").matcher(s);
            String id = m.find() ? m.group(1) : s;
            counts.merge(id, 1, Integer::sum);
        }
        say(c, "Blocks at the " + abs.size() + " card positions: " + counts);
        return 1;
    }

    private static int setGrid(CommandContext<FabricClientCommandSource> c) {
        if (DeckedOutOBS.settings == null) { say(c, "Join a world or server first."); return 0; }
        int x = IntegerArgumentType.getInteger(c, "x"), y = IntegerArgumentType.getInteger(c, "y"), z = IntegerArgumentType.getInteger(c, "z");
        class_2350 dir = GridDetector.parseDirection(StringArgumentType.getString(c, "direction"));
        if (dir == null) { say(c, "Direction must be north, east, south or west."); return 0; }
        Map<class_2338, String> offs = CardCallouts.getCallouts(new Location(dir, new class_2338(0, 0, 0)));
        class_2338 first = offs.keySet().iterator().next();
        class_2338 anchor = new class_2338(x - first.method_10263(), y - first.method_10264(), z - first.method_10260());
        DeckedOutOBS.settings.setDungeonPosition(anchor, dir);
        class_638 world = c.getSource().getWorld();
        int hit = 0;
        for (class_2338 off : offs.keySet())
            if (world.method_8321(new class_2338(anchor.method_10263() + off.method_10263(), anchor.method_10264() + off.method_10264(), anchor.method_10260() + off.method_10260())) instanceof class_2619) hit++;
        say(c, "Saved. " + hit + "/" + offs.size() + " jukeboxes line up with that position. (x y z = the jukebox for the first card in the list.)");
        return 1;
    }
}
