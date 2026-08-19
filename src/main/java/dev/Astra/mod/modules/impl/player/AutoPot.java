package dev.Astra.mod.modules.impl.player;

import dev.Astra.Astra;
import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.ClientTickEvent;
import dev.Astra.api.events.impl.UpdateEvent;
import dev.Astra.api.utils.Wrapper;
import dev.Astra.api.utils.math.Timer;
import dev.Astra.api.utils.player.EntityUtil;
import dev.Astra.api.utils.player.InventoryUtil;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.impl.combat.Aura;
import dev.Astra.mod.modules.settings.impl.BindSetting;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class AutoPot extends Module {
    public static AutoPot INSTANCE;

    // 閫氱敤璁剧疆
    private final SliderSetting delay = this.add(new SliderSetting("Delay", 0.2, 0.0, 10.0, 0.1).setSuffix("s"));
    private final SliderSetting prePotTime = this.add(new SliderSetting("PrePotTime", 0.5, 0.0, 1.0, 0.01).setSuffix("s"));
    private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", false));
    private final BooleanSetting onlyGround = this.add(new BooleanSetting("OnlyGround", true));
    private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));

    // 鍏叡鍙橀噺
    private final SliderSetting targetRange = this.add(new SliderSetting("TargetRange", 12.0, 0.0, 20.0, 0.1));
    private final SliderSetting health = this.add(new SliderSetting("Health", 16.0, 0.0, 36.0, 0.1));

    // 速度药水（无子选项）
    private final BooleanSetting speed = this.add(new BooleanSetting("Speed", true));

    // 抗性药水（无子选项）
    private final BooleanSetting resistance = this.add(new BooleanSetting("Resistance", true));

    // 力量药水（有子选项）
    private final BooleanSetting strength = this.add(new BooleanSetting("Strength", true).setParent());
    private final BooleanSetting onlyAuraStrength = this.add(new BooleanSetting("OnlyAura", true, () -> this.strength.isOpen()));

    // ----- 强制喷药快捷键 -----
    private final BindSetting speedKey = this.add(new BindSetting("SpeedKey", -1));
    private final BindSetting resistanceKey = this.add(new BindSetting("ResistanceKey", -1));
    private final BindSetting strengthKey = this.add(new BindSetting("StrengthKey", -1));

    private final Timer delayTimer = new Timer();
    private boolean throwing = false;
    // 用于防止按键重复触发
    private boolean speedPressed = false;
    private boolean resistancePressed = false;
    private boolean strengthPressed = false;
    private final AutoPotTick autoPotTick;

    public AutoPot() {
        super("AutoPot", Module.Category.Player);
        this.setChinese("自动药水");
        INSTANCE = this;
        this.autoPotTick = new AutoPotTick();
        Astra.EVENT_BUS.subscribe(this.autoPotTick);
    }

    // ========== 宸ュ叿鏂规硶 ==========
    public static int findPotionInventorySlot(StatusEffect targetEffect) {
        for (int i = 35; i >= 0; --i) {
            ItemStack itemStack = AutoPot.mc.player.getInventory().getStack(i);
            if (Item.getRawId(itemStack.getItem()) != Item.getRawId(Items.SPLASH_POTION)) continue;
            PotionContentsComponent potionContentsComponent = itemStack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);
            for (StatusEffectInstance effect : potionContentsComponent.getEffects()) {
                if (effect.getEffectType().value() != targetEffect) continue;
                return i < 9 ? i + 36 : i;
            }
        }
        return -1;
    }

    public static int findPotion(StatusEffect targetEffect) {
        for (int i = 0; i < 9; ++i) {
            ItemStack itemStack = AutoPot.mc.player.getInventory().getStack(i);
            if (Item.getRawId(itemStack.getItem()) != Item.getRawId(Items.SPLASH_POTION)) continue;
            PotionContentsComponent potionContentsComponent = itemStack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);
            for (StatusEffectInstance effect : potionContentsComponent.getEffects()) {
                if (effect.getEffectType().value() != targetEffect) continue;
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onDisable() {
        this.throwing = false;
        this.speedPressed = false;
        this.resistancePressed = false;
        this.strengthPressed = false;
        Astra.EVENT_BUS.unsubscribe(this.autoPotTick);
    }

    /**
     * 地面检测：从眼睛位置到碰撞箱底部中心下方 0.1 格做射线检测，碰到实体固体方块且不是蜘蛛网则返回 true
     */
    private boolean isNearGround() {
        Vec3d eyePos = mc.player.getEyePos();
        // 碰撞箱底部中心
        Vec3d bottomCenter = mc.player.getBoundingBox().getCenter();
        bottomCenter = new Vec3d(bottomCenter.x, mc.player.getBoundingBox().minY, bottomCenter.z);
        // 再向下 0.1 格作为终点
        Vec3d end = bottomCenter.add(0, -0.1, 0);
        BlockHitResult hit = mc.world.raycast(new RaycastContext(eyePos, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            var blockState = mc.world.getBlockState(hit.getBlockPos());
            return blockState.isSolidBlock(mc.world, hit.getBlockPos()) && blockState.getBlock() != Blocks.COBWEB;
        }
        return false;
    }

    private boolean hasNearbyPlayer() {
        double rangeSq = this.targetRange.getValue() * this.targetRange.getValue();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (mc.player.squaredDistanceTo(player) <= rangeSq) {
                return true;
            }
        }
        return false;
    }

    private boolean hasNearbyPlayerWithTotem() {
        double rangeSq = this.targetRange.getValue() * this.targetRange.getValue();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (mc.player.squaredDistanceTo(player) > rangeSq) continue;
            if (player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) {
                return true;
            }
        }
        return false;
    }

    // ---- 寮哄埗鍠疯嵂鏂规硶锛堝拷鐣ユ墍鏈夋潯浠讹紝浠呮鏌ュ欢杩燂級 ----
    private void forceThrowPotion(StatusEffect targetEffect) {
        if (!this.delayTimer.passedMs(this.delay.getValue() * 1000.0)) return;
        if (this.usingPause.getValue() && mc.player.isUsingItem()) return;
        if (!EntityUtil.inInventory()) return;
        if (findPotion(targetEffect) == -1 && !(this.inventory.getValue() && findPotionInventorySlot(targetEffect) != -1)) return;
        this.throwPotion(targetEffect);
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (this.inventory.getValue() && !EntityUtil.inInventory()) return;
        if (!this.delayTimer.passedMs(this.delay.getValue() * 1000.0)) return;
        // 地面检测：仅当 onlyGround 开启时需要检查
        if (this.onlyGround.getValue() && !isNearGround()) return;

        // ----- 速度药水（自动）-----
        if (this.speed.getValue()) {
            boolean shouldThrow = false;
            StatusEffectInstance speedEffect = mc.player.getStatusEffect(StatusEffects.SPEED);
            if (speedEffect == null) {
                shouldThrow = true;
            } else if (speedEffect.getDuration() / 20.0 < this.prePotTime.getValue()) {
                shouldThrow = true;
            }
            float currentHealth = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            if (shouldThrow && (!hasNearbyPlayer() || currentHealth <= this.health.getValue())) {
                shouldThrow = false;
            }
            if (shouldThrow && this.checkThrow(StatusEffects.SPEED.value())) {
                this.throwPotion(StatusEffects.SPEED.value());
                return;
            }
        }

        // ----- 抗性药水（自动）-----
        if (this.resistance.getValue()) {
            boolean shouldThrow = false;
            StatusEffectInstance resistanceEffect = mc.player.getStatusEffect(StatusEffects.RESISTANCE);
            if (resistanceEffect == null) {
                shouldThrow = true;
            } else if (resistanceEffect.getAmplifier() < 2) {
                shouldThrow = true;
            } else if (resistanceEffect.getDuration() / 20.0 < this.prePotTime.getValue()) {
                shouldThrow = true;
            }
            if (shouldThrow && !hasNearbyPlayerWithTotem()) {
                shouldThrow = false;
            }
            if (shouldThrow && this.checkThrow(StatusEffects.RESISTANCE.value())) {
                this.throwPotion(StatusEffects.RESISTANCE.value());
                return;
            }
        }

        // ----- 力量药水（自动）-----
        if (this.strength.getValue()) {
            boolean shouldThrow = false;
            StatusEffectInstance strengthEffect = mc.player.getStatusEffect(StatusEffects.STRENGTH);
            if (strengthEffect == null) {
                shouldThrow = true;
            } else if (strengthEffect.getDuration() / 20.0 < this.prePotTime.getValue()) {
                shouldThrow = true;
            }
            boolean auraOk = !this.onlyAuraStrength.getValue() || (Aura.target != null && Aura.target instanceof PlayerEntity);
            float currentHealth = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            if (shouldThrow && (!auraOk || currentHealth <= this.health.getValue())) {
                shouldThrow = false;
            }
            if (shouldThrow && this.checkThrow(StatusEffects.STRENGTH.value())) {
                this.throwPotion(StatusEffects.STRENGTH.value());
                return;
            }
        }
    }

    public void throwPotion(StatusEffect targetEffect) {
        int oldSlot = mc.player.getInventory().selectedSlot;
        int newSlot;
        if (this.inventory.getValue() && (newSlot = findPotionInventorySlot(targetEffect)) != -1) {
            Astra.ROTATION.snapAt(Astra.ROTATION.rotationYaw, 90.0f);
            InventoryUtil.inventorySwap(newSlot, mc.player.getInventory().selectedSlot);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Astra.ROTATION.getLastYaw(), Astra.ROTATION.getLastPitch()));
            InventoryUtil.inventorySwap(newSlot, mc.player.getInventory().selectedSlot);
            EntityUtil.syncInventory();
            Astra.ROTATION.snapBack();
            this.delayTimer.reset();
        } else if ((newSlot = findPotion(targetEffect)) != -1) {
            Astra.ROTATION.snapAt(Astra.ROTATION.rotationYaw, 90.0f);
            InventoryUtil.switchToSlot(newSlot);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, Astra.ROTATION.getLastYaw(), Astra.ROTATION.getLastPitch()));
            InventoryUtil.switchToSlot(oldSlot);
            Astra.ROTATION.snapBack();
            this.delayTimer.reset();
        }
    }

    public boolean isThrow() {
        return this.throwing;
    }

    public boolean checkThrow(StatusEffect targetEffect) {
        if (!EntityUtil.inInventory()) return false;
        if (this.usingPause.getValue() && mc.player.isUsingItem()) return false;
        return findPotion(targetEffect) != -1 || (this.inventory.getValue() && findPotionInventorySlot(targetEffect) != -1);
    }

    // 内部类：处理按键强制喷药
    public class AutoPotTick {
        @EventListener
        public void onTick(ClientTickEvent event) {
            if (event.isPost() || Module.nullCheck()) return;
            if (Wrapper.mc.currentScreen != null) return;

            // 速度按键
            if (AutoPot.this.speedKey.isPressed()) {
                if (!AutoPot.this.speedPressed) {
                    AutoPot.this.speedPressed = true;
                    AutoPot.this.forceThrowPotion(StatusEffects.SPEED.value());
                }
            } else {
                AutoPot.this.speedPressed = false;
            }

            // 抗性按键
            if (AutoPot.this.resistanceKey.isPressed()) {
                if (!AutoPot.this.resistancePressed) {
                    AutoPot.this.resistancePressed = true;
                    AutoPot.this.forceThrowPotion(StatusEffects.RESISTANCE.value());
                }
            } else {
                AutoPot.this.resistancePressed = false;
            }

            // 力量按键
            if (AutoPot.this.strengthKey.isPressed()) {
                if (!AutoPot.this.strengthPressed) {
                    AutoPot.this.strengthPressed = true;
                    AutoPot.this.forceThrowPotion(StatusEffects.STRENGTH.value());
                }
            } else {
                AutoPot.this.strengthPressed = false;
            }
        }
    }
}