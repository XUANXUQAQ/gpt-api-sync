package github.gpt.api.sync.controller;

import github.gpt.api.sync.db.DatabaseService;
import github.gpt.api.sync.mapper.ChannelMapper;
import github.gpt.api.sync.model.gptload.GptLoadGroup;
import github.gpt.api.sync.model.newapi.NewApiChannel;
import github.gpt.api.sync.service.GptLoadService;
import github.gpt.api.sync.service.NewApiService;
import io.javalin.http.Context;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class SyncController {

    private final GptLoadService gptLoadService;
    private final DatabaseService databaseService;

    private final NewApiService newApiService;

    public SyncController(GptLoadService gptLoadService, DatabaseService databaseService, NewApiService newApiService) {
        this.gptLoadService = gptLoadService;
        this.databaseService = databaseService;
        this.newApiService = newApiService;
    }

    public void syncChannels(Context ctx) {
        long startTime = System.currentTimeMillis();
        log.info("开始同步渠道配置...");

        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 测试GPT-Load连接
            if (!gptLoadService.testConnection()) {
                log.error("无法连接到GPT-Load服务");
                result.put("success", false);
                result.put("error", "无法连接到GPT-Load服务，请检查配置和服务状态");
                ctx.status(500).json(result);
                return;
            }

            // 2. 从GPT-Load获取分组数据
            List<GptLoadGroup> groups = gptLoadService.getAllGroups();

            if (groups == null || groups.isEmpty()) {
                log.warn("从GPT-Load获取到的分组列表为空");
                result.put("success", false);
                result.put("error", "从GPT-Load获取到的分组列表为空");
                ctx.status(404).json(result);
                return;
            }

            log.info("从GPT-Load获取到 {} 个分组", groups.size());

            // 3. 转换数据格式
            List<NewApiChannel> channels = groups.stream()
                    .filter(Objects::nonNull)
                    .map(ChannelMapper::transformGroupToChannel)
                    .collect(Collectors.toList());

            log.info("成功转换 {} 个渠道配置", channels.size());

            // 4. 保存到数据库
            databaseService.saveChannels(channels);

            // 5. 推送到New-API
            Map<String, Integer> syncResult = newApiService.syncChannels(channels);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            log.info("同步完成! 处理了 {} 个分组，耗时: {}ms",
                    groups.size(), duration);

            // 6. 返回成功结果
            result.put("success", true);
            result.put("message", "同步成功完成");
            result.put("groupsCount", groups.size());
            result.put("channelsProcessed", channels.size());
            result.put("newApiSync", syncResult);
            result.put("duration", duration + "ms");

            ctx.json(result);

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            log.error("同步过程中发生错误, 耗时: {}ms", duration, e);

            result.put("success", false);
            result.put("error", "同步过程中发生内部错误: " + e.getMessage());
            result.put("duration", duration + "ms");

            ctx.status(500).json(result);
        }
    }
}
