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
import net.minecraft.class_437;

import java.lang.reflect.Method;
import java.util.function.Consumer;

/** Builds the settings screen with Cloth Config (looked up by reflection, so the mod still loads without it). */
public final class ConfigScreen {
    private ConfigScreen() {}

    public static class_437 build(class_437 parent) {
        try {
            Class<?> cbClass = Class.forName("me.shedaniel.clothconfig2.api.ConfigBuilder");
            Object b = call(cbClass, "create");
            call(b, "setParentScreen", parent);
            call(b, "setTitle", text("Decked Out Card Callout"));
            call(b, "setSavingRunnable", (Runnable) () -> { CardHudConfig.save(); CardHud.push("payToWin"); });
            Object cat = call(b, "getOrCreateCategory", text("Card overlay"));
            Object eb = call(b, "entryBuilder");

            add(cat, call(call(call(eb, "startBooleanToggle", text("Show card overlay"), CardHudConfig.enabled),
                    "setDefaultValue", true),
                    "setSaveConsumer", (Consumer<Boolean>) v -> CardHudConfig.enabled = v));
            add(cat, call(call(call(eb, "startEnumSelector", text("Screen corner"), CardHudConfig.Corner.class, CardHudConfig.corner),
                    "setDefaultValue", CardHudConfig.Corner.TOP_RIGHT),
                    "setSaveConsumer", (Consumer<CardHudConfig.Corner>) v -> CardHudConfig.corner = v));
            add(cat, slider(eb, "Card size (% of screen height)", CardHudConfig.sizePercent, 10, 60, 30, v -> CardHudConfig.sizePercent = v));
            add(cat, slider(eb, "Distance from left/right edge", CardHudConfig.marginX, 0, 300, 6, v -> CardHudConfig.marginX = v));
            add(cat, slider(eb, "Distance from top/bottom edge", CardHudConfig.marginY, 0, 300, 6, v -> CardHudConfig.marginY = v));
            add(cat, slider(eb, "Seconds on screen", CardHudConfig.seconds, 3, 20, 9, v -> CardHudConfig.seconds = v));

            return (class_437) call(b, "build");
        } catch (ClassNotFoundException e) {
            DeckedOutOBS.LOGGER.warn("Decked Out Card Callout: install Cloth Config to get the settings screen (or edit config/decked-out-card-callout.properties).");
            return parent;
        } catch (Throwable t) {
            DeckedOutOBS.LOGGER.warn("Decked Out Card Callout: could not open the settings screen", t);
            return parent;
        }
    }

    private static Object slider(Object eb, String label, int value, int min, int max, int def, Consumer<Integer> save) throws Exception {
        return call(call(call(eb, "startIntSlider", text(label), value, min, max), "setDefaultValue", def), "setSaveConsumer", save);
    }

    private static void add(Object category, Object fieldBuilder) throws Exception {
        call(category, "addEntry", call(fieldBuilder, "build"));
    }

    private static Object text(String s) { return class_2561.method_43470(s); }

    /** Calls the public method called name whose parameters accept args (primitives are matched by their boxes). */
    private static Object call(Object target, String name, Object... args) throws Exception {
        Class<?> c = target instanceof Class ? (Class<?>) target : target.getClass();
        Object recv = target instanceof Class ? null : target;
        for (Method m : c.getMethods()) {
            if (!m.getName().equals(name) || m.getParameterCount() != args.length) continue;
            Class<?>[] p = m.getParameterTypes();
            boolean ok = true;
            for (int i = 0; i < p.length && ok; i++) ok = args[i] == null || box(p[i]).isInstance(args[i]);
            if (!ok) continue;
            m.setAccessible(true);
            return m.invoke(recv, args);
        }
        throw new NoSuchMethodException(c.getName() + "." + name);
    }

    private static Class<?> box(Class<?> t) {
        if (!t.isPrimitive()) return t;
        if (t == int.class) return Integer.class;
        if (t == boolean.class) return Boolean.class;
        if (t == float.class) return Float.class;
        if (t == double.class) return Double.class;
        if (t == long.class) return Long.class;
        return t;
    }
}
