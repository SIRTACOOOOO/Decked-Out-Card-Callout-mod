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
import net.minecraft.class_1041;
import net.minecraft.class_310;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiFunction;

/** Draws Decked Out card announcements in the top-right corner of the game screen (MC 1.20.x - 1.21.11). */
public final class CardHud {
    private static final long SLIDE_IN_MS = 450, SLIDE_OUT_MS = 500, MAX_QUEUE_AGE_MS = 20000;
    private static final int TEX_W = 256, TEX_H = 367;

    private static final class Pending {
        final String card; final long queuedAt = System.currentTimeMillis();
        Pending(String c) { card = c; }
    }

    private static final Queue<Pending> QUEUE = new ConcurrentLinkedQueue<>();
    private static Object currentId;
    private static long shownAt;
    private static boolean failed;
    private static BiFunction<String, String, Object> idFactory;
    private static int mode;            // 1 = pipeline+color, 2 = pipeline, 3 = legacy (pre 1.21.6)
    private static Method drawMethod;
    private static Object pipeline;

    private CardHud() {}

    private static long holdMs() { return CardHudConfig.seconds * 1000L; }

    public static void push(String card) {
        if (card == null || card.isEmpty() || card.equals("empty")) return;
        QUEUE.add(new Pending(card));
    }

    public static void render(Object ctx) {
        if (failed || !CardHudConfig.enabled) return;
        try {
            long now = System.currentTimeMillis();
            if (currentId != null && now - shownAt > SLIDE_IN_MS + holdMs() + SLIDE_OUT_MS) currentId = null;
            while (currentId == null) {
                Pending p = QUEUE.poll();
                if (p == null) return;
                if (now - p.queuedAt > MAX_QUEUE_AGE_MS) continue;
                currentId = makeId("decked-out-obs", "textures/cards/" + p.card.toLowerCase(Locale.ROOT) + ".png");
                shownAt = now;
            }

            class_1041 window = class_310.method_1551().method_22683();
            int sw = window.method_4486(), sh = window.method_4502();
            int h = Math.max(40, Math.min(500, sh * CardHudConfig.sizePercent / 100));
            int w = h * TEX_W / TEX_H;

            long t = now - shownAt;
            float alpha = 1f, slide = 0f;
            if (t < SLIDE_IN_MS) {
                float p = t / (float) SLIDE_IN_MS;
                alpha = p; slide = (1f - p) * (1f - p);
            } else if (t > SLIDE_IN_MS + holdMs()) {
                float p = Math.min(1f, (t - SLIDE_IN_MS - holdMs()) / (float) SLIDE_OUT_MS);
                alpha = 1f - p; slide = p * p;
            }
            CardHudConfig.Corner corner = CardHudConfig.corner;
            int travel = (int) (slide * (w + CardHudConfig.marginX));
            int x = corner.right() ? sw - w - CardHudConfig.marginX + travel : CardHudConfig.marginX - travel;
            int y = corner.bottom() ? sh - h - CardHudConfig.marginY : CardHudConfig.marginY;
            draw(ctx, currentId, x, y, w, h, alpha);
        } catch (Throwable e) {
            failed = true;
            DeckedOutOBS.LOGGER.warn("Decked Out Card Callout: in-game card overlay disabled after an error", e);
        }
    }

    // ---------- identifier creation (public ctor on <=1.20.x, static factory on 1.21+) ----------
    public static Object makeId(String ns, String path) throws Exception {
        if (idFactory == null) {
            final Class<?> idClass = Class.forName("net.minecraft.class_2960");
            BiFunction<String, String, Object> f = null;
            try {
                final java.lang.reflect.Constructor<?> c = idClass.getConstructor(String.class, String.class);
                f = (n, p) -> { try { return c.newInstance(n, p); } catch (Exception e) { throw new RuntimeException(e); } };
            } catch (NoSuchMethodException ignored) { }
            if (f == null) {
                for (Method m : idClass.getMethods()) {
                    Class<?>[] p = m.getParameterTypes();
                    if (java.lang.reflect.Modifier.isStatic(m.getModifiers()) && p.length == 2
                            && p[0] == String.class && p[1] == String.class && m.getReturnType() == idClass) {
                        final Method mm = m;
                        f = (n, pa) -> { try { return mm.invoke(null, n, pa); } catch (Exception e) { throw new RuntimeException(e); } };
                        break;
                    }
                }
            }
            if (f == null) throw new NoSuchMethodException("No way to create an Identifier");
            idFactory = f;
        }
        return idFactory.apply(ns, path);
    }

