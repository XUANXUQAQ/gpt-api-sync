package github.gpt.api.sync.model.gptload;

import lombok.Data;

@Data
public class GptLoadApiResponse<T> {
    private int code;
    private String message;
    private T data;
}
