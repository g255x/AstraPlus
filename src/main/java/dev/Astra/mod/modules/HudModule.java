package dev.Astra.mod.modules;

import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.ResizeEvent;
import dev.Astra.mod.modules.settings.impl.EnumSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;

public abstract class HudModule extends Module {
    // 静态缓存窗口尺寸（避免每帧重复获取）
    private static int cachedScaledWidth = -1;
    private static int cachedScaledHeight = -1;

    public final EnumSetting<Corner> corner;
    protected final SliderSetting x;
    protected final SliderSetting y;
    private int lastHudX;
    private int lastHudY;
    private int lastHudW;
    private int lastHudH;

    public HudModule(String name, String chinese, int defaultX, int defaultY) {
        this(name, "", chinese, defaultX, defaultY, Corner.LeftTop);
    }

    public HudModule(String name, String description, String chinese, int defaultX, int defaultY) {
        this(name, description, chinese, defaultX, defaultY, Corner.LeftTop);
    }

    protected HudModule(String name, String description, String chinese, int defaultX, int defaultY, Corner defaultCorner) {
        super(name, description, Category.Client);
        this.setChinese(chinese);
        this.corner = this.add(new EnumSetting<>("Corner", defaultCorner));
        this.x = this.add(new SliderSetting("X", defaultX, 0, 1500));
        this.y = this.add(new SliderSetting("Y", defaultY, 0, 1000));
    }

    // 窗口尺寸缓存更新（建议在 ClickGuiScreen 或主类中监听 ResizeEvent 调用此方法）
    public static void updateWindowSize() {
        if (mc != null && mc.getWindow() != null) {
            cachedScaledWidth = mc.getWindow().getScaledWidth();
            cachedScaledHeight = mc.getWindow().getScaledHeight();
        }
    }

    private static int getScaledWidth() {
        if (cachedScaledWidth <= 0 && mc != null && mc.getWindow() != null) {
            cachedScaledWidth = mc.getWindow().getScaledWidth();
        }
        return cachedScaledWidth;
    }

    private static int getScaledHeight() {
        if (cachedScaledHeight <= 0 && mc != null && mc.getWindow() != null) {
            cachedScaledHeight = mc.getWindow().getScaledHeight();
        }
        return cachedScaledHeight;
    }

    @EventListener
    public void onResize(ResizeEvent event) {
        updateWindowSize();
    }

    public final int getHudX() { return this.x.getValueInt(); }
    public final int getHudY() { return this.y.getValueInt(); }
    public final void setHudX(int x) { this.x.setValue(clamp(x, this.x.getMin(), this.x.getMax())); }
    public final void setHudY(int y) { this.y.setValue(clamp(y, this.y.getMin(), this.y.getMax())); }
    public final void setHudPos(int x, int y) { setHudX(x); setHudY(y); }


    public final int getHudRenderX(int elementW) {
        int sw = getScaledWidth();
        if (sw <= 0) return getHudX();
        int w = Math.max(0, elementW);
        int margin = getHudX();
        int x = corner.getValue().isRight() ? (sw - w - margin) : margin;
        return clampInt(x, 0, Math.max(0, sw - w));
    }


    public final int getHudRenderY(int elementH) {
        int sh = getScaledHeight();
        if (sh <= 0) return getHudY();
        int h = Math.max(0, elementH);
        int margin = getHudY();
        int y = corner.getValue().isBottom() ? (sh - h - margin) : margin;
        return clampInt(y, 0, Math.max(0, sh - h));
    }

    public final void setHudPosFromBounds(int boundsX, int boundsY) {
        int sw = getScaledWidth();
        int sh = getScaledHeight();
        if (sw <= 0 || sh <= 0) {
            setHudPos(boundsX, boundsY);
            return;
        }
        int w = Math.max(0, getHudBoundW());
        int h = Math.max(0, getHudBoundH());
        Corner c = corner.getValue();
        int nx = c.isRight() ? (sw - w - boundsX) : boundsX;
        int ny = c.isBottom() ? (sh - h - boundsY) : boundsY;
        setHudPos(nx, ny);
    }

    private static int clampInt(int v, int min, int max) {
        return (v < min) ? min : Math.min(v, max);
    }

    private static double clamp(double v, double min, double max) {
        return (v < min) ? min : Math.min(v, max);
    }

    public enum Corner {
        LeftTop(false, false),
        RightTop(true, false),
        LeftBottom(false, true),
        RightBottom(true, true);
        private final boolean right, bottom;
        Corner(boolean right, boolean bottom) { this.right = right; this.bottom = bottom; }
        public boolean isRight() { return right; }
        public boolean isBottom() { return bottom; }
    }

    // 边界缓存（用于拖拽选择与碰撞检测）
    protected final void setHudBounds(int x, int y, int w, int h) {
        this.lastHudX = x;
        this.lastHudY = y;
        this.lastHudW = w;
        this.lastHudH = h;
    }
    protected final void clearHudBounds() { lastHudW = lastHudH = 0; }
    public final int getHudBoundX() { return lastHudX; }
    public final int getHudBoundY() { return lastHudY; }
    public final int getHudBoundW() { return lastHudW; }
    public final int getHudBoundH() { return lastHudH; }

    public final boolean isHudHit(int mouseX, int mouseY) {
        return lastHudW > 0 && lastHudH > 0 &&
                mouseX >= lastHudX && mouseX <= lastHudX + lastHudW &&
                mouseY >= lastHudY && mouseY <= lastHudY + lastHudH;
    }

    public final boolean isHudOverlapping(int x1, int y1, int x2, int y2) {
        if (lastHudW <= 0 || lastHudH <= 0) return false;
        int rx1 = Math.min(x1, x2), ry1 = Math.min(y1, y2);
        int rx2 = Math.max(x1, x2), ry2 = Math.max(y1, y2);
        int bx1 = lastHudX, by1 = lastHudY;
        int bx2 = lastHudX + lastHudW, by2 = lastHudY + lastHudH;
        return rx1 < bx2 && rx2 > bx1 && ry1 < by2 && ry2 > by1;
    }
}