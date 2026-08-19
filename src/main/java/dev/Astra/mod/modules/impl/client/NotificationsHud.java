package dev.Astra.mod.modules.impl.client;

import dev.Astra.api.utils.render.Render2DUtil;
import dev.Astra.core.impl.FontManager;
import dev.Astra.mod.gui.fonts.FontRenderer;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;

import java.awt.Color;
import java.util.Iterator;

public class NotificationsHud extends Module {
    public static NotificationsHud INSTANCE;
    private final SliderSetting scale = this.add(new SliderSetting("Scale", 1.0, 0.5, 2.0, 0.1));
    private final SliderSetting backgroundAlpha = this.add(new SliderSetting("BackgroundAlpha", 200.0, 0.0, 255.0, 1.0));
    private final SliderSetting displayTime = this.add(new SliderSetting("DisplayTime", 2000.0, 500.0, 5000.0, 100.0));
    private final SliderSetting notifyX = this.add(new SliderSetting("X", 10.0, 0.0, 1000.0, 1.0));
    private final SliderSetting notifyY = this.add(new SliderSetting("Y", 330.0, 0.0, 1000.0, 1.0));

    public NotificationsHud() {
        super("NotificationsHud", Module.Category.Client);
        this.setChinese("通知HUD");
        INSTANCE = this;
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        if (mc.getWindow() == null) return;

        NotificationManager.INSTANCE.update();
        if (NotificationManager.INSTANCE.isEmpty()) return;

        float s = this.scale.getValueFloat();
        float boxHeight = FontManager.ui.getFontHeight() * s + 8.0F * s;
        float spacing = boxHeight + 4.0F * s;
        int bgAlpha = this.backgroundAlpha.getValueInt();
        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();
        float anchorX = screenW - this.notifyX.getValueInt();
        float anchorY = screenH - this.notifyY.getValueInt();

        Iterator<NotificationInfo> iterator = NotificationManager.INSTANCE.getNotifications().iterator();
        int index = 0;

        while (iterator.hasNext()) {
            NotificationInfo n = iterator.next();
            n.update();
            if (n.isExpired()) continue;

            float baseWidth = 100.0F * s;
            float fontPad = 4.0F * s;
            FontRenderer font = FontManager.ui;
            float boxWidth = Math.max(baseWidth, fontPad + font.getWidth(n.getTitle() + " " + n.getSubTitle()) * s);

            float renderX = anchorX - boxWidth;
            float targetY = anchorY - (index + 1) * spacing;
            if (Math.abs(targetY - n.getCurrentY()) > 0.5F) {
                n.setTargetY(targetY);
            }

            float y = n.getCurrentY();
            long time = System.currentTimeMillis() - n.getCreateTime();
            long exitTime = time - (long) n.getDisplayDuration();
            boolean skipIntro = n.shouldSkipIntroAnimation();
            Color modeColor = this.getModeColor(n.getMode(), 255);

            if (!skipIntro && time <= 300L) {
                float p = easeOutCubic((float) time / 300.0F);
                float w = boxWidth * p;
                Render2DUtil.drawRect(drawContext.getMatrices(), renderX + boxWidth - w, y, w, boxHeight, modeColor);
            } else if (!skipIntro && time <= 500L) {
                float p = easeOutCubic((float) (time - 300L) / 200.0F);
                int a = (int) (bgAlpha * p);
                Render2DUtil.drawRect(drawContext.getMatrices(), renderX, y, boxWidth, boxHeight, new Color(0, 0, 0, a));
                float sliderWidth = 3.0F * s + (boxWidth - 3.0F * s) * (1.0F - p);
                Render2DUtil.drawRect(drawContext.getMatrices(), renderX, y, sliderWidth, boxHeight, modeColor);
                this.renderText(drawContext, n, renderX, y, boxHeight, s, (int) (255.0F * p));
            } else if (exitTime < 0L) {
                Render2DUtil.drawRect(drawContext.getMatrices(), renderX, y, boxWidth, boxHeight, new Color(0, 0, 0, bgAlpha));
                Render2DUtil.drawRect(drawContext.getMatrices(), renderX, y, 3.0F * s, boxHeight, modeColor);
                this.renderText(drawContext, n, renderX, y, boxHeight, s, 255);
            } else if (exitTime <= 200L) {
                float p = easeOutCubicDec((float) exitTime / 200.0F);
                int a = (int) (bgAlpha * p);
                Render2DUtil.drawRect(drawContext.getMatrices(), renderX, y, boxWidth, boxHeight, new Color(0, 0, 0, a));
                float sliderWidth = 3.0F * s + (boxWidth - 3.0F * s) * (1.0F - p);
                Render2DUtil.drawRect(drawContext.getMatrices(), renderX, y, sliderWidth, boxHeight, modeColor);
                this.renderText(drawContext, n, renderX, y, boxHeight, s, (int) (255.0F * p));
            } else if (exitTime <= 500L) {
                float p = easeOutCubicDec((float) (exitTime - 200L) / 300.0F);
                float w = boxWidth * p;
                Render2DUtil.drawRect(drawContext.getMatrices(), renderX + boxWidth - w, y, w, boxHeight, modeColor);
            }

            index++;
        }
    }

    private void renderText(DrawContext drawContext, NotificationInfo n, float x, float y, float boxHeight, float s, int alpha) {
        float textY = y + boxHeight * 0.5F - FontManager.ui.getFontHeight() * s * 0.5F;
        Color titleColor = n.isModule() ? new Color(n.getMode() == NotificationMode.Success ? 118 : 255, n.getMode() == NotificationMode.Success ? 185 : 75, n.getMode() == NotificationMode.Success ? 0 : 75, alpha) : this.getModeColor(n.getMode(), alpha);
        FontManager.ui.drawString(drawContext.getMatrices(), n.getTitle(), (double)(x + 4.0F * s), (double)textY, titleColor.getRGB());
        FontManager.ui.drawString(drawContext.getMatrices(), " " + n.getSubTitle(), (double)(x + 4.0F * s + FontManager.ui.getWidth(n.getTitle()) * s), (double)textY, new Color(255, 255, 255, alpha).getRGB());
    }

    private Color getModeColor(NotificationMode mode, int alpha) {
        return switch (mode) {
            case Success -> new Color(118, 185, 0, alpha);
            case Error -> new Color(255, 75, 75, alpha);
            case Info -> new Color(85, 170, 255, alpha);
        };
    }

    private static float easeOutCubic(float t) {
        return 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
    }

    private static float easeOutCubicDec(float t) {
        return 1.0F - easeOutCubic(t);
    }

    public static void addModuleNotification(String moduleName, boolean enabled) {
        if (INSTANCE == null) return;
        NotificationManager.INSTANCE.postModuleNotification(moduleName, enabled, INSTANCE.displayTime.getValueInt());
    }
}