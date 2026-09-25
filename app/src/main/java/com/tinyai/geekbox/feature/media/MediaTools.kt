package com.tinyai.geekbox.feature.media

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import com.tinyai.geekbox.core.model.ToolItem

object MediaTools {
    val items = listOf(
        ToolItem("media/audio", "音频解码", "常见音频 → WAV(PCM)", Icons.Filled.MusicNote),
        ToolItem("media/video", "视频信息", "分辨率 · 码率 · 编码", Icons.Filled.Movie)
    )
}
