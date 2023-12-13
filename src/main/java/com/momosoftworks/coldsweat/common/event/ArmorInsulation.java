package com.momosoftworks.coldsweat.common.event;

import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.temperature.modifier.InsulationTempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.common.capability.IInsulatableCap;
import com.momosoftworks.coldsweat.common.capability.ItemInsulationCap;
import com.momosoftworks.coldsweat.common.capability.ItemInsulationCap.AdaptiveInsulation;
import com.momosoftworks.coldsweat.common.capability.ItemInsulationCap.Insulation;
import com.momosoftworks.coldsweat.common.capability.ItemInsulationCap.InsulationPair;
import com.momosoftworks.coldsweat.common.capability.ItemInsulationManager;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.registries.ModItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = ColdSweat.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ArmorInsulation
{
    @SubscribeEvent
    public static void addArmorModifiers(TickEvent.PlayerTickEvent event)
    {
        PlayerEntity player = event.player;
        if (event.phase == TickEvent.Phase.END && !player.level.isClientSide() && player.tickCount % 10 == 0)
        {
            Map<Item, Pair<Double, Double>> insulatingArmors = ConfigSettings.INSULATING_ARMORS.get();

            int fullyInsulated = 0;
            double cold = 0;
            double hot = 0;
            double worldTemp = Temperature.get(player, Temperature.Type.WORLD);
            double minTemp = Temperature.get(player, Temperature.Type.BURNING_POINT);
            double maxTemp = Temperature.get(player, Temperature.Type.FREEZING_POINT);
            for (ItemStack armorStack : player.getArmorSlots())
            {
                if (armorStack.getItem() instanceof ArmorItem)
                {
                    ArmorItem armorItem = (ArmorItem) armorStack.getItem();
                    // Add the armor's intrinsic insulation value (defined in configs)
                    // Mutually exclusive with Sewing Table insulation
                    Pair<Double, Double> insulationValue = insulatingArmors.get(armorStack.getItem());
                    if (insulationValue != null)
                    {
                        cold += insulationValue.getFirst();
                        hot += insulationValue.getSecond();
                    }
                    else
                    {   // Add the armor's insulation value from the Sewing Table
                        LazyOptional<IInsulatableCap> iCap = ItemInsulationManager.getInsulationCap(armorStack);
                        List<InsulationPair> insulation = iCap.map(cap ->
                        {
                            if (cap instanceof ItemInsulationCap)
                            {   ItemInsulationCap cap1 = (ItemInsulationCap) cap;
                                cap1.calcAdaptiveInsulation(worldTemp, minTemp, maxTemp);
                                return cap1.getInsulationValues();
                            }
                            return new ArrayList<InsulationPair>();
                        }).orElse(new ArrayList<>());

                        // Get the armor's insulation values
                        for (InsulationPair value : insulation)
                        {
                            if (value instanceof Insulation)
                            {   Insulation insul = (Insulation) value;
                                cold += insul.getCold();
                                hot += insul.getHot();
                            }
                            else if (value instanceof AdaptiveInsulation)
                            {   AdaptiveInsulation insul = (AdaptiveInsulation) value;
                                cold += CSMath.blend(insul.getInsulation() * 0.75, 0, insul.getFactor(), -1, 1);
                                hot += CSMath.blend(0, insul.getInsulation() * 0.75, insul.getFactor(), -1, 1);
                            }
                        }

                        // Used for tracking "fully_insulated" advancement
                        if ((cold + hot) / 2 >= ItemInsulationManager.getInsulationSlots(armorStack))
                        {   fullyInsulated++;
                        }
                    }

                    // Add the armor's defense value to the insulation value.
                    cold += armorItem.getDefense();
                    hot += armorItem.getDefense();

                }
            }

            if (cold == 0 && hot == 0)
                Temperature.removeModifiers(player, Temperature.Type.RATE, (mod) -> mod instanceof InsulationTempModifier);
            else
                Temperature.addOrReplaceModifier(player, new InsulationTempModifier(cold, hot).tickRate(20), Temperature.Type.RATE);

            // Award advancement for full insulation
            if (fullyInsulated >= 4 && player instanceof ServerPlayerEntity)
            {
                ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
                if (serverPlayer.getServer() != null)
                {
                    Advancement advancement = serverPlayer.getServer().getAdvancements().getAdvancement(new ResourceLocation("cold_sweat:full_insulation"));
                    if (advancement != null)
                    {   serverPlayer.getAdvancements().award(advancement, "requirement");
                    }
                }
            }
        }
    }

    /**
     * Prevent damage by magma blocks if the player has hoglin hooves
     */
    @SubscribeEvent
    public static void onDamageTaken(LivingAttackEvent event)
    {
        DamageSource source = event.getSource();
        if (source == DamageSource.HOT_FLOOR && event.getEntityLiving().getItemBySlot(EquipmentSlotType.FEET).getItem() == ModItems.HOGLIN_HOOVES)
        {   event.setCanceled(true);
        }
    }
}