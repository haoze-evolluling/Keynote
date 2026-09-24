package com.haoze.keynote.data.remote

/**
 * 一个 OpenAI 兼容厂商的预设：只登记厂商名与接口地址，[baseUrl] 必须能直接拼出
 * `{baseUrl}/chat/completions`。
 *
 * 这里刻意不含任何模型 ID：型号上下线太快，写死必然会过期。可用模型一律由
 * [AiModelFetcher] 现场请求 `GET {baseUrl}/models` 取得。
 */
data class AiProviderPreset(
    val id: String,
    val name: String,
    val group: String,
    val baseUrl: String
)

/**
 * 内置 AI 厂商目录，供厂商管理页点选后预填接口地址。各 [AiProviderPreset.baseUrl] 均核对自厂商
 * 官方文档，核对时间为 2026-09-24，后续维护按此判断是否需要更新。
 * 收录前提：厂商暴露可用的 OpenAI 兼容 `chat/completions`。因此以原生协议为主的
 * Anthropic、腾讯混元未收录；零一万物因平台自 2026-08 起逐步停止 API 服务同样不收录。
 * 地址或型号有变时，用户可在编辑框里直接改动 [AiProviderPreset.baseUrl] 预填值。
 */
object AiProviderPresets {

    private const val GROUP_MAINLAND = "国内厂商"
    private const val GROUP_OVERSEAS = "海外厂商"
    private const val GROUP_AGGREGATOR = "聚合与本地部署"

    val all: List<AiProviderPreset> = listOf(
        AiProviderPreset("deepseek", "DeepSeek", GROUP_MAINLAND, "https://api.deepseek.com"),
        AiProviderPreset("zhipu", "智谱 GLM", GROUP_MAINLAND, "https://open.bigmodel.cn/api/paas/v4"),
        AiProviderPreset("moonshot", "Kimi（月之暗面）", GROUP_MAINLAND, "https://api.moonshot.cn/v1"),
        AiProviderPreset(
            "dashscope",
            "阿里云百炼（通义千问）",
            GROUP_MAINLAND,
            // 官方新文档改用带业务空间 ID 的分区域主机，此处保留通用主机以便直接填 Key
            "https://dashscope.aliyuncs.com/compatible-mode/v1"
        ),
        AiProviderPreset("volcengine-ark", "火山方舟（豆包）", GROUP_MAINLAND, "https://ark.cn-beijing.volces.com/api/v3"),
        // 国内账号可改用 https://api.minimax.cn 下的同名兼容端点
        AiProviderPreset("minimax", "MiniMax", GROUP_MAINLAND, "https://api.minimax.io/v1"),
        AiProviderPreset("qianfan", "百度千帆（ERNIE）", GROUP_MAINLAND, "https://qianfan.baidubce.com/v2"),
        AiProviderPreset("stepfun", "阶跃星辰", GROUP_MAINLAND, "https://api.stepfun.com/v1"),
        AiProviderPreset("openai", "OpenAI", GROUP_OVERSEAS, "https://api.openai.com/v1"),
        AiProviderPreset(
            "gemini",
            "Google Gemini",
            GROUP_OVERSEAS,
            // 官方 OpenAI 兼容层，注意结尾的 /openai
            "https://generativelanguage.googleapis.com/v1beta/openai"
        ),
        AiProviderPreset("xai", "xAI Grok", GROUP_OVERSEAS, "https://api.x.ai/v1"),
        AiProviderPreset("mistral", "Mistral AI", GROUP_OVERSEAS, "https://api.mistral.ai/v1"),
        AiProviderPreset("groq", "Groq", GROUP_OVERSEAS, "https://api.groq.com/openai/v1"),
        AiProviderPreset("siliconflow", "硅基流动", GROUP_AGGREGATOR, "https://api.siliconflow.cn/v1"),
        AiProviderPreset("openrouter", "OpenRouter", GROUP_AGGREGATOR, "https://openrouter.ai/api/v1"),
        AiProviderPreset("ollama", "Ollama（本机）", GROUP_AGGREGATOR, "http://localhost:11434/v1")
    )
}
