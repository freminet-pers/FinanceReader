package com.nononsenseapps.feeder.openai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChunkTranslationContentTest {
    @Test
    fun shortContentStaysSingleChunk() {
        val content = "short"
        assertEquals(listOf("short"), chunkTranslationContent(content, preserveHtml = false))
    }

    @Test
    fun textSplitsAtSentenceBoundaries() {
        val sentence = "The Federal Reserve raised rates by 25 basis points. "
        val content = sentence.repeat(200) // > 3000 chars
        val chunks = chunkTranslationContent(content, preserveHtml = false)
        assertTrue(chunks.size > 1)
        assertTrue(chunks.all { it.length <= 3000 + 200 })
        assertEquals(content, chunks.joinToString(""))
    }

    @Test
    fun htmlSplitsAtBlockTags() {
        val block = "<p>The market rallied strongly today, with the S&P 500 gaining 1.2%.</p>"
        val content = block.repeat(60) // > 3000 chars
        val chunks = chunkTranslationContent(content, preserveHtml = true)
        assertTrue(chunks.size > 1)
        assertTrue(chunks.all { it.length <= 3000 + 200 })
        assertEquals(content, chunks.joinToString(""))
        // 每块以完整标签结尾，无半截标签
        assertTrue(chunks.all { it.endsWith("</p>") })
    }

    @Test
    fun htmlWithBreakVariantsSplitsWithoutCrash() {
        // 回归：<br/> 变体曾触发「look-behind 必须有界」的 PatternSyntaxException
        val block = "<p>Paragraph text with a line break<br/>continued here.</p>"
        val content = block.repeat(80) // > 3000 chars
        val chunks = chunkTranslationContent(content, preserveHtml = true)
        assertTrue(chunks.size >= 2)
        val normalized = content.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "<br>")
        assertEquals(normalized, chunks.joinToString(""))
        assertTrue(chunks.all { it.length <= 3000 + 200 })
    }

    @Test
    fun oversizedSinglePieceIsHardSplit() {
        val longSentence = "This is a very long piece without any paragraph breaks. ".repeat(80)
        val content = longSentence + "Tail content."
        val chunks = chunkTranslationContent(content, preserveHtml = false)
        assertTrue(chunks.size >= 2)
        assertTrue(chunks.all { it.length <= 3000 + 200 })
        assertEquals(content, chunks.joinToString(""))
    }
}
