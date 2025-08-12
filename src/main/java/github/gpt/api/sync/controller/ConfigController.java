package github.gpt.api.sync.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import github.gpt.api.sync.config.AppConfig;
import io.javalin.http.Context;
import lombok.extern.slf4j.Slf4j;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Slf4j
public class ConfigController {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * 处理获取当前配置的请求。
     * 出于安全考虑，会屏蔽敏感信息。
     */
    public void handleGetConfig(Context ctx) {
        // 创建配置数据的深拷贝以进行修改
        String currentConfigJson = gson.toJson(AppConfig.getConfigData());
        AppConfig.ConfigData safeConfig = gson.fromJson(currentConfigJson, AppConfig.ConfigData.class);

        // 屏蔽敏感信息
        if (safeConfig.getGptLoad() != null) {
            safeConfig.getGptLoad().setAuthKey("******");
        }
        if (safeConfig.getNewApi() != null) {
            safeConfig.getNewApi().setAccessToken("******");
        }

        ctx.json(safeConfig);
    }

    /**
     * 处理从文件重新加载配置的请求。
     */
    public void handleReloadConfig(Context ctx) {
        try {
            AppConfig.reloadConfig();
            ctx.json(Map.of("success", true, "message", "配置已成功从 config.json 重新加载。"));
        } catch (Exception e) {
            log.error("重新加载配置失败", e);
            ctx.status(500).json(Map.of("success", false, "error", "重新加载配置时发生错误: " + e.getMessage()));
        }
    }

    public boolean isConfigFileExist() {
        try {
            return Files.exists(Path.of(AppConfig.CONFIG_FILE));
        } catch (Exception e) {
            log.error("检查配置文件是否存在失败", e);
            return false;
        }
    }

    /**
     * 处理更新配置文件并重新加载的请求。
     */
    public void handleUpdateConfig(Context ctx) {
        try {
            // 获取当前配置以保留敏感字段
            AppConfig.ConfigData currentConfig = AppConfig.getConfigData();

            // 从请求体解析新配置
            String newConfigJson = ctx.body();
            AppConfig.ConfigData newConfigData = gson.fromJson(newConfigJson, AppConfig.ConfigData.class);

            // 检查并保留未更改的敏感字段
            if (newConfigData.getGptLoad() != null && "******".equals(newConfigData.getGptLoad().getAuthKey())) {
                newConfigData.getGptLoad().setAuthKey(currentConfig.getGptLoad().getAuthKey());
            }
            if (newConfigData.getNewApi() != null && "******".equals(newConfigData.getNewApi().getAccessToken())) {
                newConfigData.getNewApi().setAccessToken(currentConfig.getNewApi().getAccessToken());
            }

            // 将更新后的配置写回文件
            try (FileWriter writer = new FileWriter("config.json")) {
                gson.toJson(newConfigData, writer);
            }

            // 重新加载配置
            AppConfig.reloadConfig();

            log.info("配置文件 config.json 已被API更新并重新加载。");
            ctx.json(Map.of("success", true, "message", "配置已成功更新并重新加载。"));

        } catch (IOException e) {
            log.error("写入配置文件失败", e);
            ctx.status(500).json(Map.of("success", false, "error", "写入配置文件时发生IO错误: " + e.getMessage()));
        } catch (Exception e) {
            log.error("更新配置失败", e);
            ctx.status(500).json(Map.of("success", false, "error", "更新配置时发生未知错误: " + e.getMessage()));
        }
    }
}