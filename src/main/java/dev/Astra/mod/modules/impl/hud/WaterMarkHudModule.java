package dev.Astra.mod.modules.impl.hud;

import dev.Astra.api.utils.render.TextUtil;
import dev.Astra.core.impl.FontManager;
import dev.Astra.mod.modules.HudModule;
import dev.Astra.mod.modules.impl.client.ClickGui;
import dev.Astra.mod.modules.settings.impl.StringSetting;
import net.minecraft.client.gui.DrawContext;

import java.util.Objects;

public class WaterMarkHudModule extends HudModule {
    public static WaterMarkHudModule INSTANCE;
    public final StringSetting title = this.add(new StringSetting("Title", "Astra"));

    public WaterMarkHudModule() {
        super("WaterMark", "水印", 1, 1);
        INSTANCE = this;
    }

    @Override
    public void onRender2D(DrawContext context, float tickDelta) {
        String text = this.title.getValue();
        int w = HudSetting.useFont() ? (int)Math.ceil(FontManager.ui.getWidth(text)) : WaterMarkHudModule.mc.textRenderer.getWidth(text);
        int h;
        if (HudSetting.useFont()) {
            h = (int)Math.ceil(FontManager.ui.getFontHeight());
        } else {
            Objects.requireNonNull(WaterMarkHudModule.mc.textRenderer);
            h = 9;
        }

        int x = this.getHudRenderX(w);
        int y = this.getHudRenderY(h);

        int color = this.getHudColor(0.0);
        TextUtil.drawString(context, text, x, y, color, HudSetting.useFont(), HudSetting.useShadow());

        this.setHudBounds(x, y, Math.max(1, w), Math.max(1, h));
    }

    private int getHudColor(double delay) {
        ClickGui gui = ClickGui.getInstance();
        return gui == null ? -1 : gui.getColor(delay).getRGB();
    }
}