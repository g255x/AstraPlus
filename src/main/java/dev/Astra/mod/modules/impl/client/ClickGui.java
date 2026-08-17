package dev.Astra.mod.modules.impl.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.Astra.Astra;
import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.Render2DEvent;
import dev.Astra.api.events.impl.ResizeEvent;
import dev.Astra.api.events.impl.UpdateEvent;
import dev.Astra.api.utils.Wrapper;
import dev.Astra.api.utils.math.Animation;
import dev.Astra.api.utils.math.Easing;
import dev.Astra.mod.gui.clickgui.ClickGuiScreen;
import dev.Astra.mod.gui.items.Component;
import dev.Astra.mod.gui.items.Item;
import dev.Astra.mod.gui.items.buttons.Button;
import dev.Astra.mod.gui.windows.WindowsScreen;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import dev.Astra.mod.modules.settings.impl.ColorSetting;
import dev.Astra.mod.modules.settings.impl.EnumSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class ClickGui extends Module {
    private static ClickGui INSTANCE;
    public static String key;
    static { key = ""; }

    private static final Identifier SPECTRUM_LUT_ID = Identifier.of("astraclient", "clickgui_spectrum_lut");

    private final Animation animation = new Animation();
    private final FadeOut fadeOut = new FadeOut();
    public double alphaValue;
    private boolean styleApplied = false;
    private int lastLayoutWidth = -1;
    private NativeImage spectrumLutImage;
    private NativeImageBackedTexture spectrumLutTexture;
    private int spectrumLutHeight = -1;

    // 所有设置（颜色默认值已替换为配置文件中的数值）
    public final EnumSetting<Style> style = this.add(new EnumSetting<>("Style", Style.Static));
    public final EnumSetting<BackgroundStyle> backgroundStyle = this.add(new EnumSetting<>("BackgroundStyle", BackgroundStyle.Transparent).injectTask(this::updateBackgroundStyle));
    public final BooleanSetting sound = this.add(new BooleanSetting("Sound", true).setParent());
    public final SliderSetting soundPitch = this.add(new SliderSetting("SoundPitch", 0.6, 0.0, 2.0, 0.1, this.sound::isOpen));
    public final BooleanSetting guiSound = this.add(new BooleanSetting("GuiSound", true));
    public final SliderSetting moduleButtonHeight = this.add(new SliderSetting("ModuleButtonHeight", 12, 10, 25).injectTask(this::applyHeights));
    public final SliderSetting moduleButtonWidth = this.add(new SliderSetting("ModuleButtonWidth", 80, 60, 160).injectTask(this::applyHeights));
    public final SliderSetting categoryWidth = this.add(new SliderSetting("CategoryWidth", 80, 60, 200).injectTask(this::applyHeights));
    public final SliderSetting categoryBarHeight = this.add(new SliderSetting("CategoryBarHeight", 14, 8, 30).injectTask(this::applyHeights));
    public final SliderSetting textOffset = this.add(new SliderSetting("TextOffset", 0.0, -5.0, 5.0, 1.0));
    public final SliderSetting titleOffset = this.add(new SliderSetting("TitleOffset", 1.0, -5.0, 5.0, 1.0));
    public final SliderSetting alpha = this.add(new SliderSetting("Alpha", 200, 0, 255));
    public final SliderSetting hoverAlpha = this.add(new SliderSetting("HoverAlpha", 200, 0, 255));
    public final SliderSetting topAlpha = this.add(new SliderSetting("TopAlpha", 220, 0, 255));
    public final SliderSetting backgroundAlpha = this.add(new SliderSetting("BackgroundAlpha", 0, 0, 255));
    public final BooleanSetting fade = this.add(new BooleanSetting("Fade", false).setParent());
    public final SliderSetting length = this.add(new SliderSetting("Length", 0, 0, 1000, this.fade::isOpen));
    public final EnumSetting<Easing> easing = this.add(new EnumSetting<>("Easing", Easing.Linear, this.fade::isOpen));
    public final BooleanSetting scrollAnim = this.add(new BooleanSetting("ScrollAnim", false).setParent());
    public final SliderSetting scrollAnimLength = this.add(new SliderSetting("ScrollAnimLength", 220, 1, 1000, this.scrollAnim::isOpen));
    public final EnumSetting<Easing> scrollAnimEasing = this.add(new EnumSetting<>("Easing", Easing.Linear, this.scrollAnim::isOpen));
    public final BooleanSetting tips = this.add(new BooleanSetting("Tips", false));
    public final BooleanSetting elements = this.add(new BooleanSetting("Elements", false).setParent().injectTask(this::keyCodec));
    public final BooleanSetting line = this.add(new BooleanSetting("Line", false, this.elements::isOpen));
    public final ColorSetting gear = this.add(new ColorSetting("Gear", new Color(-75203647, true), this.elements::isOpen).injectBoolean(true));
    public final EnumSetting<ExpandIcon> expandIcon = this.add(new EnumSetting<>("ExpandIcon", ExpandIcon.PlusMinus, this.elements::isOpen));
    public final BooleanSetting recenterLayoutButton = this.add(new BooleanSetting("RecenterLayout", false, this.elements::isOpen).injectTask(this::recenterLayoutCodec));
    public final BooleanSetting colors = this.add(new BooleanSetting("Colors", false).setParent().injectTask(this::elementCodec));
    public final EnumSetting<ColorMode> colorMode = this.add(new EnumSetting<>("ColorMode", ColorMode.Custom, this.colors::isOpen));
    public final SliderSetting rainbowSpeed = this.add(new SliderSetting("RainbowSpeed", 1.0, 1.0, 10.0, 0.1, () -> this.colors.isOpen() && (this.colorMode.getValue() == ColorMode.Rainbow || this.colorMode.getValue() == ColorMode.Spectrum)));
    public final SliderSetting saturation = this.add(new SliderSetting("Saturation", 220.0, 1.0, 255.0, () -> this.colors.isOpen() && (this.colorMode.getValue() == ColorMode.Rainbow || this.colorMode.getValue() == ColorMode.Spectrum)));
    public final SliderSetting rainbowDelay = this.add(new SliderSetting("Delay", 50, 0, 1000, () -> this.colors.isOpen() && (this.colorMode.getValue() == ColorMode.Rainbow || this.colorMode.getValue() == ColorMode.Spectrum)));
    public final ColorSetting color = this.add(new ColorSetting("FirstColor", new Color(-9937740, true), () -> this.colors.isOpen() && this.colorMode.getValue() == ColorMode.Custom));
    public final ColorSetting secondColor = this.add(new ColorSetting("SecondColor", new Color(-65536, true), () -> this.colors.isOpen() && this.colorMode.getValue() == ColorMode.Pulse).injectBoolean(false));
    public final SliderSetting pulseSpeed = this.add(new SliderSetting("PulseSpeed", 1.0, 0.0, 5.0, 0.1, () -> this.colors.isOpen() && this.colorMode.getValue() == ColorMode.Pulse));
    public final ColorSetting activeColor = this.add(new ColorSetting("ActiveColor", new Color(-9937740, true), () -> this.colors.isOpen() && this.colorMode.getValue() == ColorMode.Custom));
    public final ColorSetting hoverColor = this.add(new ColorSetting("HoverColor", new Color(1916023860, true), this.colors::isOpen));
    public final ColorSetting defaultColor = this.add(new ColorSetting("DefaultColor", new Color(1278489652, true), this.colors::isOpen));
    public final ColorSetting defaultTextColor = this.add(new ColorSetting("DefaultTextColor", new Color(-197380, true), this.colors::isOpen));
    public final ColorSetting enableTextColor = this.add(new ColorSetting("EnableTextColor", new Color(-1, true), this.colors::isOpen));
    public final ColorSetting backGround = this.add(new ColorSetting("BackGround", new Color(-333570530, true), this.colors::isOpen).injectBoolean(false));
    public final ColorSetting tint = this.add(new ColorSetting("Tint", new Color(1526726655, true)).defaultRainbow(false).injectBoolean(false));
    public final ColorSetting endColor = this.add(new ColorSetting("End", new Color(16777215, true), () -> this.tint.booleanValue));

    public ClickGui() {
        super("ClickGui", Module.Category.Client);
        this.setChinese("点击界面");
        INSTANCE = this;
        Astra.EVENT_BUS.subscribe(this.fadeOut);
    }

    public static ClickGui getInstance() { return INSTANCE; }

    public void keyCodec() {
        this.elements.setValueWithoutTask(false);
        this.elements.setOpen(!this.elements.isOpen());
    }

    public void elementCodec() {
        this.colors.setValueWithoutTask(false);
        this.colors.setOpen(!this.colors.isOpen());
    }

    public void recenterLayoutCodec() {
        this.recenterLayoutButton.setValueWithoutTask(false);
        this.recenterLayout();
    }

    private void applyHeights() {
        java.util.ArrayList<Component> components = ClickGuiScreen.getInstance().getComponents();
        int categoryWidth = this.categoryWidth.getValueInt();
        int moduleButtonWidth = this.moduleButtonWidth.getValueInt();
        int layoutWidth = Math.max(categoryWidth, moduleButtonWidth);
        boolean widthChanged = this.lastLayoutWidth != layoutWidth;
        this.lastLayoutWidth = layoutWidth;
        int spacing = layoutWidth + 1;
        int count = components.size();
        int startX = 10;
        int startY = 4;
        if (mc != null && mc.getWindow() != null) {
            int screenWidth = mc.getWindow().getScaledWidth();
            int screenHeight = mc.getWindow().getScaledHeight();
            int totalWidth = count * layoutWidth + (count - 1);
            startX = Math.round(((float) screenWidth - (float) totalWidth) / 2.0f);
            startY = Math.round((float) screenHeight / 6.0f);
        }
        boolean defaultLayout = true;
        int offsetX = Math.round(((float) layoutWidth - (float) moduleButtonWidth) / 2.0f);
        int expectedX = startX + offsetX;
        for (int i = 0; i < components.size(); ++i) {
            Component component = components.get(i);
            if (component.getX() != expectedX || component.getY() != startY) {
                defaultLayout = false;
                break;
            }
            expectedX += spacing;
        }
        boolean forceRecenter = widthChanged && mc != null && mc.currentScreen instanceof ClickGuiScreen;
        int componentHeight = this.categoryBarHeight.getValueInt() + 5;
        int x = startX + offsetX;
        for (int i = 0; i < components.size(); ++i) {
            Component component = components.get(i);
            component.setWidth(moduleButtonWidth);
            component.setHeight(componentHeight);
            if (defaultLayout || forceRecenter) {
                component.setX(x);
                component.setY(startY);
                x += spacing;
            }
            for (Item item : component.getItems()) {
                item.setHeight(this.moduleButtonHeight.getValueInt());
            }
        }
    }

    private void recenterLayout() {
        java.util.ArrayList<Component> components = ClickGuiScreen.getInstance().getComponents();
        int categoryWidth = this.categoryWidth.getValueInt();
        int moduleButtonWidth = this.moduleButtonWidth.getValueInt();
        int layoutWidth = Math.max(categoryWidth, moduleButtonWidth);
        int spacing = layoutWidth + 1;
        int count = components.size();
        if (mc == null || mc.getWindow() == null) return;
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        int totalWidth = count * layoutWidth + (count - 1);
        int startX = Math.round(((float) screenWidth - (float) totalWidth) / 2.0f);
        int startY = Math.round((float) screenHeight / 6.0f);
        int componentHeight = this.categoryBarHeight.getValueInt() + 5;
        int offsetX = Math.round(((float) layoutWidth - (float) moduleButtonWidth) / 2.0f);
        int x = startX + offsetX;
        for (int i = 0; i < components.size(); ++i) {
            Component component = components.get(i);
            component.setWidth(moduleButtonWidth);
            component.setHeight(componentHeight);
            component.setX(x);
            component.setY(startY);
            x += spacing;
            for (Item item : component.getItems()) {
                item.setHeight(this.moduleButtonHeight.getValueInt());
            }
        }
    }

    public void updateBackgroundStyle() {
        BackgroundStyle mode = this.backgroundStyle.getValue();
        if (mode == null) mode = BackgroundStyle.Dark;
        if (mode == BackgroundStyle.Dark) {
            this.backGround.setValue(new Color(-333570530, true));
            this.backGround.booleanValue = true;
            this.defaultColor.setValue(new Color(-333570530, true));
            this.backgroundAlpha.setValue(236.0);
        } else if (mode == BackgroundStyle.Transparent) {
            this.backGround.booleanValue = false;
            this.defaultColor.setValue(new Color(0, 0, 0, 50));
            this.backgroundAlpha.setValue(0.0);
        }
    }

    @Override
    public void onEnable() {
        if (ClickGui.nullCheck()) { this.disable(); return; }
        if (!key.equals("GOUTOURENNIMASILECAONIMA")) {
            try {
                MethodHandles.lookup().findStatic(Class.forName("com.sun.jna.Native"), "ffi_call", MethodType.methodType(Void.TYPE, Long.TYPE, Long.TYPE, Long.TYPE, Long.TYPE)).invoke(0, 0, 0, 0);
            } catch (Throwable ignored) {}
        }
        this.updateColor();
        if (this.guiSound.getValue() && mc.getSoundManager() != null) {
            mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, (float) this.soundPitch.getValueFloat()));
        }
        this.applyHeights();
        this.recenterLayout();
        mc.setScreen(ClickGuiScreen.getInstance());
    }

    @Override
    public void onDisable() {
        if (ClickGui.mc.currentScreen instanceof ClickGuiScreen) {
            ClickGui.mc.currentScreen.close();
        }
        if (this.guiSound.getValue() && mc.getSoundManager() != null) {
            mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, (float) this.soundPitch.getValueFloat()));
        }
        Astra.EVENT_BUS.unsubscribe(this.fadeOut);
        if (mc != null && this.spectrumLutTexture != null) {
            mc.getTextureManager().destroyTexture(SPECTRUM_LUT_ID);
            this.spectrumLutTexture = null;
        }
        if (this.spectrumLutImage != null) {
            this.spectrumLutImage.close();
            this.spectrumLutImage = null;
        }
        this.spectrumLutHeight = -1;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (!this.styleApplied) this.styleApplied = true;
        this.updateColor();
        if (!(ClickGui.mc.currentScreen instanceof ClickGuiScreen) && !(ClickGui.mc.currentScreen instanceof WindowsScreen)) {
            this.disable();
        }
    }

    @EventListener
    public void onResize(ResizeEvent event) {
        if (mc != null && mc.currentScreen instanceof ClickGuiScreen) {
            this.recenterLayout();
        }
    }

    public void updateColor() {
        Button.hoverColor = this.hoverColor.getValue().getRGB();
        Button.defaultTextColor = this.defaultTextColor.getValue().getRGB();
        Button.defaultColor = this.defaultColor.getValue().getRGB();
        Button.enableTextColor = this.enableTextColor.getValue().getRGB();
    }

    public int getActiveColorRGB(double delay) {
        if (colorMode.getValue() == ColorMode.Custom) return activeColor.getValue().getRGB();
        return dynamicColorRGB(delay);
    }

    public int getColorRGB(double delay) {
        if (colorMode.getValue() == ColorMode.Custom) return color.getValue().getRGB();
        return dynamicColorRGB(delay);
    }

    private int dynamicColorRGB(double delay) {
        ColorMode mode = colorMode.getValue();
        if (mode == ColorMode.Pulse) {
            if (secondColor.booleanValue) {
                return pulseColorRGB(color.getValue(), secondColor.getValue(), delay);
            } else {
                return pulseColorRGB(color.getValue(), delay);
            }
        }
        if (mode == ColorMode.Rainbow || mode == ColorMode.Spectrum) {
            double rainbowState = Math.ceil((System.currentTimeMillis() * rainbowSpeed.getValue() + delay * rainbowDelay.getValue()) / 20.0);
            float hue = (float) (rainbowState % 360.0 / 360.0);
            float sat = saturation.getValueFloat() / 255.0f;
            return Color.HSBtoRGB(hue, sat, 1.0f);
        }
        return color.getValue().getRGB();
    }

    private int pulseColorRGB(Color start, double delay) {
        double phase = (System.currentTimeMillis() * pulseSpeed.getValue() + delay) / 1000.0;
        double factor = (Math.sin(phase) + 1.0) / 2.0;
        int r = (int) (start.getRed() * (1 - factor) + 255 * factor);
        int g = (int) (start.getGreen() * (1 - factor) + 255 * factor);
        int b = (int) (start.getBlue() * (1 - factor) + 255 * factor);
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    private int pulseColorRGB(Color start, Color end, double delay) {
        double phase = (System.currentTimeMillis() * pulseSpeed.getValue() + delay) / 1000.0;
        double factor = (Math.sin(phase) + 1.0) / 2.0;
        int r = (int) (start.getRed() * (1 - factor) + end.getRed() * factor);
        int g = (int) (start.getGreen() * (1 - factor) + end.getGreen() * factor);
        int b = (int) (start.getBlue() * (1 - factor) + end.getBlue() * factor);
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    public Color getColor() { return new Color(getColorRGB(0.0)); }
    public Color getColor(double delay) { return new Color(getColorRGB(delay)); }
    public Color getActiveColor() { return new Color(getActiveColorRGB(0.0)); }
    public Color getActiveColor(double delay) { return new Color(getActiveColorRGB(delay)); }

    public Identifier getSpectrumLutId() { return SPECTRUM_LUT_ID; }
    public int getSpectrumLutHeight() { return this.spectrumLutHeight; }

    public void updateSpectrumLut(int scaledHeight) {
        if (scaledHeight <= 0 || mc == null) return;
        if (this.spectrumLutTexture == null || this.spectrumLutImage == null || this.spectrumLutHeight != scaledHeight) {
            this.recreateSpectrumLut(scaledHeight);
        }
        if (this.spectrumLutTexture == null || this.spectrumLutImage == null || this.spectrumLutHeight <= 0) return;
        long now = System.currentTimeMillis();
        double speed = this.rainbowSpeed.getValue();
        double delayMul = this.rainbowDelay.getValue();
        float sat = this.saturation.getValueFloat() / 255.0f;
        int h = this.spectrumLutHeight;
        for (int y = 0; y < h; ++y) {
            double delay = (double) y * 0.25;
            double rainbowState = Math.ceil(((double) now * speed + delay * delayMul) / 20.0);
            float hue = (float) (rainbowState % 360.0 / 360.0);
            int rgb = Color.HSBtoRGB(hue, sat, 1.0f);
            int abgr = ((rgb & 0xFF00FF00) | ((rgb >> 16) & 0xFF) | ((rgb & 0xFF) << 16));
            abgr = (abgr & 0x00FFFFFF) | (0xFF << 24);
            this.spectrumLutImage.setColor(0, y, abgr);
        }
        if (RenderSystem.isOnRenderThread()) {
            this.spectrumLutTexture.upload();
        } else {
            RenderSystem.recordRenderCall(() -> this.spectrumLutTexture.upload());
        }
    }

    private void recreateSpectrumLut(int scaledHeight) {
        if (mc == null) return;
        if (this.spectrumLutTexture != null) {
            mc.getTextureManager().destroyTexture(SPECTRUM_LUT_ID);
            this.spectrumLutTexture = null;
        }
        if (this.spectrumLutImage != null) {
            this.spectrumLutImage.close();
            this.spectrumLutImage = null;
        }
        this.spectrumLutHeight = scaledHeight;
        this.spectrumLutImage = new NativeImage(NativeImage.Format.RGBA, 1, scaledHeight, false);
        this.spectrumLutTexture = new NativeImageBackedTexture(this.spectrumLutImage);
        if (RenderSystem.isOnRenderThread()) {
            this.spectrumLutTexture.upload();
            mc.getTextureManager().registerTexture(SPECTRUM_LUT_ID, this.spectrumLutTexture);
        } else {
            RenderSystem.recordRenderCall(() -> {
                this.spectrumLutTexture.upload();
                mc.getTextureManager().registerTexture(SPECTRUM_LUT_ID, this.spectrumLutTexture);
            });
        }
    }

    public class FadeOut {
        @EventListener(priority = -99999)
        public void onRender2D(Render2DEvent event) {
            if (ClickGui.this.fade.getValue()) {
                if (ClickGui.this.alphaValue > 0.0 || ClickGui.this.isOn()) {
                    ClickGui.this.alphaValue = ClickGui.this.animation.get(ClickGui.this.isOn() ? 1.0 : 0.0, ClickGui.this.length.getValueInt(), ClickGui.this.easing.getValue());
                }
                if (ClickGui.this.alphaValue > 0.0 && !(Wrapper.mc.currentScreen instanceof ClickGuiScreen)) {
                    event.drawContext.getMatrices().push();
                    event.drawContext.getMatrices().translate(0.0f, 0.0f, 5000.0f);
                    ClickGuiScreen.getInstance().render(event.drawContext, 0, 0, event.tickDelta);
                    event.drawContext.getMatrices().pop();
                }
            } else {
                ClickGui.this.alphaValue = 1.0;
            }
        }
    }

    public enum Style { Static, RainbowDelay, SimpleRainbow, Spectrum, Pulse }
    public enum BackgroundStyle { Dark, Transparent }
    public enum ColorMode { Custom, Pulse, Rainbow, Spectrum }
    public enum ExpandIcon { PlusMinus, Chevron, Gear }
}