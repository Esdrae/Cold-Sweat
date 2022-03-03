package dev.momostudios.coldsweat.core.init;

import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.common.block.*;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BlockInit
{
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ColdSweat.MOD_ID);

    public static Block boilerBlock = new BoilerBlock();
    public static final RegistryObject<Block> BOILER = BLOCKS.register("boiler", () -> boilerBlock);

    public static Block iceBoxBlock = new IceboxBlock();
    public static final RegistryObject<Block> ICEBOX = BLOCKS.register("icebox", () -> iceBoxBlock);

    public static Block sewingTableBlock = new SewingTableBlock(SewingTableBlock.getProperties());
    public static final RegistryObject<Block> SEWING_TABLE = BLOCKS.register("sewing_table", () -> sewingTableBlock);

    public static Block minecartInsulationBlock = new MinecartInsulationBlock(MinecartInsulationBlock.getProperties());
    public static final RegistryObject<Block> MINECART_INSULATION = BLOCKS.register("minecart_insulation", () -> minecartInsulationBlock);

    public static Block hearthBlock = new HearthBlock();
    public static final RegistryObject<Block> HEARTH = BLOCKS.register("hearth", () -> hearthBlock);

    public static Block hearthTopBlock = new HearthTopBlock();
    public static final RegistryObject<Block> HEARTH_TOP = BLOCKS.register("hearth_top", () -> hearthTopBlock);
}