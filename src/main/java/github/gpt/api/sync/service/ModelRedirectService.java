package github.gpt.api.sync.service;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ModelRedirectService {

    private final Gson gson = new Gson();

    /**
     * 根据实际模型列表，为标准模型列表生成重定向映射。
     *
     * @param standardModels 权威的标准模型名称列表
     * @param actualModels   渠道实际支持的模型名称列表
     * @return 一个JSON格式的字符串，代表模型映射关系
     */
    public String generateModelMapping(List<String> standardModels, List<String> actualModels) {
        if (standardModels == null || standardModels.isEmpty() || actualModels == null || actualModels.isEmpty()) {
            return "{}";
        }

        Map<String, String> modelMap = new HashMap<>();

        for (String standardModel : standardModels) {
            // 如果实际模型列表已经精确包含标准模型，则不需要映射
            if (actualModels.contains(standardModel)) {
                continue;
            }

            // 寻找最相似的实际模型
            String bestMatch = findBestMatch(standardModel, actualModels);

            if (bestMatch != null) {
                log.debug("为标准模型 '{}' 找到最接近的匹配: '{}'", standardModel, bestMatch);
                modelMap.put(standardModel, bestMatch);
            } else {
                log.warn("无法为标准模型 '{}' 找到任何匹配项", standardModel);
            }
        }

        return gson.toJson(modelMap);
    }

    /**
     * 在目标列表中为源字符串寻找最佳匹配（使用Levenshtein距离）。
     */
    private String findBestMatch(String source, List<String> targets) {
        // 阶段1: 包含(Substring)优先匹配
        String bestContainMatch = null;
        int shortestLength = Integer.MAX_VALUE;

        for (String target : targets) {
            if (target.contains(source)) {
                if (target.length() < shortestLength) {
                    shortestLength = target.length();
                    bestContainMatch = target;
                }
            }
        }

        // 如果找到了包含匹配，直接返回最短的那个
        if (bestContainMatch != null) {
            log.trace("为 '{}' 找到包含优先匹配: '{}'", source, bestContainMatch);
            return bestContainMatch;
        }

        // 阶段2: Levenshtein距离作为后备
        String bestLevenshteinMatch = null;
        int minDistance = Integer.MAX_VALUE;

        for (String target : targets) {
            int distance = calculateLevenshteinDistance(source, target);
            if (distance < minDistance) {
                minDistance = distance;
                bestLevenshteinMatch = target;
            }
        }

        // 设定一个阈值，避免完全不相关的匹配。
        // 如果最小距离大于源字符串长度的一半，我们认为这个匹配是不可靠的。
        if (bestLevenshteinMatch != null && minDistance > source.length() / 2) {
            log.trace("找到的最佳Levenshtein匹配 '{}' (距离: {}) 对于 '{}' 来说太远，已忽略。", bestLevenshteinMatch, minDistance, source);
            return null;
        }

        return bestLevenshteinMatch;
    }

    /**
     * 计算两个字符串之间的Levenshtein距离。
     */
    private int calculateLevenshteinDistance(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();

        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    costs[j] = j;
                } else {
                    if (j > 0) {
                        int newValue = costs[j - 1];
                        if (s1.charAt(i - 1) != s2.charAt(j - 1)) {
                            newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
                        }
                        costs[j - 1] = lastValue;
                        lastValue = newValue;
                    }
                }
            }
            if (i > 0) {
                costs[s2.length()] = lastValue;
            }
        }
        return costs[s2.length()];
    }
}