    // ---------- texture drawing, located by signature so it survives renames ----------
    private static void draw(Object ctx, Object id, int x, int y, int w, int h, float alpha) throws Exception {
        if (mode == 0) init(ctx, id);
        int a = Math.max(0, Math.min(255, (int) (alpha * 255f)));
        switch (mode) {
            case 1: drawMethod.invoke(ctx, pipeline, id, x, y, 0f, 0f, w, h, TEX_W, TEX_H, TEX_W, TEX_H, (a << 24) | 0xFFFFFF); break;
            case 2: drawMethod.invoke(ctx, pipeline, id, x, y, 0f, 0f, w, h, TEX_W, TEX_H, TEX_W, TEX_H); break;
            default:
                Legacy.begin(alpha);
                try { drawMethod.invoke(ctx, id, x, y, w, h, 0f, 0f, TEX_W, TEX_H, TEX_W, TEX_H); } finally { Legacy.end(); }
        }
    }

    private static void init(Object ctx, Object id) throws Exception {
        Class<?> idClass = id.getClass();
        Object pipe = findPipeline();
        Method best = null; int bestMode = 0;
        for (Method m : ctx.getClass().getMethods()) {
            Class<?>[] p = m.getParameterTypes();
            if (pipe != null && (p.length == 13 || p.length == 12) && p[0].isInstance(pipe) && p[1] == idClass
                    && p[2] == int.class && p[3] == int.class && p[4] == float.class && p[5] == float.class
                    && p[6] == int.class && p[7] == int.class && p[8] == int.class && p[9] == int.class
                    && p[10] == int.class && p[11] == int.class && (p.length == 12 || p[12] == int.class)) {
                int md = p.length == 13 ? 1 : 2;
                if (bestMode == 0 || md < bestMode) { best = m; bestMode = md; }
            } else if (bestMode == 0 && p.length == 11 && p[0] == idClass
                    && p[1] == int.class && p[2] == int.class && p[3] == int.class && p[4] == int.class
                    && p[5] == float.class && p[6] == float.class
                    && p[7] == int.class && p[8] == int.class && p[9] == int.class && p[10] == int.class) {
                best = m; bestMode = 3;
            }
        }
        if (best == null) throw new NoSuchMethodException("DrawContext texture draw method not found");
        drawMethod = best; pipeline = pipe; mode = bestMode;
        DeckedOutOBS.LOGGER.info("Decked Out Card Callout: HUD draw mode " + bestMode);
    }

    /** RenderPipelines.GUI_TEXTURED (1.21.6+), found by its registered name. */
    private static Object findPipeline() {
        for (String cn : new String[]{"net.minecraft.class_10799", "net.minecraft.client.gl.RenderPipelines"}) {
            try {
                Class<?> c = Class.forName(cn);
                for (java.lang.reflect.Field f : c.getFields()) {
                    if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                    Object v = f.get(null);
                    if (v == null) continue;
                    String s = String.valueOf(v);
                    try {
                        Method gl = v.getClass().getMethod("getLocation");
                        s = String.valueOf(gl.invoke(v));
                    } catch (Throwable ignored) { }
                    if (s.equals("minecraft:pipeline/gui_textured")) return v;
                }
            } catch (Throwable ignored) { }
        }
        return null;
    }

    /** Pre-1.21.6 alpha handling. Only loaded when that draw path is used. */
    private static final class Legacy {
        static void begin(float a) {
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, a);
        }
        static void end() {
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        }
    }
}
