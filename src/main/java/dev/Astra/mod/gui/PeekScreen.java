package dev.Astra.mod.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.Astra.api.utils.Wrapper;
import dev.Astra.mod.modules.impl.misc.ShulkerViewer;
import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

public class PeekScreen extends ShulkerBoxScreen {
    private static final Identifier TEXTURE = Identifier.of("textures/gui/container/shulker_box.png");
    private final ItemStack[] contents;
    private final ItemStack storageBlock;
    private final int colorArgb;  // 缓存颜色整数值

    public PeekScreen(ItemStack storageBlock, ItemStack[] contents) {
        super(new ShulkerBoxScreenHandler(0, Wrapper.mc.player.getInventory(),
                new SimpleInventory(contents)), Wrapper.mc.player.getInventory(), storageBlock.getName());
        this.contents = contents;
        this.storageBlock = storageBlock;
        this.colorArgb = getShulkerColorArgb(storageBlock);
    }

    private static int getShulkerColorArgb(ItemStack shulkerItem) {
        Item item = shulkerItem.getItem();
        if (item instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (block instanceof ShulkerBoxBlock shulkerBlock) {
                DyeColor dye = shulkerBlock.getColor();
                if (dye == null) {
                    return 0xFFFFFFFF; // 白色
                }
                int rgb = dye.getEntityColor();
                return 0xFF000000 | rgb; // 完全不透明
            }
        }
        return 0xFFFFFFFF;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 2 && this.focusedSlot != null && !this.focusedSlot.getStack().isEmpty() && Wrapper.mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
            ItemStack itemStack = this.focusedSlot.getStack();
            if (ShulkerViewer.hasItems(itemStack) || itemStack.getItem() == Items.ENDER_CHEST) {
                return ShulkerViewer.openContainer(this.focusedSlot.getStack(), this.contents, false);
            }
            if (itemStack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT) != null || itemStack.get(DataComponentTypes.WRITABLE_BOOK_CONTENT) != null) {
                this.close();
                Wrapper.mc.setScreen(new BookScreen(BookScreen.Contents.create(itemStack)));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 || Wrapper.mc.options.inventoryKey.matchesKey(keyCode, scanCode)) {
            this.close();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.close();
            return true;
        }
        return false;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        // 使用缓存的颜色整数值，直接提取分量，无需创建 Color 对象
        float r = ((colorArgb >> 16) & 0xFF) / 255.0f;
        float g = ((colorArgb >> 8) & 0xFF) / 255.0f;
        float b = (colorArgb & 0xFF) / 255.0f;
        float a = ((colorArgb >> 24) & 0xFF) / 255.0f;

        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(r, g, b, a);

        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;
        context.drawTexture(TEXTURE, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
}