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
import net.minecraft.class_2561;
import net.minecraft.class_310;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Backup detection that doesn't need any blocks: listens to every sound the game plays (the same feed the subtitles use)
 * and shows a card when a sound's name contains a card's name. Also powers /deckedout dungeon listen.
 */
public final class SoundWatcher {
    private static final String[] CARDS = {"adrenalineRush","avalanche","beastSense","boundingStrides","brilliance","cashCow","chillStep","coldSnap","deepfrost","dungeonRepairs","eerieSilence","emberSeeker","evasion","eyesOnThePrize","frostFocus","lootAndScoot","momentOfClarity","nimbleLooting","payToWin","piratesBooty","quickstep","recklessCharge","secondWind","smashAndGrab","sneak","sprint","stability","stumble","swagger","treadLightly","treasureHunter"};
    private static final Map<String, String> NORM = new LinkedHashMap<>();
    static {
        List<String> sorted = new ArrayList<>(Arrays.asList(CARDS));
        sorted.sort((a, b) -> b.length() - a.length());          // longest names first
        for (String c : sorted) NORM.put(c.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", ""), c);
    }

    private static final class Candidate { Method getter, register; Class<?> listener; }
    private static final List<Candidate> CANDIDATES = new ArrayList<>();
    private static boolean scanned, failed;
    private static int attempt, ticksSinceAttempt;
    private static volatile long received;
    private static volatile int listenMode;                      // 0 off, 1 modded sounds, 2 everything
    private static final Set<String> SEEN = Collections.synchronizedSet(new HashSet<>());
    private static final ConcurrentLinkedQueue<String> CHAT = new ConcurrentLinkedQueue<>();
    private static final Map<String, Long> LAST_FIRED = new HashMap<>();
    private static final Map<Class<?>, Method> ID_GETTERS = new HashMap<>();

    private SoundWatcher() {}

    public static void setListen(int mode) { listenMode = mode; SEEN.clear(); }
    public static int getListen() { return listenMode; }
    public static long received() { return received; }

    /** Called every world tick. */
    public static void tick() {
        if (failed) return;
        try {
            if (!scanned) { scan(); scanned = true; }
            if (received == 0 && !CANDIDATES.isEmpty() && ticksSinceAttempt++ >= 100) {
                ticksSinceAttempt = 0;
                Candidate c = CANDIDATES.get(attempt++ % CANDIDATES.size());
                Object owner = c.getter.invoke(class_310.method_1551());
                if (owner != null) c.register.invoke(owner, listener(c.listener));
            }
            String line;
            while ((line = CHAT.poll()) != null) {
                class_310 mc = class_310.method_1551();
                if (mc.field_1724 != null) mc.field_1724.method_7353(class_2561.method_43470(line), false);
            }
        } catch (Throwable t) {
            failed = true;
            DeckedOutOBS.LOGGER.warn("Decked Out Card Callout: sound watching stopped after an error", t);
        }
    }

    private static void scan() {
        Class<?> clientClass = class_310.method_1551().getClass();
        for (Method g : clientClass.getMethods()) {
            if (g.getParameterCount() != 0 || g.getReturnType().isPrimitive() || g.getReturnType() == String.class
                    || g.getReturnType() == Object.class || Modifier.isStatic(g.getModifiers())) continue;
            for (Method r : g.getReturnType().getMethods()) {
                if (r.getParameterCount() != 1 || Modifier.isStatic(r.getModifiers())) continue;
                Class<?> p = r.getParameterTypes()[0];
                if (isSoundListener(p)) {
                    Candidate c = new Candidate(); c.getter = g; c.register = r; c.listener = p;
                    CANDIDATES.add(c);
                }
            }
        }
        DeckedOutOBS.LOGGER.info("Decked Out Card Callout: sound listener candidates: " + CANDIDATES.size());
    }

    private static boolean isSoundListener(Class<?> i) {
        if (!i.isInterface()) return false;
        int abs = 0; boolean match = false;
        for (Method m : i.getMethods()) {
            if (Modifier.isStatic(m.getModifiers()) || m.isDefault()) continue;
            abs++;
            Class<?>[] p = m.getParameterTypes();
            if (p.length == 3 && p[2] == float.class) match = true;
        }
        return abs == 1 && match;
    }

    private static Object listener(Class<?> iface) {
        return Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, (proxy, m, args) -> {
            if (m.getDeclaringClass() == Object.class) {
                switch (m.getName()) {
                    case "hashCode": return System.identityHashCode(proxy);
                    case "equals": return proxy == args[0];
                    default: return "DeckedOutSoundListener";
                }
            }
            if (args != null && args.length == 3) { try { onSound(args[0]); } catch (Throwable ignored) { } }
            return null;
        });
    }

    private static String idOf(Object sound) throws Exception {
        Method g = ID_GETTERS.get(sound.getClass());
        if (g == null) {
            Class<?> idClass = Class.forName("net.minecraft.class_2960");
            for (Method m : sound.getClass().getMethods())
                if (m.getParameterCount() == 0 && m.getReturnType() == idClass) { g = m; break; }
            if (g == null) return null;
            ID_GETTERS.put(sound.getClass(), g);
        }
        Object id = g.invoke(sound);
        return id == null ? null : id.toString().toLowerCase(Locale.ROOT);
    }

    private static void onSound(Object sound) throws Exception {
        received++;
        String id = idOf(sound);
        if (id == null) return;
        int colon = id.indexOf(':');
        String ns = colon < 0 ? "minecraft" : id.substring(0, colon);
        String path = colon < 0 ? id : id.substring(colon + 1);

        int mode = listenMode;
        if (mode == 2 || (mode == 1 && !ns.equals("minecraft"))) {
            if (SEEN.add(id)) CHAT.add("[sound] " + id);
        }

        String norm = path.replaceAll("[^a-z0-9]", "");
        for (Map.Entry<String, String> e : NORM.entrySet()) {
            if (norm.contains(e.getKey())) {
                long now = System.currentTimeMillis();
                Long last;
                synchronized (LAST_FIRED) { last = LAST_FIRED.get(e.getValue()); if (last == null || now - last > 2500) LAST_FIRED.put(e.getValue(), now); }
                if (last == null || now - last > 2500) CardHud.push(e.getValue());
                return;
            }
        }
    }
}
