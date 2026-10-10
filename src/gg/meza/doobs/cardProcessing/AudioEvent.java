/*
 * Decked Out Card Callout mod
 * Copyright (C) 2026 SIRTACOOOOO
 * Based on "Decked Out OBS" by Meza (https://github.com/meza/decked-out-obs).
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License version 3 as published by the Free Software Foundation.
 * It is distributed WITHOUT ANY WARRANTY; see the LICENSE file or <https://www.gnu.org/licenses/>.
 */
package gg.meza.doobs.cardProcessing;

import gg.meza.doobs.data.CardQueueManager;
import gg.meza.doobs.data.Location;
import net.minecraft.class_638;

/** Detection moved to GridDetector; kept so the original initialisation code still links. */
public class AudioEvent {
    public AudioEvent(CardQueueManager queueManager, Location dungeon) {}
    public void processBlocks(class_638 world) {}
    public void resetProcessedIds() {}
}
