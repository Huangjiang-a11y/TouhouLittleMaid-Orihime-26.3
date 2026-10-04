package net.fabricmc.loader.api;

import java.nio.file.Path;
import java.nio.file.Paths;

/** 离线诊断用最小桩：只提供探针路径会执行到的方法 */
public interface FabricLoader {
    static FabricLoader getInstance() {
        return Impl.INSTANCE;
    }

    Path getGameDir();

    boolean isDevelopmentEnvironment();

    boolean isModLoaded(String id);

    class Impl implements FabricLoader {
        static final FabricLoader INSTANCE = new Impl();

        @Override
        public Path getGameDir() {
            return Paths.get(System.getProperty("probe.gameDir", "."));
        }

        @Override
        public boolean isDevelopmentEnvironment() {
            return false;
        }

        @Override
        public boolean isModLoaded(String id) {
            return false;
        }
    }
}
