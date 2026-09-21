package com.alexlego19.ainpcs.init;

import com.alexlego19.ainpcs.AiNpcsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class CreativeTabInit {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AiNpcsMod.MODID);

    public static final RegistryObject<CreativeModeTab> AI_NPCS_TAB = CREATIVE_MODE_TABS.register("ai_npcs_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ai_npcs_tab"))
                    .icon(() -> new ItemStack(ItemInit.NPC_SPAWN_EGG.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ItemInit.NPC_SPAWN_EGG.get());
                    })
                    .build());
}
