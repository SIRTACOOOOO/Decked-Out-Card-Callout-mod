/*
 * Decked Out Card Callout mod
 * Copyright (C) 2026 SIRTACOOOOO
 * Based on "Decked Out OBS" by Meza (https://github.com/meza/decked-out-obs).
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License version 3 as published by the Free Software Foundation.
 * It is distributed WITHOUT ANY WARRANTY; see the LICENSE file or <https://www.gnu.org/licenses/>.
 */
package gg.meza.doobs.commands;

import com.mojang.brigadier.CommandDispatcher;
import gg.meza.doobs.data.Settings;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

/** The OBS web server (and with it the port setting) was removed, so this registers nothing. */
public class ServerPortCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, Settings settings, Runnable beginProcessing) {
    }
}
