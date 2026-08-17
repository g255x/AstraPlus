/*
 * Decompiled with CFR 0.152.
 */
package dev.Astra.api.events.impl;

import dev.Astra.api.events.Event;

public class DoAttackEvent
extends Event {
    public static final DoAttackEvent INSTANCE = new DoAttackEvent();

    public static DoAttackEvent getPre() {
        DoAttackEvent.INSTANCE.stage = Event.Stage.Pre;
        return INSTANCE;
    }

    public static DoAttackEvent getPost() {
        DoAttackEvent.INSTANCE.stage = Event.Stage.Post;
        return INSTANCE;
    }
}

