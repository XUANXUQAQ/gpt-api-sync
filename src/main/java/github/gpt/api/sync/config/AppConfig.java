package github.gpt.api.sync.config;

import com.google.gson.Gson;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
public class AppConfig {

    private static final String CONFIG_FILE = "config.json";
    private static final ConfigData configData;

    // GPT-Load配置
    public static final String GPT_LOAD_BASE_URL;
    public static final String GPT_LOAD_AUTH_KEY;

    // New-API配置
    public static final String NEW_API_BASE_URL;
    public static final String NEW_API_ACCESS_TOKEN;
    public static final String NEW_API_USER_ID;

    // 服务器配置
    public static final int SERVER_PORT;

    // 同步配置
    public static final int CONNECTION_TIMEOUT;
    public static final int READ_TIMEOUT;
    public static final boolean ENABLE_MOCK_MODE;

    // 日志配置
    public static final String LOG_LEVEL;

    static {
        configData = loadConfig();

        // 从加载的配置初始化常量
        GPT_LOAD_BASE_URL = getEnvOrDefault("GPT_LOAD_BASE_URL", configData.getGptLoad().getBaseUrl());
        GPT_LOAD_AUTH_KEY = getEnvOrDefault("GPT_LOAD_AUTH_KEY", configData.getGptLoad().getAuthKey());

        NEW_API_BASE_URL = getEnvOrDefault("NEW_API_BASE_URL", configData.getNewApi().getBaseUrl());
        NEW_API_ACCESS_TOKEN = getEnvOrDefault("NEW_API_ACCESS_TOKEN", configData.getNewApi().getAccessToken());
        NEW_API_USER_ID = getEnvOrDefault("NEW_API_USER_ID", configData.getNewApi().getUserId());

        SERVER_PORT = getIntEnv("SERVER_PORT", configData.getServer().getPort());

        CONNECTION_TIMEOUT = getIntEnv("CONNECTION_TIMEOUT", configData.getSync().getConnectionTimeout());
        READ_TIMEOUT = getIntEnv("READ_TIMEOUT", configData.getSync().getReadTimeout());
        ENABLE_MOCK_MODE = getBooleanEnv("ENABLE_MOCK_MODE", configData.getSync().isEnableMockMode());

        LOG_LEVEL = getEnvOrDefault("LOG_LEVEL", configData.getLog().getLevel());

        logConfiguration();
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
        log.info("DATABASE_PATH: {}", DATABASE_PATH);
        log.info("SERVER_PORT: {}", SERVER_PORT);
        log.info("CONNECTION_TIMEOUT: {}ms", CONNECTION_TIMEOUT);
        log.info("READ_TIMEOUT: {}ms", READ_TIMEOUT);
        log.info("ENABLE_MOCK_MODE: {}", ENABLE_MOCK_MODE);
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

    private static boolean getBooleanEnv(String name, boolean defaultValue) {
        String value = System.getenv(name);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim().equalsIgnoreCase("true");
        }
        return defaultValue;
    }

    // --- Nested classes to map JSON structure ---
    @Data
    private static class ConfigData {
        private GptLoad gptLoad = new GptLoad();
        private NewApi newApi = new NewApi();
        private Server server = new Server();
        private Sync sync = new Sync();
        private Log log = new Log();
    }

    @Data
    private static class GptLoad {
        private String baseUrl = "http://localhost:3001";
        private String authKey = "";
    }

    @Data
    private static class NewApi {
        private String baseUrl = "http://localhost:3000";
        private String accessToken = "";
        private String userId = "1";
    }

    @Data
    private static class Server {
        private int port = 7000;
    }

    @Data
    private static class Sync {
        private int connectionTimeout = 10000;
        private int readTimeout = 30000;
        private boolean enableMockMode = false;
    }

    @Data
    private static class Log {
        private String level = "INFO";
    }
}