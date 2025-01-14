package com.momosoftworks.coldsweat.util.compat;

import com.blackgear.cavesandcliffs.common.entity.GoatEntity;
import com.blackgear.cavesandcliffs.core.registries.entity.CCBEntityTypes;
import com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager;
import com.momosoftworks.coldsweat.common.capability.handler.ShearableFurManager;
import de.teamlapen.werewolves.entities.player.werewolf.WerewolfPlayer;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.registries.ModDamageSources;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.jwaresoftware.mcmods.lib.Armory;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.event.CurioChangeEvent;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    private static final boolean CREATE_LOADED = modLoaded("create", 0, 5, 1);
    private static final boolean ATMOSPHERIC_LOADED = modLoaded("atmospheric");
    private static final boolean ENVIRONMENTAL_LOADED = modLoaded("environmental");
    private static final boolean TERRALITH_LOADED = modLoaded("terralith");
    private static final boolean WEATHER_LOADED = modLoaded("weather2");
    private static final boolean WYTHERS_LOADED = modLoaded("wwoo");
    private static final boolean BETTER_WEATHER_LOADED = modLoaded("betterweather");
    private static final boolean CAVES_AND_CLIFFS_LOADED = modLoaded("cavesandcliffs");
    private static final boolean TOOLTIPS_LOADED = modLoaded("legendarytooltips");
    private static final boolean PRIMAL_WINTER_LOADED = modLoaded("primalwinter");
    private static final boolean THIRST_LOADED = modLoaded("thirst");
    private static final boolean ICEBERG_LOADED = modLoaded("iceberg");
    private static final boolean SPOILED_LOADED = modLoaded("spoiled");
    private static final boolean SUPPLEMENTARIES_LOADED = modLoaded("supplementaries");

    public static boolean modLoaded(String modID, int minMajorVer, int minMinorVer, int minPatchVer)
    {
        ModContainer mod = ModList.get().getModContainerById(modID).orElse(null);
        if (mod == null) return false;

        if (minMajorVer > 0 || minMinorVer > 0 || minPatchVer > 0)
        {
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
        else return true;
    }

    public static boolean modLoaded(String modID)
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
    public static boolean isLegendaryTooltipsLoaded()
    {   return TOOLTIPS_LOADED;
    }
    public static boolean isPrimalWinterLoaded()
    {   return PRIMAL_WINTER_LOADED;
    }
    public static boolean isThirstLoaded()
    {   return THIRST_LOADED;
    }
    public static boolean isIcebergLoaded()
    {   return ICEBERG_LOADED;
    }
    public static boolean isSpoiledLoaded()
    {   return SPOILED_LOADED;
    }
    public static boolean isSupplementariesLoaded()
    {   return SUPPLEMENTARIES_LOADED;
    }

    public static boolean hasCurio(PlayerEntity player, Item curio)
    {   return CURIOS_LOADED && getCurios(player).stream().map(ItemStack::getItem).anyMatch(item -> item == curio);
    }

    public static List<ItemStack> getCurios(LivingEntity entity)
    {
        if (!CURIOS_LOADED) return new ArrayList<>();
        return entity.getCapability(CuriosCapability.INVENTORY)
                     .map(handler -> handler.getCurios().values()).map(handlers -> handlers.stream()
                     .map(ICurioStacksHandler::getStacks)
                     .map(stacks ->
                     {
                         List<ItemStack> list = new ArrayList<>();
                         for (int i = 0; i < stacks.getSlots(); i++)
                         {   list.add(stacks.getStackInSlot(i));
                         }
                         return list;
                     }).flatMap(List::stream).collect(Collectors.toList())).orElse(new ArrayList<>());
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

    public static boolean isRainstormAt(World level, BlockPos pos)
    {
        /*if (WEATHER_LOADED)
        {
            WeatherManagerServer weatherManager = ServerTickHandler.getWeatherManagerFor(level.dimension());
            if (weatherManager == null) return false;
            StormObject rainStorm = weatherManager.getClosestStormAny(new Vec3(pos.getX(), pos.getY(), pos.getZ()), 250);
            if (rainStorm == null) return false;

            if (WorldHelper.canSeeSky(level, pos, 60) && rainStorm.isPrecipitating() && rainStorm.levelTemperature > 0.0f
            && Math.sqrt(Math.pow(pos.getX() - rainStorm.pos.x, 2) + Math.pow(pos.getX() - rainStorm.pos.x, 2)) < rainStorm.getSize())
            {   return true;
            }
        }*/
        return false;
    }

    public static boolean isGoat(Entity entity)
    {   return isCavesAndCliffsLoaded() && entity instanceof GoatEntity;
    }

    public static AnimalEntity createGoatFrom(com.momosoftworks.coldsweat.common.entity.GoatEntity goat)
    {
        if (isCavesAndCliffsLoaded())
        {
            return new Object()
            {
                public AnimalEntity create()
                {
                    GoatEntity entity = new GoatEntity(CCBEntityTypes.GOAT.get(), goat.level);
                    entity.copyPosition(goat);
                    entity.yHeadRot = goat.yHeadRot;
                    entity.yBodyRot = goat.yBodyRot;
                    entity.setHealth(goat.getHealth());
                    entity.setBaby(goat.isBaby());
                    entity.setAge(goat.getAge());
                    if (goat.hasCustomName())
                    {   entity.setCustomName(goat.getCustomName());
                        entity.setCustomNameVisible(goat.isCustomNameVisible());
                    }
                    entity.setDeltaMovement(goat.getDeltaMovement());
                    entity.setAbsorptionAmount(goat.getAbsorptionAmount());
                    entity.setAirSupply(goat.getAirSupply());
                    entity.setRemainingFireTicks(goat.getRemainingFireTicks());
                    entity.setNoGravity(goat.isNoGravity());
                    entity.setInvulnerable(goat.isInvulnerable());
                    entity.setSilent(goat.isSilent());
                    entity.setInvisible(goat.isInvisible());
                    entity.setNoAi(goat.isNoAi());
                    entity.setLeftHanded(goat.isLeftHanded());
                    if (goat.isPersistenceRequired())
                    {   entity.setPersistenceRequired();
                    }
                    entity.setGlowing(goat.isGlowing());
                    entity.setInLoveTime(goat.getInLoveTime());
                    entity.setLastHurtByMob(goat.getLastHurtByMob());
                    entity.setLastHurtMob(goat.getLastHurtMob());
                    if (goat.isLeashed())
                    {   entity.setLeashedTo(goat.getLeashHolder(), true);
                    }
                    entity.getPersistentData().merge(goat.getPersistentData());
                    entity.setScreaming(goat.isScreaming());
                    entity.getAttributes().load(goat.getAttributes().save());
                    ShearableFurManager.getFurCap(entity).ifPresent(cap ->
                    {
                        ShearableFurManager.getFurCap(goat).ifPresent(goatCap ->
                        {   cap.deserializeNBT(goatCap.serializeNBT());
                        });
                    });
                    return entity;
                }
            }.create();
        }
        return null;
    }

    /* Compat Events */

    static
    {
        if (CURIOS_LOADED)
        {
            MinecraftForge.EVENT_BUS.register(new Object()
            {
                @SubscribeEvent
                public void onCurioChange(CurioChangeEvent event)
                {
                    EntityTempManager.updateInsulationAttributeModifiers(event.getEntityLiving());
                }
            });
        }
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
}
