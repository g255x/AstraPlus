/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.MinecraftClient
 */
package dev.Astra.core;

import dev.Astra.Astra;
import net.minecraft.client.MinecraftClient;

import java.io.File;

public class Manager {
    public static final MinecraftClient mc = MinecraftClient.getInstance();

    public static File getFile(String s) {
        File folder = Manager.getFolder();
        return new File(folder, s);
    }

    public static File getFolder() {
        File folder = new File(Manager.mc.runDirectory.getPath() + File.separator + Astra.CONFIG_DIR);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    }
}
