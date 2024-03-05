package com.momosoftworks.coldsweat.common.command.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.datafixers.util.Either;
import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.common.capability.EntityTempManager;
import com.momosoftworks.coldsweat.common.capability.temperature.ITemperatureCap;
import com.momosoftworks.coldsweat.common.command.BaseCommand;
import com.momosoftworks.coldsweat.common.command.argument.AbilityOrTempTypeArgument;
import com.momosoftworks.coldsweat.common.command.argument.TempModifierTypeArgument;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.serialization.StringRepresentable;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.BlockPosArgument;
import net.minecraft.command.arguments.DimensionArgument;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;

import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;

public class TempCommand extends BaseCommand
{
    public TempCommand(String name, int permissionLevel, boolean enabled) {
        super(name, permissionLevel, enabled);
    }

    @Override
    public LiteralArgumentBuilder<CommandSource> setExecution()
    {
        return builder
                /* Set temperature for entity */
                .then(Commands.literal("set")
                        .then(Commands.argument("entities", EntityArgument.entities())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(-150, 150))
                                        .executes(source -> executeSetEntityTemp(
                                        source.getSource(), EntityArgument.getEntities(source, "entities"), IntegerArgumentType.getInteger(source, "amount")))
                                )
                        )
                )
                /* Set temperature */
                .then(Commands.literal("get")
                        /* Get from entity */
                        .then(Commands.argument("entities", EntityArgument.entities())
                                .executes(source -> executeGetEntityTemp(
                                source.getSource(), EntityArgument.getEntities(source, "entities")))
                        )
                        /* Get from world */
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(source ->
                                          {   BlockPos pos = BlockPosArgument.getLoadedBlockPos(source, "pos");
                                              return executeGetWorldTemp(source.getSource(), pos.getX(), pos.getY(), pos.getZ(), source.getSource().getLevel());
                                          }
                                )
                                .then(Commands.argument("dimension", DimensionArgument.dimension())
                                              .executes(source -> executeGetWorldTemp(
                                                      source.getSource(), IntegerArgumentType.getInteger(source, "x"),
                                                      IntegerArgumentType.getInteger(source, "y"), IntegerArgumentType.getInteger(source, "z"),
                                                      DimensionArgument.getDimension(source, "dimension"))
                                              )
                                )
                        )
                )
                /* Get TempModifiers from entity */
                .then(Commands.literal("debug")
                        .then(Commands.argument("entity", EntityArgument.entity())
                                  .then(Commands.argument("type", TempModifierTypeArgument.modifier())
                                          .executes(source -> executeShowModifiers(
                                                  source.getSource(), EntityArgument.getEntity(source, "entity"), TempModifierTypeArgument.getModifier(source, "type"))
                                          )
                                  )
                        )
                )
                /* Modify attributes */
                .then(Commands.literal("modify")
                        .then(Commands.argument("entities", EntityArgument.entities())
                                /* Clear all attributes */
                                .then(Commands.literal("clear")
                                              /* Modify attribute of this type */
                                              .then(Commands.argument("type", AbilityOrTempTypeArgument.attribute())
                                                            .executes(source -> executeClearModifier(
                                                                    source.getSource(), EntityArgument.getEntities(source, "entities"),
                                                                    AbilityOrTempTypeArgument.getAttribute(source, "type"))
                                                            )
                                              )
                                              .executes(source -> executeClearAllModifiers(
                                                      source.getSource(), EntityArgument.getEntities(source, "entities"))
                                              )
                                )
                                /* Add to base value */
                                .then(Commands.literal("add")
                                              .then(Commands.argument("type", AbilityOrTempTypeArgument.attribute())
                                                            .then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                                                                          .then(Commands.argument("permanent", BoolArgumentType.bool())
                                                                                        .executes(source -> executeModifyEntityTemp(
                                                                                                source.getSource(), EntityArgument.getEntities(source, "entities"),
                                                                                                AbilityOrTempTypeArgument.getAttribute(source, "type"),
                                                                                                DoubleArgumentType.getDouble(source, "amount"),
                                                                                                AttributeModifier.Operation.ADDITION, BoolArgumentType.getBool(source, "permanent"))
                                                                                        )
                                                                          )
                                                                          /* Default to non-permanent if not specified */
                                                                          .executes(source -> executeModifyEntityTemp(
                                                                                  source.getSource(), EntityArgument.getEntities(source, "entities"),
                                                                                  AbilityOrTempTypeArgument.getAttribute(source, "type"),
                                                                                  DoubleArgumentType.getDouble(source, "amount"),
                                                                                  AttributeModifier.Operation.ADDITION, false)
                                                                          )
                                                            )
                                              )
                                )
                                /* Multiply base */
                                .then(Commands.literal("multiply_base")
                                              .then(Commands.argument("type", AbilityOrTempTypeArgument.attribute())
                                                            .then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                                                                          .then(Commands.argument("permanent", BoolArgumentType.bool())
                                                                                        .executes(source -> executeModifyEntityTemp(
                                                                                                source.getSource(), EntityArgument.getEntities(source, "entities"),
                                                                                                AbilityOrTempTypeArgument.getAttribute(source, "type"),
                                                                                                DoubleArgumentType.getDouble(source, "amount"),
                                                                                                AttributeModifier.Operation.MULTIPLY_BASE, BoolArgumentType.getBool(source, "permanent"))
                                                                                        )
                                                                          )
                                                                          .executes(source -> executeModifyEntityTemp(
                                                                                  source.getSource(), EntityArgument.getEntities(source, "entities"),
                                                                                  AbilityOrTempTypeArgument.getAttribute(source, "type"),
                                                                                  DoubleArgumentType.getDouble(source, "amount"),
                                                                                  AttributeModifier.Operation.MULTIPLY_BASE, false)
                                                                          )
                                                            )
                                              )
                                )
                                /* Multiply base */
                                .then(Commands.literal("multiply_total")
                                              .then(Commands.argument("type", AbilityOrTempTypeArgument.attribute())
                                                            .then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                                                                          .then(Commands.argument("permanent", BoolArgumentType.bool())
                                                                                        .executes(source -> executeModifyEntityTemp(
                                                                                                source.getSource(), EntityArgument.getEntities(source, "entities"),
                                                                                                AbilityOrTempTypeArgument.getAttribute(source, "type"),
                                                                                                DoubleArgumentType.getDouble(source, "amount"),
                                                                                                AttributeModifier.Operation.MULTIPLY_TOTAL, BoolArgumentType.getBool(source, "permanent"))
                                                                                        )
                                                                          )
                                                                          .executes(source -> executeModifyEntityTemp(
                                                                                  source.getSource(), EntityArgument.getEntities(source, "entities"),
                                                                                  AbilityOrTempTypeArgument.getAttribute(source, "type"),
                                                                                  DoubleArgumentType.getDouble(source, "amount"),
                                                                                  AttributeModifier.Operation.MULTIPLY_TOTAL, false)
                                                                          )
                                                            )
                                              )
                                )
                                .then(Commands.literal("set")
                                              .then(Commands.argument("type", AbilityOrTempTypeArgument.attribute())
                                                              .then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                                                                          .then(Commands.argument("permanent", BoolArgumentType.bool())
                                                                                          .executes(source -> executeModifyEntityTemp(
                                                                                                  source.getSource(), EntityArgument.getEntities(source, "entities"),
                                                                                                  AbilityOrTempTypeArgument.getAttribute(source, "type"),
                                                                                                  DoubleArgumentType.getDouble(source, "amount"),
                                                                                                  null, BoolArgumentType.getBool(source, "permanent"))
                                                                                          )
                                                                          )
                                                                          .executes(source -> executeModifyEntityTemp(
                                                                                  source.getSource(), EntityArgument.getEntities(source, "entities"),
                                                                                  AbilityOrTempTypeArgument.getAttribute(source, "type"),
                                                                                  DoubleArgumentType.getDouble(source, "amount"),
                                                                                  null, false)
                                                                          )
                                                              )
                                              )
                                )
                        )
                );
    }

    private int executeSetEntityTemp(CommandSource source, Collection<? extends Entity> entities, int temp)
    {
        if (entities.stream().anyMatch(entity -> !(entity instanceof PlayerEntity || EntityTempManager.getEntitiesWithTemperature().contains(entity.getType()))))
        {   source.sendFailure(new TranslationTextComponent("commands.cold_sweat.temperature.invalid"));
            return 0;
        }
        // Set the temperature for all affected targets
        for (Entity entity : entities)
        {
            if (!(entity instanceof LivingEntity)) continue;
            EntityTempManager.getTemperatureCap(entity).ifPresent(cap ->
            {   cap.setTemp(Temperature.Type.CORE, temp);
                Temperature.updateTemperature((LivingEntity) entity, cap, true);
            });
        }

        //Compose & send message
        if (entities.size() == 1)
        {   Entity target = entities.iterator().next();
            source.sendSuccess(new TranslationTextComponent("commands.cold_sweat.temperature.set.single.result", target.getName().getString(), temp), true);
        }
        else
        {   source.sendSuccess(new TranslationTextComponent("commands.cold_sweat.temperature.set.many.result", entities.size(), temp), true);
        }
        return entities.size();
    }

    private int executeGetEntityTemp(CommandSource source, Collection<? extends Entity> entities)
    {
        if (entities.stream().anyMatch(entity -> !(entity instanceof PlayerEntity || EntityTempManager.getEntitiesWithTemperature().contains(entity.getType()))))
        {   source.sendFailure(new TranslationTextComponent("commands.cold_sweat.temperature.invalid"));
            return 0;
        }
        for (Entity target : entities.stream().sorted(Comparator.comparing(player -> player.getName().getString())).collect(Collectors.toList()))
        {   //Compose & send message
            int bodyTemp = (int) Temperature.get((LivingEntity) target, Temperature.Type.BODY);
            source.sendSuccess(new TranslationTextComponent("commands.cold_sweat.temperature.get.result", target.getName().getString(), bodyTemp), false);
        }
        return entities.size();
    }

    private int executeGetWorldTemp(CommandSource source, int x , int y, int z, ServerWorld level)
    {   //Compose & send message
        Temperature.Units units = CSMath.getIfNotNull(source.getEntity(), ent -> EntityTempManager.getTemperatureCap(ent).map(ITemperatureCap::getPreferredUnits).orElse(Temperature.Units.F), Temperature.Units.F);
        int worldTemp = (int) Temperature.convertUnits(Temperature.getTemperatureAt(new BlockPos(x, y, z), level != null
                                                                                                           ? level
                                                                                                           : source.getLevel()),
                                                       Temperature.Units.MC, units, true);
        source.sendSuccess(new TranslationTextComponent("commands.cold_sweat.temperature.get.world.result", x, y, z, worldTemp, units.getFormattedName()), true);
        return Command.SINGLE_SUCCESS;
    }

    private int executeShowModifiers(CommandSource source, Entity entity, Temperature.Type type)
    {
        if (!(entity instanceof PlayerEntity || EntityTempManager.getEntitiesWithTemperature().contains(entity.getType())))
        {   source.sendFailure(new TranslationTextComponent("commands.cold_sweat.temperature.invalid"));
            return 0;
        }
        for (TempModifier modifier : Temperature.getModifiers((LivingEntity) entity, type))
        {
            source.sendSuccess(new StringTextComponent(CSMath.truncate(modifier.getLastInput(), 2)+"").withStyle(TextFormatting.WHITE)
                       .append(new StringTextComponent(" → ").withStyle(TextFormatting.WHITE))
                       .append(new StringTextComponent(modifier.toString()).withStyle(TextFormatting.GOLD))
                       .append(new StringTextComponent(" → ").withStyle(TextFormatting.WHITE))
                       .append(new StringTextComponent(CSMath.truncate(modifier.getLastOutput(), 2)+"").withStyle(TextFormatting.AQUA)), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private int executeModifyEntityTemp(CommandSource source, Collection<? extends Entity> entities, Either<Temperature.Type, Temperature.Ability> attribute,
                                        double amount, AttributeModifier.Operation operation, boolean permanent)
    {
        for (Entity entity : entities)
        {
            if (EntityTempManager.getEntitiesWithTemperature().contains(entity.getType()) && entity instanceof LivingEntity)
            {
                LivingEntity livingEntity = ((LivingEntity) entity);
                EntityTempManager.getTemperatureCap(entity).ifPresent(cap ->
                {   ModifiableAttributeInstance instance = EntityTempManager.getAttribute(attribute, ((LivingEntity) entity));
                    if (instance == null) return;
                    if (operation != null)
                    {   AttributeModifier modifier = EntityTempManager.makeAttributeModifier(attribute, amount, operation);
                        instance.addPermanentModifier(modifier);
                    }
                    else
                    {   EntityTempManager.getAttribute(attribute, ((LivingEntity) entity)).setBaseValue(amount);
                    }
                    if (permanent)
                    {   cap.markPersistentAttribute(instance.getAttribute());
                    }
                    else cap.clearPersistentAttribute(instance.getAttribute());
                });
            }
            else
            {   source.sendFailure(new TranslationTextComponent("commands.cold_sweat.temperature.invalid"));
                return 0;
            }
        }
        if (entities.size() == 1)
        {
            if (operation == null)
            source.sendSuccess(new TranslationTextComponent("commands.cold_sweat.temperature.modify.set.single.result",
                                                            attribute.left().map(StringRepresentable::getSerializedName)
                                                                    .orElse(attribute.right().map(StringRepresentable::getSerializedName)
                                                                    .orElse("")),
                                                            entities.iterator().next().getName().getString(), amount), true);
            else source.sendSuccess(new TranslationTextComponent("commands.cold_sweat.temperature.modify.add_modifier.single.result",
                                                                 attribute.left().map(StringRepresentable::getSerializedName)
                                                                         .orElse(attribute.right().map(StringRepresentable::getSerializedName)
                                                                         .orElse("")),
                                                                 entities.iterator().next().getName().getString()), true);
        }
        else
        {
            if (operation == null)
            source.sendSuccess(new TranslationTextComponent("commands.cold_sweat.temperature.modify.set.many.result",
                                                            attribute.left().map(StringRepresentable::getSerializedName)
                                                                    .orElse(attribute.right().map(StringRepresentable::getSerializedName)
                                                                    .orElse("")),
                                                            entities.size(), amount), true);
            else source.sendSuccess(new TranslationTextComponent("commands.cold_sweat.temperature.modify.add_modifier.many.result",
                                                                 attribute.left().map(StringRepresentable::getSerializedName)
                                                                         .orElse(attribute.right().map(StringRepresentable::getSerializedName)
                                                                         .orElse("")),
                                                                 entities.size()), true);
        }
        return entities.size();
    }

    private int executeClearModifier(CommandSource source, Collection<? extends Entity> entities, Either<Temperature.Type, Temperature.Ability> attribute)
    {
        for (Entity entity : entities)
        {
            if (EntityTempManager.getEntitiesWithTemperature().contains(entity.getType()) && entity instanceof LivingEntity)
            {
                EntityTempManager.getTemperatureCap(entity).ifPresent(cap ->
                {   ModifiableAttributeInstance instance = EntityTempManager.getAttribute(attribute, ((LivingEntity) entity));
                    if (instance == null) return;
                    instance.removeModifiers();
                    EntityTempManager.getAttribute(attribute, ((LivingEntity) entity)).setBaseValue(Double.NaN);
                    cap.clearPersistentAttribute(instance.getAttribute());
                });
            }
            else
            {   source.sendFailure(new TranslationTextComponent("commands.cold_sweat.temperature.invalid"));
                return 0;
            }
        }
        if (entities.size() == 1)
        {   source.sendSuccess(new TranslationTextComponent("commands.cold_sweat.temperature.clear.single.result",
                                                            attribute.left().map(StringRepresentable::getSerializedName)
                                                                    .orElse(attribute.right().map(StringRepresentable::getSerializedName)
                                                                                    .orElse("")),
                                                            entities.iterator().next().getName().getString()), true);
        }
        else
        {   source.sendSuccess(new TranslationTextComponent("commands.cold_sweat.temperature.clear.many.result",
                                                            attribute.left().map(StringRepresentable::getSerializedName)
                                                                    .orElse(attribute.right().map(StringRepresentable::getSerializedName)
                                                                                    .orElse("")),
                                                            entities.size()), true);
        }
        return entities.size();
    }

    private int executeClearAllModifiers(CommandSource source, Collection<? extends Entity> entities)
    {
        for (Entity entity : entities)
        {
            if (EntityTempManager.getEntitiesWithTemperature().contains(entity.getType()) && entity instanceof LivingEntity)
            {
                EntityTempManager.getTemperatureCap(entity).ifPresent(cap ->
                {
                    for (Either<Temperature.Type, Temperature.Ability> attribute : EntityTempManager.VALID_ATTRIBUTE_TYPES)
                    {
                        ModifiableAttributeInstance instance = EntityTempManager.getAttribute(attribute, ((LivingEntity) entity));
                        if (instance == null) continue;
                        instance.removeModifiers();
                        EntityTempManager.getAttribute(attribute, ((LivingEntity) entity)).setBaseValue(Double.NaN);
                        cap.clearPersistentAttribute(instance.getAttribute());
                    }
                });
            }
            else
            {   source.sendFailure(new TranslationTextComponent("commands.cold_sweat.temperature.invalid"));
                return 0;
            }
        }
        if (entities.size() == 1)
        {   source.sendSuccess(new TranslationTextComponent("commands.cold_sweat.temperature.clear.all.single.result",
                                                            entities.iterator().next().getName().getString()), true);
        }
        else
        {   source.sendSuccess(new TranslationTextComponent("commands.cold_sweat.temperature.clear.all.many.result",
                                                            entities.size()), true);
        }
        return entities.size();
    }
}
