package com.watermelon_sir.ragdollslingshot.item.custom;

import dev.leo.sableplayerragdoll.api.RagdollAPI;
import dev.leo.sableplayerragdoll.api.RagdollLaunchOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import static java.lang.Math.pow;

public class SlingshotItem extends Item {

    public static int MAX_DRAW_TICKS = 20*3;
    static float HIGHEST_FLING_LEVEL = 6;
    static float LOWEST_FLING_LEVEL = 0.5f;
    static float FLING_BASELINE = LOWEST_FLING_LEVEL/HIGHEST_FLING_LEVEL;

    private boolean startSoundPlayed = false;
    private boolean midLoadSoundPlayed = false;

    public SlingshotItem(Properties properties) {
        super(properties);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entityLiving, int timeLeft) {
        if (entityLiving instanceof Player player) {

            if (player.onGround()) {
                int charge = this.getUseDuration(stack, entityLiving) - timeLeft;
                charge = net.neoforged.neoforge.event.EventHooks.onArrowLoose(stack, level, player, charge, player.onGround());
                if (charge < 0) return;
                float chargeProgress = Math.min(((float)charge), MAX_DRAW_TICKS)/MAX_DRAW_TICKS;

                EquipmentSlot equipmentslot = stack.equals(player.getItemBySlot(EquipmentSlot.OFFHAND)) ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;

                stack.hurtAndBreak(1+(int)Math.floor(
                        getEasedPower(chargeProgress)*6
                ), entityLiving, equipmentslot);

                if (level instanceof ServerLevel serverlevel && player instanceof ServerPlayer serverPlayer) {
                    Vec3 dir = serverPlayer.getLookAngle();
                    Vec3 velocity = dir.normalize().scale((((1-(FLING_BASELINE))*getEasedPower(chargeProgress)+(FLING_BASELINE))*HIGHEST_FLING_LEVEL) *15d);
                    RagdollLaunchOptions options = RagdollLaunchOptions.builder().autoSeat(true).lockDismount(false).build();
                    RagdollAPI.launch(serverPlayer,velocity,options);

                    serverlevel.playSound(
                            null,
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            SoundEvents.ARROW_SHOOT,
                            SoundSource.PLAYERS,
                            0.8F*getEasedPower(chargeProgress),
                            0.5f+getEasedPower(chargeProgress)
                    );
                    serverlevel.playSound(
                            null,
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            SoundEvents.BREEZE_LAND,
                            SoundSource.PLAYERS,
                            0.7f*getEasedPower(chargeProgress),
                            0.9f
                    );
                }

                player.awardStat(Stats.ITEM_USED.get(this));
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        boolean flag = player.onGround();

        InteractionResultHolder<ItemStack> ret = net.neoforged.neoforge.event.EventHooks.onArrowNock(itemstack, level, player, hand, flag);
        if (ret != null) return ret;

        if (!flag) {
            return InteractionResultHolder.fail(itemstack);
        } else {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(itemstack);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    public static float getEasedPower(float input) {
        //return (float) Math.pow(input,2.5); //Somewhere between a square and a cube

        //return (float) ((-1/((0.5*input)-1))-1); //Hyperbola

        return input < 0.5 ? 4 * input * input * input : (float) (1 - pow(-2 * input + 2, 3) / 2);
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int count) {
        if (!level.isClientSide) {
            float f = (float)(stack.getUseDuration(livingEntity) - count) / MAX_DRAW_TICKS;
            if (f < 0.1F) {
                this.startSoundPlayed = false;
                this.midLoadSoundPlayed = false;
            }

            if (f >= 0.1F && !this.startSoundPlayed) {
                this.startSoundPlayed = true;
                level.playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), SoundEvents.CROSSBOW_LOADING_START, SoundSource.PLAYERS, 0.5F, 0.7F);
            }

            if (f >= 0.4F && !this.midLoadSoundPlayed) {
                this.midLoadSoundPlayed = true;
                level.playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), SoundEvents.CROSSBOW_LOADING_MIDDLE, SoundSource.PLAYERS, 0.5F, 0.7F);
            }
        }
    }
}
