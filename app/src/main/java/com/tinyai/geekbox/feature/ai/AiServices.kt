package com.tinyai.geekbox.feature.ai

data class AiService(
    val name: String,
    val url: String,
    val desc: String
)

object AiServices {
    val list = listOf(
        AiService("DeepSeek", "https://chat.deepseek.com/", "深度求索 · 推理"),
        AiService("豆包", "https://www.doubao.com/chat/", "字节跳动 · 豆包"),
        AiService("通义千问", "https://www.tongyi.com/", "阿里 · 通义"),
        AiService("文心一言", "https://yiyan.baidu.com/", "百度 · 文心"),
        AiService("Kimi", "https://kimi.moonshot.cn/", "月之暗面 · 长文本"),
        AiService("腾讯元宝", "https://yuanbao.tencent.com/", "腾讯 · 混元"),
        AiService("智谱清言", "https://chatglm.cn/", "智谱 · GLM"),
        AiService("讯飞星火", "https://xinghuo.xfyun.cn/", "科大讯飞 · 星火"),
        AiService("海螺 AI", "https://hailuoai.com/", "MiniMax · 海螺"),
        AiService("阶跃 AI", "https://yuewen.cn/", "阶跃星辰 · 跃问"),
        AiService("天工 AI", "https://www.tiangong.cn/", "昆仑万维 · 天工"),
        AiService("百小应", "https://ying.baichuan-ai.com/", "百川智能")
    )
}
