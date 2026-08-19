/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  by.radioegor146.nativeobfuscator.Native
 *  com.google.common.collect.Lists
 *  net.minecraft.client.network.AbstractClientPlayerEntity
 *  net.minecraft.entity.Entity
 *  net.minecraft.util.math.BlockPos
 */
package dev.Astra.core.impl;

import com.google.common.collect.Lists;
import dev.Astra.Astra;
import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.ClientTickEvent;
import dev.Astra.api.events.impl.UpdateEvent;
import dev.Astra.api.utils.Wrapper;
import dev.Astra.api.utils.render.JelloUtil;
import dev.Astra.api.utils.world.BlockUtil;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.impl.client.ClientSetting;
import dev.Astra.mod.modules.impl.combat.AutoAnchor;
import dev.Astra.mod.modules.impl.combat.AutoCrystal;
import dev.Astra.mod.modules.impl.render.HoleESP;
import dev.Astra.mod.modules.impl.render.PlaceRender;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadManager
implements Wrapper {
    // use a bounded pool to avoid unbounded thread creation
    public static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    public static ClientService clientService;
    public volatile Iterable<Entity> threadSafeEntityList = Collections.emptyList();
    public volatile List<AbstractClientPlayerEntity> threadSafePlayersList = Collections.emptyList();
    public volatile boolean tickRunning = false;

    public ThreadManager() {
        this.init();
    }

    public void init() {
        Astra.EVENT_BUS.subscribe(this);
        clientService = new ClientService();
        clientService.setName("AstraClientService");
        clientService.setDaemon(true);
        clientService.start();
    }

    public Iterable<Entity> getEntities() {
        return this.threadSafeEntityList;
    }

    public List<AbstractClientPlayerEntity> getPlayers() {
        return this.threadSafePlayersList;
    }

    public void execute(Runnable runnable) {
        EXECUTOR.execute(runnable);
    }

    @EventListener(priority=200)
    public void onEvent(ClientTickEvent event) {
        Astra.POP.onUpdate();
        Astra.SERVER.onUpdate();
        if (event.isPre()) {
            JelloUtil.updateJello();
            this.tickRunning = true;
            BlockUtil.placedPos.forEach(pos -> PlaceRender.INSTANCE.create((BlockPos)pos));
            BlockUtil.placedPos.clear();
            Astra.PLAYER.onUpdate();
            if (!Module.nullCheck()) {
                Astra.EVENT_BUS.post(UpdateEvent.INSTANCE);
            }
        } else {
            this.tickRunning = false;
            if (ThreadManager.mc.world == null || ThreadManager.mc.player == null) {
                return;
            }
            this.threadSafeEntityList = Lists.newArrayList((Iterable)ThreadManager.mc.world.getEntities());
            this.threadSafePlayersList = Lists.newArrayList((Iterable)ThreadManager.mc.world.getPlayers());
        }
        if (!clientService.isAlive() || clientService.isInterrupted()) {
            clientService = new ClientService();
            clientService.setName("AstraService");
            clientService.setDaemon(true);
            clientService.start();
        }
    }

    public class ClientService
    extends Thread {
        @Override
        public void run() {
            long lastErrorTime = 0L;
            while (true) {
                try {
                    if (ThreadManager.this.tickRunning) {
                        // when waiting for a tick, yield a bit instead of spinning
                        Thread.sleep(5);
                    } else {
                        AutoCrystal.INSTANCE.onThread();
                        HoleESP.INSTANCE.onThread();
                        AutoAnchor.INSTANCE.onThread();
                        // small pause between iterations to avoid busy loop
                        Thread.sleep(10);
                    }
                } catch (InterruptedException ie) {
                    // propagate interruption, exit thread
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                    if (ClientSetting.INSTANCE.debug.getValue()) {
                        CommandManager.sendMessage("\u00a74An error has occurred [Thread] Message: [" + e.getMessage() + "]");
                    }
                    // cooldown before retrying after an error
                    try {
                        long now = System.currentTimeMillis();
                        if (now - lastErrorTime < 50L) {
                            Thread.sleep(50L - (now - lastErrorTime));
                        } else {
                            Thread.sleep(50L);
                        }
                        lastErrorTime = System.currentTimeMillis();
                    } catch (InterruptedException ie2) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }
}
