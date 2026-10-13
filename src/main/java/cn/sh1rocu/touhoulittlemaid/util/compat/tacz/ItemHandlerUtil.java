package cn.sh1rocu.touhoulittlemaid.util.compat.tacz;

import cn.sh1rocu.touhoulittlemaid.util.transfer.ItemUtil;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.item.ItemStack;

/**
 * 从女仆背包里取弹药（26.3 版：背包抽象已从 IItemHandler 换成 Fabric 传输 API 的 SlottedStorage）。
 */
public class ItemHandlerUtil {
    public static int findAndExtractInventoryAmmo(SlottedStorage<ItemVariant> storage, ItemStack gunItem, int needAmmoCount) {
        int cnt = needAmmoCount;
        for (int i = 0; i < storage.getSlotCount(); i++) {
            ItemStack checkAmmoStack = ItemUtil.getStack(storage, i);
            if (checkAmmoStack.isEmpty()) {
                continue;
            }
            if (checkAmmoStack.getItem() instanceof IAmmo iAmmo && iAmmo.isAmmoOfGun(gunItem, checkAmmoStack)) {
                int extracted;
                try (Transaction transaction = Transaction.openOuter()) {
                    extracted = (int) storage.getSlot(i).extract(ItemVariant.of(checkAmmoStack), cnt, transaction);
                    transaction.commit();
                }
                cnt -= extracted;
                if (cnt <= 0) {
                    break;
                }
            }
            if (checkAmmoStack.getItem() instanceof IAmmoBox iAmmoBox && iAmmoBox.isAmmoBoxOfGun(gunItem, checkAmmoStack)) {
                int boxAmmoCount = iAmmoBox.getAmmoCount(checkAmmoStack);
                int extractCount = Math.min(boxAmmoCount, cnt);
                int remainCount = boxAmmoCount - extractCount;
                iAmmoBox.setAmmoCount(checkAmmoStack, remainCount);
                if (remainCount <= 0) {
                    iAmmoBox.setAmmoId(checkAmmoStack, DefaultAssets.EMPTY_AMMO_ID);
                }
                cnt -= extractCount;
                if (cnt <= 0) {
                    break;
                }
            }
        }
        return needAmmoCount - cnt;
    }
}
