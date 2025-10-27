package com.dalvi.webcamhead.mixin.client;

import com.dalvi.webcamhead.client.render.SkinOverlayRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public class PlayerEntityModelMixin {

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void webcamhead$modifySkinTexture(CallbackInfoReturnable<SkinTextures> cir) {
        AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) (Object) this;
        Identifier modifiedTexture = SkinOverlayRenderer.getModifiedSkinTexture(player.getUuid());

        if (modifiedTexture != null) {
            // Get the original SkinTextures
            SkinTextures original = cir.getReturnValue();

            // Create a new SkinTextures with our modified texture
            SkinTextures modified = new SkinTextures(
                modifiedTexture,  // Use our modified texture
                original.textureUrl(),
                original.capeTexture(),
                original.elytraTexture(),
                original.model(),
                original.secure()
            );

            cir.setReturnValue(modified);
        }
    }
}
