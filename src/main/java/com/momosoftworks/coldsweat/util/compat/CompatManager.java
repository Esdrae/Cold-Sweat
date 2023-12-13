package com.momosoftworks.coldsweat.util.compat;

import com.blackgear.cavesandcliffs.common.entity.GoatEntity;
import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.api.temperature.modifier.compat.CuriosTempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import de.teamlapen.werewolves.entities.player.werewolf.WerewolfPlayer;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.registries.ModDamageSources;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.jwaresoftware.mcmods.lib.Armory;
import top.theillusivec4.curios.api.CuriosApi;

@Mod.EventBusSubscriber
public class CompatManager
{
    private static final boolean BOP_LOADED = modLoaded("biomesoplenty");
    private static final boolean SEASONS_LOADED = modLoaded("sereneseasons");
    private static final boolean CURIOS_LOADED = modLoaded("curios");
    private static final boolean WEREWOLVES_LOADED = modLoaded("werewolves");
    private static final boolean SPIRIT_LOADED = modLoaded("spirit");
    private static final boolean ARMOR_UNDERWEAR_LOADED = modLoaded("armorunder");
    private static final boolean BYG_LOADED = modLoaded("byg");
    private static final boolean CREATE_LOADED = modLoaded("create", "0.5.1");
    private static final boolean ATMOSPHERIC_LOADED = modLoaded("atmospheric");
    private static final boolean ENVIRONMENTAL_LOADED = modLoaded("environmental");
    private static final boolean TERRALITH_LOADED = modLoaded("terralith");
    private static final boolean WEATHER_LOADED = modLoaded("weather2");
    private static final boolean WYTHERS_LOADED = modLoaded("wwoo");
    private static final boolean BETTER_WEATHER_LOADED = modLoaded("betterweather");
    private static final boolean CAVES_AND_CLIFFS_LOADED = modLoaded("cavesandcliffs");

    private static boolean modLoaded(String modID, int minMajorVer, int minMinorVer, int minPatchVer)
    {
        if (minMajorVer > 0 || minMinorVer > 0 || minPatchVer > 0)
        {
            ModContainer mod = ModList.get().getModContainerById(modID).orElse(null);
            if (mod == null) return false;

            ArtifactVersion version = mod.getModInfo().getVersion();
            if (version.getMajorVersion() >= minMajorVer
            &&  version.getMinorVersion() >= minMinorVer
            &&  version.getIncrementalVersion() >= minPatchVer)
            {   return true;
            }
            else
            {   ColdSweat.LOGGER.error("Cold Sweat requires {} {} or higher for compat to be enabled!", modID.substring(0, 1).toUpperCase() + modID.substring(1),
                                                                                                        minMajorVer + "." + minMinorVer + "." + minPatchVer);
                return false;
            }
        }
        else return ModList.get().isLoaded(modID);
    }

    private static boolean modLoaded(String modID, String ver)
    {   String[] version = ver.split("\\.");
        return modLoaded(modID, Integer.parseInt(version[0]), Integer.parseInt(version[1]), Integer.parseInt(version[2]));
    }

    private static boolean modLoaded(String modID)
    {   return modLoaded(modID, 0, 0, 0);
    }

    public static boolean isBiomesOPlentyLoaded()
    {   return BOP_LOADED;
    }
    public static boolean isSereneSeasonsLoaded()
    {   return SEASONS_LOADED;
    }
    public static boolean isCuriosLoaded()
    {   return CURIOS_LOADED;
    }
    public static boolean isWerewolvesLoaded()
    {   return WEREWOLVES_LOADED;
    }
    public static boolean isSpiritLoaded()
    {   return SPIRIT_LOADED;
    }
    public static boolean isArmorUnderwearLoaded()
    {   return ARMOR_UNDERWEAR_LOADED;
    }
    public static boolean isBiomesYoullGoLoaded()
    {   return BYG_LOADED;
    }
    public static boolean isCreateLoaded()
    {   return CREATE_LOADED;
    }
    public static boolean isAtmosphericLoaded()
    {   return ATMOSPHERIC_LOADED;
    }
    public static boolean isEnvironmentalLoaded()
    {   return ENVIRONMENTAL_LOADED;
    }
    public static boolean isTerralithLoaded()
    {   return TERRALITH_LOADED;
    }
    public static boolean isWeather2Loaded()
    {   return WEATHER_LOADED;
    }
    public static boolean isWythersLoaded()
    {   return WYTHERS_LOADED;
    }
    public static boolean isBetterWeatherLoaded()
    {   return BETTER_WEATHER_LOADED;
    }
    public static boolean isCavesAndCliffsLoaded()
    {   return CAVES_AND_CLIFFS_LOADED;
    }

