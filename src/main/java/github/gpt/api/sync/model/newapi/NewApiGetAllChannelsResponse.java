package github.gpt.api.sync.model.newapi;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class NewApiGetAllChannelsResponse {
    private List<NewApiChannel> items;
    private int page;
    private int page_size;
    private int total;
    private Map<String, Integer> type_counts;
}