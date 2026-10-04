import com.github.tartaricacid.simplebedrockmodel.client.bedrock.pojo.CubesItem;
import com.github.tartaricacid.touhoulittlemaid.client.resource.bedrock.GeckoContainerBuilder;
import com.github.tartaricacid.touhoulittlemaid.client.resource.pojo.CustomModelPack;
import com.github.tartaricacid.touhoulittlemaid.client.resource.pojo.MaidModelInfo;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.render.built.GeoBone;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.render.built.GeoModel;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.resource.GeckoContainer;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.resource.GeckoLibCache;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.Identifier;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 离线诊断：绕开需要 Minecraft 运行时的贴图注册，直接调用 GeckoContainerBuilder 注册酒狐 Gecko 容器，
 * 检查 GeckoLibCache 里是否真的有容器，以及几何体解析出的骨骼/立方体数量。
 */
public class GeoProbe {
    private static final Type PACK_TYPE = new TypeToken<CustomModelPack<MaidModelInfo>>() {
    }.getType();
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Identifier.class, new IdentifierAdapter())
            .registerTypeAdapter(CubesItem.class, new CubesItem.Deserializer())
            .create();

    private static String assetPath(Identifier id) {
        return "assets/%s/%s".formatted(id.getNamespace(), id.getPath());
    }

    public static void main(String[] args) throws Exception {
        Path root = Paths.get(args[0]);
        File[] packs = root.toFile().listFiles();
        int geckoTotal = 0, missingFile = 0, noContainer = 0, ok = 0, nullGeo = 0;
        long bones = 0, cubes = 0;
        List<String> problems = new ArrayList<>();

        for (File pack : packs) {
            File[] domains = new File(pack, "assets").listFiles();
            if (domains == null) {
                continue;
            }
            for (File domainDir : domains) {
                String domain = domainDir.getName();
                Path json = pack.toPath().resolve("assets").resolve(domain).resolve("maid_model.json");
                if (!Files.isRegularFile(json)) {
                    continue;
                }
                CustomModelPack<MaidModelInfo> mp;
                try (InputStream is = Files.newInputStream(json)) {
                    mp = GSON.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), PACK_TYPE);
                    mp.decorate(domain);
                } catch (Throwable t) {
                    problems.add("[pack-error] " + pack.getName() + "/" + domain + " -> " + t);
                    continue;
                }
                for (MaidModelInfo info : mp.getModelList()) {
                    if (!info.isGeckoModel()) {
                        continue;
                    }
                    geckoTotal++;
                    String id = info.getModelId().toString();
                    String modelFile = assetPath(info.getModel());
                    boolean exists = Files.isRegularFile(pack.toPath().resolve(modelFile));
                    if (!exists) {
                        missingFile++;
                        problems.add("[MISSING-GEO-FILE] " + id + " -> " + modelFile);
                        continue;
                    }
                    try {
                        GeckoContainerBuilder.registerModelContainer(info.getModelId(),
                                () -> Files.newInputStream(pack.toPath().resolve(modelFile)),
                                key -> null, List.of(), info.getTexture(), GeckoContainer.Type.MAID);
                    } catch (Throwable t) {
                        problems.add("[register-error] " + id + " -> " + t);
                        continue;
                    }
                    GeckoContainer c = GeckoLibCache.getInstance().getModels().get(info.getModelId());
                    if (c == null) {
                        noContainer++;
                        problems.add("[NO-CONTAINER] " + id + " -> registered nothing");
                        continue;
                    }
                    if (c.model() == null) {
                        nullGeo++;
                        problems.add("[NULL-GEO] " + id);
                        continue;
                    }
                    ok++;
                    GeoModel gm = c.model();
                    int b = gm.flatBoneList().size();
                    int cu = 0;
                    for (GeoBone bone : gm.flatBoneList()) {
                        if (bone.cubes() != null) {
                            cu += bone.cubes().getCubeCount();
                        }
                    }
                    bones += b;
                    cubes += cu;
                    if (ok <= 8) {
                        System.out.printf("[ok] %-50s bones=%-4d cubes=%-5d%n", id, b, cu);
                    }
                }
            }
        }

        System.out.println();
        System.out.println("gecko entries       : " + geckoTotal);
        System.out.println("missing geo file    : " + missingFile);
        System.out.println("no container        : " + noContainer);
        System.out.println("container null geo  : " + nullGeo);
        System.out.println("container ok        : " + ok);
        System.out.println("bones / cubes (ok)  : " + bones + " / " + cubes);
        System.out.println("cache size (total)  : " + GeckoLibCache.getInstance().getModels().size());
        System.out.println("problems            : " + problems.size());
        for (String p : problems) {
            System.out.println("   " + p);
        }
    }
}
