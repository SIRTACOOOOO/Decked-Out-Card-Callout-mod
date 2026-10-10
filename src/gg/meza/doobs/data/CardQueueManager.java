/*
 * Decked Out Card Callout mod
 * Copyright (C) 2026 SIRTACOOOOO
 * Based on "Decked Out OBS" by Meza (https://github.com/meza/decked-out-obs).
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License version 3 as published by the Free Software Foundation.
 * It is distributed WITHOUT ANY WARRANTY; see the LICENSE file or <https://www.gnu.org/licenses/>.
 */
package gg.meza.doobs.data;

import gg.meza.doobs.DeckedOutOBS;
import gg.meza.doobs.hud.CardHud;
import gg.meza.doobs.server.Session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CardQueueManager {
    private final Map<Session, CardQueue> cardQueues = new ConcurrentHashMap<>();
    public void addQueueForSession(Session session) { cardQueues.put(session, new CardQueue()); }
    public void queueCard(String card) {
        CardHud.push(card);
        cardQueues.values().forEach(q -> q.addCard(card));
        DeckedOutOBS.LOGGER.debug("Queued sound: {}", card);
    }
    public void removeQueueForSession(Session session) { cardQueues.remove(session); }
    public CardQueue getQueueForSession(Session session) { return cardQueues.get(session); }
}
