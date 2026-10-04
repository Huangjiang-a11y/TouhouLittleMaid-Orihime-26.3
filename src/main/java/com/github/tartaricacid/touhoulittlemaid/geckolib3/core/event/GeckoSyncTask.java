package com.github.tartaricacid.touhoulittlemaid.geckolib3.core.event;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.GeckoRenderData;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Callable;

public class GeckoSyncTask<TData extends GeckoRenderData> extends GeckoUpdateTask<TData> {
    private TData result;

    public GeckoSyncTask(Callable<@Nullable TData> supplier) {
        super(supplier);
    }

    @Override
    public void start() {
        if (supplier != null) {
            try {
                result = supplier.call();
            } catch (Exception e) {
                // 原来只 printStackTrace，stderr 不会进 Minecraft/启动器日志，导致 Gecko 渲染失败时"零报错"
                TouhouLittleMaid.LOGGER.error("Gecko 同步渲染任务执行失败", e);
                e.printStackTrace();
            }
            supplier = null;
        }
    }

    @Override
    public @Nullable TData getResult() {
        start();
        return result;
    }
}