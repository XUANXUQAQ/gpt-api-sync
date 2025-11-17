package github.gpt.api.sync.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import github.gpt.api.sync.config.AppConfig;
import github.gpt.api.sync.config.AuthHeaderType;
import github.gpt.api.sync.model.newapi.NewApiChannel;
import github.gpt.api.sync.model.newapi.NewApiChannelResponseWrapper;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class NewApiService {

    private final Gson gson;

    public NewApiService() {
        this.gson = new Gson();
        log.info("NewApiService初始化完成");
    }

    /**
     * 测试与New-API的连接
     *
     * @return 连接是否成功
     */
    public boolean testConnection() {
        try {
            String url = AppConfig.NEW_API_BASE_URL + "/api/status";
            HttpURLConnection connection = (HttpURLConnection) new URI(url).toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(AppConfig.CONNECTION_TIMEOUT);
            connection.setReadTimeout(AppConfig.READ_TIMEOUT);

            int responseCode = connection.getResponseCode();
            boolean success = responseCode == 200;

            log.info("New-API连接测试 - URL: {}, 响应码: {}, 结果: {}", url, responseCode, success ? "成功" : "失败");
            connection.disconnect();

            return success;
        } catch (Exception e) {
            log.error("New-API连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 创建渠道
     *
     * @param channel 渠道配置
     * @return 是否创建成功
     */
    public boolean createChannel(NewApiChannel channel) {
        if (channel == null) {
            log.warn("尝试创建空的渠道配置");
            return false;
        }

        try {
            String url = AppConfig.NEW_API_BASE_URL + "/api/channel/";

            // 根据 AuthHeaderType 构建不同的请求体
            String jsonBody;
            if (AppConfig.NEW_API_AUTH_HEADER_TYPE == AuthHeaderType.VELOERA) {
                // Veloera 格式: 直接发送 channel 数据
                jsonBody = gson.toJson(buildChannelData(channel));
                log.debug("正在创建渠道 (Veloera): {} - {}", channel.getName(), jsonBody);
            } else {
                // New-API 格式: 需要 mode 和 channel 包装
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("mode", "single");
                requestBody.put("channel", buildChannelData(channel));
                jsonBody = gson.toJson(requestBody);
                log.debug("正在创建渠道 (New-API): {} - {}", channel.getName(), jsonBody);
            }

            HttpURLConnection connection = (HttpURLConnection) new URI(url).toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            if (AppConfig.NEW_API_ACCESS_TOKEN != null && !AppConfig.NEW_API_ACCESS_TOKEN.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + AppConfig.NEW_API_ACCESS_TOKEN);
                connection.setRequestProperty(AppConfig.NEW_API_AUTH_HEADER_TYPE.getHeaderName(), AppConfig.NEW_API_USER_ID);
            }
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();

            if (responseCode == 200 || responseCode == 201) {
                log.info("成功创建渠道: {}", channel.getName());
                return true;
            } else {
                String errorMsg = readErrorResponse(connection);
                log.error("创建渠道失败 - 渠道: {}, 响应码: {}, 错误: {}",
                        channel.getName(), responseCode, errorMsg);
                return false;
            }

        } catch (IOException | URISyntaxException e) {
            log.error("创建渠道时发生IO异常 - 渠道: {}, 错误: {}", channel.getName(), e.getMessage());
            return false;
        }
    }

    /**
     * 更新渠道
     *
     * @param channel 渠道配置
     * @return 是否更新成功
     */
    public boolean updateChannel(NewApiChannel channel) {
        if (channel == null) {
            log.warn("尝试更新空的渠道配置");
            return false;
        }

        try {
            String url = AppConfig.NEW_API_BASE_URL + "/api/channel/";

            Map<String, Object> channelData = buildChannelData(channel);
            String jsonBody = gson.toJson(channelData);

            log.debug("正在更新渠道: {} - {}", channel.getName(), jsonBody);

            HttpURLConnection connection = (HttpURLConnection) new URI(url).toURL().openConnection();
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/json");
            if (AppConfig.NEW_API_ACCESS_TOKEN != null && !AppConfig.NEW_API_ACCESS_TOKEN.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + AppConfig.NEW_API_ACCESS_TOKEN);
                connection.setRequestProperty(AppConfig.NEW_API_AUTH_HEADER_TYPE.getHeaderName(), AppConfig.NEW_API_USER_ID);
            }
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                log.info("成功更新渠道: {}", channel.getName());
                return true;
            } else {
                String errorMsg = readErrorResponse(connection);
                log.error("更新渠道失败 - 渠道: {}, 响应码: {}, 错误: {}",
                        channel.getName(), responseCode, errorMsg);
                return false;
            }

        } catch (IOException | URISyntaxException e) {
            log.error("更新渠道时发生IO异常 - 渠道: {}, 错误: {}", channel.getName(), e.getMessage());
            return false;
        }
    }


    /**
     * 为指定渠道获取可用模型列表
     *
     * @param channelId 渠道ID
     * @return 模型名称列表
     * @throws IOException 当API调用失败时抛出异常
     */
    public List<String> fetchModelsForChannel(int channelId) throws IOException, URISyntaxException {
        String url = AppConfig.NEW_API_BASE_URL + "/api/channel/fetch_models/" + channelId;
        log.info("正在为渠道ID {} 获取模型列表: {}", channelId, url);

        HttpURLConnection connection = (HttpURLConnection) new URI(url).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + AppConfig.NEW_API_ACCESS_TOKEN);
        connection.setRequestProperty(AppConfig.NEW_API_AUTH_HEADER_TYPE.getHeaderName(), AppConfig.NEW_API_USER_ID);
        connection.setConnectTimeout(AppConfig.CONNECTION_TIMEOUT);
        connection.setReadTimeout(AppConfig.READ_TIMEOUT);

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            String errorMsg = "为渠道 " + channelId + " 获取模型列表失败. 响应码: " + responseCode;
            log.error(errorMsg);
            throw new IOException(errorMsg);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        // 假设响应体是 {"data": ["model1", "model2"], ...}
        Map<String, Object> apiResponse = gson.fromJson(response.toString(), new TypeToken<Map<String, Object>>() {
        }.getType());

        if (apiResponse == null || !apiResponse.containsKey("data") || !(apiResponse.get("data") instanceof List)) {
            throw new IOException("从 fetch_models API 返回的响应格式无效");
        }

        @SuppressWarnings("unchecked")
        List<String> models = (List<String>) apiResponse.get("data");
        log.info("成功为渠道ID {} 获取到 {} 个模型", channelId, models.size());
        return models;
    }

    /**
     * 获取所有渠道信息
     *
     * @return 渠道列表
     * @throws IOException 当API调用失败时抛出异常
     */
    public List<NewApiChannel> getAllChannels() throws IOException, URISyntaxException {
        String url = AppConfig.NEW_API_BASE_URL + "/api/channel/?page=1&page_size=100000";
        log.info("正在从 {} 获取渠道信息: {}", AppConfig.NEW_API_AUTH_HEADER_TYPE.name(), url);

        HttpURLConnection connection = (HttpURLConnection) new URI(url).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + AppConfig.NEW_API_ACCESS_TOKEN);
        connection.setRequestProperty(AppConfig.NEW_API_AUTH_HEADER_TYPE.getHeaderName(), AppConfig.NEW_API_USER_ID);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(AppConfig.CONNECTION_TIMEOUT);
        connection.setReadTimeout(AppConfig.READ_TIMEOUT);

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            String errorMsg = "从 " + AppConfig.NEW_API_AUTH_HEADER_TYPE.name() + " 获取渠道失败. 响应码: " + responseCode;
            log.error(errorMsg);
            throw new IOException(errorMsg);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        List<NewApiChannel> channels;

        // 根据 AuthHeaderType 判断响应格式
        if (AppConfig.NEW_API_AUTH_HEADER_TYPE == AuthHeaderType.VELOERA) {
            // Veloera 格式: {"data": [...], "message": "", "success": true}
            log.debug("使用 Veloera 响应格式解析");
            Map<String, Object> apiResponse = gson.fromJson(response.toString(), new TypeToken<Map<String, Object>>() {
            }.getType());

            if (apiResponse == null || !apiResponse.containsKey("data")) {
                throw new IOException("Veloera 返回无效响应或缺少 data 字段");
            }

            // data 字段直接是 List<NewApiChannel>
            String dataJson = gson.toJson(apiResponse.get("data"));
            channels = gson.fromJson(dataJson, new TypeToken<List<NewApiChannel>>() {
            }.getType());
        } else {
            // New-API 格式: {"data": {"items": [...], "page": 1, ...}}
            log.debug("使用 New-API 响应格式解析");
            NewApiChannelResponseWrapper apiResponse = gson.fromJson(response.toString(), NewApiChannelResponseWrapper.class);

            if (apiResponse == null || apiResponse.getData() == null) {
                throw new IOException("New-API 返回无效响应或空的 items 列表");
            }

            channels = apiResponse.getData().getItems();
        }

        log.info("成功获取到 {} 个渠道", channels != null ? channels.size() : 0);
        return channels;
    }

    /**
     * 构建New-API渠道数据
     */
    private Map<String, Object> buildChannelData(NewApiChannel channel) {
        Map<String, Object> data = new HashMap<>();
        boolean isVeloera = AppConfig.NEW_API_AUTH_HEADER_TYPE == AuthHeaderType.VELOERA;

        // 基本信息 - 始终包含
        data.put("type", channel.getType());
        data.put("name", channel.getName() != null ? channel.getName() : "");
        data.put("key", channel.getKey() != null ? channel.getKey() : "");
        data.put("models", channel.getModels() != null ? channel.getModels() : "");
        data.put("base_url", channel.getBaseUrl() != null ? channel.getBaseUrl() : "");
        data.put("auto_ban", channel.getAutoBan());

        // Veloera 需要 groups 数组，New-API 需要 group 字符串
        if (isVeloera) {
            // Veloera 使用 groups 数组
            String groupName = channel.getGroupName() != null ? channel.getGroupName() : "default";
            data.put("groups", new String[]{groupName});
        } else {
            // New-API 使用 group 字符串
            if (channel.getGroupName() != null) {
                data.put("group", channel.getGroupName());
            }
        }

        // ID 字段 - 仅在更新时需要
        if (channel.getId() > 0) {
            data.put("id", channel.getId());
        }

        // 其他数值字段
        data.put("status", channel.getStatus());
        data.put("priority", channel.getPriority());
        data.put("weight", channel.getWeight());

        // 可选字符串字段 - Veloera 需要空字符串，New-API 可以省略
        if (isVeloera) {
            data.put("openai_organization", channel.getOpenaiOrganization() != null ? channel.getOpenaiOrganization() : "");
            data.put("test_model", channel.getTestModel() != null ? channel.getTestModel() : "");
            data.put("other", channel.getOther() != null ? channel.getOther() : "");
            data.put("model_mapping", channel.getModelMapping() != null ? channel.getModelMapping() : "");
            data.put("status_code_mapping", channel.getStatusCodeMapping() != null ? channel.getStatusCodeMapping() : "");
            data.put("other_info", channel.getOtherInfo() != null ? channel.getOtherInfo() : "");
            data.put("tag", channel.getTag() != null ? channel.getTag() : "");
            data.put("setting", channel.getSetting() != null ? channel.getSetting() : "");
            data.put("param_override", ""); // Veloera 特有字段
            data.put("system_prompt", ""); // Veloera 特有字段
            data.put("model_prefix", ""); // Veloera 特有字段

            // Veloera 的 max_input_tokens 字段
            data.put("max_input_tokens", 0);
        } else {
            // New-API 格式 - 仅在非空时添加
            if (channel.getOpenaiOrganization() != null) {
                data.put("openai_organization", channel.getOpenaiOrganization());
            }
            if (channel.getTestModel() != null) {
                data.put("test_model", channel.getTestModel());
            }
            if (channel.getOther() != null) {
                data.put("other", channel.getOther());
            }
            if (channel.getModelMapping() != null) {
                data.put("model_mapping", channel.getModelMapping());
            }
            if (channel.getStatusCodeMapping() != null) {
                data.put("status_code_mapping", channel.getStatusCodeMapping());
            }
            if (channel.getOtherInfo() != null) {
                data.put("other_info", channel.getOtherInfo());
            }
            if (channel.getSettings() != null) {
                data.put("settings", channel.getSettings());
            }
            if (channel.getTag() != null) {
                data.put("tag", channel.getTag());
            }
            if (channel.getSetting() != null) {
                data.put("setting", channel.getSetting());
            }
            if (channel.getChannelInfo() != null) {
                data.put("channel_info", channel.getChannelInfo());
            }
        }

        return data;
    }

    /**
     * 读取错误响应
     */
    private String readErrorResponse(HttpURLConnection connection) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                connection.getErrorStream() != null ? connection.getErrorStream() : connection.getInputStream(),
                StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        } catch (Exception e) {
            return "无法读取错误响应: " + e.getMessage();
        }
    }
}