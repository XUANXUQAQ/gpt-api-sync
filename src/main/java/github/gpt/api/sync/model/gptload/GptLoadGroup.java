package github.gpt.api.sync.model.gptload;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class GptLoadGroup {
    private int id;
    private String name;
    @SerializedName("display_name")
    private String displayName;
    private String description;
    @SerializedName("channel_type")
    private String channelType;
    @SerializedName("test_model")
    private String testModel;
    private List<Upstream> upstreams;
    @SerializedName("api_keys")
    private List<ApiKey> apiKeys;
}
