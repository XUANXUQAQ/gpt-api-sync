package github.gpt.api.sync.model.newapi;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class NewApiChannel {
    private Integer id;
    private int type;
    private String key;
    private int status;
    private String name;
    private Long priority;
    @SerializedName("base_url")
    private String baseUrl;
    @SerializedName("group_name")
    private String groupName;
    private String models;
}
