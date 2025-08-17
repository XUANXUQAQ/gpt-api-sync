package github.gpt.api.sync.service;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class ModelRedirectService {

    /**
     * 根据实际模型列表，为标准模型列表生成重定向映射。
     *
     * @param standardModels 权威的标准模型名称列表
     * @param actualModels   渠道实际支持的模型名称列表
     * @return 一个JSON格式的字符串，代表模型映射关系
     */
    public Map<String, String> generateModelMapping(List<String> standardModels, List<String> actualModels) {
        if (standardModels == null || standardModels.isEmpty() || actualModels == null || actualModels.isEmpty()) {
            return Collections.emptyMap();
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

        return modelMap;
    }

    /**
     * 在目标列表中为源字符串寻找最佳匹配（使用Levenshtein距离）。
     */
    private String findBestMatch(String source, List<String> targets) {
        // 过滤掉可能导致“降级”的匹配项
        // 例如，防止 "gpt-4o" 映射到 "gpt-4o-mini"
        List<String> eligibleTargets = new ArrayList<>();
        for (String target : targets) {
            boolean isPotentialDowngrade = target.startsWith(source) &&
                                           target.length() > source.length() &&
                                           (target.endsWith("-mini") || target.endsWith("-nano") || target.endsWith("-lite"));

            if (!isPotentialDowngrade) {
                eligibleTargets.add(target);
            } else {
                log.debug("排除了潜在的降级匹配: source='{}', target='{}'", source, target);
            }
        }

        if (eligibleTargets.isEmpty()) {
            log.warn("为 '{}' 没有找到合适的（非降级）候选模型", source);
            return null;
        }

        // 对于很短的模型名称（长度 <= 3），使用更严格的匹配策略
        if (source.length() <= 3) {
            // 优先精确匹配
            for (String target : eligibleTargets) {
                if (target.equals(source)) {
                    log.trace("为短模型名 '{}' 找到精确匹配: '{}'", source, target);
                    return target;
                }
            }

            // 其次前缀匹配
            String bestPrefixMatch = null;
            int shortestPrefixLength = Integer.MAX_VALUE;
            for (String target : eligibleTargets) {
                if (target.startsWith(source + "-") || target.startsWith(source + "_")) {
                    if (target.length() < shortestPrefixLength) {
                        shortestPrefixLength = target.length();
                        bestPrefixMatch = target;
                    }
                }
            }

            if (bestPrefixMatch != null) {
                log.trace("为短模型名 '{}' 找到前缀匹配: '{}'", source, bestPrefixMatch);
                return bestPrefixMatch;
            }

            // 最后才考虑单词边界匹配
            String bestWordBoundaryMatch = null;
            int shortestWordBoundaryLength = Integer.MAX_VALUE;
            for (String target : eligibleTargets) {
                if (isWordBoundaryMatch(source, target)) {
                    if (target.length() < shortestWordBoundaryLength) {
                        shortestWordBoundaryLength = target.length();
                        bestWordBoundaryMatch = target;
                    }
                }
            }

            if (bestWordBoundaryMatch != null) {
                log.trace("为短模型名 '{}' 找到单词边界匹配: '{}'", source, bestWordBoundaryMatch);
                return bestWordBoundaryMatch;
            }
        } else {
            // 对于较长的模型名称，使用改进的包含匹配逻辑
            String bestContainMatch = null;
            int shortestLength = Integer.MAX_VALUE;

            for (String target : eligibleTargets) {
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
        }

        // 阶段2: Levenshtein距离作为后备
        String bestLevenshteinMatch = null;
        int minDistance = Integer.MAX_VALUE;

        for (String target : eligibleTargets) {
            // 对于很短的目标模型名，需要额外验证
            if (target.length() <= 3 && source.length() > target.length() * 2) {
                // 检查是否是合理的短目标匹配
                if (!isValidShortTargetMatch(source, target)) {
                    log.debug("在Levenshtein匹配中排除了不合理的短目标: source='{}', target='{}'", source, target);
                    continue;
                }
            }

            // 对于很短的源模型名匹配到很长的目标模型名，需要额外验证
            if (source.length() <= 3 && target.length() > source.length() * 3) {
                // 检查是否是合理的短源匹配到长目标
                if (!isValidShortSourceMatch(source, target)) {
                    log.debug("在Levenshtein匹配中排除了不合理的短源到长目标匹配: source='{}', target='{}'", source, target);
                    continue;
                }
            }

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
     * 计算两个字符串之间的Levenshtein距离，使用滑动窗口比较子串。
     * 这对于比较 'claude-4-sonnet' 和 'claude-sonnet-4-20251233' 这样的情况很有用。
     */
    private int calculateLevenshteinDistance(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();

        String shorter = s1; // 标准模型作为 shorter
        String longer = s2;  // 实际模型作为 longer

        int minDistance = Integer.MAX_VALUE;

        if (longer.length() < shorter.length()) {
            // 如果实际模型较短，直接计算距离
            return rawLevenshteinDistance(shorter, longer);
        } else {
            // 在实际模型上滑动标准模型长度的窗口
            for (int i = 0; i <= longer.length() - shorter.length(); i++) {
                String sub = longer.substring(i, i + shorter.length());
                int distance = rawLevenshteinDistance(shorter, sub);
                if (distance < minDistance) {
                    minDistance = distance;
                }
                // 优化：如果找到了完美匹配（距离为0），则无需继续搜索
                if (minDistance == 0) {
                    break;
                }
            }
            return minDistance;
        }
    }

    /**
     * 计算两个字符串之间的原始Levenshtein距离。
     */
    private int rawLevenshteinDistance(String s1, String s2) {
        // s1 and s2 are assumed to be pre-processed (e.g., toLowerCase)
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

    /**
     * 检查源字符串是否在目标字符串中作为完整单词存在
     * 单词边界定义为：字符串开始、结束，或者非字母数字字符
     */
    private boolean isWordBoundaryMatch(String source, String target) {
        String lowerSource = source.toLowerCase();
        String lowerTarget = target.toLowerCase();

        int index = lowerTarget.indexOf(lowerSource);
        if (index == -1) {
            return false;
        }

        // 检查所有匹配位置
        while (index != -1) {
            boolean validStart = (index == 0) || !Character.isLetterOrDigit(lowerTarget.charAt(index - 1));
            boolean validEnd = (index + lowerSource.length() >= lowerTarget.length()) ||
                               !Character.isLetterOrDigit(lowerTarget.charAt(index + lowerSource.length()));

            if (validStart && validEnd) {
                return true;
            }

            // 查找下一个匹配位置
            index = lowerTarget.indexOf(lowerSource, index + 1);
        }

        return false;
    }

    /**
     * 验证长标准模型名与短目标模型名的匹配是否合理
     * 防止如 "claude-3.7-sonnet" 错误匹配到 "o3" 的情况
     */
    private boolean isValidShortTargetMatch(String source, String target) {
        String lowerSource = source.toLowerCase();
        String lowerTarget = target.toLowerCase();

        // 1. 检查是否是精确的单词边界匹配
        if (isWordBoundaryMatch(target, source)) {
            return true;
        }

        // 2. 检查是否是有意义的前缀匹配
        // 例如：gpt-4o 可以匹配到 gpt，但 claude-3.7-sonnet 不应该匹配到 o3
        if (lowerSource.startsWith(lowerTarget + "-") || lowerSource.startsWith(lowerTarget + "_")) {
            return true;
        }

        // 3. 检查是否是版本号相关的合理匹配
        // 例如：claude-4-sonnet 可以匹配到 c4，但不应该匹配到随机的短字符串
        if (isVersionRelatedMatch(lowerSource, lowerTarget)) {
            return true;
        }

        // 4. 其他情况都认为是不合理的匹配
        log.debug("拒绝不合理的短目标匹配: '{}' -> '{}'", source, target);
        return false;
    }

    /**
     * 检查是否是版本号相关的合理匹配
     */
    private boolean isVersionRelatedMatch(String source, String target) {
        // 检查目标字符串是否可能是源字符串的有意义缩写
        // 例如：claude-4 -> c4, gpt-3.5 -> g35

        // 提取源字符串中的主要组件
        String[] sourceParts = source.split("[-_.]");
        StringBuilder abbreviation = new StringBuilder();

        for (String part : sourceParts) {
            if (!part.isEmpty()) {
                // 取每个部分的首字母或数字
                char firstChar = part.charAt(0);
                if (Character.isLetterOrDigit(firstChar)) {
                    abbreviation.append(firstChar);
                }

                // 如果部分包含数字，也添加数字
                for (char c : part.toCharArray()) {
                    if (Character.isDigit(c)) {
                        abbreviation.append(c);
                    }
                }
            }
        }

        // 检查生成的缩写是否与目标匹配
        String generatedAbbr = abbreviation.toString().toLowerCase();
        return generatedAbbr.equals(target) || generatedAbbr.contains(target) || target.contains(generatedAbbr);
    }

    /**
     * 验证短源模型名与长目标模型名的匹配是否合理
     * 防止如 "o3" 错误匹配到 "claude-4-sonnet" 的情况
     */
    private boolean isValidShortSourceMatch(String source, String target) {
        String lowerSource = source.toLowerCase();
        String lowerTarget = target.toLowerCase();

        // 1. 检查是否是精确的单词边界匹配
        if (isWordBoundaryMatch(source, target)) {
            return true;
        }

        // 2. 检查是否是有意义的前缀匹配
        // 例如：o3 可以匹配到 o3-mini，但不应该匹配到 claude-4-sonnet
        if (lowerTarget.startsWith(lowerSource + "-") || lowerTarget.startsWith(lowerSource + "_")) {
            return true;
        }

        // 3. 检查是否是版本号相关的合理匹配
        // 例如：o3 可以匹配到 o3-2025，但不应该匹配到随机的长字符串
        if (isVersionRelatedMatch(target, source)) {
            return true;
        }

        // 4. 检查源字符串是否是目标字符串的合理缩写
        if (isReasonableAbbreviation(source, target)) {
            return true;
        }

        // 5. 其他情况都认为是不合理的匹配
        log.debug("拒绝不合理的短源到长目标匹配: '{}' -> '{}'", source, target);
        return false;
    }

    /**
     * 检查源字符串是否是目标字符串的合理缩写
     */
    private boolean isReasonableAbbreviation(String source, String target) {
        String lowerSource = source.toLowerCase();
        String lowerTarget = target.toLowerCase();

        // 提取目标字符串中的主要组件
        String[] targetParts = lowerTarget.split("[-_.]");
        StringBuilder abbreviation = new StringBuilder();

        for (String part : targetParts) {
            if (!part.isEmpty()) {
                // 取每个部分的首字母
                char firstChar = part.charAt(0);
                if (Character.isLetterOrDigit(firstChar)) {
                    abbreviation.append(firstChar);
                }
            }
        }

        // 检查生成的缩写是否与源匹配
        String generatedAbbr = abbreviation.toString();
        return generatedAbbr.contains(lowerSource) || lowerSource.contains(generatedAbbr);
    }
}