    public static boolean hasOzzyLiner(ItemStack stack)
    {
        return ARMOR_UNDERWEAR_LOADED && Armory.getXLining(stack).has(Armory.XLining.TEMPERATURE_REGULATOR);
    }
    public static boolean hasOttoLiner(ItemStack stack)
    {
        return ARMOR_UNDERWEAR_LOADED && Armory.getXLining(stack).has(Armory.XLining.ANTIFREEZE_SHIELD);
    }
    public static boolean hasOllieLiner(ItemStack stack)
    {
        return false;//ARMOR_UNDERWEAR_LOADED && Armory.getXLining(stack).has(Armory.XLining.ANTIBURN_SHIELD);
    }

    public static boolean isWerewolf(PlayerEntity player)
    {
        return WEREWOLVES_LOADED && WerewolfPlayer.getOpt(player).filter(w -> w.getLevel() > 0).map(w -> w.getForm().isTransformed()).orElse(false);
    }

    public static boolean isWeather2RainingAt(World world, BlockPos pos)
    {
        /*if (!WEATHER_LOADED) return false;
        WeatherManagerServer weatherManager = ServerTickHandler.getWeatherManagerFor(world.dimension());
        if (weatherManager == null) return false;
        StormObject rainStorm = weatherManager.getClosestStormAny(new Vector3d(pos.getX(), pos.getY(), pos.getZ()), 250);
        if (rainStorm == null) return false;

        return WorldHelper.canSeeSky(world, pos, 60) && rainStorm.isPrecipitating() && rainStorm.levelTemperature > 0.0f
            && Math.sqrt(Math.pow(pos.getX() - rainStorm.pos.x, 2) + Math.pow(pos.getX() - rainStorm.pos.x, 2)) < rainStorm.getSize();*/
        return false;
    }

    public static boolean isGoat(Entity entity)
    {   return isCavesAndCliffsLoaded() && entity instanceof GoatEntity;
    }

    @SubscribeEvent
    public static void onLivingTempDamage(LivingEvent event)
    {
        if (!(event instanceof LivingDamageEvent || event instanceof LivingAttackEvent)) return;
        // Armor Underwear compat
        if (ARMOR_UNDERWEAR_LOADED && !event.getEntityLiving().level.isClientSide)
        {
            // Get the damage source from the event (different methods for LivingDamage/LivingAttack)
            DamageSource source = event instanceof LivingDamageEvent
                                  ? ((LivingDamageEvent) event).getSource()
                                  : ((LivingAttackEvent) event).getSource();
            if (source == null) return;

            boolean isDamageCold;
            if (((isDamageCold = source == ModDamageSources.COLD) || source == ModDamageSources.HOT))
            {
                int liners = 0;
                for (ItemStack stack : event.getEntityLiving().getArmorSlots())
                {
                    if (isDamageCold ? hasOttoLiner(stack) : hasOllieLiner(stack))
                        liners++;
                }
                // Cancel the event if full liners
                if (liners >= 4)
                {   event.setCanceled(true);
                    return;
                }
                // Dampen the damage as the number of liners increases
                if (event instanceof LivingDamageEvent)
                {   LivingDamageEvent damageEvent = (LivingDamageEvent) event;
                    damageEvent.setAmount(CSMath.blend(damageEvent.getAmount(), 0, liners, 0, 4));
                }
            }
        }
    }

    @SubscribeEvent
    public static void tickCurios(TickEvent.PlayerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END && CURIOS_LOADED && event.player.tickCount % 20 == 0)
        {
            CuriosApi.getCuriosHelper().getCuriosHandler(event.player).ifPresent(curiosInv ->
            {
                curiosInv.getCurios().forEach((identifier, curioStackHandler) ->
                {
                    for (int i = 0; i < curioStackHandler.getStacks().getSlots(); i++)
                    {
                        ItemStack stack = curioStackHandler.getStacks().getStackInSlot(i);
                        if (!stack.isEmpty() && ConfigSettings.INSULATING_CURIOS.get().containsKey(stack.getItem()))
                        {
                            Pair<Double, Double> insulation = ConfigSettings.INSULATING_CURIOS.get().get(stack.getItem());
                            double cold = insulation.getFirst();
                            double hot = insulation.getSecond();
                            Temperature.addOrReplaceModifier(event.player, new CuriosTempModifier(cold, hot).expires(20), Temperature.Type.RATE);
                        }
                    }
                });
            });
        }
    }
}