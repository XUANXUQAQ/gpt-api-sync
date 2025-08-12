package github.gpt.api.sync.model.gptload;

import lombok.Data;

@Data
public class Upstream {
    private String url;
    private int weight;
}
