package dev.Astra.mod.modules.impl.combat;

import dev.Astra.Astra;
import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.UpdateEvent;
import dev.Astra.api.utils.combat.CombatUtil;
import dev.Astra.api.utils.math.PredictUtil;
import dev.Astra.api.utils.math.Timer;
import dev.Astra.api.utils.player.EntityUtil;
import dev.Astra.api.utils.player.InventoryUtil;
import dev.Astra.api.utils.world.BlockUtil;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.impl.exploit.Blink;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

public class HoleFiller extends Module {
    public static HoleFiller INSTANCE;
    public final SliderSetting placeDelay = this.add(new SliderSetting("PlaceDelay", 200.0, 0, 500).setSuffix("ms"));
    public final BooleanSetting inAirPause = this.add(new BooleanSetting("InAirPause", false));
    private final Timer timer = new Timer();
    private final SliderSetting blocksPer = this.add(new SliderSetting("BlocksPer", 4, 1, 8));
    private final SliderSetting placeRange = this.add(new SliderSetting("PlaceRange", 4.5, 0.0, 8.0, 0.1).setSuffix("m"));
    private final SliderSetting enemyRange = this.add(new SliderSetting("EnemyRange", 6.0, 0.0, 8.0, 0.1).setSuffix("m"));
    private final SliderSetting holeRange = this.add(new SliderSetting("HoleRange", 2.0, 0.0, 8.0, 0.1).setSuffix("m"));
    private final SliderSetting selfRange = this.add(new SliderSetting("SelfRange", 2.0, 0.0, 8.0, 0.1).setSuffix("m"));
    private final SliderSetting predictTicks = this.add(new SliderSetting("Predict", 4, 1, 8).setSuffix("ticks"));
    private final BooleanSetting detectMining = this.add(new BooleanSetting("DetectMining", true));
    private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
    private final BooleanSetting packetPlace = this.add(new BooleanSetting("PacketPlace", true));
    private final BooleanSetting breakCrystal = this.add(new BooleanSetting("Break", true).setParent());
    private final BooleanSetting usingPauseCrystal = this.add(new BooleanSetting("UsingPause", true, this.breakCrystal::isOpen));
    private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", true));
    private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
    private final BooleanSetting web = this.add(new BooleanSetting("Web", false));
    int progress = 0;

    public HoleFiller() {
        super("HoleFiller", Module.Category.Combat);
        this.setChinese("自动填坑");
        INSTANCE = this;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (!this.timer.passed((long)this.placeDelay.getValue())) {
            return;
        }
        if (this.inventory.getValue() && !EntityUtil.inInventory()) {
            return;
        }
        this.progress = 0;
        if (this.getBlock() == -1) {
            return;
        }
        if (Blink.INSTANCE.isOn() && Blink.INSTANCE.pauseModule.getValue()) {
            return;
        }
        if (this.usingPause.getValue() && HoleFiller.mc.player.isUsingItem()) {
            return;
        }
        if (this.inAirPause.getValue() && !HoleFiller.mc.player.isOnGround()) {
            return;
        }
        CombatUtil.getEnemies(this.enemyRange.getValue()).stream().flatMap(enemy -> BlockUtil.getSphere(this.holeRange.getValueFloat(), PredictUtil.getPos(enemy, this.predictTicks.getValueInt())).stream()).filter(pos -> pos.toCenterPos().distanceTo(HoleFiller.mc.player.getPos()) > this.selfRange.getValue() && (Astra.HOLE.isHole((BlockPos)pos, true, true, false) || Astra.HOLE.isDoubleHole((BlockPos)pos))).distinct().forEach(this::tryPlaceBlock);
    }

    private void tryPlaceBlock(BlockPos pos) {
        if (pos == null) {
            return;
        }
        if (this.detectMining.getValue() && Astra.BREAK.isMining(pos)) {
            return;
        }
        if (!((double)this.progress < this.blocksPer.getValue())) {
            return;
        }
        int block = this.getBlock();
        if (block == -1) {
            return;
        }
        if (!BlockUtil.canPlace(pos, this.placeRange.getValue(), true)) {
            return;
        }
        if (this.breakCrystal.getValue()) {
            CombatUtil.attackCrystal(pos, this.rotate.getValue(), this.usingPauseCrystal.getValue());
        } else if (BlockUtil.hasEntity(pos, false)) {
            return;
        }
        int old = HoleFiller.mc.player.getInventory().selectedSlot;
        this.doSwap(block);
        BlockUtil.placeBlock(pos, this.rotate.getValue(), this.packetPlace.getValue());
        if (this.inventory.getValue()) {
            this.doSwap(block);
            EntityUtil.syncInventory();
        } else {
            this.doSwap(old);
        }
        ++this.progress;
        this.timer.reset();
    }

    private void doSwap(int slot) {
        if (this.inventory.getValue()) {
            InventoryUtil.inventorySwap(slot, HoleFiller.mc.player.getInventory().selectedSlot);
        } else {
            InventoryUtil.switchToSlot(slot);
        }
    }

    private int getBlock() {
        if (this.inventory.getValue()) {
            if (this.web.getValue() && InventoryUtil.findBlockInventorySlot(Blocks.COBWEB) != -1) {
                return InventoryUtil.findBlockInventorySlot(Blocks.COBWEB);
            }
            return InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN);
        }
        if (this.web.getValue() && InventoryUtil.findBlock(Blocks.COBWEB) != -1) {
            return InventoryUtil.findBlock(Blocks.COBWEB);
        }
        return InventoryUtil.findBlock(Blocks.OBSIDIAN);
    }
}