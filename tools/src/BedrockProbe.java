import com.github.tartaricacid.simplebedrockmodel.client.bedrock.pojo.BedrockModelPOJO;
import com.github.tartaricacid.simplebedrockmodel.client.bedrock.pojo.BedrockVersion;
import com.github.tartaricacid.simplebedrockmodel.client.bedrock.pojo.CubesItem;
import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.EntityMaidModel;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 离线验证：Gecko 的 .geo.json 能否被基岩解析器读成 EntityMaidModel。
 * 只依赖基岩库与 EntityMaidModel，绕开 mod 加载器静态初始化（避免 FabricLoader 提前调用）。
 */
public class BedrockProbe {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Identifier.class, new IdentifierAdapter())
            .registerTypeAdapter(CubesItem.class, new CubesItem.Deserializer())
            .create();

    public static void main(String[] args) throws Exception {
        Path root = Paths.get(args[0]);
        int total = 0, ok = 0, fail = 0;
        for (File pack : root.toFile().listFiles()) {
            File[] domains = new File(pack, "assets").listFiles();
            if (domains == null) {
                continue;
            }
            for (File domainDir : domains) {
                Path json = pack.toPath().resolve("assets").resolve(domainDir.getName()).resolve("maid_model.json");
                if (!Files.isRegularFile(json)) {
                    continue;
                }
                JsonObject obj;
                try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(json), StandardCharsets.UTF_8)) {
                    obj = GSON.fromJson(reader, JsonObject.class);
                } catch (Throwable t) {
                    continue;
                }
                JsonArray list = obj.getAsJsonArray("model_list");
                if (list == null) {
                    continue;
                }
                for (JsonElement el : list) {
                    JsonObject entry = el.getAsJsonObject();
                    if (entry.get("is_gecko") == null || !entry.get("is_gecko").getAsBoolean()) {
                        continue;
                    }
                    total++;
                    // 复刻 MaidModelInfo.decorate() 的默认推导：没有 model 字段时按 model_id 推 models/entity/<path>.json
                    JsonElement modelEl = entry.get("model");
                    String model;
                    if (modelEl == null || modelEl.isJsonNull()) {
                        String rawId = entry.get("model_id").getAsString();
                        String[] idSplit = rawId.split(":", 2);
                        model = idSplit[0] + ":models/entity/" + idSplit[1] + ".json";
                    } else {
                        model = modelEl.getAsString();
                    }
                    String[] split = model.split(":", 2);
                    Path modelFile = pack.toPath().resolve("assets").resolve(split[0]).resolve(split[1]);
                    if (!Files.isRegularFile(modelFile)) {
                        fail++;
                        System.out.println("[missing] " + model);
                        continue;
                    }
                    try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(modelFile), StandardCharsets.UTF_8)) {
                        BedrockModelPOJO pojo = GSON.fromJson(reader, BedrockModelPOJO.class);
                        BedrockVersion version = BedrockVersion.isNewVersion(pojo) ? BedrockVersion.NEW : BedrockVersion.LEGACY;
                        EntityMaidModel maidModel = new EntityMaidModel(pojo, version);
                        ok++;
                        if (ok <= 8) {
                            System.out.printf("[bedrock-ok] %-44s version=%-6s parts=%d%n",
                                    model, version, maidModel.getModelMap().size());
                        }
                    } catch (Throwable t) {
                        fail++;
                        System.out.println("[throw] " + model + " -> " + t);
                    }
                }
            }
        }
        System.out.println();
        System.out.println("gecko entries      : " + total);
        System.out.println("bedrock parsed ok  : " + ok);
        System.out.println("bedrock failed     : " + fail);
    }
}
