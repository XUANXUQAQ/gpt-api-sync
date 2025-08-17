package github.gpt.api.sync.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ModelRedirectServiceTest {

    private ModelRedirectService modelRedirectService;

    @BeforeEach
    void setUp() {
        modelRedirectService = new ModelRedirectService();
    }

    @Test
    void testShortModelNames_ShouldNotMatchIncorrectly() {
        // 测试场景：短模型名称不应该错误匹配到长模型名称中
        List<String> standardModels = Arrays.asList("o3", "o4");
        List<String> actualModels = Arrays.asList(
                "claude-3.7-sonnet",
                "claude-4-haiku",
                "gpt-4o",
                "gpt-4o-mini"
        );

        Map<String, String> mapping = modelRedirectService.generateModelMapping(standardModels, actualModels);

        // o3 不应该匹配到 claude-3.7-sonnet（因为包含"3"）
        // o4 不应该匹配到 claude-4-haiku（因为包含"4"）
        // 由于没有合适的匹配，这些短模型名称应该不被映射
        assertFalse(mapping.containsKey("o3"), "o3 不应该被错误匹配");
        assertFalse(mapping.containsKey("o4"), "o4 不应该被错误匹配");
    }

    @Test
    void testShortModelNames_ExactMatch() {
        // 测试场景：短模型名称应该能精确匹配
        List<String> standardModels = Arrays.asList("o3", "o4");
        List<String> actualModels = Arrays.asList("o3", "o4", "claude-3.7-sonnet");

        Map<String, String> mapping = modelRedirectService.generateModelMapping(standardModels, actualModels);

        // 由于实际模型列表中有精确匹配，不需要映射
        assertFalse(mapping.containsKey("o3"), "精确匹配时不需要映射");
        assertFalse(mapping.containsKey("o4"), "精确匹配时不需要映射");
    }

    @Test
    void testShortModelNames_PrefixMatch() {
        // 测试场景：短模型名称应该能前缀匹配
        List<String> standardModels = Arrays.asList("o3", "o4");
        List<String> actualModels = Arrays.asList(
                "o3-turbo",
                "o4-preview",
                "claude-3.7-sonnet"
        );

        Map<String, String> mapping = modelRedirectService.generateModelMapping(standardModels, actualModels);

        // 应该匹配到前缀相同的模型
        assertEquals("o3-turbo", mapping.get("o3"), "o3 应该匹配到 o3-turbo");
        assertEquals("o4-preview", mapping.get("o4"), "o4 应该匹配到 o4-preview");
    }

    @Test
    void testShortModelNames_WordBoundaryMatch() {
        // 测试场景：短模型名称的单词边界匹配
        List<String> standardModels = Arrays.asList("o3");
        List<String> actualModels = Arrays.asList(
                "model-o3-advanced",  // 应该匹配（单词边界）
                "claude-3o-sonnet",   // 不应该匹配（不是单词边界）
                "gpt-4o3-mini"        // 不应该匹配（不是单词边界）
        );

        Map<String, String> mapping = modelRedirectService.generateModelMapping(standardModels, actualModels);

        // 应该匹配到单词边界正确的模型
        assertEquals("model-o3-advanced", mapping.get("o3"), "o3 应该匹配到 model-o3-advanced");
    }

    @Test
    void testLongModelNames_ShouldUseContainsMatch() {
        // 测试场景：长模型名称应该使用包含匹配逻辑
        List<String> standardModels = Arrays.asList("claude-4-sonnet");
        List<String> actualModels = Arrays.asList(
                "claude-4-sonnet-20241120",
                "claude-3.7-sonnet",
                "gpt-4o"
        );

        Map<String, String> mapping = modelRedirectService.generateModelMapping(standardModels, actualModels);

        // 长模型名称应该能正常使用包含匹配
        assertEquals("claude-4-sonnet-20241120", mapping.get("claude-4-sonnet"),
                "claude-4-sonnet 应该匹配到 claude-4-sonnet-20241120");
    }

    @Test
    void testLongStandardToShortTarget_ShouldNotMatchIncorrectly() {
        // 测试场景：长标准模型名不应该错误匹配到短目标模型名
        List<String> standardModels = Arrays.asList("claude-3.7-sonnet", "gpt-4-turbo");
        List<String> actualModels = Arrays.asList("o3", "o4", "gpt", "claude");

        Map<String, String> mapping = modelRedirectService.generateModelMapping(standardModels, actualModels);

        // claude-3.7-sonnet 不应该匹配到 o3（仅因为包含"3"）
        // gpt-4-turbo 不应该匹配到 gpt（长度差异太大）
        assertFalse(mapping.containsKey("claude-3.7-sonnet"), "claude-3.7-sonnet 不应该错误匹配到短模型名");
        assertTrue(mapping.containsKey("gpt-4-turbo"), "gpt-4-turbo 应该能匹配到 gpt（合理的前缀匹配）");
        assertEquals("gpt", mapping.get("gpt-4-turbo"));
    }

    @Test
    void testValidShortTargetMatch() {
        // 测试场景：验证合理的长到短匹配
        List<String> standardModels = Arrays.asList("gpt-4o", "claude-4-sonnet");
        List<String> actualModels = Arrays.asList("gpt", "c4");

        Map<String, String> mapping = modelRedirectService.generateModelMapping(standardModels, actualModels);

        // gpt-4o 应该能匹配到 gpt（前缀匹配）
        assertEquals("gpt", mapping.get("gpt-4o"), "gpt-4o 应该匹配到 gpt");

        // claude-4-sonnet 应该能匹配到 c4（版本相关匹配）
        assertEquals("c4", mapping.get("claude-4-sonnet"), "claude-4-sonnet 应该匹配到 c4");
    }

    @Test
    void testInvalidShortTargetMatch() {
        // 测试场景：验证不合理的长到短匹配被拒绝
        List<String> standardModels = Arrays.asList("claude-3.7-sonnet", "anthropic-claude");
        List<String> actualModels = Arrays.asList("o3", "xyz");

        Map<String, String> mapping = modelRedirectService.generateModelMapping(standardModels, actualModels);

        // 这些都应该被拒绝，因为不是合理的匹配
        assertFalse(mapping.containsKey("claude-3.7-sonnet"), "claude-3.7-sonnet 不应该匹配到 o3");
        assertFalse(mapping.containsKey("anthropic-claude"), "anthropic-claude 不应该匹配到 xyz");
    }

    @Test
    void testMixedModelNames() {
        // 测试场景：混合长短模型名称
        List<String> standardModels = Arrays.asList("o3", "claude-4-sonnet", "gpt-4o");
        List<String> actualModels = Arrays.asList(
                "o3-turbo",
                "claude-4-sonnet-20241120",
                "claude-3.7-sonnet",
                "gpt-4o-mini",
                "gpt-4o"
        );

        Map<String, String> mapping = modelRedirectService.generateModelMapping(standardModels, actualModels);

        // 短模型名称使用前缀匹配
        assertEquals("o3-turbo", mapping.get("o3"));

        // 长模型名称使用包含匹配
        assertEquals("claude-4-sonnet-20241120", mapping.get("claude-4-sonnet"));

        // gpt-4o 精确匹配，不需要映射
        assertFalse(mapping.containsKey("gpt-4o"));
    }

    @Test
    void testPreventDowngrade() {
        // 测试场景：防止降级匹配
        List<String> standardModels = Arrays.asList("gpt-4o");
        List<String> actualModels = Arrays.asList("gpt-4o-mini", "gpt-4o-nano");

        Map<String, String> mapping = modelRedirectService.generateModelMapping(standardModels, actualModels);

        // 不应该映射到降级版本
        assertFalse(mapping.containsKey("gpt-4o"), "gpt-4o 不应该被映射到降级版本");
    }

    @Test
    void testEmptyInputs() {
        // 测试场景：空输入
        Map<String, String> mapping1 = modelRedirectService.generateModelMapping(null, Arrays.asList("model1"));
        Map<String, String> mapping2 = modelRedirectService.generateModelMapping(Arrays.asList("model1"), null);
        Map<String, String> mapping3 = modelRedirectService.generateModelMapping(Arrays.asList(), Arrays.asList("model1"));

        assertTrue(mapping1.isEmpty(), "null standardModels 应该返回空映射");
        assertTrue(mapping2.isEmpty(), "null actualModels 应该返回空映射");
        assertTrue(mapping3.isEmpty(), "空 standardModels 应该返回空映射");
    }
@Test
    void testShortSourceToLongTarget_ShouldNotMatchIncorrectly() {
        // 测试场景：短源模型名不应该错误匹配到长目标模型名
        // 这是用户报告的具体问题：o3 不应该匹配到 claude-4-sonnet
        List<String> standardModels = Arrays.asList("o3", "o4");
        List<String> actualModels = Arrays.asList(
                "claude-4-sonnet",
                "claude-3-haiku", 
                "anthropic-claude-instant",
                "openai-gpt-4-turbo"
        );

        Map<String, String> mapping = modelRedirectService.generateModelMapping(standardModels, actualModels);

        // o3 不应该匹配到 claude-4-sonnet 或任何长模型名
        assertFalse(mapping.containsKey("o3"), "o3 不应该错误匹配到长模型名");
        assertFalse(mapping.containsKey("o4"), "o4 不应该错误匹配到长模型名");
    }

    @Test
    void testShortSourceToLongTarget_ValidMatch() {
        // 测试场景：短源模型名的合理匹配
        List<String> standardModels = Arrays.asList("o3", "gpt");
        List<String> actualModels = Arrays.asList(
                "o3-advanced-model",  // 应该匹配（单词边界）
                "gpt-4-turbo-preview" // 应该匹配（前缀）
        );

        Map<String, String> mapping = modelRedirectService.generateModelMapping(standardModels, actualModels);

        // 这些是合理的匹配
        assertEquals("o3-advanced-model", mapping.get("o3"), "o3 应该匹配到 o3-advanced-model");
        assertEquals("gpt-4-turbo-preview", mapping.get("gpt"), "gpt 应该匹配到 gpt-4-turbo-preview");
    }
}