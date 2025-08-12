package github.gpt.api.sync.controller;

import github.gpt.api.sync.model.gptload.GptLoadGroup;
import github.gpt.api.sync.model.newapi.NewApiChannel;
import github.gpt.api.sync.service.GptLoadService;
import github.gpt.api.sync.service.NewApiService;
import io.javalin.http.Context;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public class ApiController {

    private final GptLoadService gptLoadService;
    private final NewApiService newApiService;

    public ApiController(GptLoadService gptLoadService, NewApiService newApiService) {
        this.gptLoadService = gptLoadService;
        this.newApiService = newApiService;
    }

    /**
     * 获取所有 gpt-load 的分组信息
     */
    public void getGptLoadInfo(Context ctx) {
        try {
            log.info("接收到获取 gpt-load 信息的请求");
            List<GptLoadGroup> groups = gptLoadService.getAllGroups();
            ctx.json(groups);
        } catch (Exception e) {
            log.error("获取 gpt-load 信息失败", e);
            ctx.status(500).json(Map.of("error", "获取 gpt-load 信息失败: " + e.getMessage()));
        }
    }

    /**
     * 获取所有 new-api 的渠道信息
     */
    public void getNewApiInfo(Context ctx) {
        try {
            log.info("接收到获取 new-api 信息的请求");
            List<NewApiChannel> channels = newApiService.getAllChannels();
            ctx.json(channels);
        } catch (Exception e) {
            log.error("获取 new-api 信息失败", e);
            ctx.status(500).json(Map.of("error", "获取 new-api 信息失败: " + e.getMessage()));
        }
    }
}