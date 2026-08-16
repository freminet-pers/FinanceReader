package com.nononsenseapps.feeder.model

import android.content.Context
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LanguageDetectorTest {
    private val detector = LanguageDetector(mockk<Context>())

    @Test
    fun detectsEnglish() {
        assertEquals(
            "en",
            detector.detectLanguageTag(
                "The Federal Reserve raised interest rates by 25 basis points, citing stubborn inflation.",
            ),
        )
    }

    @Test
    fun detectsSimplifiedChinese() {
        assertEquals(
            "zh-Hans",
            detector.detectLanguageTag(
                "美联储宣布加息二十五个基点，并表示通胀仍然顽固。这国经济数据引发了市场的关注。",
            ),
        )
    }

    @Test
    fun detectsTraditionalChinese() {
        assertEquals(
            "zh-Hant",
            detector.detectLanguageTag(
                "美聯儲宣布加息二十五個基點，並表示通脹仍然頑固。這國經濟數據引發了市場的關注。",
            ),
        )
    }

    @Test
    fun detectsJapanese() {
        assertEquals(
            "ja",
            detector.detectLanguageTag(
                "日本銀行は政策金利を据え置き、物価上昇率の見通しを上方修正しました。",
            ),
        )
    }

    @Test
    fun detectsKorean() {
        assertEquals(
            "ko",
            detector.detectLanguageTag(
                "한국은행은 기준금리를 동결하고 물가 상승률 전망을 상향 조정했습니다.",
            ),
        )
    }

    @Test
    fun detectsRussian() {
        assertEquals(
            "ru",
            detector.detectLanguageTag(
                "Центральный банк России сохранил ключевую ставку на прежнем уровне.",
            ),
        )
    }

    @Test
    fun detectsFrench() {
        assertEquals(
            "fr",
            detector.detectLanguageTag(
                "La Banque centrale européenne a annoncé une nouvelle hausse des taux d'intérêt.",
            ),
        )
    }

    @Test
    fun returnsNullForEmptyText() {
        assertNull(detector.detectLanguageTag(""))
        assertNull(detector.detectLanguageTag("1234567890"))
    }

    @Test
    fun mapsLanguageNames() {
        assertEquals("English", detector.languageName("en"))
        assertEquals("Simplified Chinese", detector.languageName("zh-Hans"))
        assertEquals("Traditional Chinese", detector.languageName("zh-Hant"))
        assertEquals("Japanese", detector.languageName("ja"))
        assertEquals("Korean", detector.languageName("ko"))
        assertEquals("custom", detector.languageName("custom"))
    }
}
