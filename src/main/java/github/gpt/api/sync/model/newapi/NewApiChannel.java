package github.gpt.api.sync.model.newapi;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class NewApiChannel {
    private int id;
    private int type;
    private String key;
    @SerializedName("openai_organization")
    private String openaiOrganization;
    @SerializedName("test_model")
    private String testModel;
    private int status;
    private String name;
    private int weight;
    @SerializedName("created_time")
    private long createdTime;
    @SerializedName("test_time")
    private long testTime;
    @SerializedName("response_time")
    private int responseTime;
    @SerializedName("base_url")
    private String baseUrl;
    private String other;
    private double balance;
    @SerializedName("balance_updated_time")
    private long balanceUpdatedTime;
    private String models;
    @SerializedName("group")
    private String groupName;
    @SerializedName("used_quota")
    private long usedQuota;
    @SerializedName("model_mapping")
    private String modelMapping;
    @SerializedName("status_code_mapping")
    private String statusCodeMapping;
    private int priority;
    @SerializedName("auto_ban")
    private int autoBan;
    @SerializedName("other_info")
    private String otherInfo;
    private String settings;
    private String tag;
    private String setting;
    @SerializedName("channel_info")
    private ChannelInfo channelInfo;

    @Data
    public static class ChannelInfo {
        @SerializedName("is_multi_key")
        private boolean isMultiKey;
        @SerializedName("multi_key_size")
        private int multiKeySize;
        @SerializedName("multi_key_polling_index")
        private int multiKeyPollingIndex;
        @SerializedName("multi_key_mode")
        private String multiKeyMode;
    }
}
