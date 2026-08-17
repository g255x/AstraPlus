/*
 * Decompiled with CFR 0.152.
 */
package dev.Astra.mod.modules.impl.client;

import dev.Astra.Astra;
import dev.Astra.api.utils.math.Animation;
import dev.Astra.api.utils.math.Easing;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.settings.impl.*;

import java.awt.*;

public class ClientSetting
extends Module {
    public static final Animation animation = new Animation();
    public static ClientSetting INSTANCE;
    public final EnumSetting<Page> page = this.add(new EnumSetting<Page>("Page", Page.Game));
    public final BooleanSetting lowVersion = this.add(new BooleanSetting("1.12", false, () -> this.page.is(Page.Game)));
    public final BooleanSetting crawl = this.add(new BooleanSetting("Crawl", true, () -> this.page.is(Page.Game)));
    public final BooleanSetting rotations = this.add(new BooleanSetting("ShowRotations", true, () -> this.page.is(Page.Game)).setParent());
    public final BooleanSetting sync = this.add(new BooleanSetting("Sync", false, () -> this.page.is(Page.Game) && this.rotations.isOpen()));
    public final BooleanSetting titleFix = this.add(new BooleanSetting("TitleFix", true, () -> this.page.is(Page.Game)));
    public final BooleanSetting fuckFPSLimit = this.add(new BooleanSetting("FuckFPSLimit", false, () -> this.page.is(Page.Game)));
    private final BooleanSetting portalGui = this.add(new BooleanSetting("BlockTickNausea", true, () -> this.page.is(Page.Game)));
    public final BooleanSetting optimizedCalc = this.add(new BooleanSetting("OptimizedCalc", false, () -> this.page.is(Page.Game)));
    public final BooleanSetting mioCompatible = this.add(new BooleanSetting("MioCompatible", false, () -> this.page.is(Page.Game)));
    public final StringSetting prefix = this.add(new StringSetting("Prefix", ";", () -> this.page.is(Page.Misc)));
    public final BooleanSetting chinese = this.add(new BooleanSetting("Chinese", true, () -> this.page.is(Page.Misc)));
    public final BooleanSetting titleOverride = this.add(new BooleanSetting("TitleOverride", true, () -> this.page.is(Page.Misc)).setParent());
    public final StringSetting windowTitle = this.add(new StringSetting("WindowTitle", Astra.NAME, () -> this.page.is(Page.Misc) && this.titleOverride.isOpen()));
    public final BooleanSetting debug = this.add(new BooleanSetting("DebugException", true, () -> this.page.is(Page.Misc)));
    public final BooleanSetting caughtException = this.add(new BooleanSetting("CaughtException", false, () -> this.page.is(Page.Misc)).setParent());
    public final BooleanSetting log = this.add(new BooleanSetting("Log", true, () -> this.page.is(Page.Misc) && this.caughtException.isOpen()));
    private final BooleanSetting hotbar = this.add(new BooleanSetting("HotbarAnim", true, () -> this.page.is(Page.Gui)));
    public final SliderSetting hotbarTime = this.add(new SliderSetting("HotbarTime", 100, 0, 1000, () -> this.page.is(Page.Gui)));
    public final EnumSetting<Easing> animEase = this.add(new EnumSetting<Easing>("AnimEase", Easing.CubicInOut, () -> this.page.is(Page.Gui)));
    public final BooleanSetting darkening = this.add(new BooleanSetting("Darkening", true, () -> this.page.is(Page.Gui)));
    public final StringSetting hackName = this.add(new StringSetting("Notification", "[" + Astra.NAME + "]", () -> this.page.getValue() == Page.Notification));
    public final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 38, 38), () -> this.page.getValue() == Page.Notification));
    public final EnumSetting<Style> messageStyle = this.add(new EnumSetting<Style>("Style", Style.Mio, () -> this.page.getValue() == Page.Notification));
    public final BooleanSetting toggle = this.add(new BooleanSetting("ModuleToggle", true, () -> this.page.getValue() == Page.Notification).setParent());
    public final BooleanSetting onlyOne = this.add(new BooleanSetting("OnlyOne", false, () -> this.page.getValue() == Page.Notification && this.toggle.isOpen()));
    public final BooleanSetting keepHistory = this.add(new BooleanSetting("KeepHistory", true, () -> this.page.getValue() == Page.ChatHud));
    public final BooleanSetting infiniteChat = this.add(new BooleanSetting("InfiniteChat", true, () -> this.page.getValue() == Page.ChatHud));
    public final BooleanSetting hideIndicator = this.add(new BooleanSetting("HideIndicator", true, () -> this.page.getValue() == Page.ChatHud));
    public final SliderSetting animationTime = this.add(new SliderSetting("AnimationTime", 300, 0, 1000, () -> this.page.getValue() == Page.ChatHud));
    public final EnumSetting<Easing> ease = this.add(new EnumSetting<Easing>("Ease", Easing.CubicInOut, () -> this.page.getValue() == Page.ChatHud));
    public final SliderSetting animateOffset = this.add(new SliderSetting("Offset", -40, -200, 100, () -> this.page.getValue() == Page.ChatHud));
    public final BooleanSetting fade = this.add(new BooleanSetting("Fade", true, () -> this.page.getValue() == Page.ChatHud));

    public ClientSetting() {
        super("ClientSetting", Module.Category.Client);
        this.setChinese("\u5ba2\u6237\u7aef\u8bbe\u7f6e");
        INSTANCE = this;
    }

    public boolean portalGui() {
        return this.isOn() && this.portalGui.getValue();
    }

    public boolean hotbar() {
        return this.isOn() && this.hotbar.getValue();
    }

    @Override
    public void enable() {
        this.state = true;
    }

    @Override
    public void disable() {
        this.state = true;
    }

    @Override
    public boolean isOn() {
        return true;
    }

    public static enum Page {
        Game,
        Gui,
        Misc,
        Notification,
        ChatHud;
    }

    public static enum Style {
        Mio,
        Debug,
        Lowercase,
        Normal,
        Future,
        Earth,
        Moon,
        Melon,
        Chinese,
        None;
    }
}
