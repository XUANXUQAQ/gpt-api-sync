package github.gpt.api.sync.service;

import github.gpt.api.sync.model.gptload.GptLoadGroup;
import github.gpt.api.sync.model.newapi.NewApiChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ChannelMapperService {

    public List<NewApiChannel> mapToNewApiChannels(List<GptLoadGroup> gptLoadGroups) {
        if (gptLoadGroups == null) {
            return new ArrayList<>();
        }
        return gptLoadGroups.stream()
                .map(this::mapToNewApiChannel)
                .collect(Collectors.toList());
    }

    public NewApiChannel mapToNewApiChannel(GptLoadGroup gptLoadGroup) {
        if (gptLoadGroup == null) {
            return null;
        }

        NewApiChannel newApiChannel = new NewApiChannel();
        newApiChannel.setName(gptLoadGroup.getName());
        newApiChannel.setBaseUrl(gptLoadGroup.getEndpoint());
        newApiChannel.setModels(gptLoadGroup.getTestModel());
        newApiChannel.setGroupName(gptLoadGroup.getName()); // 使用 gpt-load 的 group name 作为 new-api 的 group
        newApiChannel.setType(1); // 默认为 OpenAI 类型
        newApiChannel.setStatus(1); // 默认启用
        newApiChannel.setPriority(0);

        // 使用 gpt-load 的第一个 API Key
        if (gptLoadGroup.getApiKeys() != null && !gptLoadGroup.getApiKeys().isEmpty()) {
            newApiChannel.setKey(gptLoadGroup.getApiKeys().get(0).getKeyValue());
        } else {
            newApiChannel.setKey(""); // 如果没有 key，则设置为空字符串
        }

        log.debug("映射 GptLoadGroup 到 NewApiChannel: {} -> {}", gptLoadGroup.getName(), newApiChannel.getName());
        return newApiChannel;
    }
}