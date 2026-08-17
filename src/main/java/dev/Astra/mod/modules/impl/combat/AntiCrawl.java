package dev.Astra.mod.modules.impl.combat;

import dev.Astra.Astra;
import dev.Astra.api.events.eventbus.EventListener;
import dev.Astra.api.events.impl.UpdateEvent;
import dev.Astra.api.utils.world.BlockPosX;
import dev.Astra.api.utils.world.BlockUtil;
import dev.Astra.mod.modules.Module;
import dev.Astra.mod.modules.impl.player.PacketMine;
import dev.Astra.mod.modules.settings.impl.BooleanSetting;
import dev.Astra.mod.modules.settings.impl.EnumSetting;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

public class AntiCrawl extends Module {
    public static AntiCrawl INSTANCE;
    final double[] xzOffset = new double[]{0.0, 0.3, -0.3};
    private final EnumSetting<While> whileSetting = this.add(new EnumSetting<While>("While", While.Mining));
    private final BooleanSetting web = this.add(new BooleanSetting("Web", false));
    public boolean work = false;

    public AntiCrawl() {
        super("AntiCrawl", Module.Category.Combat);
        this.setChinese("反爬下");
        INSTANCE = this;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        this.work = false;
        if (AntiCrawl.mc.player.isFallFlying()) {
            return;
        }
        if (this.whileSetting.is(While.Always) && BlockUtil.getBlock(AntiCrawl.mc.player.getBlockPos()) != Blocks.BEDROCK || AntiCrawl.mc.player.isCrawling() || this.whileSetting.is(While.Mining) && Astra.BREAK.isMining(AntiCrawl.mc.player.getBlockPos())) {
            for (double offset : this.xzOffset) {
                for (double offset2 : this.xzOffset) {
                    BlockPosX web;
                    BlockPosX pos = new BlockPosX(AntiCrawl.mc.player.getX() + offset, AntiCrawl.mc.player.getY() + 1.2, AntiCrawl.mc.player.getZ() + offset2);
                    if (this.canBreak(pos)) {
                        PacketMine.INSTANCE.mine(pos);
                        this.work = true;
                        return;
                    }
                    if (!this.web.getValue() || AntiCrawl.mc.world.getBlockState((BlockPos)(web = new BlockPosX(AntiCrawl.mc.player.getX() + offset, AntiCrawl.mc.player.getY(), AntiCrawl.mc.player.getZ() + offset2))).getBlock() != Blocks.COBWEB || !this.canBreak(web)) continue;
                    PacketMine.INSTANCE.mine(web);
                    this.work = true;
                    return;
                }
            }
        }
    }

    private boolean canBreak(BlockPos pos) {
        return (BlockUtil.getClickSideStrict(pos) != null || pos.equals((Object)PacketMine.getBreakPos())) && !PacketMine.unbreakable(pos) && !AntiCrawl.mc.world.isAir(pos);
    }

    private static enum While {
        Crawling,
        Mining,
        Always;

    }
}