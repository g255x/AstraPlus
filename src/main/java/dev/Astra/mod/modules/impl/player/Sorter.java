package dev.Astra.mod.modules.impl.player;

import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.UpdateEvent;
import dev.Astra.api.utils.math.Timer;
import dev.Astra.api.utils.player.EntityUtil;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import dev.Astra.mod.modules.settings.impl.EnumSetting;
import dev.Astra.mod.modules.settings.impl.SliderSetting;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;

import java.util.*;

public class Sorter extends Module {
    public static Sorter INSTANCE;

    public enum Page {
        General,
        Trash
    }

    // ======================== General 页面 ========================
    private final EnumSetting<Page> page = this.add(new EnumSetting<>("Page", Page.General));
    private final SliderSetting tasksPerTicks = this.add(new SliderSetting("TasksPerTick", 2, 1, 20, () -> this.page.is(Page.General)));
    private final SliderSetting delay = this.add(new SliderSetting("Delay", 0.1, 0.0, 5.0, 0.01, () -> this.page.is(Page.General)).setSuffix("s"));
    private final BooleanSetting drop = this.add(new BooleanSetting("Drop", true, () -> this.page.is(Page.General)));
    private final BooleanSetting stack = this.add(new BooleanSetting("Stack", true, () -> this.page.is(Page.General)));
    private final BooleanSetting sort = this.add(new BooleanSetting("Sort", true, () -> this.page.is(Page.General)).setParent());
    private final BooleanSetting descending = this.add(new BooleanSetting("Descending", false, () -> this.page.is(Page.General) && this.sort.isOpen()));

    // ======================== Trash 页面 ========================
    private final BooleanSetting trashUselessMinerals = this.add(new BooleanSetting("UselessMinerals", false, () -> this.page.is(Page.Trash)));
    private final BooleanSetting trashRawOres = this.add(new BooleanSetting("RawOres", false, () -> this.page.is(Page.Trash)));
    private final BooleanSetting trashNuggets = this.add(new BooleanSetting("Nuggets", false, () -> this.page.is(Page.Trash)));
    private final BooleanSetting trashStones = this.add(new BooleanSetting("Stones", false, () -> this.page.is(Page.Trash)));
    private final BooleanSetting trashPlants = this.add(new BooleanSetting("Plants", false, () -> this.page.is(Page.Trash)));
    private final BooleanSetting trashMobDrops = this.add(new BooleanSetting("MobDrops", false, () -> this.page.is(Page.Trash)));
    private final BooleanSetting trashUselessOreBlocks = this.add(new BooleanSetting("UselessOreBlocks", false, () -> this.page.is(Page.Trash)));
    private final BooleanSetting trashAnimalDrops = this.add(new BooleanSetting("AnimalDrops", false, () -> this.page.is(Page.Trash)));
    private final BooleanSetting trashMisc = this.add(new BooleanSetting("Misc", false, () -> this.page.is(Page.Trash)));
    private final BooleanSetting trashEquipment = this.add(new BooleanSetting("Equipment", false, () -> this.page.is(Page.Trash)));

    private final Timer timer = new Timer();

    public Sorter() {
        super("Sorter", Module.Category.Player);
        this.setChinese("背包整理");
        INSTANCE = this;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (!this.timer.passedS(this.delay.getValue())) return;
        if (!EntityUtil.inInventory()) return;
        int iterations = (int) Math.round(this.tasksPerTicks.getValue());
        for (int i = 0; i < iterations; i++) {
            boolean actionDone = false;
            if (this.drop.getValue() && this.tryDropTrash()) actionDone = true;
            else if (this.stack.getValue() && this.tryStackItems()) actionDone = true;
            else if (this.sort.getValue() && this.trySortItems()) actionDone = true;
            else if (this.trashEquipment.getValue() && this.tryOptimizeEquipment()) actionDone = true;
            if (actionDone) { this.timer.reset(); return; }
        }
    }

