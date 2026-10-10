/*
 * Decked Out Card Callout mod
 * Copyright (C) 2026 SIRTACOOOOO
 * Based on "Decked Out OBS" by Meza (https://github.com/meza/decked-out-obs).
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License version 3 as published by the Free Software Foundation.
 * It is distributed WITHOUT ANY WARRANTY; see the LICENSE file or <https://www.gnu.org/licenses/>.
 */
package gg.meza.doobs.server;

import gg.meza.doobs.data.CardQueueManager;

/** The OBS web server was replaced by the in-game HUD overlay; this is a no-op shell. */
public class BasicHttpServer {
    public BasicHttpServer(CardQueueManager cardQueueManager) {}
    public void startServer(int port) {}
    public void stopServer() {}
    public static boolean isPortAvailable(int port) { return true; }
}
