package com.momosoftworks.coldsweat.common.block;

import com.momosoftworks.coldsweat.core.init.ItemInit;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.registries.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import java.util.*;

public class HearthTopBlock extends Block
{
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final Map<Direction, VoxelShape> SHAPES = new HashMap<>();

    public static Properties getProperties()
    {
        return Properties
                .of(Material.STONE)
                .sound(SoundType.STONE)
                .strength(2, 10)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .dynamicShape();
    }

    public HearthTopBlock(Block.Properties properties)
    {   super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
        calculateFacingShapes(VoxelShapes.or(
            Block.box(3, -16, 3.5, 13, 2, 12.5), // Shell
            Block.box(4, 2, 5, 9, 11, 10), // Exhaust
            Block.box(-1, -13, 6, 17, -5, 10))); // Canisters
    }

    static void calculateFacingShapes(VoxelShape shape)
    {
        for (Direction direction : Direction.values())
        {   SHAPES.put(direction, CSMath.rotateShape(direction, shape));
        }
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, IBlockReader world, BlockPos pos)
    {
        return true;
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context)
    {   return SHAPES.get(state.getValue(FACING));
    }

    @SuppressWarnings("deprecation")
    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult)
    {
        if (!world.isClientSide && world.getBlockState(pos.below()).getBlock() == ModBlocks.HEARTH_BOTTOM)
        {
            ModBlocks.HEARTH_BOTTOM.use(world.getBlockState(pos.below()), world, pos.below(), player, hand, rayTraceResult);
        }
        return ActionResultType.SUCCESS;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving)
    {   super.neighborChanged(state, world, pos, block, fromPos, isMoving);
        if (world.getBlockState(pos.below()).getBlock() != ModBlocks.HEARTH_BOTTOM)
        {   this.destroy(world, pos, state);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving)
    {
        if (state.getBlock() != newState.getBlock())
        {
            if (world.getBlockState(pos.below()).getBlock() == ModBlocks.HEARTH_BOTTOM)
            {   world.destroyBlock(pos.below(), false);
            }
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }

    @Override
    public ItemStack getCloneItemStack(IBlockReader getter, BlockPos pos, BlockState state)
    {   return new ItemStack(ItemInit.HEARTH.get());
    }

    @SuppressWarnings("deprecation")
    @Override
    public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder)
    {   return ModBlocks.HEARTH_BOTTOM.getDrops(state, builder);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation direction)
    {   return state.setValue(FACING, direction.rotate(state.getValue(FACING)));
    }

    public BlockState mirror(BlockState state, Mirror mirror)
    {   return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> p_48855_)
    {   p_48855_.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context)
    {   return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
}