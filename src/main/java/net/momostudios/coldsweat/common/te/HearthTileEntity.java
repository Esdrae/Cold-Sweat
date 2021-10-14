package net.momostudios.coldsweat.common.te;

import net.minecraft.block.*;
import net.minecraft.client.particle.CloudParticle;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.LockableLootTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import net.momostudios.coldsweat.ColdSweat;
import net.momostudios.coldsweat.common.block.HearthBlock;
import net.momostudios.coldsweat.common.container.HearthContainer;
import net.momostudios.coldsweat.common.temperature.Temperature;
import net.momostudios.coldsweat.config.ColdSweatConfig;
import net.momostudios.coldsweat.config.FuelItemsConfig;
import net.momostudios.coldsweat.core.init.BlockInit;
import net.momostudios.coldsweat.core.init.ParticleTypesInit;
import net.momostudios.coldsweat.core.init.TileEntityInit;
import net.momostudios.coldsweat.core.util.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class HearthTileEntity extends LockableLootTileEntity implements ITickableTileEntity
{
    public static int slots = 10;
    protected NonNullList<ItemStack> items = NonNullList.withSize(slots, ItemStack.EMPTY);
    public int ticksExisted;
    private int resetTimer;
    private int insulationLevel;
    protected static int MAX_FUEL = 1000;

    public HearthTileEntity(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }

    @Override
    protected ITextComponent getDefaultName() {
        return new TranslationTextComponent("container." + ColdSweat.MOD_ID + ".hearth");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> itemsIn)
    {
        this.items = itemsIn;
    }

    public HearthTileEntity()
    {
        this(TileEntityInit.HEARTH_TILE_ENTITY_TYPE.get());
    }

    public void tick()
    {
        this.ticksExisted = (this.ticksExisted + 1) % 1000;
        resetTimer = this.getTileData().getInt("resetTimer");
        insulationLevel = this.getTileData().getInt("insulationLevel");

        if (resetTimer > 0)
            this.getTileData().putInt("resetTimer", resetTimer - 1);

        if (insulationLevel < 2400)
            this.getTileData().putInt("insulationLevel", insulationLevel + 1);

        if ((this.getHotFuel() > 0 || this.getColdFuel() > 0) && !world.canBlockSeeSky(pos))
        {
            List<PlayerEntity> affectedPlayers = new ArrayList<>();

            // Represents the NBT list
            List<BlockPos> poss = this.getPoints();

            // Create temporary list to add back to the NBT
            List<BlockPos> positions2 = new ArrayList<>();

            /*
             Partition all points into multiple lists (max of 19)
            */
            // Size of each partition
            int partSize = 80;
            // Number of partitions with 150 elements each
            int partitionCount = (int) Math.ceil(poss.size() / (double) partSize);
            // Index of the last point being worked on this tick
            int lastIndex = Math.min(poss.size(), partSize * (this.ticksExisted % partitionCount + 1) + 1);
            // Index of the first point being worked on this tick
            int firstIndex = Math.max(0, lastIndex - partSize);

            boolean shouldReset = false;

            /*
             Iterate through the partition with index [scanningIndex]
             */
            for (BlockPos blockPos : poss.subList(firstIndex, lastIndex))
            {
                // Get if any adjacent block is solid
                boolean touchingSolid = WorldInfo.adjacentPositions(blockPos).stream().anyMatch(newPos -> world.getBlockState(pos).isSolid());

                // Create detection box for PlayerEntities
                int x = blockPos.getX();
                int y = blockPos.getY();
                int z = blockPos.getZ();
                AxisAlignedBB aabb = touchingSolid ? new AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1) :
                    new AxisAlignedBB(x - 1, y - 1, z - 1, x + 2, y + 2, z + 2);

                // Add players to list [affectedPlayers]
                affectedPlayers.addAll(world.getEntitiesWithinAABB(PlayerEntity.class, aabb));

                // Show radius if enabled
                if (this.getTileData().getBoolean("showRadius") && world.isRemote && Math.random() < 0.2)
                for (int p = 0; p < (touchingSolid ? 1 : Math.random() * 3); p++)
                {
                    double xr = touchingSolid ? Math.random() : Math.random() * 3 - 1;
                    double xm = Math.random() / 20 - 0.025;
                    double yr = touchingSolid ? Math.random() : Math.random() * 3 - 1;
                    double zr = touchingSolid ? Math.random() : Math.random() * 3 - 1;
                    double zm = Math.random() / 20 - 0.025;

                    world.addParticle(ParticleTypesInit.HEARTH_AIR.get(), blockPos.getX() + xr, blockPos.getY() + yr, blockPos.getZ() + zr, xm, 0, zm);
                }

                // If a block has changed in the area, trigger a reset of the area shape
                if (!isBlockSpreadable(blockPos) && resetTimer == 0 && !shouldReset) {
                    shouldReset = true;
                }

                // Check in all 6 directions
                for (Direction direction : Direction.values())
                {
                    // Create new BlockPos with an offset of [direction] from [blockPos]
                    BlockPos testpos = touchingSolid ?
                            blockPos.add(direction.getDirectionVec()) :
                            blockPos.add(direction.getXOffset() * 2, direction.getYOffset() * 2, direction.getZOffset() * 2);

                    if (testpos.withinDistance(pos, 20))
                    {
                        if (isBlockSpreadable(testpos) && !world.canBlockSeeSky(testpos) && !poss.contains(testpos) && !positions2.contains(testpos) &&
                        (touchingSolid || MathHelperCS.isEvenPosition(testpos)))
                        {
                            positions2.add(testpos);
                        }
                    }
                }
            }
            // Add new positions to NBT
            this.addPoints(positions2);

            // Reset points if a block has been added
            if (shouldReset)
            {
                this.clearPoints();
                this.getTileData().putInt("resetTimer", 1200);
            }

            ColdSweatConfig config = ColdSweatConfig.getInstance();
            for (PlayerEntity player : affectedPlayers)
            {
                Temperature playerTemp = PlayerTemp.getTemperature(player, PlayerTemp.Types.AMBIENT);
                if (playerTemp.get() < config.minHabitable() && this.getHotFuel() > 0 ||
                    playerTemp.get() > config.maxHabitable() && this.getColdFuel() > 0 ||
                   (playerTemp.get() > config.minHabitable() && playerTemp.get() < config.maxHabitable()))
                {
                    player.addPotionEffect(new EffectInstance(ModEffects.INSULATION, 200, Math.max(0, insulationLevel / 240 - 1),
                            false, false));
                }
            }
        }

        // Input fuel types
        if (!getFuelItem(this.getItemInSlot(0)).isEmpty())
        {
            ItemStack fuel = this.getItemInSlot(0);
            int amount = (int) getFuelItem(this.getItemInSlot(0)).get(1);
            if (this.getHotFuel() <= Math.max(MAX_FUEL - amount, MAX_FUEL * 0.9) && amount > 0)
            {
                if (fuel.hasContainerItem() && fuel.getCount() == 1)
                {
                    this.setItemInSlot(0, fuel.getContainerItem());
                }
                else this.getItemInSlot(0).shrink(1);
                this.setHotFuel(this.getHotFuel() + amount);
            }

            if (this.getColdFuel() <= Math.max(MAX_FUEL - amount, MAX_FUEL * 0.9) && amount < 0)
            {
                if (fuel.hasContainerItem() && fuel.getCount() == 1)
                {
                    this.setItemInSlot(0, fuel.getContainerItem());
                }
                else this.getItemInSlot(0).shrink(1);
                this.setColdFuel(this.getColdFuel() - amount);
            }
        }

        // Drain fuel
        if (this.ticksExisted % 40 == 0)
        {
            if (this.getColdFuel() > 0)
            {
                this.setColdFuel(this.getColdFuel() - 1);
            }
            if (this.getHotFuel() > 0)
            {
                this.setHotFuel(this.getHotFuel() - 1);
            }
        }

        // Update BlockState
        if (this.ticksExisted % 5 == 0 && world.getBlockState(pos).getBlock() == BlockInit.HEARTH.get())
        {
            BlockState state = world.getBlockState(pos);
            int waterLevel = this.getColdFuel() == 0 ? 0 : (this.getColdFuel() < MAX_FUEL / 2 ? 1 : 2);
            int lavaLevel  = this.getHotFuel()  == 0 ? 0 : (this.getHotFuel()  < MAX_FUEL / 2 ? 1 : 2);

            BlockState desiredState = state.with(HearthBlock.WATER, waterLevel).with(HearthBlock.LAVA, lavaLevel);
            if (state.get(HearthBlock.WATER) != waterLevel || state.get(HearthBlock.LAVA) != lavaLevel)
            {
                world.setBlockState(pos, desiredState);
            }
        }

        // Particles
        int coldFuel = getColdFuel();
        int hotFuel = getHotFuel();

        if (Math.random() < coldFuel / 2000d)
        {
            double d0 = pos.getX() + 0.5d;
            double d1 = pos.getY() + 2d;
            double d2 = pos.getZ() + 0.5d;
            double d3 = (Math.random() - 0.5) / 4;
            double d4 = (Math.random() - 0.5) / 4;
            double d5 = (Math.random() - 0.5) / 4;
            world.addParticle(ParticleTypes.CLOUD, d0 + d3, d1 + d4, d2 + d5, 0.0D, 0.02D, 0.0D);
        }
        if (Math.random() < hotFuel / 2000d)
        {
            double d0 = pos.getX() + 0.5d;
            double d1 = pos.getY() + 2d;
            double d2 = pos.getZ() + 0.5d;
            double d3 = (Math.random() - 0.5) / 2;
            double d4 = (Math.random() - 0.5) / 2;
            double d5 = (Math.random() - 0.5) / 2;
            world.addParticle(ParticleTypes.SMOKE, d0 + d3, d1 + d4, d2 + d5, 0.0D, 0.02D, 0.0D);
        }
    }

    public List getFuelItem(ItemStack item)
    {
        List returnList = Arrays.asList(item, 0);
        for (List<String> iterator : FuelItemsConfig.getInstance().hearthItems())
        {
            String testItem = iterator.get(0);

            if (new ResourceLocation(testItem).equals(ForgeRegistries.ITEMS.getKey(item.getItem())))
            {
                returnList = Arrays.asList(item, Integer.parseInt(iterator.get(1)));
            }
        }
        return returnList;
    }

    public ItemStack getItemInSlot(int index)
    {
        AtomicReference<ItemStack> stack = new AtomicReference<>(ItemStack.EMPTY);
        this.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).ifPresent(capability ->
        {
            stack.set(capability.getStackInSlot(index));
        });
        return stack.get();
    }

    public void setItemInSlot(int index, ItemStack stack)
    {
        this.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).ifPresent(capability ->
        {
            if (stack != null && stack != capability.getStackInSlot(index))
            {
                capability.extractItem(index, capability.getStackInSlot(index).getCount(), false);
            }
            capability.insertItem(index, stack, false);
        });
    }

    public int getHotFuel()
    {
        return this.getTileData().getInt("hot_fuel");
    }

    public int getColdFuel()
    {
        return this.getTileData().getInt("cold_fuel");
    }

    public void setHotFuel(int amount)
    {
        this.getTileData().putInt("hot_fuel", Math.min(amount, MAX_FUEL));
    }

    public void setColdFuel(int amount)
    {
        this.getTileData().putInt("cold_fuel", Math.min(amount, MAX_FUEL));
    }

    public boolean isBlockSpreadable(BlockPos pos)
    {
        BlockState state = world.getBlockState(pos);
        return !state.isSolid() &&
                            !state.isNormalCube(world.getBlockReader(world.getChunk(pos).getPos().x, world.getChunk(pos).getPos().z), pos) &&
                            (!(state.getBlock() instanceof PaneBlock) ||
                            state.isReplaceable(Fluids.WATER) ||
                            (state.hasProperty(DoorBlock.OPEN) && state.get(DoorBlock.OPEN)) ||
                            (state.hasProperty(TrapDoorBlock.OPEN) && state.get(TrapDoorBlock.OPEN)) ||
                            world.isAirBlock(pos) ||
                            state.getBlock() == BlockInit.HEARTH.get());
    }

    public List<BlockPos> getPoints()
    {
        return this.getPoints(0, this.getTileData().getList("points", 11).size());
    }

    public List<BlockPos> getPoints(int firstIndex, int lastIndex)
    {
        List<BlockPos> pointList = new ArrayList<>();

        for (INBT point : this.getTileData().getList("points", 11).subList(firstIndex, lastIndex))
        {
            int[] values = ((IntArrayNBT) point).getIntArray();
            pointList.add(new BlockPos(values[0], values[1], values[2]));
        }
        if (pointList.isEmpty())
        {
            pointList.addAll(Arrays.asList(
                pos,
                pos.up(),
                pos.north(),
                pos.north().east(),
                pos.south(),
                pos.south().west(),
                pos.east(),
                pos.east().north(),
                pos.west(),
                pos.west().south()
            ));
        }
        return pointList;
    }

    public void addPoints(List<BlockPos> pointList)
    {
        ListNBT points = this.getTileData().getList("points", 11);
        for (BlockPos bpos : pointList)
        {
            int[] values = {bpos.getX(), bpos.getY(), bpos.getZ()};
            points.add(new IntArrayNBT(values));
        }
        this.getTileData().put("points", points);
    }

    public void clearPoints()
    {
        this.getTileData().put("points", new ListNBT());
    }

    @Override
    public int getSizeInventory() {
        return slots;
    }

    @Override
    protected Container createMenu(int id, PlayerInventory player)
    {
        return new HearthContainer(id, player, this);
    }

    @Override
    public void read(BlockState state, CompoundNBT nbt)
    {
        super.read(state, nbt);
        this.items = NonNullList.withSize(getSizeInventory(), ItemStack.EMPTY);
        if (!this.checkLootAndRead(nbt))
        {
            ItemStackHelper.loadAllItems(nbt, this.items);
        }
    }

    @Override
    public CompoundNBT write(CompoundNBT compound)
    {
        super.write(compound);
        if (!this.checkLootAndWrite(compound))
        {
            ItemStackHelper.saveAllItems(compound, items);
        }
        return compound;
    }
}