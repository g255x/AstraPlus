package dev.Astra.mod.modules.impl.render;

import dev.Astra.Astra;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.settings.impl.ColorSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;

import java.awt.*;

public class Skybox extends Module {
    public static Skybox INSTANCE;
    public final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 255, 255, 255)));
    public final ColorSetting backgroundColor = this.add(new ColorSetting("BackgroundColor", new Color(0, 0, 0, 255)));
    public final SliderSetting scale = this.add(new SliderSetting("Scale", 1.0, 0.1, 5.0, 0.05));
    public final SliderSetting speed = this.add(new SliderSetting("Speed", 1.0, 0.0, 10.0, 0.1));
    public final SliderSetting opacity = this.add(new SliderSetting("Opacity", 1.0, 0.0, 1.0, 0.01));

    public Skybox() {
        super("Skybox", Module.Category.Render);
        this.setChinese("天空盒");
        INSTANCE = this;
    }

    public void renderSkybox() {
        if (this.isOn()) {
            Astra.SHADER.renderSkyboxFullscreen(this.color.getValue(), this.backgroundColor.getValue(), this.scale.getValueFloat(), this.speed.getValueFloat(), this.opacity.getValueFloat());
        }
    }
}