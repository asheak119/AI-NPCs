package com.alexlego19.ainpcs.init;

import com.alexlego19.ainpcs.AiNpcsMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ItemInit {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AiNpcsMod.MODID);

    public static final RegistryObject<Item> NPC_SPAWN_EGG = ITEMS.register("npc_spawn_egg",
            () -> new ForgeSpawnEggItem(EntityInit.NPC, 0x00FF00, 0x0000FF, new Item.Properties()));
}
