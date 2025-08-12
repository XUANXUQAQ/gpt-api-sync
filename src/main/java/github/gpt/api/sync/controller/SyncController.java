package github.gpt.api.sync.controller;

import github.gpt.api.sync.model.gptload.GptLoadGroup;
import github.gpt.api.sync.model.newapi.NewApiChannel;
import github.gpt.api.sync.service.ChannelMapperService;
import github.gpt.api.sync.service.GptLoadService;
import github.gpt.api.sync.service.NewApiService;
import io.javalin.http.Context;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class SyncController {

    private final GptLoadService gptLoadService;
    private final NewApiService newApiService;
    private final ChannelMapperService channelMapperService;

    public SyncController(GptLoadService gptLoadService, NewApiService newApiService, ChannelMapperService channelMapperService) {
        this.gptLoadService = gptLoadService;
        this.newApiService = newApiService;
        this.channelMapperService = channelMapperService;
    }

    public void syncChannels(Context ctx) {
        long startTime = System.currentTimeMillis();
        log.info("开始智能同步渠道配置...");

        Map<String, Object> result = new HashMap<>();
        int createdCount = 0;
        int updatedCount = 0;
        int failedCount = 0;

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

                if (existingChannel != null) {
                    // 更新现有渠道
                    channelToSync.setId(existingChannel.getId()); // 必须设置ID才能更新
                    log.info("找到匹配渠道，准备更新: {} (ID: {})", channelToSync.getName(), channelToSync.getId());
                    if (newApiService.updateChannel(channelToSync)) {
                        updatedCount++;
                    } else {
                        failedCount++;
                        log.error("更新渠道失败: {}", channelToSync.getName());
                    }
                } else {
                    // 创建新渠道
                    log.info("未找到匹配渠道，准备创建: {}", channelToSync.getName());
                    if (newApiService.createChannel(channelToSync)) {
                        createdCount++;
                    } else {
                        failedCount++;
                        log.error("创建渠道失败: {}", channelToSync.getName());
                    }
                }
            }
            log.info("渠道同步处理完成。创建: {}, 更新: {}, 失败: {}", createdCount, updatedCount, failedCount);

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
}
