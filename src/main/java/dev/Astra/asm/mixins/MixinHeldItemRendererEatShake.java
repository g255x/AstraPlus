/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.minecraft.client.render.item.HeldItemRenderer
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.ModifyArgs
 *  org.spongepowered.asm.mixin.injection.invoke.arg.Args
 */
package dev.Astra.asm.mixins;

import dev.Astra.mod.modules.impl.render.ViewModel;
import net.minecraft.client.render.item.HeldItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(value={HeldItemRenderer.class})
public abstract class MixinHeldItemRendererEatShake {
    @ModifyArgs(method={"applyEatOrDrinkTransformation"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V", ordinal=0))
    private void onApplyEatOrDrinkTransformation(Args args) {
        if (ViewModel.INSTANCE != null && ViewModel.INSTANCE.isOn() && ViewModel.INSTANCE.eatShakeMultiplier.getValue() != 1.0) {
            float currentY = (Float) args.get(1);
            args.set(1, currentY * (float) ViewModel.INSTANCE.eatShakeMultiplier.getValue());
        }
    }
}
