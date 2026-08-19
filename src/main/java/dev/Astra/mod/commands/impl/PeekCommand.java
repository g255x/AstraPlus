/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.ItemStack
 */
package dev.Astra.mod.commands.impl;

import dev.Astra.mod.commands.Command;
import dev.Astra.mod.modules.impl.misc.ShulkerViewer;
import net.minecraft.item.ItemStack;

import java.util.List;

public class PeekCommand
extends Command {
    private static final ItemStack[] ITEMS = new ItemStack[27];

    public PeekCommand() {
        super("peek", "");
    }

    @Override
    public void runCommand(String[] parameters) {
        ShulkerViewer.openContainer(PeekCommand.mc.player.getMainHandStack(), ITEMS, true);
    }

    @Override
    public String[] getAutocorrect(int count, List<String> seperated) {
        return null;
    }
}