    // ---------- 常规垃圾丢弃 ----------
    private boolean tryDropTrash() {
        Set<Item> trashItems = collectTrashItems();
        for (int slot = 35; slot >= 0; slot--) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (stack.isEmpty()) continue;
            if (trashItems.contains(stack.getItem())) {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot < 9 ? slot + 36 : slot, 1, SlotActionType.THROW, mc.player);
                return true;
            }
        }
        return false;
    }

    // ---------- 完整的垃圾物品列表 ----------
    private Set<Item> collectTrashItems() {
        Set<Item> trash = new HashSet<>();

        if (this.trashUselessMinerals.getValue()) {
            trash.addAll(Set.of(Items.COAL, Items.REDSTONE, Items.LAPIS_LAZULI, Items.COPPER_INGOT,
                    Items.QUARTZ, Items.DEEPSLATE_COAL_ORE, Items.DEEPSLATE_REDSTONE_ORE,
                    Items.DEEPSLATE_LAPIS_ORE, Items.DEEPSLATE_COPPER_ORE, Items.NETHER_QUARTZ_ORE));
        }

        if (this.trashRawOres.getValue()) {
            trash.addAll(Set.of(Items.RAW_COPPER, Items.RAW_IRON, Items.RAW_GOLD));
        }

        if (this.trashNuggets.getValue()) {
            trash.addAll(Set.of(Items.IRON_NUGGET, Items.GOLD_NUGGET));
        }

        // ===== Stones 分类（新增 CLAY 和 CLAY_BALL） =====
        if (this.trashStones.getValue()) {
            trash.addAll(Set.of(Items.STONE, Items.COBBLESTONE, Items.GRANITE, Items.DIORITE, Items.ANDESITE,
                    Items.POLISHED_GRANITE, Items.POLISHED_DIORITE, Items.POLISHED_ANDESITE,
                    Items.STONE_BRICKS, Items.MOSSY_COBBLESTONE, Items.MOSSY_STONE_BRICKS,
                    Items.CHISELED_STONE_BRICKS, Items.SMOOTH_STONE,
                    Items.INFESTED_STONE, Items.INFESTED_COBBLESTONE, Items.INFESTED_STONE_BRICKS,
                    Items.INFESTED_MOSSY_STONE_BRICKS, Items.INFESTED_CHISELED_STONE_BRICKS,
                    Items.GRAVEL, Items.FLINT, Items.TUFF, Items.CALCITE, Items.DRIPSTONE_BLOCK,
                    // 新增粘土和粘土球
                    Items.CLAY, Items.CLAY_BALL,
                    // 深板岩圆石和苔藓块（之前已加）
                    Items.COBBLED_DEEPSLATE, Items.MOSS_BLOCK,
                    Items.STONE_SLAB, Items.STONE_STAIRS,
                    Items.COBBLESTONE_SLAB, Items.COBBLESTONE_STAIRS, Items.COBBLESTONE_WALL,
                    Items.GRANITE_SLAB, Items.GRANITE_STAIRS, Items.GRANITE_WALL,
                    Items.DIORITE_SLAB, Items.DIORITE_STAIRS, Items.DIORITE_WALL,
                    Items.ANDESITE_SLAB, Items.ANDESITE_STAIRS, Items.ANDESITE_WALL,
                    Items.POLISHED_GRANITE_SLAB, Items.POLISHED_GRANITE_STAIRS, Items.POLISHED_DIORITE_SLAB,
                    Items.POLISHED_DIORITE_STAIRS, Items.POLISHED_ANDESITE_SLAB, Items.POLISHED_ANDESITE_STAIRS,
                    Items.SMOOTH_STONE_SLAB, Items.STONE_BRICK_SLAB, Items.STONE_BRICK_STAIRS,
                    Items.STONE_BRICK_WALL, Items.MOSSY_STONE_BRICK_SLAB, Items.MOSSY_STONE_BRICK_STAIRS,
                    Items.MOSSY_STONE_BRICK_WALL, Items.BRICK_SLAB, Items.BRICK_STAIRS, Items.BRICK_WALL,
                    Items.DEEPSLATE, Items.POLISHED_DEEPSLATE, Items.DEEPSLATE_BRICKS, Items.DEEPSLATE_TILES,
                    Items.CHISELED_DEEPSLATE,
                    Items.POLISHED_DEEPSLATE_SLAB, Items.POLISHED_DEEPSLATE_STAIRS, Items.POLISHED_DEEPSLATE_WALL,
                    Items.DEEPSLATE_BRICK_SLAB, Items.DEEPSLATE_BRICK_STAIRS, Items.DEEPSLATE_BRICK_WALL,
                    Items.DEEPSLATE_TILE_SLAB, Items.DEEPSLATE_TILE_STAIRS, Items.DEEPSLATE_TILE_WALL,
                    Items.SANDSTONE, Items.SMOOTH_SANDSTONE, Items.CHISELED_SANDSTONE,
                    Items.RED_SANDSTONE, Items.SMOOTH_RED_SANDSTONE, Items.CHISELED_RED_SANDSTONE,
                    Items.SANDSTONE_SLAB, Items.SANDSTONE_STAIRS, Items.SANDSTONE_WALL,
                    Items.RED_SANDSTONE_SLAB, Items.RED_SANDSTONE_STAIRS, Items.RED_SANDSTONE_WALL,
                    Items.QUARTZ_BLOCK, Items.SMOOTH_QUARTZ, Items.CHISELED_QUARTZ_BLOCK, Items.QUARTZ_PILLAR,
                    Items.QUARTZ_SLAB, Items.QUARTZ_STAIRS, Items.SMOOTH_QUARTZ_SLAB, Items.SMOOTH_QUARTZ_STAIRS,
                    Items.NETHERRACK, Items.BLACKSTONE, Items.POLISHED_BLACKSTONE, Items.POLISHED_BLACKSTONE_BRICKS,
                    Items.CHISELED_POLISHED_BLACKSTONE, Items.BASALT, Items.POLISHED_BASALT, Items.SMOOTH_BASALT,
                    Items.NETHER_BRICKS, Items.RED_NETHER_BRICKS, Items.SOUL_SAND, Items.SOUL_SOIL, Items.MAGMA_BLOCK,
                    Items.NETHER_BRICK_SLAB, Items.NETHER_BRICK_STAIRS, Items.NETHER_BRICK_WALL,
                    Items.RED_NETHER_BRICK_SLAB, Items.RED_NETHER_BRICK_STAIRS, Items.RED_NETHER_BRICK_WALL,
                    Items.BLACKSTONE_SLAB, Items.BLACKSTONE_STAIRS, Items.BLACKSTONE_WALL,
                    Items.POLISHED_BLACKSTONE_SLAB, Items.POLISHED_BLACKSTONE_STAIRS, Items.POLISHED_BLACKSTONE_WALL,
                    Items.END_STONE, Items.END_STONE_BRICKS, Items.PURPUR_BLOCK, Items.PURPUR_PILLAR,
                    Items.PURPUR_SLAB, Items.PURPUR_STAIRS,
                    Items.END_STONE_BRICK_SLAB, Items.END_STONE_BRICK_STAIRS, Items.END_STONE_BRICK_WALL,
                    Items.PRISMARINE, Items.PRISMARINE_BRICKS, Items.DARK_PRISMARINE,
                    Items.PRISMARINE_SLAB, Items.PRISMARINE_STAIRS, Items.PRISMARINE_WALL,
                    Items.PRISMARINE_BRICK_SLAB, Items.PRISMARINE_BRICK_STAIRS,
                    Items.DARK_PRISMARINE_SLAB, Items.DARK_PRISMARINE_STAIRS));
        }

        if (this.trashPlants.getValue()) {
            trash.addAll(Set.of(Items.WHEAT_SEEDS, Items.PUMPKIN_SEEDS, Items.MELON_SEEDS, Items.BEETROOT_SEEDS,
                    Items.NETHER_WART, Items.TORCHFLOWER_SEEDS,
                    Items.OAK_SAPLING, Items.SPRUCE_SAPLING, Items.BIRCH_SAPLING, Items.JUNGLE_SAPLING,
                    Items.ACACIA_SAPLING, Items.DARK_OAK_SAPLING, Items.MANGROVE_PROPAGULE, Items.CHERRY_SAPLING,
                    Items.OAK_LEAVES, Items.SPRUCE_LEAVES, Items.BIRCH_LEAVES, Items.JUNGLE_LEAVES,
                    Items.ACACIA_LEAVES, Items.DARK_OAK_LEAVES, Items.MANGROVE_LEAVES, Items.CHERRY_LEAVES,
                    Items.AZALEA_LEAVES, Items.FLOWERING_AZALEA_LEAVES,
                    Items.DANDELION, Items.POPPY, Items.BLUE_ORCHID, Items.ALLIUM,
                    Items.AZURE_BLUET, Items.RED_TULIP, Items.ORANGE_TULIP, Items.WHITE_TULIP,
                    Items.PINK_TULIP, Items.OXEYE_DAISY, Items.CORNFLOWER, Items.LILY_OF_THE_VALLEY,
                    Items.WITHER_ROSE, Items.SUNFLOWER, Items.LILAC, Items.ROSE_BUSH, Items.PEONY,
                    Items.PITCHER_PLANT,
                    Items.WHEAT, Items.CARROT, Items.POTATO, Items.BEETROOT,
                    Items.SWEET_BERRIES, Items.GLOW_BERRIES, Items.SUGAR_CANE, Items.BAMBOO,
                    Items.KELP, Items.DRIED_KELP,
                    Items.VINE, Items.LILY_PAD, Items.HANGING_ROOTS, Items.BIG_DRIPLEAF,
                    Items.SMALL_DRIPLEAF, Items.SPORE_BLOSSOM, Items.GLOW_LICHEN, Items.DEAD_BUSH,
                    Items.FERN, Items.LARGE_FERN, Items.TALL_GRASS,
                    Items.CRIMSON_FUNGUS, Items.WARPED_FUNGUS, Items.CRIMSON_ROOTS, Items.WARPED_ROOTS,
                    Items.NETHER_SPROUTS, Items.CHORUS_FLOWER, Items.CHORUS_PLANT));
        }

        if (this.trashMobDrops.getValue()) {
            trash.addAll(Set.of(Items.ROTTEN_FLESH, Items.BONE, Items.SPIDER_EYE, Items.GUNPOWDER,
                    Items.STRING, Items.SLIME_BALL, Items.GHAST_TEAR, Items.MAGMA_CREAM));
        }

        if (this.trashUselessOreBlocks.getValue()) {
            trash.addAll(Set.of(Items.COAL_ORE, Items.DEEPSLATE_COAL_ORE, Items.REDSTONE_ORE, Items.DEEPSLATE_REDSTONE_ORE,
                    Items.LAPIS_ORE, Items.DEEPSLATE_LAPIS_ORE, Items.COPPER_ORE, Items.DEEPSLATE_COPPER_ORE,
                    Items.IRON_ORE, Items.DEEPSLATE_IRON_ORE, Items.GOLD_ORE, Items.DEEPSLATE_GOLD_ORE,
                    Items.NETHER_GOLD_ORE, Items.NETHER_QUARTZ_ORE));
        }

        if (this.trashAnimalDrops.getValue()) {
            trash.addAll(Set.of(Items.BEEF, Items.PORKCHOP, Items.MUTTON, Items.CHICKEN,
                    Items.RABBIT, Items.COD, Items.SALMON, Items.TROPICAL_FISH,
                    Items.PUFFERFISH, Items.LEATHER, Items.FEATHER, Items.RABBIT_HIDE, Items.RABBIT_FOOT));
        }

        if (this.trashMisc.getValue()) {
            trash.addAll(Set.of(Items.DIRT, Items.COARSE_DIRT, Items.PODZOL, Items.ROOTED_DIRT,
                    Items.BRICKS, Items.TERRACOTTA,
                    Items.WHITE_TERRACOTTA, Items.ORANGE_TERRACOTTA, Items.MAGENTA_TERRACOTTA,
                    Items.LIGHT_BLUE_TERRACOTTA, Items.YELLOW_TERRACOTTA, Items.LIME_TERRACOTTA,
                    Items.PINK_TERRACOTTA, Items.GRAY_TERRACOTTA, Items.LIGHT_GRAY_TERRACOTTA,
                    Items.CYAN_TERRACOTTA, Items.PURPLE_TERRACOTTA, Items.BLUE_TERRACOTTA,
                    Items.BROWN_TERRACOTTA, Items.GREEN_TERRACOTTA, Items.RED_TERRACOTTA, Items.BLACK_TERRACOTTA,
                    Items.WHITE_CONCRETE_POWDER, Items.ORANGE_CONCRETE_POWDER, Items.MAGENTA_CONCRETE_POWDER,
                    Items.LIGHT_BLUE_CONCRETE_POWDER, Items.YELLOW_CONCRETE_POWDER, Items.LIME_CONCRETE_POWDER,
                    Items.PINK_CONCRETE_POWDER, Items.GRAY_CONCRETE_POWDER, Items.LIGHT_GRAY_CONCRETE_POWDER,
                    Items.CYAN_CONCRETE_POWDER, Items.PURPLE_CONCRETE_POWDER, Items.BLUE_CONCRETE_POWDER,
                    Items.BROWN_CONCRETE_POWDER, Items.GREEN_CONCRETE_POWDER, Items.RED_CONCRETE_POWDER,
                    Items.BLACK_CONCRETE_POWDER,
                    Items.GLASS, Items.GLASS_PANE,
                    Items.WHITE_STAINED_GLASS, Items.WHITE_STAINED_GLASS_PANE,
                    Items.ORANGE_STAINED_GLASS, Items.ORANGE_STAINED_GLASS_PANE,
                    Items.MAGENTA_STAINED_GLASS, Items.MAGENTA_STAINED_GLASS_PANE,
                    Items.LIGHT_BLUE_STAINED_GLASS, Items.LIGHT_BLUE_STAINED_GLASS_PANE,
                    Items.YELLOW_STAINED_GLASS, Items.YELLOW_STAINED_GLASS_PANE,
                    Items.LIME_STAINED_GLASS, Items.LIME_STAINED_GLASS_PANE,
                    Items.PINK_STAINED_GLASS, Items.PINK_STAINED_GLASS_PANE,
                    Items.GRAY_STAINED_GLASS, Items.GRAY_STAINED_GLASS_PANE,
                    Items.LIGHT_GRAY_STAINED_GLASS, Items.LIGHT_GRAY_STAINED_GLASS_PANE,
                    Items.CYAN_STAINED_GLASS, Items.CYAN_STAINED_GLASS_PANE,
                    Items.PURPLE_STAINED_GLASS, Items.PURPLE_STAINED_GLASS_PANE,
                    Items.BLUE_STAINED_GLASS, Items.BLUE_STAINED_GLASS_PANE,
                    Items.BROWN_STAINED_GLASS, Items.BROWN_STAINED_GLASS_PANE,
                    Items.GREEN_STAINED_GLASS, Items.GREEN_STAINED_GLASS_PANE,
                    Items.RED_STAINED_GLASS, Items.RED_STAINED_GLASS_PANE,
                    Items.BLACK_STAINED_GLASS, Items.BLACK_STAINED_GLASS_PANE,
                    Items.WHITE_CARPET, Items.ORANGE_CARPET, Items.MAGENTA_CARPET, Items.LIGHT_BLUE_CARPET,
                    Items.YELLOW_CARPET, Items.LIME_CARPET, Items.PINK_CARPET, Items.GRAY_CARPET,
                    Items.LIGHT_GRAY_CARPET, Items.CYAN_CARPET, Items.PURPLE_CARPET, Items.BLUE_CARPET,
                    Items.BROWN_CARPET, Items.GREEN_CARPET, Items.RED_CARPET, Items.BLACK_CARPET,
                    Items.WHITE_BANNER, Items.ORANGE_BANNER, Items.MAGENTA_BANNER, Items.LIGHT_BLUE_BANNER,
                    Items.YELLOW_BANNER, Items.LIME_BANNER, Items.PINK_BANNER, Items.GRAY_BANNER,
                    Items.LIGHT_GRAY_BANNER, Items.CYAN_BANNER, Items.PURPLE_BANNER, Items.BLUE_BANNER,
                    Items.BROWN_BANNER, Items.GREEN_BANNER, Items.RED_BANNER, Items.BLACK_BANNER,
                    Items.SKELETON_SKULL, Items.WITHER_SKELETON_SKULL, Items.ZOMBIE_HEAD,
                    Items.CREEPER_HEAD, Items.DRAGON_HEAD, Items.PIGLIN_HEAD,
                    Items.PAINTING, Items.FLOWER_POT, Items.ARMOR_STAND,
                    Items.MUSIC_DISC_13, Items.MUSIC_DISC_CAT, Items.MUSIC_DISC_BLOCKS,
                    Items.MUSIC_DISC_CHIRP, Items.MUSIC_DISC_FAR, Items.MUSIC_DISC_MALL,
                    Items.MUSIC_DISC_MELLOHI, Items.MUSIC_DISC_STAL, Items.MUSIC_DISC_STRAD,
                    Items.MUSIC_DISC_WARD, Items.MUSIC_DISC_11, Items.MUSIC_DISC_WAIT,
                    Items.MUSIC_DISC_OTHERSIDE, Items.MUSIC_DISC_RELIC, Items.MUSIC_DISC_5,
                    Items.MUSIC_DISC_PIGSTEP, Items.MUSIC_DISC_CREATOR, Items.MUSIC_DISC_CREATOR_MUSIC_BOX,
                    Items.MUSIC_DISC_PRECIPICE,
                    Items.OAK_DOOR, Items.SPRUCE_DOOR, Items.BIRCH_DOOR, Items.JUNGLE_DOOR,
                    Items.ACACIA_DOOR, Items.DARK_OAK_DOOR, Items.MANGROVE_DOOR, Items.CHERRY_DOOR,
                    Items.OAK_TRAPDOOR, Items.SPRUCE_TRAPDOOR, Items.BIRCH_TRAPDOOR, Items.JUNGLE_TRAPDOOR,
                    Items.ACACIA_TRAPDOOR, Items.DARK_OAK_TRAPDOOR, Items.MANGROVE_TRAPDOOR, Items.CHERRY_TRAPDOOR,
                    Items.OAK_FENCE, Items.SPRUCE_FENCE, Items.BIRCH_FENCE, Items.JUNGLE_FENCE,
                    Items.ACACIA_FENCE, Items.DARK_OAK_FENCE, Items.MANGROVE_FENCE, Items.CHERRY_FENCE,
                    Items.OAK_FENCE_GATE, Items.SPRUCE_FENCE_GATE, Items.BIRCH_FENCE_GATE, Items.JUNGLE_FENCE_GATE,
                    Items.ACACIA_FENCE_GATE, Items.DARK_OAK_FENCE_GATE, Items.MANGROVE_FENCE_GATE, Items.CHERRY_FENCE_GATE,
                    Items.CRIMSON_STEM, Items.WARPED_STEM, Items.STRIPPED_CRIMSON_STEM, Items.STRIPPED_WARPED_STEM,
                    Items.NETHER_WART_BLOCK, Items.WARPED_WART_BLOCK,
                    Items.CRIMSON_PLANKS, Items.WARPED_PLANKS,
                    Items.CRIMSON_SLAB, Items.WARPED_SLAB, Items.CRIMSON_STAIRS, Items.WARPED_STAIRS,
                    Items.CRIMSON_FENCE, Items.WARPED_FENCE, Items.CRIMSON_FENCE_GATE, Items.WARPED_FENCE_GATE,
                    Items.CRIMSON_DOOR, Items.WARPED_DOOR, Items.CRIMSON_TRAPDOOR, Items.WARPED_TRAPDOOR,
                    Items.CRIMSON_BUTTON, Items.WARPED_BUTTON,
                    Items.CRIMSON_PRESSURE_PLATE, Items.WARPED_PRESSURE_PLATE));
        }

        return trash;
    }

    // ---------- 堆叠 ----------
    private boolean tryStackItems() {
        for (int slot1 = 35; slot1 >= 9; slot1--) {
            ItemStack stack1 = mc.player.getInventory().getStack(slot1);
            if (stack1.isEmpty() || !stack1.isStackable() || stack1.getCount() == stack1.getMaxCount()) continue;
            for (int slot2 = 0; slot2 < 36; slot2++) {
                if (slot1 == slot2) continue;
                ItemStack stack2 = mc.player.getInventory().getStack(slot2);
                if (stack2.isEmpty() || !canMerge(stack1, stack2) || stack2.getCount() == stack2.getMaxCount()) continue;
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot1 < 9 ? slot1 + 36 : slot1, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot2 < 9 ? slot2 + 36 : slot2, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot1 < 9 ? slot1 + 36 : slot1, 0, SlotActionType.PICKUP, mc.player);
                return true;
            }
        }
        return false;
    }

    // ---------- ID排序 ----------
    private boolean trySortItems() {
        boolean reverse = this.descending.getValue();
        for (int slot = 9; slot < 36; slot++) {
            ItemStack currentStack = mc.player.getInventory().getStack(slot);
            int currentId = currentStack.isEmpty() ? 0 : Item.getRawId(currentStack.getItem());
            int targetId = currentId;
            int targetSlot = slot;
            for (int other = slot + 1; other < 36; other++) {
                ItemStack otherStack = mc.player.getInventory().getStack(other);
                int otherId = otherStack.isEmpty() ? 0 : Item.getRawId(otherStack.getItem());
                boolean shouldSwap = reverse ? otherId > targetId : otherId < targetId;
                if (shouldSwap) { targetId = otherId; targetSlot = other; }
            }
            if (targetSlot != slot) {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot < 9 ? slot + 36 : slot, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, targetSlot < 9 ? targetSlot + 36 : targetSlot, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot < 9 ? slot + 36 : slot, 0, SlotActionType.PICKUP, mc.player);
                return true;
            }
        }
        return false;
    }

    // ======================== 装备优化（精简版，通过注册名判断材质） ========================
    private boolean tryOptimizeEquipment() {
        if (optimizeArmor()) return true;
        if (optimizeTools()) return true;
        return optimizeWeapons();
    }

    private int getArmorMaterialLevel(ItemStack stack) {
        if (!(stack.getItem() instanceof ArmorItem armor)) return 0;
        Identifier id = Registries.ITEM.getId(armor);
        String path = id.getPath();
        if (path.contains("netherite")) return 4;
        if (path.contains("diamond")) return 3;
        if (path.contains("iron")) return 2;
        if (path.contains("gold")) return 1;
        return 0;
    }

    private int getToolMaterialLevel(ItemStack stack) {
        if (!(stack.getItem() instanceof ToolItem tool)) return 0;
        Identifier id = Registries.ITEM.getId(tool);
        String path = id.getPath();
        if (path.contains("netherite")) return 4;
        if (path.contains("diamond")) return 3;
        if (path.contains("iron")) return 2;
        if (path.contains("gold") || path.contains("stone")) return 1;
        if (path.contains("wood")) return 0;
        return 0;
    }

    private boolean optimizeArmor() {
        Map<ArmorItem.Type, List<ItemStack>> armorMap = new EnumMap<>(ArmorItem.Type.class);
        for (ArmorItem.Type type : ArmorItem.Type.values()) armorMap.put(type, new ArrayList<>());
        for (int slot = 0; slot < 40; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof ArmorItem armor) armorMap.get(armor.getType()).add(stack);
        }
        Set<ItemStack> keep = new HashSet<>();
        for (ArmorItem.Type type : ArmorItem.Type.values()) {
            List<ItemStack> list = armorMap.get(type);
            if (list.isEmpty()) continue;
            list.stream().filter(s -> getArmorMaterialLevel(s) >= 3).forEach(keep::add);
            ItemStack best = list.stream().max(Comparator.comparingInt(this::getArmorMaterialLevel)).orElse(null);
            if (best != null) keep.add(best);
        }
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof ArmorItem && !keep.contains(stack)) {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot < 9 ? slot + 36 : slot, 1, SlotActionType.THROW, mc.player);
                return true;
            }
        }
        return false;
    }

    private boolean optimizeTools() {
        Class<?>[] toolClasses = { PickaxeItem.class, AxeItem.class, ShovelItem.class, HoeItem.class, ShearsItem.class, FishingRodItem.class };
        return optimizeGeneric(toolClasses);
    }

    private boolean optimizeWeapons() {
        Class<?>[] weaponClasses = { SwordItem.class, BowItem.class, CrossbowItem.class, TridentItem.class, ShieldItem.class, MaceItem.class };
        return optimizeGeneric(weaponClasses);
    }

    private boolean optimizeGeneric(Class<?>[] classes) {
        Map<Class<?>, List<ItemStack>> map = new HashMap<>();
        for (Class<?> clazz : classes) map.put(clazz, new ArrayList<>());
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (stack.isEmpty()) continue;
            Item item = stack.getItem();
            for (Class<?> clazz : classes) {
                if (clazz.isInstance(item)) { map.get(clazz).add(stack); break; }
            }
        }
        Set<ItemStack> keep = new HashSet<>();
        for (Class<?> clazz : classes) {
            List<ItemStack> list = map.get(clazz);
            if (list.isEmpty()) continue;
            list.stream().filter(s -> getToolMaterialLevel(s) >= 3).forEach(keep::add);
            ItemStack best = list.stream().max(Comparator.comparingInt(this::getToolMaterialLevel)).orElse(null);
            if (best != null) keep.add(best);
        }
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (stack.isEmpty()) continue;
            Item item = stack.getItem();
            for (Class<?> clazz : classes) {
                if (clazz.isInstance(item) && !keep.contains(stack)) {
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot < 9 ? slot + 36 : slot, 1, SlotActionType.THROW, mc.player);
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean canMerge(ItemStack a, ItemStack b) {
        return ItemStack.areItemsAndComponentsEqual(a, b);
    }
}