/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.minecraft.block.AbstractBlock
 *  net.minecraft.block.BlockState
 *  net.minecraft.block.ShapeContext
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.shape.VoxelShape
 *  net.minecraft.world.BlockView
 */
package dev.Astra.asm.mixins;

import net.minecraft.block.AbstractBlock;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = {AbstractBlock.class})
public abstract class MixinAbstractBlock {
    // 仅保留 Mixin 声明，AO 光照已由 MixinAbstractBlockState 处理
}
