package com.github.tartaricacid.touhoulittlemaid.entity.task.meal;

import com.github.tartaricacid.touhoulittlemaid.api.task.meal.IMaidMeal;
import com.github.tartaricacid.touhoulittlemaid.api.task.meal.MaidMealType;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.favorability.Type;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.event.MaidMealRegConfigEvent;
import com.github.tartaricacid.touhoulittlemaid.network.NetworkHandler;
import com.github.tartaricacid.touhoulittlemaid.network.message.SpawnParticlePackage;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

public class DefaultMaidWorkMeal implements IMaidMeal {
    public static boolean isWorkMeal(ItemStack stack) {
        return stack.has(DataComponents.FOOD)
                && !IMaidMeal.isBlockList(stack, MaidConfig.MAID_WORK_MEALS_BLOCK_LIST.get())
                && !IMaidMeal.isBlockList(stack, MaidMealRegConfigEvent.WORK_MEAL_REGEX);
    }

    @Override
    public boolean canMaidEat(EntityMaid maid, ItemStack stack, InteractionHand hand) {
        return isWorkMeal(stack);
    }

    @Override
    public void onMaidEat(EntityMaid maid, ItemStack stack, InteractionHand hand) {
        FoodProperties foodProperties = stack.get(DataComponents.FOOD);
        if (foodProperties != null) {
            // 调用饰品
            maid.getMaidBauble().fireEvent((b, s) -> {
                b.onMaidEat(maid, s, stack, MaidMealType.WORK_MEAL);
                return false;
            });

            maid.startUsingItem(hand);
            int nutrition = foodProperties.nutrition();
            float saturationModifier = foodProperties.saturation();
            float total = nutrition + nutrition * saturationModifier * 2;
            // 原版的熟牛肉之类的一般在 20 左右（除了迷之炖菜为 34.2）
            // 修正：原 `nextInt(100) < total ? 0 : 1` 三元写反，实际只有 (100-total)% 概率给分，
            // 高饱食度食物（total≈92）几乎拿不到好感度；故固定 100% 给 1 点。
            int point = 1;
            maid.getFavorabilityManager().apply(Type.WORK_MEAL, point);
            if (point == 1) {
                NetworkHandler.sendToNearby(maid, new SpawnParticlePackage(maid.getId(), SpawnParticlePackage.Type.HEART, stack.getUseDuration(maid)));
            }
        }
    }
}
