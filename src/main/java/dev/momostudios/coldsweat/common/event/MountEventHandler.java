package dev.momostudios.coldsweat.common.event;

import dev.momostudios.coldsweat.util.registrylists.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import dev.momostudios.coldsweat.common.temperature.modifier.MountTempModifier;
import dev.momostudios.coldsweat.config.EntitySettingsConfig;
import dev.momostudios.coldsweat.core.init.BlockInit;
import dev.momostudios.coldsweat.util.PlayerHelper;

import java.util.List;

@Mod.EventBusSubscriber
public class MountEventHandler
{
    @SubscribeEvent
    public static void onMinecartRightclick(PlayerInteractEvent.EntityInteract event)
    {
        Entity entity = event.getTarget();
        Player sourceentity = event.getPlayer();
        if (event.getHand() != sourceentity.getUsedItemHand())
        {
            return;
        }
        double x = event.getPos().getX();
        double y = event.getPos().getY();
        double z = event.getPos().getZ();
        Level world = event.getWorld();
        if (entity instanceof Minecart minecart && sourceentity.getMainHandItem().getItem() == ModItems.MINECART_INSULATION)
        {
            event.setCanceled(true);
            if (!sourceentity.isCreative())
            {
                sourceentity.getMainHandItem().shrink(1);
            }
            sourceentity.swing(InteractionHand.MAIN_HAND, true);
            world.playSound(null, new BlockPos(x, y, z), SoundEvents.LLAMA_SWAG, SoundSource.NEUTRAL, 1f, (float) ((Math.random() / 5) + 0.9));
            minecart.setDisplayBlockState(BlockInit.MINECART_INSULATION.get().defaultBlockState());
            minecart.setDisplayOffset(5);
        }
    }

    @SubscribeEvent
    public static void playerRiding(TickEvent.PlayerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
        {
            Player player = event.player;
            if (player.getVehicle() != null)
            {
                if (player.getVehicle() instanceof Minecart minecart && minecart.getDisplayBlockState().getBlock() == BlockInit.MINECART_INSULATION.get())
                {
                    PlayerHelper.addModifier(player, new MountTempModifier(1).expires(1), PlayerHelper.Types.RATE, false);
                }
                else
                {
                    for (List<Object> entity : EntitySettingsConfig.INSTANCE.insulatedEntities())
                    {
                        if (ForgeRegistries.ENTITIES.getKey(player.getVehicle().getType()).toString().equals(entity.get(0)))
                        {
                            Number number = (Number) entity.get(1);
                            double value = number.doubleValue();
                            PlayerHelper.addModifier(player, new MountTempModifier(value).expires(1), PlayerHelper.Types.RATE, false);
                        }
                    }
                }
            }
        }
    }
}