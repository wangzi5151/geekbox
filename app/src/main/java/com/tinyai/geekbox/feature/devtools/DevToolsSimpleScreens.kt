package com.tinyai.geekbox.feature.devtools

import androidx.compose.runtime.Composable
import com.tinyai.geekbox.core.util.Codecs

@Composable
fun Base64Screen(onBack: () -> Unit) {
    ConverterScreen(
        title = "Base64",
        onBack = onBack,
        inputHint = "输入文本或 Base64",
        transform = { Codecs.base64Encode(it, noWrap = true) },
        reverse = { Codecs.base64Decode(it) }
    )
}

@Composable
fun HexScreen(onBack: () -> Unit) {
    ConverterScreen(
        title = "HEX",
        onBack = onBack,
        inputHint = "输入文本或十六进制",
        transform = { Codecs.hexEncode(it) },
        reverse = { Codecs.hexDecode(it) }
    )
}

@Composable
fun UrlScreen(onBack: () -> Unit) {
    ConverterScreen(
        title = "URL",
        onBack = onBack,
        inputHint = "输入原始文本或 URL 编码",
        transform = { Codecs.urlEncode(it) },
        reverse = { Codecs.urlDecode(it) }
    )
}

@Composable
fun XmlScreen(onBack: () -> Unit) {
    ConverterScreen(
        title = "XML",
        onBack = onBack,
        inputHint = "粘贴 XML 内容",
        transform = { XmlFormatter.pretty(it) }
    )
}
