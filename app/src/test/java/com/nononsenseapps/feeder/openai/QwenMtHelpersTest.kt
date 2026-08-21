package com.nononsenseapps.feeder.openai

import com.nononsenseapps.feeder.archmodel.OpenAISettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QwenMtHelpersTest {
    @Test
    fun detectsQwenMtModels() {
        assertTrue(OpenAISettings(modelId = "qwen-mt-flash").isQwenMtModel)
        assertTrue(OpenAISettings(modelId = "qwen-mt-plus").isQwenMtModel)
        assertTrue(OpenAISettings(modelId = "qwen-mt-turbo").isQwenMtModel)
        assertTrue(OpenAISettings(modelId = "QWEN-MT-FLASH").isQwenMtModel)
        assertFalse(OpenAISettings(modelId = "qwen-plus").isQwenMtModel)
        assertFalse(OpenAISettings(modelId = "deepseek-chat").isQwenMtModel)
        assertFalse(OpenAISettings(modelId = "gpt-4o").isQwenMtModel)
    }

    @Test
    fun mapsLanguageNamesAndCodes() {
        assertEquals("Chinese", toQwenMtLanguageName("Simplified Chinese"))
        assertEquals("Chinese", toQwenMtLanguageName("中文"))
        assertEquals("Chinese", toQwenMtLanguageName("zh"))
        assertEquals("Traditional Chinese", toQwenMtLanguageName("zh-TW"))
        assertEquals("English", toQwenMtLanguageName("English"))
        assertEquals("English", toQwenMtLanguageName("en"))
        assertEquals("Japanese", toQwenMtLanguageName("Japanese"))
        assertEquals("Japanese", toQwenMtLanguageName("ja"))
        assertEquals("Korean", toQwenMtLanguageName("Korean"))
        assertEquals("Russian", toQwenMtLanguageName("Russian"))
        assertEquals("auto", toQwenMtLanguageName("auto"))
        assertEquals("auto", toQwenMtLanguageName(""))
        assertNull(toQwenMtLanguageName("Klingon"))
    }

    @Test
    fun stripsHtmlToParagraphText() {
        val html = "<p>First paragraph with <b>bold</b>.</p><p>Second paragraph.</p>"
        val text = htmlToTranslatableText(html)
        assertTrue(text.contains("First paragraph"))
        assertTrue(text.contains("Second paragraph"))
        // Block tags become paragraph breaks
        assertTrue(text.contains("\n"))
    }

    @Test
    fun wrapsPlainTextInParagraphs() {
        val result = wrapPlainTextInParagraphs("段落一\n\n段落二")
        assertEquals("<p>段落一</p>\n<p>段落二</p>", result)
    }
}
