package dev.Astra.mod.gui.clickgui;

public final class ClickGuiFrame {
    public float scale;
    public float slideY;
    public float pageOffsetX;
    public int pageW;
    public int panelX;
    public int panelY;
    public int panelW;
    public int panelH;
    public float totalOffsetX;
    public float totalOffsetY;
    public int screenW;
    public int screenH;

    public ClickGuiFrame() {}

    public ClickGuiFrame(float scale, float slideY, float pageOffsetX, int pageW,
                         int panelX, int panelY, int panelW, int panelH,
                         float totalOffsetX, float totalOffsetY, int screenW, int screenH) {
        set(scale, slideY, pageOffsetX, pageW, panelX, panelY, panelW, panelH,
                totalOffsetX, totalOffsetY, screenW, screenH);
    }

    public void set(float scale, float slideY, float pageOffsetX, int pageW,
                    int panelX, int panelY, int panelW, int panelH,
                    float totalOffsetX, float totalOffsetY, int screenW, int screenH) {
        this.scale = scale;
        this.slideY = slideY;
        this.pageOffsetX = pageOffsetX;
        this.pageW = pageW;
        this.panelX = panelX;
        this.panelY = panelY;
        this.panelW = panelW;
        this.panelH = panelH;
        this.totalOffsetX = totalOffsetX;
        this.totalOffsetY = totalOffsetY;
        this.screenW = screenW;
        this.screenH = screenH;
    }

    public float unitMouseX(int mouseX) {
        return scale == 0.0f ? (float) mouseX : (float) mouseX / scale;
    }

    public float unitMouseY(int mouseY) {
        return scale == 0.0f ? (float) mouseY : ((float) mouseY - slideY) / scale;
    }

    public float pageUnitW() {
        return scale == 0.0f ? (float) pageW : (float) pageW / scale;
    }

    public float baseX(ClickGuiScreen.Page page) {
        return pageOffsetX + (float) page.ordinal() * pageUnitW();
    }
}