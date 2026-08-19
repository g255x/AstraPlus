/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  by.radioegor146.nativeobfuscator.Native
 *  net.minecraft.entity.player.PlayerEntity
 *  org.apache.commons.io.IOUtils
 */
package dev.Astra.core.impl;

import dev.Astra.core.Manager;
import net.minecraft.entity.player.PlayerEntity;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FriendManager
        extends Manager {
    public final ArrayList<String> friendList = new ArrayList();

    public FriendManager() {
        this.read();
    }

    public boolean isFriend(String name) {
        return this.friendList.contains(name);
    }

    public boolean isFriend(PlayerEntity entity) {
        return this.isFriend(entity.getGameProfile().getName());
    }

    public void remove(String name) {
        this.friendList.remove(name);
    }

    public void add(String name) {
        if (!this.friendList.contains(name)) {
            this.friendList.add(name);
        }
    }

    public void friend(PlayerEntity entity) {
        this.friend(entity.getGameProfile().getName());
    }

    public void friend(String name) {
        if (this.friendList.contains(name)) {
            this.friendList.remove(name);
        } else {
            this.friendList.add(name);
        }
    }

    public void read() {
        try {
            File friendFile = FriendManager.getFile("friends.txt");
            if (!friendFile.exists()) {
                return;
            }
            List<String> list = IOUtils.readLines((InputStream)new FileInputStream(friendFile), (Charset)StandardCharsets.UTF_8);
            for (String s : list) {
                this.add(s);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void save() {
        try {
            File friendFile = FriendManager.getFile("friends.txt");
            try (PrintWriter printwriter = new PrintWriter(new OutputStreamWriter((OutputStream)new FileOutputStream(friendFile), StandardCharsets.UTF_8))) {
                for (String str : this.friendList) {
                    printwriter.println(str);
                }
            }
        }
        catch (Exception exception) {
            exception.printStackTrace();
            System.out.println("[Astra Client] Failed to save friends");
        }
    }
}