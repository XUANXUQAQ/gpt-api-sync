package github.gpt.api.sync.model.gptload;

import lombok.Data;

import java.util.List;

@Data
public class GptLoadApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

    public boolean isSuccess() {
        return success;
    }
}
