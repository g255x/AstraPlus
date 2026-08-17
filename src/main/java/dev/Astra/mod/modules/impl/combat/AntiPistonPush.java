package dev.Astra.mod.modules.impl.combat;

import dev.Astra.Astra;
import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.ClientTickEvent;
import dev.Astra.api.events.impl.RotationEvent;
import dev.Astra.api.utils.math.Timer;
import dev.Astra.api.utils.player.EntityUtil;
import dev.Astra.api.utils.player.InventoryUtil;
import dev.Astra.api.utils.world.BlockPosX;
import dev.Astra.api.utils.world.BlockUtil;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.impl.client.AntiCheat;
import dev.Astra.mod.modules.impl.exploit.Blink;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import net.minecraft.util.Hand;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.hit.BlockHitResult;

public class AntiPistonPush extends Module {
    public static AntiPistonPush INSTANCE;
    public final BooleanSetting onlyBlock = this.add(new BooleanSetting("OnlyBurrow", true));
    public final SliderSetting placeDelay = this.add(new SliderSetting("PlaceDelay", 50, 0, 500));
    public final SliderSetting blocksPer = this.add(new SliderSetting("BlocksPer", 2, 1, 10));
    public final BooleanSetting feet = this.add(new BooleanSetting("Feet", true));
    public final BooleanSetting face = this.add(new BooleanSetting("Face", true));
    public final BooleanSetting inventorySwap = this.add(new BooleanSetting("InventorySwap", true));
    public final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
    public final BooleanSetting swing = this.add(new BooleanSetting("Swing", true));
    public final BooleanSetting autoburrow = this.add(new BooleanSetting("AutoBurrow", false));
    private final Timer timer = new Timer();
    private boolean webPlaced = false;
    public Vec3d directionVec = null;
    int progress = 0;
    final ArrayList<BlockPos> pos = new ArrayList<>();

    public AntiPistonPush() {
        super("AntiPistonPush", Module.Category.Combat);
        this.setChinese("防活塞推");
        INSTANCE = this;
    }

    @Override
    public String getInfo() {
        return this.pos.isEmpty() ? null : "Working";
    }

    @EventListener
    public void onRotate(RotationEvent event) {
        if (this.rotate.getValue() && this.directionVec != null) {
            Astra.ROTATION.lookAt(this.directionVec);
        }
    }

    @EventListener
    public void onTick(ClientTickEvent event) {
        if (!nullCheck()) {
            this.update();
        }
    }

    private void update() {
        if (this.timer.passed((long) this.placeDelay.getValueInt())) {
            if (!this.inventorySwap.getValue() || EntityUtil.inInventory()) {
                if (!this.onlyBlock.getValue() || EntityUtil.isInsideBlock()) {
                    this.pos.clear();
                    this.progress = 0;
                    this.directionVec = null;
                    if (this.getWebSlot() != -1) {
                        if (Blink.INSTANCE == null || !Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
                            Vec3d playerPos = mc.player.getPos();
                            if (this.feet.getValue()) {
                                this.placeWeb(new BlockPosX(playerPos.getX(), playerPos.getY(), playerPos.getZ()));
                            }
                            if (this.face.getValue()) {
                                this.placeWeb(new BlockPosX(playerPos.getX(), playerPos.getY() + 1.1, playerPos.getZ()));
                            }
                            if (this.webPlaced && !this.isBurrowed() && this.autoburrow.getValue() && AutoWeb.INSTANCE != null && !AutoWeb.INSTANCE.isOn()) {
                                AutoWeb.INSTANCE.enable();
                                this.webPlaced = false;
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean placeWeb(BlockPos pos) {
        if (this.pos.contains(pos)) {
            return false;
        } else {
            this.pos.add(pos);
            if (this.progress >= this.blocksPer.getValueInt()) {
                return false;
            } else if (this.getWebSlot() == -1) {
                return false;
            } else if (BlockUtil.getPlaceSide(pos) != null && (mc.world.isAir(pos) || BlockUtil.getBlock(pos) == Blocks.COBWEB)) {
                int oldSlot = mc.player.getInventory().selectedSlot;
                int webSlot = this.getWebSlot();
                if (!this.placeBlock(pos, this.rotate.getValue(), webSlot)) {
                    return false;
                } else {
                    BlockUtil.placedPos.add(pos);
                    ++this.progress;
                    this.webPlaced = true;
                    if (this.inventorySwap.getValue()) {
                        this.doSwap(webSlot);
                        EntityUtil.syncInventory();
                    } else {
                        this.doSwap(oldSlot);
                    }
                    this.timer.reset();
                    return true;
                }
            } else {
                return false;
            }
        }
    }

    public boolean placeBlock(BlockPos pos, boolean rotate, int slot) {
        Direction side = BlockUtil.getPlaceSide(pos);
        if (side == null) {
            return BlockUtil.allowAirPlace() ? this.clickBlock(pos, Direction.DOWN, rotate, slot) : false;
        } else {
            return this.clickBlock(pos.offset(side), side.getOpposite(), rotate, slot);
        }
    }

    public boolean clickBlock(BlockPos pos, Direction side, boolean rotate, int slot) {
        Vec3d directionVec = new Vec3d((double) pos.getX() + 0.5 + (double) side.getVector().getX() * 0.5, (double) pos.getY() + 0.5 + (double) side.getVector().getY() * 0.5, (double) pos.getZ() + 0.5 + (double) side.getVector().getZ() * 0.5);
        if (rotate) {
            this.directionVec = directionVec;
            Astra.ROTATION.lookAt(directionVec);
        }
        this.doSwap(slot);
        BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
        Module.sendSequencedPacket((id) -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, id));
        if (this.swing.getValue()) {
            EntityUtil.swingHand(Hand.MAIN_HAND, AntiCheat.INSTANCE.interactSwing.getValue());
        }
        if (rotate) {
            Astra.ROTATION.snapBack();
        }
        return true;
    }

    private void doSwap(int slot) {
        if (this.inventorySwap.getValue()) {
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
        } else {
            InventoryUtil.switchToSlot(slot);
        }
    }

    private int getWebSlot() {
        return this.inventorySwap.getValue() ? InventoryUtil.findBlockInventorySlot(Blocks.COBWEB) : InventoryUtil.findBlock(Blocks.COBWEB);
    }

    private boolean isBurrowed() {
        BlockPos playerPos = mc.player.getBlockPos();
        return !mc.world.isAir(playerPos);
    }
}