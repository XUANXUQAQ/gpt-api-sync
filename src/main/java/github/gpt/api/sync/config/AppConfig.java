package github.gpt.api.sync.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@Slf4j
public class AppConfig {

    private static final String DATA_PATH = "./data";
    public static final String CONFIG_FILE = DATA_PATH + "/config.json";
    @Getter
    private static ConfigData configData;

    // GPT-Load配置
    public static String GPT_LOAD_BASE_URL;
    public static String GPT_LOAD_AUTH_KEY;

    // New-API配置
    public static String NEW_API_BASE_URL;
    public static String NEW_API_ACCESS_TOKEN;
    public static String NEW_API_USER_ID;
    public static AuthHeaderType NEW_API_AUTH_HEADER_TYPE;

    // 服务器配置
    public static int SERVER_PORT;

    // 同步配置
    public static int CONNECTION_TIMEOUT;
    public static int READ_TIMEOUT;

    // 模型重定向配置
    public static List<String> STANDARD_MODELS;

    // 日志配置
    public static String LOG_LEVEL;

    public static final boolean isFirstStart;

    static {
        reloadConfig();
        isFirstStart = Files.notExists(Path.of(CONFIG_FILE));

        if (isFirstStart) {
            try {
                Files.createDirectories(Path.of(DATA_PATH));
            } catch (IOException e) {
                log.error("创建data文件夹失败", e);
            }
        }
        // 将更新后的配置写回文件
        var gson = new GsonBuilder().serializeNulls().create();
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            gson.toJson(configData, writer);
        } catch (IOException e) {
            log.error("写入配置文件失败", e);
        }
    }

    /**
     * 重新加载配置文件并更新所有配置字段。
     * 这是一个同步方法，以防止并发重载问题。
     */
    public static synchronized void reloadConfig() {
        log.info("正在重新加载配置...");
        configData = loadConfig();

        // 从加载的配置初始化字段
        GPT_LOAD_BASE_URL = getEnvOrDefault("GPT_LOAD_BASE_URL", configData.getGptLoad().getBaseUrl());
        GPT_LOAD_AUTH_KEY = getEnvOrDefault("GPT_LOAD_AUTH_KEY", configData.getGptLoad().getAuthKey());

        NEW_API_BASE_URL = getEnvOrDefault("NEW_API_BASE_URL", configData.getNewApi().getBaseUrl());
        NEW_API_ACCESS_TOKEN = getEnvOrDefault("NEW_API_ACCESS_TOKEN", configData.getNewApi().getAccessToken());
        NEW_API_USER_ID = getEnvOrDefault("NEW_API_USER_ID", configData.getNewApi().getUserId());
        String authHeaderTypeStr = getEnvOrDefault("NEW_API_AUTH_HEADER_TYPE", configData.getNewApi().getAuthType());
        try {
            NEW_API_AUTH_HEADER_TYPE = AuthHeaderType.valueOf(authHeaderTypeStr);
        } catch (IllegalArgumentException e) {
            log.warn("无效的 authHeaderType 值 '{}', 将使用默认值 'NEW_API'", authHeaderTypeStr);
            NEW_API_AUTH_HEADER_TYPE = AuthHeaderType.NEW_API;
        }

        SERVER_PORT = getIntEnv("SERVER_PORT", configData.getServer().getPort());

        CONNECTION_TIMEOUT = getIntEnv("CONNECTION_TIMEOUT", configData.getSync().getConnectionTimeout());
        READ_TIMEOUT = getIntEnv("READ_TIMEOUT", configData.getSync().getReadTimeout());

        List<String> defaultStandardModels = List.of(
                "gpt-4o",
                "gpt-4o-mini",
                "gpt-4.1-nano",
                "gpt-4.1-mini",
                "gpt-4.1",
                "claude-4-opus",
                "claude-4-sonnet",
                "claude-4-haiku",
                "claude-3.7-sonnet",
                "gemini-2.5-flash-lite",
                "gemini-2.5-flash",
                "gemini-2.5-pro"
        );
        if (configData.getModelRedirect() == null || configData.getModelRedirect().getStandardModels().isEmpty()) {
            ModelRedirect modelRedirect = new ModelRedirect();
            modelRedirect.setStandardModels(defaultStandardModels);
            configData.setModelRedirect(modelRedirect);
        }
        STANDARD_MODELS = configData.getModelRedirect().getStandardModels();

        LOG_LEVEL = getEnvOrDefault("LOG_LEVEL", configData.getLog().getLevel());

        logConfiguration();
        log.info("配置重新加载完成。");
    }

    private static ConfigData loadConfig() {
        try (FileReader reader = new FileReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
            log.info("从 {} 加载配置...", CONFIG_FILE);
            return new Gson().fromJson(reader, ConfigData.class);
        } catch (IOException e) {
            log.error("无法加载配置文件 '{}'. 请确保文件存在且格式正确。将使用默认配置。", CONFIG_FILE, e);
            return new ConfigData(); // 返回默认配置
        }
    }

    private static void logConfiguration() {
        log.info("==================== 应用配置 ====================");
        log.info("GPT_LOAD_BASE_URL: {}", GPT_LOAD_BASE_URL);
        log.info("GPT_LOAD_AUTH_KEY: {}", GPT_LOAD_AUTH_KEY.isEmpty() ? "未设置" : "已设置");
        log.info("NEW_API_BASE_URL: {}", NEW_API_BASE_URL);
        log.info("NEW_API_ACCESS_TOKEN: {}", NEW_API_ACCESS_TOKEN.isEmpty() ? "未设置" : "已设置");
        log.info("NEW_API_USER_ID: {}", NEW_API_USER_ID);
        log.info("NEW_API_AUTH_HEADER_TYPE: {}", NEW_API_AUTH_HEADER_TYPE.getHeaderName());
        log.info("SERVER_PORT: {}", SERVER_PORT);
        log.info("CONNECTION_TIMEOUT: {}ms", CONNECTION_TIMEOUT);
        log.info("READ_TIMEOUT: {}ms", READ_TIMEOUT);
        log.info("STANDARD_MODELS_COUNT: {}", STANDARD_MODELS.size());
        log.info("LOG_LEVEL: {}", LOG_LEVEL);
        log.info("==================================================");
    }

    private static String getEnvOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value != null && !value.trim().isEmpty()) ? value : defaultValue;
    }

    private static int getIntEnv(String name, int defaultValue) {
        String value = System.getenv(name);
        if (value != null && !value.trim().isEmpty()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.warn("无效的整数环境变量: {}={}, 使用默认值 {}", name, value, defaultValue);
            }
        }
        return defaultValue;
    }

    // --- Nested classes to map JSON structure ---
    @Data
    public static class ConfigData {
        private GptLoad gptLoad = new GptLoad();
        private NewApi newApi = new NewApi();
        private Server server = new Server();
        private Sync sync = new Sync();
        private Log log = new Log();
        private ModelRedirect modelRedirect = new ModelRedirect();
    }

    @Data
    public static class GptLoad {
        private String baseUrl = "http://localhost:3001";
        private String authKey = "";
    }

    @Data
    public static class NewApi {
        private String baseUrl = "http://localhost:3000";
        private String accessToken = "";
        private String userId = "1";
        private String authType = AuthHeaderType.NEW_API.name();
    }

    @Data
    public static class Server {
        private int port = 7000;
    }

    @Data
    public static class Sync {
        private int connectionTimeout = 10000;
        private int readTimeout = 30000;
    }

    @Data
    public static class Log {
        private String level = "INFO";
    }

    @Data
    public static class ModelRedirect {
        private List<String> standardModels = Collections.emptyList();
    }
}