package github.gpt.api.sync.model.gptload;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class ApiKey {
    private int id;
    @SerializedName("key_value")
    private String keyValue;
}
