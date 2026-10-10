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

import net.fabricmc.api.ClientModInitializer;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;

/** All Fabric API hooks go through proxies so the exact callback signatures don't matter across Minecraft versions. */
public class CardHudInit implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CardHudConfig.load();
        try { registerHud(); } catch (Throwable t) { gg.meza.doobs.DeckedOutOBS.LOGGER.warn("Decked Out Card Callout: could not hook the HUD", t); }
        try {
            hook("net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents$EndWorldTick",
                 "net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents", "END_WORLD_TICK",
                 a -> GridDetector.tick(a[0]));
        } catch (Throwable t) { gg.meza.doobs.DeckedOutOBS.LOGGER.warn("Decked Out Card Callout: could not hook the world tick", t); }
        try {
            hook("net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback",
                 "net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback", "EVENT",
                 a -> { @SuppressWarnings("unchecked") com.mojang.brigadier.CommandDispatcher<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> d =
                            (com.mojang.brigadier.CommandDispatcher<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource>) a[0];
                        GridCommands.register(d); });
        } catch (Throwable t) { gg.meza.doobs.DeckedOutOBS.LOGGER.warn("Decked Out Card Callout: could not register commands", t); }
    }

    private static Object proxy(Class<?> iface, Consumer<Object[]> handler) {
        return Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, (p, m, args) -> {
            if (m.getDeclaringClass() == Object.class) {
                switch (m.getName()) {
                    case "hashCode": return System.identityHashCode(p);
                    case "equals": return p == args[0];
                    default: return "DeckedOutListener";
                }
            }
            if (!m.isDefault() || m.getName().startsWith("on") || m.getName().equals("register")) handler.accept(args);
            return null;
        });
    }

    private static void hook(String ifaceName, String holderName, String field, Consumer<Object[]> handler) throws Exception {
        Class<?> iface = Class.forName(ifaceName);
        Object event = Class.forName(holderName).getField(field).get(null);
        Method reg = event.getClass().getMethod("register", Object.class);
        reg.setAccessible(true);
        reg.invoke(event, proxy(iface, handler));
    }

    private static void registerHud() throws Exception {
        try {
            hook("net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback",
                 "net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback", "EVENT",
                 a -> CardHud.render(a[0]));
            return;
        } catch (ClassNotFoundException ignored) { }
        // newer Fabric API: HudElementRegistry
        Class<?> reg = Class.forName("net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry");
        Class<?> elem = Class.forName("net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement");
        Class<?> van = Class.forName("net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements");
        Object after = van.getField("CHAT").get(null);
        Object id = CardHud.makeId("decked-out-obs", "card_overlay");
        for (Method m : reg.getMethods())
            if (m.getName().equals("attachElementAfter") && m.getParameterCount() == 3) {
                m.invoke(null, after, id, proxy(elem, a -> CardHud.render(a[0])));
                return;
            }
        throw new NoSuchMethodException("HudElementRegistry.attachElementAfter");
    }
}
