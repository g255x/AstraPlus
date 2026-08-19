package dev.Astra.mod.modules.impl.render;

import dev.Astra.Astra;
import dev.Astra.asm.accessors.IGameRenderer;
import dev.Astra.core.impl.ShaderManager;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import dev.Astra.mod.modules.settings.impl.ColorSetting;
import dev.Astra.mod.modules.settings.impl.EnumSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

import java.awt.*;

public class ShaderModule extends Module {
    public static ShaderModule INSTANCE;
    private final EnumSetting<Page> page = this.add(new EnumSetting<Page>("Page", Page.Target));
    public final EnumSetting<ShaderManager.Shader> mode = this.add(new EnumSetting<ShaderManager.Shader>("Mode", ShaderManager.Shader.Solid, () -> this.page.getValue() == Page.Shader));
    public final SliderSetting speed = this.add(new SliderSetting("Speed", 0.0, 0.0, 20.0, 0.1, () -> this.page.getValue() == Page.Shader));
    public final ColorSetting fill = this.add(new ColorSetting("Color", new Color(-1685490725, true), () -> this.page.getValue() == Page.Shader));
    public final SliderSetting maxSample = this.add(new SliderSetting("MaxSample", 0.0, 0.0, 20.0, () -> this.page.getValue() == Page.Shader));
    public final SliderSetting divider = this.add(new SliderSetting("Divider", 0.0, 0.0, 300.0, () -> this.page.getValue() == Page.Shader));
    public final SliderSetting radius = this.add(new SliderSetting("Radius", 2.6, 0.0, 6.0, () -> this.page.getValue() == Page.Shader));
    public final SliderSetting smoothness = this.add(new SliderSetting("Smoothness", 0.1, 0.0, 1.0, 0.01, () -> this.page.getValue() == Page.Shader));
    public final SliderSetting alpha = this.add(new SliderSetting("GlowAlpha", 255, 0, 255, () -> this.page.getValue() == Page.Shader));
    public final SliderSetting maxRange = this.add(new SliderSetting("MaxRange", 32, 16, 512, () -> this.page.getValue() == Page.Target).setSuffix("m"));
    public final SliderSetting pulseSpeed = this.add(new SliderSetting("PulseSize", 0.0, 0.0, 200.0, 0.1, () -> this.page.getValue() == Page.Color));
    public final ColorSetting pulse = this.add(new ColorSetting("Pulse", new Color(-10262857, true), () -> this.page.getValue() == Page.Color));
    public final SliderSetting step = this.add(new SliderSetting("Step", 0.0, 0.0, 4.0, 0.01, () -> this.page.getValue() == Page.Color));
    public final SliderSetting octaves = this.add(new SliderSetting("Octaves", 5, 5, 30, () -> this.page.getValue() == Page.Color));
    public final ColorSetting smoke1 = this.add(new ColorSetting("Color1", new Color(-50331649, true), () -> this.page.getValue() == Page.Color));
    public final ColorSetting smoke2 = this.add(new ColorSetting("Color2", new Color(-1, true), () -> this.page.getValue() == Page.Color));
    public final ColorSetting smoke3 = this.add(new ColorSetting("Color3", new Color(-197380, true), () -> this.page.getValue() == Page.Color));
    public final ColorSetting smoke4 = this.add(new ColorSetting("Color4", new Color(-772, true), () -> this.page.getValue() == Page.Color));
    private final BooleanSetting hands = this.add(new BooleanSetting("Hands", true, () -> this.page.getValue() == Page.Target));
    private final BooleanSetting self = this.add(new BooleanSetting("Self", false, () -> this.page.getValue() == Page.Target));
    private final BooleanSetting players = this.add(new BooleanSetting("Players", true, () -> this.page.getValue() == Page.Target));
    private final BooleanSetting friends = this.add(new BooleanSetting("Friends", true, () -> this.page.getValue() == Page.Target));
    private final BooleanSetting crystals = this.add(new BooleanSetting("Crystals", true, () -> this.page.getValue() == Page.Target));
    private final BooleanSetting creatures = this.add(new BooleanSetting("Creatures", false, () -> this.page.getValue() == Page.Target));
    private final BooleanSetting monsters = this.add(new BooleanSetting("Monsters", true, () -> this.page.getValue() == Page.Target));
    private final BooleanSetting ambients = this.add(new BooleanSetting("Ambients", false, () -> this.page.getValue() == Page.Target));
    private final BooleanSetting items = this.add(new BooleanSetting("Items", true, () -> this.page.getValue() == Page.Target));
    private final BooleanSetting others = this.add(new BooleanSetting("Others", true, () -> this.page.getValue() == Page.Target));

    public ShaderModule() {
        super("Shader", Module.Category.Render);
        this.setChinese("着色器");
        INSTANCE = this;
    }

    @Override
    public String getInfo() {
        return this.mode.getValue().name();
    }

    public boolean shouldRender(Entity entity) {
        if (entity == null) return false;
        if (ShaderModule.mc.player == null) return false;
        if ((double) MathHelper.sqrt((float) ((float) ShaderModule.mc.player.squaredDistanceTo(entity.getPos()))) > this.maxRange.getValue()) return false;
        if (entity instanceof PlayerEntity) {
            PlayerEntity playerEntity = (PlayerEntity) entity;
            if (entity == ShaderModule.mc.player) return this.self.getValue();
            if (Astra.FRIEND.isFriend(playerEntity)) return this.friends.getValue();
            return this.players.getValue();
        }
        if (entity instanceof EndCrystalEntity) return this.crystals.getValue();
        if (entity instanceof ItemEntity) return this.items.getValue();
        return switch (entity.getType().getSpawnGroup()) {
            case SpawnGroup.CREATURE, SpawnGroup.WATER_CREATURE -> this.creatures.getValue();
            case SpawnGroup.MONSTER -> this.monsters.getValue();
            case SpawnGroup.AMBIENT, SpawnGroup.WATER_AMBIENT -> this.ambients.getValue();
            default -> this.others.getValue();
        };
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (this.hands.getValue()) {
            Astra.SHADER.renderShader(() -> ((IGameRenderer) ShaderModule.mc.gameRenderer).IRenderHand(ShaderModule.mc.gameRenderer.getCamera(), mc.getRenderTickCounter().getTickDelta(true), matrixStack.peek().getPositionMatrix()), this.mode.getValue());
        }
    }

    @Override
    public void onToggle() {
        Astra.SHADER.reloadShaders();
    }

    @Override
    public void onLogin() {
        Astra.SHADER.reloadShaders();
    }

    private static enum Page {
        Shader,
        Target,
        Color;
    }
}