/*
 * Decompiled with CFR 0.152.
 */
package dev.Astra.api.interfaces;

import dev.Astra.api.utils.math.FadeUtils;

public interface IChatHudLineHook {
    public int frogClient$getMessageId();

    public void frogClient$setMessageId(int var1);

    public boolean frogClient$getSync();

    public void frogClient$setSync(boolean var1);

    public FadeUtils frogClient$getFade();

    public void frogClient$setFade(FadeUtils var1);
}

