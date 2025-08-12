package github.gpt.api.sync.service;

import com.google.gson.Gson;
import github.gpt.api.sync.model.newapi.NewApiChannel;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class NewApiService {
    
    private final Gson gson;
    private final String newApiBaseUrl;
    private final String accessToken;
    
    public NewApiService() {
        this.gson = new Gson();
        this.newApiBaseUrl = github.gpt.api.sync.config.AppConfig.NEW_API_BASE_URL;
        this.accessToken = github.gpt.api.sync.config.AppConfig.NEW_API_ACCESS_TOKEN;
        log.info("NewApiService初始化完成, 基础URL: {}", newApiBaseUrl);
    }
    
    /**
     * 测试与New-API的连接
     * @return 连接是否成功
     */
    public boolean testConnection() {
        try {
            String url = newApiBaseUrl + "/api/status";
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(github.gpt.api.sync.config.AppConfig.CONNECTION_TIMEOUT);
            connection.setReadTimeout(github.gpt.api.sync.config.AppConfig.READ_TIMEOUT);
            
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
     * @param channel 渠道配置
     * @return 是否创建成功
     */
    public boolean createChannel(NewApiChannel channel) {
        if (channel == null) {
            log.warn("尝试创建空的渠道配置");
            return false;
        }
        
        try {
            String url = newApiBaseUrl + "/api/channel/";
            
            Map<String, Object> channelData = buildChannelData(channel);
            String jsonBody = gson.toJson(channelData);
            
            log.debug("正在创建渠道: {} - {}", channel.getName(), jsonBody);
            
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            if (!accessToken.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);
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
            
        } catch (IOException e) {
            log.error("创建渠道时发生IO异常 - 渠道: {}, 错误: {}", channel.getName(), e.getMessage());
            return false;
        }
    }
    
    /**
     * 更新渠道
     * @param channel 渠道配置
     * @return 是否更新成功
     */
    public boolean updateChannel(NewApiChannel channel) {
        if (channel == null) {
            log.warn("尝试更新空的渠道配置");
            return false;
        }
        
        try {
            String url = newApiBaseUrl + "/api/channel/";
            
            Map<String, Object> channelData = buildChannelData(channel);
            String jsonBody = gson.toJson(channelData);
            
            log.debug("正在更新渠道: {} - {}", channel.getName(), jsonBody);
            
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/json");
            if (!accessToken.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);
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
            
        } catch (IOException e) {
            log.error("更新渠道时发生IO异常 - 渠道: {}, 错误: {}", channel.getName(), e.getMessage());
            return false;
        }
    }
    
    /**
     * 批量同步渠道（创建或更新）
     * @param channels 渠道列表
     * @return 同步结果统计
     */
    public Map<String, Integer> syncChannels(List<NewApiChannel> channels) {
        Map<String, Integer> result = new HashMap<>();
        int successCount = 0;
        int failureCount = 0;
        
        for (NewApiChannel channel : channels) {
            boolean success = createChannel(channel); // 先尝试创建
            if (!success) {
                success = updateChannel(channel); // 创建失败则尝试更新
            }
            
            if (success) {
                successCount++;
            } else {
                failureCount++;
            }
        }
        
        result.put("success", successCount);
        result.put("failure", failureCount);
        result.put("total", channels.size());
        
        log.info("批量同步渠道完成 - 总数: {}, 成功: {}, 失败: {}", 
            channels.size(), successCount, failureCount);
        
        return result;
    }
    
    /**
     * 构建New-API渠道数据
     */
    private Map<String, Object> buildChannelData(NewApiChannel channel) {
        Map<String, Object> data = new HashMap<>();
        
        data.put("name", channel.getName());
        data.put("type", channel.getType());
        data.put("key", channel.getKey());
        data.put("status", channel.getStatus());
        data.put("priority", channel.getPriority() != null ? channel.getPriority() : 1);
        
        if (channel.getBaseUrl() != null && !channel.getBaseUrl().isEmpty()) {
            data.put("base_url", channel.getBaseUrl());
        }
        
        if (channel.getGroupName() != null && !channel.getGroupName().isEmpty()) {
            data.put("group", channel.getGroupName());
        }
        
        if (channel.getModels() != null && !channel.getModels().isEmpty()) {
            data.put("models", channel.getModels());
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