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
        log.info("开始同步渠道配置...");

        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 从GPT-Load获取分组数据
            log.info("步骤 1/3: 从 gpt-load 获取分组...");
            List<GptLoadGroup> groups = gptLoadService.getAllGroups();
            if (groups == null || groups.isEmpty()) {
                throw new IllegalStateException("从 gpt-load 获取的分组列表为空或获取失败");
            }
            log.info("成功从 gpt-load 获取到 {} 个分组", groups.size());

            // 2. 将 GptLoadGroup 映射为 NewApiChannel
            log.info("步骤 2/3: 正在将 gpt-load 分组映射为 new-api 渠道...");
            List<NewApiChannel> channelsToSync = channelMapperService.mapToNewApiChannels(groups);
            log.info("成功映射 {} 个渠道", channelsToSync.size());

            // 3. 将转换后的渠道同步到 New-API
            log.info("步骤 3/3: 正在将渠道同步到 new-api...");
            Map<String, Integer> syncResult = newApiService.syncChannels(channelsToSync);
            log.info("成功与 new-api 同步: {}", syncResult);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            log.info("同步成功完成! 处理了 {} 个分组，耗时: {}ms", groups.size(), duration);

            // 4. 返回成功结果
            result.put("success", true);
            result.put("message", "同步成功完成");
            result.put("groups_fetched", groups.size());
            result.put("channels_processed", channelsToSync.size());
            result.put("new_api_sync_result", syncResult);
            result.put("duration_ms", duration);

            ctx.json(result);

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            log.error("同步过程中发生错误, 耗时: {}ms", duration, e);

            result.put("success", false);
            result.put("error", "同步失败: " + e.getMessage());
            result.put("duration_ms", duration);

            ctx.status(500).json(result);
        }
    }
}
