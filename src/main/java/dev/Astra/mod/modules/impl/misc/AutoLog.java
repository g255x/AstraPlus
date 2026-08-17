/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.Items
 *  net.minecraft.network.packet.Packet
 *  net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
 *  net.minecraft.network.packet.s2c.common.DisconnectS2CPacket
 *  net.minecraft.text.Text
 */
package dev.Astra.mod.modules.impl.misc;

import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.PacketEvent;
import dev.Astra.api.events.impl.TotemEvent;
import dev.Astra.api.events.impl.UpdateEvent;
import dev.Astra.api.utils.math.Timer;
import dev.Astra.api.utils.player.EntityUtil;
import dev.Astra.api.utils.player.InventoryUtil;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import dev.Astra.mod.modules.settings.impl.EnumSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import dev.Astra.mod.modules.settings.impl.StringSetting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Random;

public class AutoLog
        extends Module {
    public static boolean loggedOut = false;
    private final Random random = new Random();
    private final Timer lagTimer = new Timer();

    // ============ 退出理由消息池 ============
    private final List<String> exitMessages = List.of(
            "猫踩到关机键了",
            "该吃饭了，先下了",
            "外卖到了，88",
            "网费到期了，告辞",
            "突然有事，下次再打",
            "电脑蓝屏了，G",
            "停电了，不是打不过",
            "键盘进水了，寄",
            "显示器冒烟了",
            "去收衣服，先撤",
            "快递到了，拿一下",
            "水烧开了，关火去",
            "门铃响了，88",
            "家里来人了，不玩了",
            "肚子疼，厕所在召唤",
            "来电话了，接一下",
            "电脑被抢了",
            "朋友拉我打别的",
            "突然困了，睡觉去",
            "该遛狗了，溜了",
            "猫把网线咬断了",
            "楼下有情况，跑路了",
            "点的奶茶到了",
            "作业还没写，溜了",
            "该洗澡了，先下",
            "手机没电了，88",
            "突然想出去走走",
            "该做饭了，告辞",
            "电视剧更新了，去看",
            "忘了关火，先去厨房"
    );

    // ============ 退出消息模式 ============
    public enum ExitMode {
        Random,   // 随机消息
        Fixed     // 固定消息
    }

    // ============ 设置项 ============
    private final BooleanSetting logOnEnable = this.add(new BooleanSetting("LogOnEnable", false));
    private final BooleanSetting onPop = this.add(new BooleanSetting("OnPop", true));

    private final BooleanSetting lowArmor = this.add(new BooleanSetting("LowArmor", true).setParent());
    private final SliderSetting durabilityThreshold = this.add(new SliderSetting("DurabilityThreshold", 5, 0, 100, 1, this.lowArmor::isOpen).setSuffix("%"));

    private final BooleanSetting totemLess = this.add(new BooleanSetting("TotemLess", true).setParent());
    private final SliderSetting totems = this.add(new SliderSetting("Totems", 2, 0, 10, 1, this.totemLess::isOpen));

    private final BooleanSetting serverNotResponding = this.add(new BooleanSetting("ServerNotResponding", true).setParent());
    private final SliderSetting serverTimeout = this.add(new SliderSetting("ServerTimeout", 0.2, 0.0, 5.0, 0.1, this.serverNotResponding::isOpen).setSuffix("s"));

    private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true));
    private final BooleanSetting showReason = this.add(new BooleanSetting("ShowReason", false));

    private final BooleanSetting sendExitMessage = this.add(new BooleanSetting("SendExitMessage", true).setParent());
    private final EnumSetting<ExitMode> exitMode = this.add(new EnumSetting<>("ExitMode", ExitMode.Random, this.sendExitMessage::isOpen));
    private final StringSetting fixedMessage = this.add(new StringSetting("FixedMessage", "客户端崩了", () -> this.exitMode.getValue() == ExitMode.Fixed));

    public AutoLog() {
        super("AutoLog", Module.Category.Misc);
        this.setChinese("自动下线");
    }

    @Override
    public void onEnable() {
        if (this.logOnEnable.getValue()) {
            this.disconnect("Enabled");
        }
        this.lagTimer.reset();
    }

    @EventListener
    public void onPacketEvent(PacketEvent.Receive event) {
        this.lagTimer.reset();
        if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            this.lagTimer.reset();
        }
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        if (this.serverNotResponding.getValue()) {
            double timeout = this.serverTimeout.getValue();
            if (this.lagTimer.passedS(timeout)) {
                this.disconnect("Server not responding (" + String.format("%.1f", (double)this.lagTimer.getMs() / 1000.0) + "s)");
                return;
            }
        }

        if (this.totemLess.getValue()) {
            int totem = InventoryUtil.getItemCount(Items.TOTEM_OF_UNDYING);
            if ((double)totem <= this.totems.getValue()) {
                this.disconnect("You have too few totems (" + totem + ").");
                return;
            }
        }

        if (this.lowArmor.getValue()) {
            double threshold = this.durabilityThreshold.getValue();
            for (ItemStack armor : AutoLog.mc.player.getInventory().armor) {
                if (armor.isEmpty()) continue;
                double durability = EntityUtil.getDamagePercent(armor);
                if (durability >= threshold) continue;
                this.disconnect("Your armor durability is " + String.format("%.1f", durability) + "% (below " + threshold + "%).");
                return;
            }
        }
    }

    @EventListener
    public void onPop(TotemEvent event) {
        if (this.onPop.getValue() && event.getPlayer() == AutoLog.mc.player) {
            this.disconnect("You popped 1 totem!");
        }
    }

    @Override
    public void onLogout() {
        if (this.autoDisable.getValue()) {
            this.disable();
        }
    }

    // ============ 修改后的 disconnect 方法（已删除 CommandManager.sendMessage） ============
    private void disconnect(String reason) {
        loggedOut = true;

        // 发送退出消息到聊天栏
        if (sendExitMessage.getValue()) {
            String exitMsg = getExitMessage();
            if (exitMsg != null && !exitMsg.isEmpty()) {
                mc.getNetworkHandler().sendChatMessage(exitMsg);
            }
        }

        // 发送切换槽位包触发断开
        mc.getNetworkHandler().sendPacket((Packet) new UpdateSelectedSlotC2SPacket(114514));

        // 显示断开原因
        if (this.showReason.getValue()) {
            AutoLog.mc.player.networkHandler.onDisconnect(
                    new DisconnectS2CPacket((Text) Text.literal("[AutoLog] " + reason))
            );
        }
    }

    private String getExitMessage() {
        String msg = null;

        if (exitMode.getValue() == ExitMode.Random) {
            if (!exitMessages.isEmpty()) {
                msg = exitMessages.get(random.nextInt(exitMessages.size()));
            }
        } else if (exitMode.getValue() == ExitMode.Fixed) {
            msg = fixedMessage.getValue();
        }

        if (msg != null && !msg.isEmpty()) {
            return "\u00a77[\u00a7cAutoLog\u00a77] \u00a7f" + msg;
        }

        return null;
    }
}