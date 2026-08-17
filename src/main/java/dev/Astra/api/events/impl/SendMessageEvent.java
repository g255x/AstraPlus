/*
 * Decompiled with CFR 0.152.
 */
package dev.Astra.api.events.impl;

import dev.Astra.api.events.Event;

public class SendMessageEvent
extends Event {
    private static final SendMessageEvent INSTANCE = new SendMessageEvent();
    public String defaultMessage;
    public String message;

    private SendMessageEvent() {
    }

    public static SendMessageEvent get(String message) {
        SendMessageEvent.INSTANCE.defaultMessage = message;
        SendMessageEvent.INSTANCE.message = message;
        INSTANCE.setCancelled(false);
        return INSTANCE;
    }
}

