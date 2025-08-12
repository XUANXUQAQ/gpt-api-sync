package github.gpt.api.sync.service;

import github.gpt.api.sync.config.AppConfig;
import github.gpt.api.sync.model.gptload.GptLoadGroup;
import github.gpt.api.sync.model.newapi.NewApiChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChannelMapperService {

    public NewApiChannel mapToNewApiChannel(GptLoadGroup gptLoadGroup) {
        if (gptLoadGroup == null) {
            return null;
        }

        NewApiChannel newApiChannel = new NewApiChannel();
        newApiChannel.setName(gptLoadGroup.getDisplayName() == null ? gptLoadGroup.getName() : gptLoadGroup.getDisplayName());
        newApiChannel.setBaseUrl(gptLoadGroup.getEndpoint());
        newApiChannel.setModels(gptLoadGroup.getTestModel());
        newApiChannel.setGroupName("default"); // 使用默认group
        // 根据 gpt-load 的 channel_type 映射 new-api 的 type
        switch (gptLoadGroup.getChannelType()) {
            case "openai":
                newApiChannel.setType(1);
                break;
            case "gemini":
                newApiChannel.setType(24);
                break;
            case "anthropic":
                newApiChannel.setType(14);
                break;
            default:
                log.warn("未知的 channel_type: '{}', 将默认为 OpenAI (1)", gptLoadGroup.getChannelType());
                newApiChannel.setType(1); // 默认为 OpenAI 类型
                break;
        }
        newApiChannel.setStatus(1); // 默认启用
        newApiChannel.setPriority(0);

        // 设置key，优先使用proxy_keys，如果为空则使用全局的auth key
        String proxyKeys = gptLoadGroup.getProxyKeys();
        if (proxyKeys != null && !proxyKeys.trim().isEmpty()) {
            newApiChannel.setKey(proxyKeys);
        } else {
            newApiChannel.setKey(AppConfig.GPT_LOAD_AUTH_KEY);
        }

        log.debug("映射 GptLoadGroup 到 NewApiChannel: {} -> {}", gptLoadGroup.getName(), newApiChannel.getName());
        return newApiChannel;
    }
}