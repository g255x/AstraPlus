package dev.Astra.mod.gui.fonts;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.Astra.api.utils.Wrapper;
import dev.Astra.api.utils.render.Render3DUtil;
import dev.Astra.mod.gui.clickgui.ClickGuiScreen;
import dev.Astra.mod.modules.impl.client.ClickGui;
import it.unimi.dsi.fastutil.chars.Char2IntArrayMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.Closeable;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FontRenderer implements Closeable {
    private static final Char2IntArrayMap colorCodes = new Char2IntArrayMap() {{
        put('0', 0);
        put('1', 170);
        put('2', 43520);
        put('3', 43690);
        put('4', 0xAA0000);
        put('5', 0xAA00AA);
        put('6', 0xFFAA00);
        put('7', 0xAAAAAA);
        put('8', 0x555555);
        put('9', 0x5555FF);
        put('A', 0x55FF55);
        put('B', 0x55FFFF);
        put('C', 0xFF5555);
        put('D', 0xFF55FF);
        put('E', 0xFFFF55);
        put('F', 0xFFFFFF);
    }};
    private static final ExecutorService ASYNC_WORKER = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    // 以下字段原用于缓存，在优化版中不再使用，但保留以避免编译错误（不影响性能）
    private final Object2ObjectMap<Identifier, ObjectList<DrawEntry>> GLYPH_PAGE_CACHE = new Object2ObjectOpenHashMap<>();

    private final float originalSize;
    private final ObjectList<GlyphMap> maps = new ObjectArrayList<>();
    private final Char2ObjectArrayMap<Glyph> allGlyphs = new Char2ObjectArrayMap();
    private final int charsPerPage;
    private final int padding;
    private final String prebakeGlyphs;
    private int scaleMul = 0;
    private Font font;
    private Font secondFont;
    private int previousGameScale = -1;
    private Future<Void> prebakeGlyphsFuture;
    private boolean initialized;

    // 兼容原来的 DrawEntry record（不再使用，但为了编译保留）
    private record DrawEntry(float atX, float atY, float r, float g, float elementCodec, Glyph toDraw) {}

    public FontRenderer(Font font, float sizePx, int charactersPerPage, int paddingBetweenCharacters, @Nullable String prebakeCharacters) {
        this.originalSize = sizePx;
        this.charsPerPage = charactersPerPage;
        this.padding = paddingBetweenCharacters;
        this.prebakeGlyphs = prebakeCharacters;
        this.init(font, sizePx);
    }

    public FontRenderer(Font font, Font secondFont, float sizePx, int charactersPerPage, int paddingBetweenCharacters, @Nullable String prebakeCharacters) {
        this(font, sizePx, charactersPerPage, paddingBetweenCharacters, prebakeCharacters);
        this.secondFont = secondFont.deriveFont(sizePx * (float)this.scaleMul);
    }

    public FontRenderer(Font font, float sizePx) {
        this(font, sizePx, 256, 5, null);
    }

    public FontRenderer(Font font, Font secondFont, float sizePx) {
        this(font, secondFont, sizePx, 256, 5, null);
    }

    private static int floorNearestMulN(int x, int n) {
        return n * (int)Math.floor((double)x / (double)n);
    }

    public static String stripControlCodes(String text) {
        char[] chars = text.toCharArray();
        StringBuilder f = new StringBuilder();
        for (int i = 0; i < chars.length; ++i) {
            char c = chars[i];
            if (c == '\u00a7') {
                ++i;
                continue;
            }
            f.append(c);
        }
        return f.toString();
    }

    @Contract(value="-> new", pure=true)
    @NotNull
    public static Identifier randomIdentifier() {
        return Identifier.of("astraclient", "temp/" + randomString());
    }

    private static String randomString() {
        return IntStream.range(0, 32).mapToObj(operand -> String.valueOf((char)new Random().nextInt(97, 123))).collect(Collectors.joining());
    }

    @Contract(value="_ -> new", pure=true)
    public static int @NotNull [] RGBIntToRGB(int in) {
        int red = in >> 16 & 0xFF;
        int green = in >> 8 & 0xFF;
        int blue = in & 0xFF;
        return new int[]{red, green, blue};
    }

    public static double roundToDecimal(double n, int point) {
        if (point == 0) return Math.floor(n);
        double factor = Math.pow(10.0, point);
        return (double)Math.round(n * factor) / factor;
    }

    private void sizeCheck() {
        int gs = (int)Wrapper.mc.getWindow().getScaleFactor();
        if (gs != this.previousGameScale) {
            this.close();
            this.init(this.font, this.originalSize);
            if (this.secondFont != null)
                this.secondFont = this.secondFont.deriveFont(this.originalSize * (float)this.scaleMul);
        }
    }

    private void init(Font font, float sizePx) {
        if (this.initialized) throw new IllegalStateException("Double call to init()");
        this.initialized = true;
        this.scaleMul = this.previousGameScale = (int)Wrapper.mc.getWindow().getScaleFactor();
        this.font = font.deriveFont(sizePx * (float)this.scaleMul);
        if (this.prebakeGlyphs != null && !this.prebakeGlyphs.isEmpty()) {
            this.prebakeGlyphsFuture = this.prebake();
        }
    }

    private Future<Void> prebake() {
        return ASYNC_WORKER.submit(() -> {
            for (char c : this.prebakeGlyphs.toCharArray()) {
                if (Thread.interrupted()) break;
                this.locateGlyph1(c);
            }
            return null;
        });
    }

    private GlyphMap generateMap(char from, char to) {
        GlyphMap gm = this.secondFont != null ? new GlyphMap(from, to, this.font, this.secondFont, randomIdentifier(), this.padding)
                : new GlyphMap(from, to, this.font, randomIdentifier(), this.padding);
        this.maps.add(gm);
        return gm;
    }

    private Glyph locateGlyph0(char glyph) {
        for (GlyphMap map : this.maps) {
            if (!map.contains(glyph)) continue;
            return map.getGlyph(glyph);
        }
        int base = floorNearestMulN(glyph, this.charsPerPage);
        GlyphMap glyphMap = this.generateMap((char)base, (char)(base + this.charsPerPage));
        return glyphMap.getGlyph(glyph);
    }

    @Nullable
    private Glyph locateGlyph1(char glyph) {
        return this.allGlyphs.computeIfAbsent(glyph, this::locateGlyph0);
    }

    // 兼容原 API（无阴影）
    public void drawString(MatrixStack stack, String s, double x, double y, int color) {
        float r = (color >> 16 & 0xFF) / 255f;
        float g = (color >> 8 & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = (color >> 24 & 0xFF) / 255f;
        this.drawString(stack, s, (float)x, (float)y, r, g, b, a, false);
    }

    public void drawString(MatrixStack stack, String s, double x, double y, Color color) {
        this.drawString(stack, s, (float)x, (float)y, color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f, color.getAlpha()/255f, false);
    }

    public void drawString(MatrixStack stack, String s, float x, float y, float r, float g, float b, float a) {
        this.drawString(stack, s, x, y, r, g, b, a, false);
    }

    public void drawString(MatrixStack stack, String s, double x, double y, int color, boolean shadow) {
        float r = (color >> 16 & 0xFF) / 255f;
        float g = (color >> 8 & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = (color >> 24 & 0xFF) / 255f;
        this.drawString(stack, s, (float)x, (float)y, r, g, b, a, shadow);
    }

    public void drawString(MatrixStack stack, String s, double x, double y, Color color, boolean shadow) {
        this.drawString(stack, s, (float)x, (float)y, color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f, color.getAlpha()/255f, shadow);
    }

    public void drawStringWithShadow(MatrixStack stack, String s, double x, double y, int color) {
        float r = (color >> 16 & 0xFF) / 255f;
        float g = (color >> 8 & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = (color >> 24 & 0xFF) / 255f;
        this.drawString(stack, s, (float)x, (float)y, r, g, b, a, true);
    }

    public void drawStringWithShadow(MatrixStack stack, String s, double x, double y, Color color) {
        this.drawString(stack, s, (float)x, (float)y, color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f, color.getAlpha()/255f, true);
    }

    public void drawStringWithShadow(MatrixStack stack, String s, float x, float y, float r, float g, float b, float a) {
        this.drawString(stack, s, x, y, r, g, b, a, true);
    }

    /**
     * 核心绘制方法 - 极速版，零分配，直接构建顶点
     * 修复：先绘制阴影，再绘制正常文字，避免阴影覆盖字体
     */
    public void drawString(MatrixStack stack, String s, float x, float y, float r, float g, float b, float a, boolean shadow) {
        if (this.prebakeGlyphsFuture != null && !this.prebakeGlyphsFuture.isDone()) {
            try { this.prebakeGlyphsFuture.get(); } catch (InterruptedException | ExecutionException ignored) {}
        }
        this.sizeCheck();

        float baseR = r, baseG = g, baseB = b;
        float r2 = r, g2 = g, b2 = b;
        boolean useRainbow = this.useClickGuiRainbowByY();
        if (useRainbow) {
            Color active = ClickGui.getInstance().getActiveColor(y / 10.0);
            baseR = active.getRed() / 255f;
            baseG = active.getGreen() / 255f;
            baseB = active.getBlue() / 255f;
            r2 = baseR; g2 = baseG; b2 = baseB;
        }

        stack.push();
        stack.translate(roundToDecimal(x, 1), roundToDecimal(y, 1), 0.0);
        stack.scale(1f / scaleMul, 1f / scaleMul, 1f);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        Matrix4f mat = stack.peek().getPositionMatrix();

        char[] chars = s.toCharArray();
        float xOffset = 0f, yOffset = 0f;
        boolean inSel = false;
        int lineStart = 0;

        Identifier currentTex = null;
        BufferBuilder currentBuilder = null;

        for (int i = 0; i < chars.length; ++i) {
            char c = chars[i];
            if (inSel) {
                inSel = false;
                char c1 = Character.toUpperCase(c);
                if (colorCodes.containsKey(c1)) {
                    int[] col = RGBIntToRGB(colorCodes.get(c1));
                    r2 = col[0] / 255f; g2 = col[1] / 255f; b2 = col[2] / 255f;
                    continue;
                }
                if (c1 == 'R') { r2 = baseR; g2 = baseG; b2 = baseB; }
                continue;
            }
            if (c == '\u00a7') { inSel = true; continue; }
            if (c == '\n') {
                yOffset += this.getStringHeight(s.substring(lineStart, i)) * scaleMul;
                xOffset = 0f;
                lineStart = i + 1;
                if (useRainbow) {
                    Color active = ClickGui.getInstance().getActiveColor((y + yOffset) / 10.0);
                    baseR = active.getRed() / 255f;
                    baseG = active.getGreen() / 255f;
                    baseB = active.getBlue() / 255f;
                    r2 = baseR; g2 = baseG; b2 = baseB;
                }
                continue;
            }
            Glyph glyph = this.locateGlyph1(c);
            if (glyph == null || glyph.value() == ' ') {
                if (glyph != null) xOffset += glyph.width();
                continue;
            }

            Identifier tex = glyph.owner().bindToTexture;
            if (currentTex != tex) {
                if (currentBuilder != null) Render3DUtil.endBuilding(currentBuilder);
                currentTex = tex;
                RenderSystem.setShaderTexture(0, currentTex);
                currentBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            }

            GlyphMap owner = glyph.owner();
            float xo = xOffset;
            float yo = yOffset;
            float w = glyph.width();
            float h = glyph.height();
            float u1 = glyph.u() / (float) owner.width;
            float v1 = glyph.v() / (float) owner.height;
            float u2 = (glyph.u() + w) / (float) owner.width;
            float v2 = (glyph.v() + h) / (float) owner.height;

            // ★ 修复：先绘制阴影（如果开启），再绘制正常文字，避免阴影覆盖字体
            if (shadow) {
                float sx = xo + 1f, sy = yo + 1f;
                currentBuilder.vertex(mat, sx, sy + h, 0).texture(u1, v2).color(0f, 0f, 0f, a);
                currentBuilder.vertex(mat, sx + w, sy + h, 0).texture(u2, v2).color(0f, 0f, 0f, a);
                currentBuilder.vertex(mat, sx + w, sy, 0).texture(u2, v1).color(0f, 0f, 0f, a);
                currentBuilder.vertex(mat, sx, sy, 0).texture(u1, v1).color(0f, 0f, 0f, a);
            }

            // 正常字符
            currentBuilder.vertex(mat, xo, yo + h, 0).texture(u1, v2).color(r2, g2, b2, a);
            currentBuilder.vertex(mat, xo + w, yo + h, 0).texture(u2, v2).color(r2, g2, b2, a);
            currentBuilder.vertex(mat, xo + w, yo, 0).texture(u2, v1).color(r2, g2, b2, a);
            currentBuilder.vertex(mat, xo, yo, 0).texture(u1, v1).color(r2, g2, b2, a);

            xOffset += glyph.width();
        }

        if (currentBuilder != null) Render3DUtil.endBuilding(currentBuilder);
        stack.pop();
    }

    public void drawCenteredString(MatrixStack stack, String s, double x, double y, int color) {
        float r = (color >> 16 & 0xFF) / 255f;
        float g = (color >> 8 & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = (color >> 24 & 0xFF) / 255f;
        this.drawStringWithShadow(stack, s, (float)(x - this.getWidth(s) / 2.0f), (float)y, r, g, b, a);
    }

    public void drawCenteredString(MatrixStack stack, String s, double x, double y, Color color) {
        this.drawStringWithShadow(stack, s, (float)(x - this.getWidth(s) / 2.0f), (float)y,
                color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f, color.getAlpha()/255f);
    }

    public void drawCenteredString(MatrixStack stack, String s, float x, float y, float r, float g, float b, float a) {
        this.drawStringWithShadow(stack, s, x - this.getWidth(s) / 2.0f, y, r, g, b, a);
    }

    public float getWidth(String text) {
        char[] c = stripControlCodes(text).toCharArray();
        float currentLine = 0f;
        float maxPreviousLines = 0f;
        for (char c1 : c) {
            if (c1 == '\n') {
                maxPreviousLines = Math.max(currentLine, maxPreviousLines);
                currentLine = 0f;
                continue;
            }
            Glyph glyph = this.locateGlyph1(c1);
            currentLine += glyph == null ? 0f : glyph.width() / (float)this.scaleMul;
        }
        return Math.max(currentLine, maxPreviousLines);
    }

    public float getStringHeight(String text) {
        char[] c = stripControlCodes(text).toCharArray();
        if (c.length == 0) c = new char[]{' '};
        float currentLine = 0f;
        float previous = 0f;
        for (char c1 : c) {
            if (c1 == '\n') {
                if (currentLine == 0f) {
                    currentLine = this.locateGlyph1(' ') == null ? 0f : Objects.requireNonNull(this.locateGlyph1(' ')).height() / (float)this.scaleMul;
                }
                previous += currentLine;
                currentLine = 0f;
                continue;
            }
            Glyph glyph = this.locateGlyph1(c1);
            currentLine = Math.max(glyph == null ? 0f : glyph.height() / (float)this.scaleMul, currentLine);
        }
        return currentLine + previous;
    }

    @Override
    public void close() {
        try {
            if (this.prebakeGlyphsFuture != null && !this.prebakeGlyphsFuture.isDone() && !this.prebakeGlyphsFuture.isCancelled()) {
                this.prebakeGlyphsFuture.cancel(true);
                this.prebakeGlyphsFuture.get();
                this.prebakeGlyphsFuture = null;
            }
            for (GlyphMap map : this.maps) map.destroy();
            this.maps.clear();
            this.allGlyphs.clear();
            this.initialized = false;
        } catch (Exception ignored) {}
    }

    public float getFontHeight(String str) { return this.getStringHeight(str); }
    public float getFontHeight() { return this.getStringHeight("A"); }

    public void drawGradientString(MatrixStack stack, String s, float x, float y) {
        this.drawString(stack, s, x, y, 255f, 255f, 255f, 255f);
    }
    public void drawGradientCenteredString(MatrixStack matrices, String s, float x, float y) {
        this.drawGradientString(matrices, s, x - this.getWidth(s) / 2.0f, y);
    }

    private boolean useClickGuiRainbowByY() {
        if (Wrapper.mc == null || !(Wrapper.mc.currentScreen instanceof ClickGuiScreen)) return false;
        ClickGui clickGui = ClickGui.getInstance();
        if (clickGui == null) return false;
        return clickGui.style.getValue() != ClickGui.Style.RainbowDelay && clickGui.colors.getValue() && clickGui.colorMode.getValue() == ClickGui.ColorMode.Rainbow;
    }
}