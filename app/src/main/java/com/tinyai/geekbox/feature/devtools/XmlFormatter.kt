package com.tinyai.geekbox.feature.devtools

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.io.StringWriter

object XmlFormatter {

    fun pretty(input: String, indent: Int = 2): String {
        if (input.isBlank()) return ""
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(input))
        val writer = StringWriter()
        val serializer = Xml.newSerializer()
        serializer.setOutput(writer)
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    serializer.startTag(parser.namespace, parser.name)
                    for (i in 0 until parser.attributeCount) {
                        serializer.attribute(parser.getAttributeNamespace(i), parser.getAttributeName(i), parser.getAttributeValue(i))
                    }
                }
                XmlPullParser.END_TAG -> serializer.endTag(parser.namespace, parser.name)
                XmlPullParser.TEXT -> serializer.text(parser.text)
                XmlPullParser.CDSECT -> serializer.cdsect(parser.text)
                XmlPullParser.COMMENT -> serializer.comment(parser.text)
                XmlPullParser.DOCDECL -> serializer.docdecl(parser.text)
                XmlPullParser.ENTITY_REF -> serializer.entityRef(parser.text)
                XmlPullParser.PROCESSING_INSTRUCTION -> serializer.processingInstruction(parser.text)
                else -> Unit
            }
            event = parser.next()
        }
        serializer.flush()
        return writer.toString().trim()
    }
}
