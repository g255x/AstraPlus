/*
 * Decompiled with CFR 0.152.
 */
package dev.Astra.mod.commands.impl;

import dev.Astra.mod.commands.Command;
import dev.Astra.mod.modules.impl.hud.WaterMarkHudModule;

import java.util.Arrays;
import java.util.List;

public class WatermarkCommand
extends Command {
    public WatermarkCommand() {
        super("watermark", "[text]");
    }

    @Override
    public void runCommand(String[] parameters) {
        if (parameters.length == 0) {
            this.sendUsage();
            return;
        }
        StringBuilder text = new StringBuilder();
        boolean first = true;
        for (String s : Arrays.stream(parameters).toList()) {
            if (first) {
                text.append(s);
                first = false;
                continue;
            }
            text.append(" ").append(s);
        }
        WaterMarkHudModule.INSTANCE.title.setValue(text.toString());
    }

    @Override
    public String[] getAutocorrect(int count, List<String> seperated) {
        return null;
    }
}

