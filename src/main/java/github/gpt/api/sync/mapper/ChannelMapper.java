package github.gpt.api.sync.mapper;

import github.gpt.api.sync.model.gptload.ApiKey;
import github.gpt.api.sync.model.gptload.GptLoadGroup;
import github.gpt.api.sync.model.newapi.NewApiChannel;

import java.util.stream.Collectors;

public class ChannelMapper {

    public static NewApiChannel transformGroupToChannel(GptLoadGroup group) {
        NewApiChannel channel = new NewApiChannel();
        channel.setName(group.getDisplayName());
        channel.setGroupName(group.getName()); // Use original name as group_name
        channel.setType(mapChannelType(group.getChannelType()));

        if (group.getUpstreams() != null && !group.getUpstreams().isEmpty()) {
            channel.setBaseUrl(group.getUpstreams().get(0).getUrl());
        }

        if (group.getApiKeys() != null && !group.getApiKeys().isEmpty()) {
            String keys = group.getApiKeys().stream()
                    .map(ApiKey::getKeyValue)
                    .collect(Collectors.joining(" "));
            channel.setKey(keys);
        }

        channel.setStatus(1); // Enabled by default
        channel.setModels(group.getTestModel()); // Use test_model as the default models
        channel.setPriority((long) group.getId());

        return channel;
    }

    private static int mapChannelType(String gptLoadType) {
        if (gptLoadType == null) {
            return 0;
        }
        return switch (gptLoadType.toLowerCase()) {
            case "openai" -> 1;
            case "anthropic" -> 3;
            case "midjourney" -> 4;
            // Add other mappings as needed
            default -> 0; // Unknown
        };
    }
}
