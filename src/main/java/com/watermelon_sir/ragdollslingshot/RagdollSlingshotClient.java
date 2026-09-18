package com.watermelon_sir.ragdollslingshot;

import com.watermelon_sir.ragdollslingshot.item.custom.SlingshotItem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import static com.watermelon_sir.ragdollslingshot.item.custom.SlingshotItem.getEasedPower;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = RagdollSlingshotMod.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = RagdollSlingshotMod.MODID, value = Dist.CLIENT)
public class RagdollSlingshotClient {
    public RagdollSlingshotClient(ModContainer container) {

        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }



    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        event.enqueueWork(() -> { // ItemProperties#register is not threadsafe, so we need to call it on the main thread
            ItemProperties.register(
                    RagdollSlingshotMod.SLINGSHOT_ITEM.get(),
                    ResourceLocation.withDefaultNamespace("pulling"),
                    (stack, level, entity, i) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F
            );
            ItemProperties.register(RagdollSlingshotMod.SLINGSHOT_ITEM.get(), ResourceLocation.withDefaultNamespace("pull"), (itemStack, clientLevel, livingEntity, i) -> {
                if (livingEntity == null) {
                    return 0.0F;
                } else {
                    return livingEntity.getUseItem() != itemStack ? 0.0F : getEasedPower((itemStack.getUseDuration(livingEntity) - livingEntity.getUseItemRemainingTicks()) / (float)SlingshotItem.MAX_DRAW_TICKS);
                }
            });
        });
    }


}
