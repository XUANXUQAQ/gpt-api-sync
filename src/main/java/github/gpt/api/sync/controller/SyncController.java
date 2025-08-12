package github.gpt.api.sync.controller;

import github.gpt.api.sync.model.gptload.GptLoadGroup;
import github.gpt.api.sync.config.AppConfig;
import github.gpt.api.sync.model.newapi.NewApiChannel;
import github.gpt.api.sync.service.ChannelMapperService;
import github.gpt.api.sync.service.GptLoadService;
import github.gpt.api.sync.service.ModelRedirectService;
import github.gpt.api.sync.service.NewApiService;
import io.javalin.http.Context;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class SyncController {

    private final GptLoadService gptLoadService;
    private final NewApiService newApiService;
    private final ChannelMapperService channelMapperService;
    private final ModelRedirectService modelRedirectService;

    public SyncController(GptLoadService gptLoadService, NewApiService newApiService, ChannelMapperService channelMapperService, ModelRedirectService modelRedirectService) {
        this.gptLoadService = gptLoadService;
        this.newApiService = newApiService;
        this.channelMapperService = channelMapperService;
        this.modelRedirectService = modelRedirectService;
    }

    public synchronized void syncChannels(Context ctx) {
        long startTime = System.currentTimeMillis();
        log.info("开始智能同步渠道配置...");

        Map<String, Object> result = new HashMap<>();
        int createdCount = 0;
        int updatedCount = 0;
        int failedCount = 0;
        List<NewApiChannel> newlyCreatedChannels = new ArrayList<>();

        try {
            // 1. 从 gpt-load 获取源分组
            log.info("步骤 1/4: 从 gpt-load 获取分组...");
            List<GptLoadGroup> sourceGroups = gptLoadService.getAllGroups();
            if (sourceGroups == null || sourceGroups.isEmpty()) {
                throw new IllegalStateException("从 gpt-load 获取的分组列表为空或获取失败");
            }
            log.info("成功从 gpt-load 获取到 {} 个分组", sourceGroups.size());

            // 2. 从 new-api 获取现有渠道
            log.info("步骤 2/4: 从 new-api 获取现有渠道...");
            List<NewApiChannel> existingChannelsList = newApiService.getAllChannels();
            Map<String, NewApiChannel> existingChannelsMap = new HashMap<>();
            for (NewApiChannel channel : existingChannelsList) {
                if (channel.getBaseUrl() != null && !channel.getBaseUrl().isEmpty()) {
                    existingChannelsMap.put(channel.getBaseUrl(), channel);
                }
            }
            log.info("成功从 new-api 获取到 {} 个渠道", existingChannelsList.size());

            // 3. 比较并同步
            log.info("步骤 3/4: 比较并同步渠道 (创建/更新)...");
            for (GptLoadGroup sourceGroup : sourceGroups) {
                NewApiChannel channelToSync = channelMapperService.mapToNewApiChannel(sourceGroup);
                if (channelToSync == null) {
                    log.warn("映射失败，跳过分组: {}", sourceGroup.getName());
                    failedCount++;
                    continue;
                }

                NewApiChannel existingChannel = existingChannelsMap.get(channelToSync.getBaseUrl());

                try {
                    if (existingChannel != null) {
                        // 更新现有渠道
                        channelToSync.setId(existingChannel.getId());
                        log.info("找到匹配渠道，准备更新: {} (ID: {})", channelToSync.getName(), channelToSync.getId());
                        if (newApiService.updateChannel(channelToSync)) {
                            updatedCount++;
                            // 更新成功后，立即获取模型并再次更新
                            updateModelsForChannel(channelToSync);
                        } else {
                            failedCount++;
                            log.error("更新渠道失败: {}", channelToSync.getName());
                        }
                    } else {
                        // 创建新渠道
                        log.info("未找到匹配渠道，准备创建: {}", channelToSync.getName());
                        if (newApiService.createChannel(channelToSync)) {
                            createdCount++;
                            // 优化：先记录下来，循环结束后再统一处理
                            newlyCreatedChannels.add(channelToSync);
                        } else {
                            failedCount++;
                            log.error("创建渠道失败: {}", channelToSync.getName());
                        }
                    }
                } catch (Exception e) {
                    failedCount++;
                    log.error("处理渠道 {} 时发生异常", channelToSync.getName(), e);
                }
            }

            log.info("渠道同步处理完成。创建: {}, 更新: {}, 失败: {}", createdCount, updatedCount, failedCount);

            // 3.5. 为新创建的渠道获取并更新模型
            if (!newlyCreatedChannels.isEmpty()) {
                log.info("步骤 3.5/4: 为 {} 个新创建的渠道更新模型列表...", newlyCreatedChannels.size());
                try {
                    List<NewApiChannel> refreshedChannels = newApiService.getAllChannels();
                    Map<String, NewApiChannel> refreshedChannelsMap = new HashMap<>();
                    for (NewApiChannel channel : refreshedChannels) {
                        refreshedChannelsMap.put(channel.getBaseUrl(), channel);
                    }

                    for (NewApiChannel newChannel : newlyCreatedChannels) {
                        NewApiChannel fullNewChannel = refreshedChannelsMap.get(newChannel.getBaseUrl());
                        if (fullNewChannel != null) {
                            updateModelsForChannel(fullNewChannel);
                        } else {
                            log.error("无法在刷新后找到新创建的渠道: {}", newChannel.getName());
                        }
                    }
                } catch (Exception e) {
                    log.error("为新创建的渠道更新模型时发生错误", e);
                }
            }

            // 4. 准备并返回结果
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            log.info("步骤 4/4: 同步成功完成! 总耗时: {}ms", duration);

            result.put("success", true);
            result.put("message", "同步成功完成");
            result.put("groups_fetched", sourceGroups.size());
            result.put("channels_created", createdCount);
            result.put("channels_updated", updatedCount);
            result.put("channels_failed", failedCount);
            result.put("duration_ms", duration);

            ctx.json(result);

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            log.error("同步过程中发生严重错误, 耗时: {}ms", duration, e);

            result.put("success", false);
            result.put("error", "同步失败: " + e.getMessage());
            result.put("duration_ms", duration);

            ctx.status(500).json(result);
        }
    }

    private void updateModelsForChannel(NewApiChannel channel) {
        log.info("步骤 3.5/4: 为渠道 {} (ID: {}) 获取并更新模型列表...", channel.getName(), channel.getId());
        try {
            List<String> models = newApiService.fetchModelsForChannel(channel.getId());
            if (models != null && !models.isEmpty()) {
                // 更新模型列表
                channel.setModels(String.join(",", models));
                log.info("为渠道 {} 获取到 {} 个模型", channel.getName(), models.size());

                // 生成并设置模型重定向映射
                String modelMappingJson = modelRedirectService.generateModelMapping(AppConfig.STANDARD_MODELS, models);
                if (modelMappingJson != null && !modelMappingJson.equals("{}")) {
                    channel.setModelMapping(modelMappingJson);
                    log.info("为渠道 {} 生成了模型重定向映射: {}", channel.getName(), modelMappingJson);
                } else {
                    log.info("渠道 {} 无需模型重定向", channel.getName());
                }

                // 统一更新渠道
                if (newApiService.updateChannel(channel)) {
                    log.info("成功为渠道 {} 更新了模型列表和重定向映射", channel.getName());
                } else {
                    log.error("为渠道 {} 更新模型列表和重定向映射失败", channel.getName());
                }
            } else {
                log.info("渠道 {} 没有可用的模型列表，跳过模型更新", channel.getName());
            }
        } catch (IOException | URISyntaxException e) {
            log.error("为渠道 {} 获取模型列表时发生IO异常", channel.getName(), e);
        }
    }
}
