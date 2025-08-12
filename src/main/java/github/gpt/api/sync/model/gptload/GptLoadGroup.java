package github.gpt.api.sync.model.gptload;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class GptLoadGroup {
    private int id;
    private String name;
    private String endpoint;
    @SerializedName("display_name")
    private String displayName;
    private String description;
    private List<Upstream> upstreams;
    @SerializedName("channel_type")
    private String channelType;
    private int sort;
    @SerializedName("test_model")
    private String testModel;
    @SerializedName("validation_endpoint")
    private String validationEndpoint;
    @SerializedName("param_overrides")
    private Object paramOverrides;
    private Object config;
    @SerializedName("proxy_keys")
    private String proxyKeys;
    @SerializedName("last_validated_at")
    private String lastValidatedAt;
    @SerializedName("created_at")
    private String createdAt;
    @SerializedName("updated_at")
    private String updatedAt;
}
