package dev.Astra.mod.modules;

import dev.Astra.Astra;
import dev.Astra.core.impl.CommandManager;
import dev.Astra.mod.Mod;
import dev.Astra.mod.modules.impl.client.*;
import dev.Astra.mod.modules.impl.hud.HudSetting;
import dev.Astra.mod.modules.settings.Setting;
import dev.Astra.mod.modules.settings.impl.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;
import java.util.List;

public abstract class Module extends Mod {
    public final BooleanSetting drawn;
    private final List<Setting> settings = new ArrayList<>();
    private final String description;
    private final Category category;
    private final BindSetting bindSetting;
    protected boolean state;
    private String chinese;

    public Module(String name, Category category) {
        this(name, "", category);
    }

    public Module(String name, String description, Category category) {
        super(name);
        this.category = category;
        this.description = description;
        this.bindSetting = this.add(new BindSetting("Key", this.isGuiModule() ? 344 : -1));
        this.drawn = this.add(new BooleanSetting("Drawn", !this.hideInModuleList()));
    }

    private boolean isGuiModule() { return this instanceof ClickGui; }
    private boolean hideInModuleList() { return this instanceof ColorsModule || this instanceof BaritoneModule || this instanceof AntiCheat || this instanceof ClientSetting || this instanceof HudSetting || this.getName().equals("Info"); }

    public void setChinese(String chinese) { this.chinese = chinese; }
    public String getArrayName() { return getDisplayName() + getArrayInfo(); }
    public String getArrayInfo() { return getInfo() == null ? "" : " §7[§f" + getInfo() + "§7]"; }
    public String getInfo() { return null; }
    public String getDisplayName() { return (ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.chinese.getValue() && chinese != null) ? chinese : getName(); }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public BindSetting getBindSetting() { return bindSetting; }
    public boolean isOn() { return state; }
    public boolean isOff() { return !isOn(); }
    public void toggle() { if (isOn()) disable(); else enable(); }

    public void enable() {
        if (state) return;
        ClientSetting cs = ClientSetting.INSTANCE;
        boolean shouldSend = !nullCheck() && drawn.getValue() && cs != null && cs.toggle.getValue();
        if (shouldSend) {
            int id = cs.onlyOne.getValue() ? -1 : System.identityHashCode(this);
            switch (cs.messageStyle.getValue()) {
                case Mio: CommandManager.sendMessageId("§2[+] §f" + getDisplayName(), id); break;
                case Debug: CommandManager.sendMessageId(getCategory().name().toLowerCase() + "." + getDisplayName().toLowerCase() + ".§aenable", id); break;
                case Lowercase: CommandManager.sendMessageId(getDisplayName().toLowerCase() + " §aenabled", id); break;
                case Melon: CommandManager.sendMessageId("§b" + getDisplayName() + " §aEnabled.", id); break;
                case Normal: CommandManager.sendMessageId("§f" + getDisplayName() + " §aEnabled", id); break;
                case Future: CommandManager.sendMessageId("§7" + getDisplayName() + " toggled §aon", id); break;
                case Chinese: CommandManager.sendMessageId(getDisplayName() + " §a开启", id); break;
                case Moon: CommandManager.sendChatMessageWidthIdNoSync("§f[§b" + cs.hackName.getValue() + "§f] [§3" + getDisplayName() + "§f] §7toggled §aon", id); break;
                case Earth: CommandManager.sendChatMessageWidthIdNoSync("§l" + getDisplayName() + " §aenabled.", id); break;
            }
        }
        if (Astra.MODULE != null) Astra.MODULE.showToggleBanner(this, true);
        state = true;
        Astra.EVENT_BUS.subscribe(this);
        onToggle();
        onEnable();
    }

