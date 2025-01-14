package com.momosoftworks.coldsweat.api.temperature.block_temp;

import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.StriderEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class LavaBlockTemp extends BlockTemp
{
    public LavaBlockTemp()
    {   super(Blocks.LAVA);
    }

    @Override
    public double getTemperature(World world, LivingEntity entity, BlockState state, BlockPos pos, double distance)
    {   FluidState fluidState = state.getFluidState();
        double temp = (fluidState.getAmount()/7f) / (entity.getVehicle() instanceof StriderEntity ? 50d : 3d);
        return CSMath.blend(temp, 0, distance, 0.5, 7);
    }

    @Override
    public double maxEffect() {
        return Temperature.convert(300, Temperature.Units.F, Temperature.Units.MC, false);
    }

    @Override
    public double maxTemperature() {
        return Temperature.convert(1000, Temperature.Units.F, Temperature.Units.MC, true);
    }
}
