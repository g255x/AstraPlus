package dev.Astra.mod.gui.clickgui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.Astra.Astra;
import dev.Astra.api.utils.Wrapper;
import dev.Astra.api.utils.math.AnimateUtil;
import dev.Astra.api.utils.math.Animation;
import dev.Astra.api.utils.math.Easing;
import dev.Astra.api.utils.render.Render2DUtil;
import dev.Astra.api.utils.render.TextUtil;
import dev.Astra.core.impl.CommandManager;
import dev.Astra.core.impl.FontManager;
import dev.Astra.mod.gui.clickgui.pages.ClickGuiConfigPage;
import dev.Astra.mod.gui.clickgui.pages.ClickGuiHudPage;
import dev.Astra.mod.gui.clickgui.pages.ClickGuiModulePage;
import dev.Astra.mod.gui.items.Component;
import dev.Astra.mod.gui.items.Item;
import dev.Astra.mod.gui.windows.WindowBase;
import dev.Astra.mod.modules.HudModule;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.impl.client.ClickGui;
import dev.Astra.mod.modules.impl.client.ClientSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ClickGuiScreen extends Screen {
    private static ClickGuiScreen INSTANCE = new ClickGuiScreen();
    private final ArrayList<Component> components = new ArrayList<>();
    private boolean layoutCorrected = false;
    private int lastLayoutScreenW = -1, lastLayoutScreenH = -1;

    private final ArrayList<TopTab> topTabs = new ArrayList<>();
    private Page page = Page.Module;
    private final Animation pageSlide = new Animation();
    private final ClickGuiModulePage modulePage = new ClickGuiModulePage(this);
    private final ClickGuiConfigPage configPage = new ClickGuiConfigPage(this);
    private final ClickGuiHudPage hudPage = new ClickGuiHudPage(this);
    private final ClickGuiFrame reusableFrame = new ClickGuiFrame();
    private ClickGuiFrame lastFrame;
    private float topTabAnimX, topTabAnimW;
    private boolean topTabAnimInit;
    private boolean confirmOpen;
    private String confirmTitle, confirmMessage;
    private Runnable confirmYesAction;

    // 缓存窗口尺寸与语言
    private int lastScreenW = -1;
    private boolean lastChineseLang = false;

    // 组件边界脏标记
    private boolean boundsDirty = true;
    private int cachedPanelX, cachedPanelY, cachedPanelW, cachedPanelH;
    private int cachedAlpha;
    private boolean cachedFocused;

    private static final int COLOR_BLACK_140 = 0x8C000000;

    // 字符串宽度缓存
    private final Map<String, Integer> textWidthCache = new HashMap<>();
    private boolean lastTextWidthCustomFont = true;
    private boolean lastTextWidthChinese = false;

    private static final String[] TIPS_EN = {
            "LMB drag, RMB expand/collapse",
            "Scroll up/down, SHIFT+scroll left/right",
            "SHIFT+LMB: toggle hold/release",
            "SHIFT+LMB: reset this setting",
            "RMB on String setting: edit"
    };
    private static final String[] TIPS_ZH = {
            "左键拖动列 右键展开/折叠",
            "滚轮是上下移动 SHIFT+滚轮是左右移动",
            "SHIFT+左键 快捷键按钮 切换功能(按住/松开)触发",
            "SHIFT+左键 功能按钮 重置设置",
            "文本设置 右键编辑"
    };

    public ClickGuiScreen() {
        super(Text.literal("Astra"));
        setInstance();
        load();
    }

    public static ClickGuiScreen getInstance() {
        if (INSTANCE == null) INSTANCE = new ClickGuiScreen();
        return INSTANCE;
    }

    public Page getPage() { return page; }
    private void setInstance() { INSTANCE = this; }

    private void load() {
        topTabs.clear();
        topTabs.add(new TopTab(Page.Module, "Module", "模块"));
        topTabs.add(new TopTab(Page.Config, "Config", "配置"));
        topTabs.add(new TopTab(Page.Hud, "HUD", "HUD"));
        modulePage.load();
        hudPage.init();
        boundsDirty = true;
        invalidateTextWidthCache();
    }

    private void renderHudModules(DrawContext context, float delta) {
        for (Module module : Astra.MODULE.getModules()) {
            if (!(module instanceof HudModule) || !module.isOn()) continue;
            try {
                module.onRender2D(context, delta);
            } catch (Exception e) {
                e.printStackTrace();
                if (ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.debug.getValue())
                    CommandManager.sendMessage("§4Error in " + module.getName() + " [onRender2D]: " + e.getMessage());
            }
        }
    }

    private void invalidateBounds() { boundsDirty = true; }
    private void invalidateTextWidthCache() { textWidthCache.clear(); }

    private void updateBoundsIfNeeded(int sw, int sh) {
        if (!boundsDirty) return;
        boundsDirty = false;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (Component c : components) {
            int x = c.getX(), y = c.getY(), w = c.getWidth(), h = c.getHeight();
            minX = Math.min(minX, x); minY = Math.min(minY, y);
            maxX = Math.max(maxX, x + w); maxY = Math.max(maxY, y + h);
        }
        int margin = 16;
        int panelX = Math.max(8, minX - margin);
        int panelY = Math.max(6, minY - margin);
        int panelW = Math.min(sw - panelX - 8, maxX - minX + margin * 2);
        int panelH = Math.min(sh - panelY - 6, maxY - minY + margin * 2 + 24);
        cachedPanelX = panelX; cachedPanelY = panelY; cachedPanelW = panelW; cachedPanelH = panelH;
    }

    private void updateFocusAndAlpha(int mouseX, int mouseY) {
        boolean focused = mouseX >= cachedPanelX && mouseX <= cachedPanelX + cachedPanelW &&
                mouseY >= cachedPanelY && mouseY <= cachedPanelY + cachedPanelH;
        cachedFocused = focused;
        cachedAlpha = focused ? 242 : 227;
    }

    private int getTextWidthCached(String text, boolean customFont, boolean chinese) {
        if (customFont != lastTextWidthCustomFont || chinese != lastTextWidthChinese) {
            invalidateTextWidthCache();
            lastTextWidthCustomFont = customFont;
            lastTextWidthChinese = chinese;
        }
        return textWidthCache.computeIfAbsent(text, k -> {
            if (customFont && FontManager.isCustomFontEnabled())
                return (int) FontManager.ui.getWidth(k);
            else return Wrapper.mc != null ? Wrapper.mc.textRenderer.getWidth(k) : 0;
        });
    }

    private void updateTopTabsLayoutIfNeeded(int screenWidth, boolean chinese) {
        if (screenWidth == lastScreenW && chinese == lastChineseLang) return;
        lastScreenW = screenWidth; lastChineseLang = chinese;
        int gap = 0, padX = 8, y = 6, h = getFontHeight() + 6;
        int total = 0;
        for (int i = 0; i < topTabs.size(); i++) {
            TopTab tab = topTabs.get(i);
            String label = tab.getLabel(chinese);
            int w = getTextWidthCached(label, FontManager.isCustomFontEnabled(), chinese) + padX * 2;
            tab.w = w; tab.h = h; tab.y = y;
            total += w;
            if (i != topTabs.size() - 1) total += gap;
        }
        int x = Math.round((screenWidth - total) / 2.0f);
        for (TopTab tab : topTabs) { tab.x = x; x += tab.w + gap; }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        ClickGui gui = ClickGui.getInstance();
        ClientSetting clientSetting = ClientSetting.INSTANCE;
        if (gui == null) return;

        float dt = AnimateUtil.deltaTime();
        if (dt <= 0.0f) dt = 0.016f;

        float keyCodec = (float) gui.alphaValue;
        float scale = 0.92f + 0.08f * keyCodec;
        float slideY = (1.0f - keyCodec) * 20.0f;

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, keyCodec);
        Item.context = context;
        renderBackground(context, mouseX, mouseY, delta);

        if (page == Page.Hud) renderHudModules(context, delta);

        if (gui.colorMode.getValue() == ClickGui.ColorMode.Spectrum)
            gui.updateSpectrumLut(context.getScaledWindowHeight());

        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();
        if (sw != lastLayoutScreenW || sh != lastLayoutScreenH) {
            layoutCorrected = false;
            lastLayoutScreenW = sw;
            lastLayoutScreenH = sh;
            hudPage.resetHudLayout();
            invalidateBounds();
        }

        if (!layoutCorrected && Wrapper.mc != null && Wrapper.mc.getWindow() != null) {
            int categoryWidth = gui.categoryWidth.getValueInt();
            int moduleButtonWidth = gui.moduleButtonWidth.getValueInt();
            int layoutWidth = Math.max(categoryWidth, moduleButtonWidth);
            int spacing = layoutWidth + 1;
            int count = components.size();
            if (count > 0) {
                int totalWidth = count * layoutWidth + (count - 1);
                int startX = Math.round(((float) sw - (float) totalWidth) / 2.0f);
                int startY = Math.round((float) sh / 6.0f);
                int offsetX = Math.round(((float) layoutWidth - (float) moduleButtonWidth) / 2.0f);
                int x = startX - spacing;
                for (Component component : components) {
                    x += spacing;
                    component.setX(x + offsetX);
                    component.setY(startY);
                }
                invalidateBounds();
            }
            layoutCorrected = true;
        }

        updateBoundsIfNeeded(sw, sh);
        updateFocusAndAlpha(mouseX, mouseY);

        // 移除模糊和晃动逻辑，仅保留核心渲染

        renderTopTabs(context, mouseX, mouseY, dt, gui, clientSetting);

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        float centerX = cachedPanelX + cachedPanelW / 2.0f;
        float centerY = cachedPanelY + cachedPanelH / 2.0f;
        matrices.translate(centerX, centerY + slideY, 0);
        matrices.scale(scale, scale, 1);
        matrices.translate(-centerX, -centerY, 0);
        matrices.pop();

        int categoryWidth = gui.categoryWidth.getValueInt();
        int moduleButtonWidth = gui.moduleButtonWidth.getValueInt();
        int layoutWidth = Math.max(categoryWidth, moduleButtonWidth);
        int count = components.size();
        int totalWidth = count > 0 ? count * layoutWidth + (count - 1) : sw;
        int pageW = Math.max(sw, totalWidth + 32);
        float pageX = (float) pageSlide.get(-(double) page.ordinal() * (double) pageW, 260L, Easing.SineOut);
        float pageOffsetX = scale == 0 ? pageX : pageX / scale;
        for (Component c : components) c.setPageOffsetX(pageOffsetX);

        reusableFrame.set(scale, slideY, pageOffsetX, pageW, cachedPanelX, cachedPanelY, cachedPanelW, cachedPanelH,
                0, 0, sw, sh); // totalOffsetX/Y 已无作用，置为0
        lastFrame = reusableFrame;

        matrices.push();
        matrices.translate(0, slideY, 0);
        matrices.scale(scale, scale, 1);
        modulePage.render(context, mouseX, mouseY, delta);
        configPage.render(context, mouseX, mouseY, delta, reusableFrame);
        hudPage.render(context, mouseX, mouseY, delta, reusableFrame);
        matrices.pop();

        if (gui.tips.getValue()) renderTips(context, gui, clientSetting);
        if (confirmOpen) renderConfirmDialog(context, mouseX, mouseY, gui, clientSetting);

        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    private void renderTips(DrawContext ctx, ClickGui gui, ClientSetting cs) {
        MatrixStack matrices = ctx.getMatrices();
        matrices.push();
        float pageX = (float) pageSlide.get(-(double) page.ordinal() * (double) Math.max(ctx.getScaledWindowWidth(), 0), 260L, Easing.SineOut);
        matrices.translate(pageX, 0, 0);
        boolean customFont = FontManager.isCustomFontEnabled();
        boolean shadow = FontManager.isShadowEnabled();
        float lineHeight = customFont ? FontManager.ui.getFontHeight() : TextUtil.getHeight();
        float marginBottom = 6;
        int lines = 5;
        float baseY = ctx.getScaledWindowHeight() - marginBottom - lineHeight * lines;
        int tipX = 6;
        int tipY = Math.round(baseY);
        boolean chinese = cs != null && cs.chinese.getValue();
        String[] tips = chinese ? TIPS_ZH : TIPS_EN;
        boolean spectrum = gui.colorMode.getValue() == ClickGui.ColorMode.Spectrum;
        for (int i = 0; i < tips.length; i++) {
            double delay = spectrum ? (tipY + i * lineHeight) * 0.25 : (tipY + i * lineHeight) / 10.0;
            int color = gui.getActiveColorRGB(delay);
            color = injectAlpha(color, cachedAlpha);
            TextUtil.drawString(ctx, tips[i], tipX, tipY + (int)(i * lineHeight), color, customFont, shadow);
        }
        matrices.pop();
    }

    private void renderTopTabs(DrawContext ctx, int mx, int my, float dt, ClickGui gui, ClientSetting cs) {
        if (Wrapper.mc == null || Wrapper.mc.getWindow() == null) return;
        boolean chinese = cs != null && cs.chinese.getValue();
        int sw = Wrapper.mc.getWindow().getScaledWidth();
        updateTopTabsLayoutIfNeeded(sw, chinese);
        boolean customFont = FontManager.isCustomFontEnabled();
        boolean shadow = FontManager.isShadowEnabled();
        TopTab activeTab = null;
        for (TopTab tab : topTabs) if (page == tab.page) { activeTab = tab; break; }
        if (activeTab == null) return;
        float targetX = activeTab.x, targetW = activeTab.w;
        if (!topTabAnimInit) {
            topTabAnimX = targetX; topTabAnimW = targetW;
            topTabAnimInit = true;
        } else {
            float a = dt * 18;
            if (a > 0.35f) a = 0.35f;
            topTabAnimX += (targetX - topTabAnimX) * a;
            topTabAnimW += (targetW - topTabAnimW) * a;
        }
        int defaultBg = gui.defaultColor.getValue().getRGB();
        int hoverBg = gui.hoverColor.getValue().getRGB();
        for (TopTab tab : topTabs) {
            boolean hover = mx >= tab.x && mx <= tab.x+tab.w && my >= tab.y && my <= tab.y+tab.h;
            int bg = hover ? hoverBg : defaultBg;
            Render2DUtil.rect(ctx.getMatrices(), tab.x, tab.y, tab.x+tab.w, tab.y+tab.h-0.5f, bg);
        }
        boolean hoverActive = mx >= activeTab.x && mx <= activeTab.x+activeTab.w && my >= activeTab.y && my <= activeTab.y+activeTab.h;
        int activeAlpha = hoverActive ? gui.hoverAlpha.getValueInt() : (int) (gui.alphaValue * 255);
        if (gui.colorMode.getValue() == ClickGui.ColorMode.Spectrum) {
            Render2DUtil.drawLutRect(ctx.getMatrices(), topTabAnimX, activeTab.y, topTabAnimW, activeTab.h-0.5f,
                    gui.getSpectrumLutId(), gui.getSpectrumLutHeight(), activeAlpha);
        } else {
            int activeColor = gui.getActiveColorRGB(activeTab.y * 0.25);
            activeColor = injectAlpha(activeColor, activeAlpha);
            Render2DUtil.rect(ctx.getMatrices(), topTabAnimX, activeTab.y, topTabAnimX+topTabAnimW, activeTab.y+activeTab.h-0.5f, activeColor);
        }
        int enableTextColor = gui.enableTextColor.getValue().getRGB();
        int defaultTextColor = gui.defaultTextColor.getValue().getRGB();
        int padX = 8;
        for (TopTab tab : topTabs) {
            boolean hover = mx >= tab.x && mx <= tab.x+tab.w && my >= tab.y && my <= tab.y+tab.h;
            boolean active = page == tab.page;
            int textColor = (active || hover) ? enableTextColor : defaultTextColor;
            float textY = getCenteredTextY(tab.y, tab.h-0.5f);
            TextUtil.drawString(ctx, tab.getLabel(chinese), tab.x+padX, textY, textColor, customFont, shadow);
        }
    }

    private boolean handleTopTabClick(int mx, int my) {
        if (Wrapper.mc == null || Wrapper.mc.getWindow() == null) return false;
        boolean chinese = ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.chinese.getValue();
        int sw = Wrapper.mc.getWindow().getScaledWidth();
        updateTopTabsLayoutIfNeeded(sw, chinese);
        for (TopTab tab : topTabs) {
            if (mx >= tab.x && mx <= tab.x+tab.w && my >= tab.y && my <= tab.y+tab.h) {
                setPage(tab.page);
                return true;
            }
        }
        return false;
    }

    private void renderConfirmDialog(DrawContext ctx, int mx, int my, ClickGui gui, ClientSetting cs) {
        boolean chn = cs != null && cs.chinese.getValue();
        boolean customFont = FontManager.isCustomFontEnabled();
        boolean shadow = FontManager.isShadowEnabled();
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        Render2DUtil.rect(ctx.getMatrices(), 0, 0, sw, sh, COLOR_BLACK_140);
        float lineH = customFont ? FontManager.ui.getFontHeight() : TextUtil.getHeight();
        float pad = 10, boxW = Math.min(340, sw-40), btnH = lineH+6;
        float boxH = pad + lineH + 6 + lineH + 12 + btnH + pad;
        float x = (sw - boxW)/2, y = (sh - boxH)/2;
        int bgColor = gui.defaultColor.getValue().getRGB();
        Render2DUtil.rect(ctx.getMatrices(), x, y, x+boxW, y+boxH, bgColor);
        String title = (confirmTitle==null||confirmTitle.isEmpty()) ? (chn?"确认":"Confirm") : confirmTitle;
        String msg = confirmMessage==null ? "" : confirmMessage;
        float titleY = y+pad, msgY = titleY+lineH+6;
        int titleColor = gui.enableTextColor.getValue().getRGB();
        int msgColor = gui.defaultTextColor.getValue().getRGB();
        float titleX = x + (boxW - getTextWidthCached(title, customFont, chn))/2;
        float msgX = x + (boxW - getTextWidthCached(msg, customFont, chn))/2;
        TextUtil.drawString(ctx, title, titleX, titleY, titleColor, customFont, shadow);
        TextUtil.drawString(ctx, msg, msgX, msgY, msgColor, customFont, shadow);
        float gap = 8, btnW = (boxW - pad*2 - gap)/2, btnY = y+boxH-pad-btnH;
        float yesX = x+pad, noX = yesX+btnW+gap;
        boolean hYes = mx >= yesX && mx <= yesX+btnW && my >= btnY && my <= btnY+btnH;
        boolean hNo  = mx >= noX  && mx <= noX+btnW  && my >= btnY && my <= btnY+btnH;
        int activeAlpha = hYes ? gui.hoverAlpha.getValueInt() : (int) (gui.alphaValue * 255);
        if (gui.colorMode.getValue() == ClickGui.ColorMode.Spectrum) {
            Render2DUtil.drawLutRect(ctx.getMatrices(), yesX, btnY, btnW, btnH, gui.getSpectrumLutId(), gui.getSpectrumLutHeight(), activeAlpha);
        } else {
            int activeColor = gui.getActiveColorRGB(btnY * 0.25);
            activeColor = injectAlpha(activeColor, activeAlpha);
            Render2DUtil.rect(ctx.getMatrices(), yesX, btnY, yesX+btnW, btnY+btnH, activeColor);
        }
        int bgNo = hNo ? gui.hoverColor.getValue().getRGB() : gui.defaultColor.getValue().getRGB();
        Render2DUtil.rect(ctx.getMatrices(), noX, btnY, noX+btnW, btnY+btnH, bgNo);
        String yes = chn ? "确认" : "Yes", no = chn ? "取消" : "No";
        float yesTx = yesX + (btnW - getTextWidthCached(yes, customFont, chn))/2;
        float noTx = noX + (btnW - getTextWidthCached(no, customFont, chn))/2;
        float btnTy = getCenteredTextY(btnY, btnH);
        TextUtil.drawString(ctx, yes, yesTx, btnTy, titleColor, customFont, shadow);
        TextUtil.drawString(ctx, no, noTx, btnTy, titleColor, customFont, shadow);
    }

    // 鼠标事件
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (confirmOpen) return handleConfirmClick((int)mouseX, (int)mouseY, button);
        if (button == 0 && handleTopTabClick((int)mouseX, (int)mouseY)) return true;
        if (page == Page.Module) {
            modulePage.mouseClicked((int)mouseX, (int)mouseY, button);
            return super.mouseClicked(mouseX, mouseY, button);
        }
        ClickGuiFrame frame = lastFrame;
        if (frame == null) return super.mouseClicked(mouseX, mouseY, button);
        if (page == Page.Config && configPage.mouseClicked((int)mouseX, (int)mouseY, button, frame)) return true;
        if (page == Page.Hud && hudPage.mouseClicked((int)mouseX, (int)mouseY, button, frame)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (page == Page.Module) modulePage.mouseReleased((int)mouseX, (int)mouseY, button);
        else if (page == Page.Hud) {
            ClickGuiFrame frame = lastFrame;
            int mx = frame != null ? (int)frame.unitMouseX((int)mouseX) : (int)mouseX;
            int my = frame != null ? (int)frame.unitMouseY((int)mouseY) : (int)mouseY;
            hudPage.mouseReleased(mx, my, button);
        }
        invalidateBounds();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horiz, double vert) {
        if (page == Page.Module) modulePage.mouseScrolled(vert);
        else if (page == Page.Config) {
            if (Wrapper.mc != null && Wrapper.mc.getWindow() != null)
                configPage.mouseScrolled(vert, Wrapper.mc.getWindow().getScaledHeight());
        } else if (page == Page.Hud) hudPage.mouseScrolled(vert);
        return super.mouseScrolled(mx, my, horiz, vert);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int mods) {
        if (confirmOpen) {
            if (keyCode == 256) { closeConfirm(); return true; }
            if (keyCode == 257 || keyCode == 335) { confirmYes(); return true; }
            return true;
        }
        if (page == Page.Module) { modulePage.keyPressed(keyCode); return super.keyPressed(keyCode, scanCode, mods); }
        if (page == Page.Hud) { hudPage.keyPressed(keyCode); return super.keyPressed(keyCode, scanCode, mods); }
        if (page == Page.Config && configPage.keyPressed(keyCode)) return true;
        return super.keyPressed(keyCode, scanCode, mods);
    }

    @Override
    public boolean charTyped(char chr, int mods) {
        if (page == Page.Module) { modulePage.charTyped(chr, mods); return super.charTyped(chr, mods); }
        if (page == Page.Hud) { hudPage.charTyped(chr, mods); return super.charTyped(chr, mods); }
        if (page == Page.Config && configPage.charTyped(chr)) return true;
        return super.charTyped(chr, mods);
    }

    public void openConfirm(String title, String msg, Runnable yesAction) {
        confirmOpen = true;
        confirmTitle = title;
        confirmMessage = msg;
        confirmYesAction = yesAction;
        configPage.stopNameListening();
    }

    private void closeConfirm() { confirmOpen = false; confirmTitle = confirmMessage = null; confirmYesAction = null; }
    private void confirmYes() { if (confirmYesAction != null) confirmYesAction.run(); closeConfirm(); }

    private boolean handleConfirmClick(int mx, int my, int btn) {
        if (btn != 0) { closeConfirm(); return true; }
        ClickGui gui = ClickGui.getInstance();
        if (gui == null) { closeConfirm(); return true; }
        int sw = Item.context != null ? Item.context.getScaledWindowWidth() : (Wrapper.mc != null && Wrapper.mc.getWindow() != null ? Wrapper.mc.getWindow().getScaledWidth() : 0);
        int sh = Item.context != null ? Item.context.getScaledWindowHeight() : (Wrapper.mc != null && Wrapper.mc.getWindow() != null ? Wrapper.mc.getWindow().getScaledHeight() : 0);
        if (sw <= 0 || sh <= 0) { closeConfirm(); return true; }
        boolean customFont = FontManager.isCustomFontEnabled();
        float lineH = customFont ? FontManager.ui.getFontHeight() : TextUtil.getHeight();
        float pad = 10, boxW = Math.min(340, sw-40), btnH = lineH+6;
        float boxH = pad + lineH + 6 + lineH + 12 + btnH + pad;
        float x = (sw - boxW)/2, y = (sh - boxH)/2, gap = 8;
        float btnW = (boxW - pad*2 - gap)/2, btnY = y + boxH - pad - btnH;
        float yesX = x+pad, noX = yesX+btnW+gap;
        boolean inYes = mx >= yesX && mx <= yesX+btnW && my >= btnY && my <= btnY+btnH;
        boolean inNo  = mx >= noX  && mx <= noX+btnW  && my >= btnY && my <= btnY+btnH;
        if (inYes) confirmYes();
        else if (inNo) closeConfirm();
        else closeConfirm();
        return true;
    }

    @Override public boolean shouldPause() { return false; }
    public final ArrayList<Component> getComponents() { return components; }
    public int getTextOffset() { return -ClickGui.getInstance().textOffset.getValueInt() - 6; }

    private void setPage(Page page) { setPage(page, true); }
    private void setPage(Page page, boolean resetHud) {
        if (page == null) return;
        this.page = page;
        for (Component c : components) c.drag = false;
        configPage.stopNameListening();
        if (page == Page.Config) configPage.onOpen();
        if (page == Page.Hud && resetHud) hudPage.resetHudLayout();
        invalidateBounds();
    }

    public void openHudWindow(WindowBase window) { hudPage.openHudWindow(window); setPage(Page.Hud, false); }

    public int getFontHeight() { return FontManager.isCustomFontEnabled() ? (int)FontManager.ui.getFontHeight() : 9; }
    public int getTextWidth(String s) {
        boolean customFont = FontManager.isCustomFontEnabled();
        boolean chinese = ClientSetting.INSTANCE != null && ClientSetting.INSTANCE.chinese.getValue();
        return getTextWidthCached(s, customFont, chinese);
    }
    public float getCenteredTextY(float baseY, float boxHeight) {
        return baseY + (boxHeight - getFontHeight())/2 + ClickGui.getInstance().textOffset.getValueInt();
    }

    private static int injectAlpha(int color, int alpha) { return (color & 0x00FFFFFF) | (alpha << 24); }

    public enum Page { Module, Config, Hud, AiAssistant }

    private static final class TopTab {
        private final Page page;
        private final String labelEn, labelZh;
        private int x, y, w, h;
        TopTab(Page page, String en, String zh) { this.page = page; this.labelEn = en; this.labelZh = zh; }
        String getLabel(boolean chinese) { return chinese && labelZh != null ? labelZh : labelEn; }
    }
}