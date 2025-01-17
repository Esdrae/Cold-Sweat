package com.momosoftworks.coldsweat.mixin.compat;

import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import sereneseasons.api.season.Season;
import sereneseasons.handler.season.RandomUpdateHandler;

@Mixin(RandomUpdateHandler.class)
public class MixinSereneIceMelt
{
    @Inject(method = "meltInChunk",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;below()Lnet/minecraft/util/math/BlockPos;"),
            locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true)
    private void getBiomeTemperatureOverride(ChunkManager chunkManager, Chunk chunkIn, Season.SubSeason subSeason, CallbackInfo ci,
                                             //locals
                                             ServerWorld level, ChunkPos chunkpos, int i, int j, int meltRand, BlockPos topAirPos)
    {
        BlockPos groundPos = topAirPos.below();
        if (WorldHelper.getBiomeTemperatureAt(level, level.getBiome(groundPos), groundPos) < 0.15F)
        {
            ci.cancel();
        }
    }
}
