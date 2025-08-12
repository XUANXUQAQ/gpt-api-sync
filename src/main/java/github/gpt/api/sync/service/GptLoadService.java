package github.gpt.api.sync.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import github.gpt.api.sync.config.AppConfig;
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
        this.gptLoadBaseUrl = AppConfig.GPT_LOAD_BASE_URL;
        this.authKey = AppConfig.GPT_LOAD_AUTH_KEY;
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
            connection.setConnectTimeout(AppConfig.CONNECTION_TIMEOUT);
            connection.setReadTimeout(AppConfig.READ_TIMEOUT);

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

            if (apiResponse.getCode() != 0 || !"success".equalsIgnoreCase(apiResponse.getMessage())) {
                String errorMsg = "GPT-Load API返回错误: " + (apiResponse.getMessage() != null ? apiResponse.getMessage() : "未知错误 (code: " + apiResponse.getCode() + ")");
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
}
