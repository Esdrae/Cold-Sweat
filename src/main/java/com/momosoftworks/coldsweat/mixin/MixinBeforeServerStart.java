package com.momosoftworks.coldsweat.mixin;

import com.momosoftworks.coldsweat.config.ConfigSettings;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLifecycleHooks.class)
public class MixinBeforeServerStart
{
    @Inject(method = "handleServerAboutToStart", at = @At(value = "RETURN"), remap = false)
    private static void beforeServerStart(MinecraftServer server, CallbackInfoReturnable<Boolean> cir)
    {   ConfigSettings.load(server.registryAccess());
    }
}