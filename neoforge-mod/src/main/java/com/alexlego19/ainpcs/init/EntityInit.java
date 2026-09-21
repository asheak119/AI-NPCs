package com.alexlego19.ainpcs.init;

import com.alexlego19.ainpcs.AiNpcsMod;
import com.alexlego19.ainpcs.entity.NpcEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EntityInit {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AiNpcsMod.MODID);

    public static final RegistryObject<EntityType<NpcEntity>> NPC = ENTITY_TYPES.register("npc",
            () -> EntityType.Builder.of(NpcEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F) // Standard humanoid size
                    .clientTrackingRange(8)
                    .build("npc"));
}
