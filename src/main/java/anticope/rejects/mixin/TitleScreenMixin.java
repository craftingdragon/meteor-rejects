package anticope.rejects.mixin;

import meteordevelopment.meteorclient.systems.config.Config;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {
    // Minecraft can render a frame in its constructor, before Meteor's tail injection initializes Config.
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void rejects$skipRenderBeforeMeteorInit(CallbackInfo info) {
        if (Config.get() == null) info.cancel();
    }
}