    public void disable() {
        if (!state) return;
        ClientSetting cs = ClientSetting.INSTANCE;
        boolean shouldSend = !nullCheck() && drawn.getValue() && cs != null && cs.toggle.getValue();
        if (shouldSend) {
            int id = cs.onlyOne.getValue() ? -1 : System.identityHashCode(this);
            switch (cs.messageStyle.getValue()) {
                case Mio: CommandManager.sendMessageId("§4[-] §f" + getDisplayName(), id); break;
                case Debug: CommandManager.sendMessageId(getCategory().name().toLowerCase() + "." + getDisplayName().toLowerCase() + ".§cdisable", id); break;
                case Lowercase: CommandManager.sendMessageId(getDisplayName().toLowerCase() + " §cdisabled", id); break;
                case Normal: CommandManager.sendMessageId("§f" + getDisplayName() + " §cDisabled", id); break;
                case Melon: CommandManager.sendMessageId("§b" + getDisplayName() + " §cDisabled.", id); break;
                case Future: CommandManager.sendMessageId("§7" + getDisplayName() + " toggled §coff", id); break;
                case Earth: CommandManager.sendChatMessageWidthIdNoSync("§l" + getDisplayName() + " §cdisabled.", id); break;
                case Chinese: CommandManager.sendMessageId(getDisplayName() + " §c关闭", id); break;
                case Moon: CommandManager.sendChatMessageWidthIdNoSync("§f[§b" + cs.hackName.getValue() + "§f] [§3" + getDisplayName() + "§f] §7toggled §coff", id); break;
            }
        }
        if (Astra.MODULE != null) Astra.MODULE.showToggleBanner(this, false);
        state = false;
        Astra.EVENT_BUS.unsubscribe(this);
        onToggle();
        onDisable();
    }

    public void sendMessage(String message) { CommandManager.sendMessage(message); }
    public void setState(boolean state) { if (this.state != state) { if (state) enable(); else disable(); } }

    public boolean setBind(String rkey) {
        if (rkey.equalsIgnoreCase("none")) { bindSetting.setValue(-1); return true; }
        try {
            int key = InputUtil.fromTranslationKey("key.keyboard." + rkey.toLowerCase()).getCode();
            if (key == 0) return false;
            bindSetting.setValue(key);
            return true;
        } catch (NumberFormatException e) {
            if (!nullCheck()) sendMessage("§4Bad bind!");
            return false;
        }
    }

    public void onDisable() {}
    public void onEnable() {}
    public void onToggle() {}
    public void onLogin() {}
    public void onLogout() {}
    public void onRender2D(DrawContext drawContext, float tickDelta) {}
    public void onRender3D(MatrixStack matrixStack) {}

    public void addSetting(Setting setting) { settings.add(setting); }
    public StringSetting add(StringSetting setting) { addSetting(setting); return setting; }
    public ColorSetting add(ColorSetting setting) { addSetting(setting); return setting; }
    public SliderSetting add(SliderSetting setting) { addSetting(setting); return setting; }
    public BooleanSetting add(BooleanSetting setting) { addSetting(setting); return setting; }
    public <T extends Enum<T>> EnumSetting<T> add(EnumSetting<T> setting) { addSetting(setting); return setting; }
    public BindSetting add(BindSetting setting) { addSetting(setting); return setting; }
    public List<Setting> getSettings() { return settings; }

    public static boolean nullCheck() { return mc.player == null || mc.player.input == null || mc.world == null; }

    public static void sendSequencedPacket(SequencedPacketCreator packetCreator) {
        if (mc.getNetworkHandler() == null || mc.world == null) return;
        try (PendingUpdateManager pendingUpdateManager = mc.world.getPendingUpdateManager().incrementSequence()) {
            mc.getNetworkHandler().sendPacket(packetCreator.predict(pendingUpdateManager.getSequence()));
        }
    }

    public enum Category {
        Combat { public String getIcon() { return "b"; } },
        Misc   { public String getIcon() { return "["; } },
        Render { public String getIcon() { return "a"; } },
        Movement { public String getIcon() { return "8"; } },
        Player  { public String getIcon() { return "5"; } },
        Exploit { public String getIcon() { return "6"; } },
        Client  { public String getIcon() { return "7"; } };

        public abstract String getIcon();

        public String getDisplayName() {
            if (ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.chinese.getValue()) {
                switch (this) {
                    case Combat: return "战斗类";
                    case Misc: return "杂项";
                    case Render: return "渲染类";
                    case Movement: return "移动类";
                    case Player: return "玩家类";
                    case Exploit: return "漏洞类";
                    case Client: return "客户端类";
                }
            }
            return name();
        }
    }
}