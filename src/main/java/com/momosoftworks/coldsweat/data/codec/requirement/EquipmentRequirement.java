package com.momosoftworks.coldsweat.data.codec.requirement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.nbt.CompoundNBT;

import java.util.Optional;

public class EquipmentRequirement
{
    public final Optional<ItemRequirement> head;
    public final Optional<ItemRequirement> chest;
    public final Optional<ItemRequirement> legs;
    public final Optional<ItemRequirement> feet;
    public final Optional<ItemRequirement> mainHand;
    public final Optional<ItemRequirement> offHand;

    public EquipmentRequirement(Optional<ItemRequirement> head, Optional<ItemRequirement> chest,
                                Optional<ItemRequirement> legs, Optional<ItemRequirement> feet,
                                Optional<ItemRequirement> mainHand, Optional<ItemRequirement> offHand)
    {
        this.head = head;
        this.chest = chest;
        this.legs = legs;
        this.feet = feet;
        this.mainHand = mainHand;
        this.offHand = offHand;
    }

    public static final Codec<EquipmentRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemRequirement.CODEC.optionalFieldOf("head").forGetter(requirement -> requirement.head),
            ItemRequirement.CODEC.optionalFieldOf("chest").forGetter(requirement -> requirement.chest),
            ItemRequirement.CODEC.optionalFieldOf("legs").forGetter(requirement -> requirement.legs),
            ItemRequirement.CODEC.optionalFieldOf("feet").forGetter(requirement -> requirement.feet),
            ItemRequirement.CODEC.optionalFieldOf("mainhand").forGetter(requirement -> requirement.mainHand),
            ItemRequirement.CODEC.optionalFieldOf("offhand").forGetter(requirement -> requirement.offHand)
    ).apply(instance, EquipmentRequirement::new));

    public boolean test(Entity entity)
    {
        return !head.isPresent() && !chest.isPresent() && !legs.isPresent() && !feet.isPresent() && !mainHand.isPresent() && !offHand.isPresent()
            || entity instanceof LivingEntity
            && (!head.isPresent() || head.get().test(((LivingEntity) entity).getItemBySlot(EquipmentSlotType.HEAD), true))
            && (!chest.isPresent() || chest.get().test(((LivingEntity) entity).getItemBySlot(EquipmentSlotType.CHEST), true))
            && (!legs.isPresent() || legs.get().test(((LivingEntity) entity).getItemBySlot(EquipmentSlotType.LEGS), true))
            && (!feet.isPresent() || feet.get().test(((LivingEntity) entity).getItemBySlot(EquipmentSlotType.FEET), true))
            && (!mainHand.isPresent() || mainHand.get().test(((LivingEntity) entity).getMainHandItem(), true))
            && (!offHand.isPresent() || offHand.get().test(((LivingEntity) entity).getOffhandItem(), true));
    }

    public CompoundNBT serialize()
    {
        CompoundNBT tag = new CompoundNBT();
        head.ifPresent(requirement  -> tag.put("head", requirement.serialize()));
        chest.ifPresent(requirement -> tag.put("chest", requirement.serialize()));
        legs.ifPresent(requirement  -> tag.put("legs", requirement.serialize()));
        feet.ifPresent(requirement  -> tag.put("feet", requirement.serialize()));
        mainHand.ifPresent(requirement -> tag.put("main_hand", requirement.serialize()));
        offHand.ifPresent(requirement  -> tag.put("off_hand", requirement.serialize()));
        return tag;
    }

    public static EquipmentRequirement deserialize(CompoundNBT tag)
    {
        return new EquipmentRequirement(
            tag.contains("head") ? Optional.of(ItemRequirement.deserialize(tag.getCompound("head"))) : Optional.empty(),
            tag.contains("chest") ? Optional.of(ItemRequirement.deserialize(tag.getCompound("chest"))) : Optional.empty(),
            tag.contains("legs") ? Optional.of(ItemRequirement.deserialize(tag.getCompound("legs"))) : Optional.empty(),
            tag.contains("feet") ? Optional.of(ItemRequirement.deserialize(tag.getCompound("feet"))) : Optional.empty(),
            tag.contains("main_hand") ? Optional.of(ItemRequirement.deserialize(tag.getCompound("main_hand"))) : Optional.empty(),
            tag.contains("off_hand") ? Optional.of(ItemRequirement.deserialize(tag.getCompound("off_hand"))) : Optional.empty()
        );
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {   return true;
        }
        if (obj == null || getClass() != obj.getClass())
        {   return false;
        }

        EquipmentRequirement that = (EquipmentRequirement) obj;

        return head.equals(that.head) && chest.equals(that.chest) && legs.equals(that.legs) && feet.equals(that.feet) && mainHand.equals(that.mainHand) && offHand.equals(that.offHand);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        head.ifPresent(requirement  -> builder.append("Head: ").append(requirement.toString()));
        chest.ifPresent(requirement -> builder.append("Chest: ").append(requirement.toString()));
        legs.ifPresent(requirement  -> builder.append("Legs: ").append(requirement.toString()));
        feet.ifPresent(requirement  -> builder.append("Feet: ").append(requirement.toString()));
        mainHand.ifPresent(requirement -> builder.append("Main Hand: ").append(requirement.toString()));
        offHand.ifPresent(requirement  -> builder.append("Off Hand: ").append(requirement.toString()));

        return builder.toString();
    }
}