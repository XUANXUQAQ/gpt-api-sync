package github.gpt.api.sync.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import github.gpt.api.sync.model.gptload.GptLoadApiResponse;
import github.gpt.api.sync.model.gptload.GptLoadGroup;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
public class GptLoadService {

    private final Gson gson;
    private final String gptLoadBaseUrl;
    private final String authKey;

    public GptLoadService() {
        this.gson = new Gson();
        this.gptLoadBaseUrl = github.gpt.api.sync.config.AppConfig.GPT_LOAD_BASE_URL;
        this.authKey = github.gpt.api.sync.config.AppConfig.GPT_LOAD_AUTH_KEY;
        log.info("GptLoadService初始化完成, 基础URL: {}", gptLoadBaseUrl);
    }

    /**
     * 获取所有分组信息
     *
     * @return 分组列表
     * @throws IOException 当API调用失败时抛出异常
     */
    public List<GptLoadGroup> getAllGroups() throws IOException {
        String url = gptLoadBaseUrl + "/api/groups";
        log.info("正在从GPT-Load获取分组信息: {}", url);

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + authKey);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(github.gpt.api.sync.config.AppConfig.CONNECTION_TIMEOUT);
            connection.setReadTimeout(github.gpt.api.sync.config.AppConfig.READ_TIMEOUT);

            int responseCode = connection.getResponseCode();
            log.info("GPT-Load API响应码: {}", responseCode);

            if (responseCode != 200) {
                String errorMsg = "从GPT-Load获取分组失败. 响应码: " + responseCode;
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

            String responseBody = response.toString();
            log.debug("GPT-Load API响应: {}", responseBody);

            GptLoadApiResponse<List<GptLoadGroup>> apiResponse = gson.fromJson(
                    responseBody,
                    new TypeToken<GptLoadApiResponse<List<GptLoadGroup>>>() {
                    }.getType()
            );

            if (apiResponse == null) {
                throw new IOException("GPT-Load API返回空响应");
            }

            if (!apiResponse.isSuccess()) {
                String errorMsg = "GPT-Load API返回错误: " + (apiResponse.getMessage() != null ? apiResponse.getMessage() : "未知错误");
                log.error(errorMsg);
                throw new IOException(errorMsg);
            }

            List<GptLoadGroup> groups = apiResponse.getData();
            log.info("成功获取到 {} 个分组", groups != null ? groups.size() : 0);
            return groups;

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 测试与GPT-Load的连接
     *
     * @return 连接是否成功
     */
    public boolean testConnection() {
        try {
            String url = gptLoadBaseUrl + "/api/groups";
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + authKey);
            connection.setConnectTimeout(github.gpt.api.sync.config.AppConfig.CONNECTION_TIMEOUT);
            connection.setReadTimeout(github.gpt.api.sync.config.AppConfig.READ_TIMEOUT);

            int responseCode = connection.getResponseCode();
            boolean success = responseCode == 200;

            log.info("GPT-Load连接测试 - URL: {}, 响应码: {}, 结果: {}", url, responseCode, success ? "成功" : "失败");
            connection.disconnect();

            return success;
        } catch (Exception e) {
            log.error("GPT-Load连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取模拟数据，用于开发和测试
     *
     * @return GptLoadApiResponse对象
     */
    public GptLoadApiResponse getMockGroups() {
        log.warn("使用模拟数据，请确保这是在开发/测试环境");
        String mockData = getMockGptLoadData();
        return gson.fromJson(mockData, GptLoadApiResponse.class);
    }

    private String getMockGptLoadData() {
        return """
                {
                  "success": true,
                  "data": [
                    {
                      "id": 1,
                      "name": "openai-group",
                      "display_name": "OpenAI通用分组",
                      "description": "用于处理所有 OpenAI 模型的请求",
                      "channel_type": "openai",
                      "endpoint": "http://localhost:3001/proxy/openai-group",
                      "test_model": "gpt-4o-mini,gpt-4-turbo",
                      "upstreams": [
                        {
                          "url": "https://api.openai.com",
                          "weight": 1
                        }
                      ],
                      "api_keys": [
                        { "id": 101, "key_value": "sk-openai-key-1" },
                        { "id": 102, "key_value": "sk-openai-key-2" }
                      ]
                    },
                    {
                      "id": 2,
                      "name": "anthropic-claude",
                      "display_name": "Anthropic Claude 分组",
                      "description": "处理 Claude Sonnet 和 Opus 模型",
                      "channel_type": "anthropic",
                      "test_model": "claude-3-sonnet-20240229",
                      "upstreams": [
                        {
                          "url": "https://api.anthropic.com",
                          "weight": 1
                        }
                      ],
                      "api_keys": [
                        { "id": 201, "key_value": "sk-anthropic-key-1" }
                      ]
                    }
                  ]
                }
                """;
    }
}
