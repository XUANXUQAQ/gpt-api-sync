package github.gpt.api.sync;

import com.google.gson.GsonBuilder;
import github.gpt.api.sync.config.AppConfig;
import github.gpt.api.sync.controller.ApiController;
import github.gpt.api.sync.controller.SyncController;
import github.gpt.api.sync.db.DatabaseService;
import github.gpt.api.sync.service.GptLoadService;
import github.gpt.api.sync.service.NewApiService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinGson;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class Main {

    private static DatabaseService databaseService;
    private static GptLoadService gptLoadService;
    private static NewApiService newApiService;

    public static void main(String[] args) {
        log.info("GPT-API同步服务启动中...");

        try {
            // 初始化服务
            initializeServices();

            // 设置Web服务器
            Javalin app = setupWebServer();

            // 获取端口配置
            int port = AppConfig.SERVER_PORT;

            // 启动服务器
            app.start(port);

            log.info("GPT-API同步服务启动成功!");
            log.info("服务端点:");
            log.info("  - 同步: http://localhost:{}/sync", port);
            log.info("  - 状态: http://localhost:{}/status", port);
            log.info("  - 健康检查: http://localhost:{}/health", port);

        } catch (Exception e) {
            log.error("服务启动失败: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    /**
     * 初始化所有服务
     */
    private static void initializeServices() {
        log.info("正在初始化服务组件...");

        // 初始化数据库服务
        databaseService = new DatabaseService();
        log.info("数据库服务初始化完成");

        // 初始化数据库表结构
        databaseService.initDatabase();

        // 初始化GPT-Load服务
        gptLoadService = new GptLoadService();
        log.info("GPT-Load服务初始化完成");

        // 初始化New-API服务
        newApiService = new NewApiService();
        log.info("New-API服务初始化完成");

        // 测试服务连接
        testServicesConnection();

        log.info("所有服务组件初始化完成");
    }

    /**
     * 设置Web服务器和路由
     */
    private static Javalin setupWebServer() {
        SyncController syncController = new SyncController(gptLoadService, databaseService, newApiService);
        ApiController apiController = new ApiController(gptLoadService, newApiService);

        return Javalin.create(config -> {
                    config.showJavalinBanner = false;
                    config.http.defaultContentType = "application/json; charset=utf-8";
                    config.jsonMapper(new JavalinGson(new GsonBuilder().serializeNulls().create(), true));
                })
                .get("/", ctx -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("service", "GPT-API同步服务");
                    response.put("version", "1.0.0");
                    response.put("status", "running");
                    response.put("endpoints", Map.of(
                            "sync", "/sync - 触发同步操作",
                            "status", "/status - 查看服务状态",
                            "health", "/health - 健康检查",
                            "getGptLoadInfo", "/api/gpt-load - 获取gpt-load信息",
                            "getNewApiInfo", "/api/new-api - 获取new-api信息"
                    ));
                    ctx.json(response);
                })
                .get("/sync", syncController::syncChannels)
                .get("/status", Main::handleStatusRequest)
                .get("/health", Main::handleHealthCheck)
                .get("/api/gpt-load", apiController::getGptLoadInfo)
                .get("/api/new-api", apiController::getNewApiInfo)
                .exception(Exception.class, (e, ctx) -> {
                    log.error("请求处理出现异常: {}", e.getMessage(), e);
                    ctx.status(500).json(Map.of(
                            "success", false,
                            "error", "内部服务器错误: " + e.getMessage(),
                            "timestamp", System.currentTimeMillis()
                    ));
                });
    }

    /**
     * 处理状态请求
     */
    private static void handleStatusRequest(Context ctx) {
        Map<String, Object> status = new HashMap<>();

        // 系统状态
        status.put("service", "GPT-API同步服务");
        status.put("version", "1.0.0");

        // 服务连接状态
        Map<String, Object> connections = new HashMap<>();
        connections.put("database", databaseService.testConnection());
        connections.put("gptLoad", gptLoadService.testConnection());
        connections.put("newApi", newApiService.testConnection());
        status.put("connections", connections);

        // 环境配置
        Map<String, String> config = new HashMap<>();
        config.put("gptLoadUrl", AppConfig.GPT_LOAD_BASE_URL);
        config.put("newApiUrl", AppConfig.NEW_API_BASE_URL);
        config.put("databasePath", AppConfig.DATABASE_PATH);
        status.put("config", config);

        ctx.json(status);
    }

    /**
     * 处理健康检查请求
     */
    private static void handleHealthCheck(Context ctx) {
        boolean isHealthy = true;
        Map<String, Object> health = new HashMap<>();

        // 检查数据库连接
        boolean dbHealthy = databaseService.testConnection();
        health.put("database", dbHealthy ? "ok" : "error");
        if (!dbHealthy) isHealthy = false;

        // 检查GPT-Load连接
        boolean gptLoadHealthy = gptLoadService.testConnection();
        health.put("gptLoad", gptLoadHealthy ? "ok" : "error");
        if (!gptLoadHealthy) isHealthy = false;

        health.put("status", isHealthy ? "healthy" : "unhealthy");
        health.put("timestamp", System.currentTimeMillis());

        ctx.status(isHealthy ? 200 : 503).json(health);
    }

    /**
     * 测试服务连接
     */
    private static void testServicesConnection() {
        log.info("正在测试服务连接...");

        // 测试数据库连接
        if (databaseService.testConnection()) {
            log.info("✓ 数据库连接正常");
        } else {
            log.warn("✗ 数据库连接失败");
        }

        // 测试GPT-Load连接
        if (gptLoadService.testConnection()) {
            log.info("✓ GPT-Load服务连接正常");
        } else {
            log.warn("✗ GPT-Load服务连接失败，请检查配置和服务状态");
        }

        // 测试New-API连接
        if (newApiService.testConnection()) {
            log.info("✓ New-API服务连接正常");
        } else {
            log.warn("✗ New-API服务连接失败，请检查配置和服务状态");
        }
    }
}
