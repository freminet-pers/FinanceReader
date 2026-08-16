package com.nononsenseapps.feeder.model

import android.content.Context
import android.os.Build
import com.github.pemistahl.lingua.api.Language
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder
import java.util.Locale

/**
 * 无 GMS 的语言自动识别管线（详见 ARCHITECTURE.md §5）：
 * ① Unicode 文字脚本粗筛（假名→日文、谚文→韩文、纯汉字→中文、西里尔→俄/乌组、拉丁→拉丁组）
 * ② 系统 TextClassifier.detectLanguage（API 29+，AOSP 原生）
 * ③ pemistahl/lingua 兜底（限定语言子集，纯 JVM）
 * ④ 简繁特征字表区分 zh-Hans / zh-Hant
 * 返回 BCP-47 风格语言标签（"en"、"zh-Hans"、"ja"…），无法判定时返回 null。
 */
class LanguageDetector(
    private val context: Context,
) {
    /** 识别文本语言标签；文本过短或无字母时返回 null。 */
    fun detectLanguageTag(text: String): String? {
        // 采样前 2000 个字母字符，长文够用且省时
        val sample =
            buildString {
                var letters = 0
                for (ch in text) {
                    if (ch.isLetter()) {
                        append(ch)
                        letters++
                        if (letters >= MAX_SAMPLE_LETTERS) {
                            break
                        }
                    }
                }
            }
        if (sample.isEmpty()) {
            return null
        }
        return detectFromLetters(sample)
    }

    fun languageName(tag: String): String =
        when (tag) {
            "en" -> "English"
            "zh", "zh-Hans" -> "Simplified Chinese"
            "zh-Hant" -> "Traditional Chinese"
            "ja" -> "Japanese"
            "ko" -> "Korean"
            "ru" -> "Russian"
            "uk" -> "Ukrainian"
            "fr" -> "French"
            "de" -> "German"
            "es" -> "Spanish"
            "pt" -> "Portuguese"
            "it" -> "Italian"
            "nl" -> "Dutch"
            else -> tag
        }

    private fun detectFromLetters(sample: String): String? {
        var han = 0
        var hiragana = 0
        var katakana = 0
        var hangul = 0
        var cyrillic = 0
        var latin = 0
        val total = sample.length

        for (ch in sample) {
            when {
                ch.isHan() -> han++
                ch.isHiragana() -> hiragana++
                ch.isKatakana() -> katakana++
                ch.isHangul() -> hangul++
                ch.isCyrillic() -> cyrillic++
                ch.isLatin() -> latin++
            }
        }

        // ① 脚本粗筛（确定性，零依赖）
        if (hiragana > 0 || katakana > 0) {
            return "ja"
        }
        if (hangul * 2 >= total) {
            return "ko"
        }
        if (han > 0 && (han * 2 >= total || (hiragana + katakana + hangul + cyrillic + latin) == 0)) {
            return refineChineseScript(sample)
        }
        if (cyrillic * 2 >= total) {
            return linguaDetect(sample, CYRILLIC_LANGUAGES)?.let { linguaTag(it) }
        }
        if (latin > 0 && (latin * 2 >= total || han == 0)) {
            // ② 系统 TextClassifier（API 29+，本设备无 GMS 亦可用）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val frameworkTag =
                    runCatching {
                        context
                            .detectLocaleFromText(text = sample, minConfidence = 80.0f)
                            .firstOrNull()
                            ?.locale
                            ?.let(::normalizeFrameworkTag)
                    }.getOrNull()
                if (frameworkTag != null && frameworkTag != "und") {
                    if (frameworkTag.startsWith("zh")) {
                        return refineChineseScript(sample)
                    }
                    return frameworkTag
                }
            }
            // ③ lingua 兜底
            return linguaDetect(sample, LATIN_LANGUAGES)?.let { linguaTag(it) }
        }
        return null
    }

    /** ④ 简繁特征字表（仅汉字文本）。 */
    private fun refineChineseScript(sample: String): String {
        var simplified = 0
        var traditional = 0
        for (ch in sample) {
            if (ch in SIMPLIFIED_ONLY_CHARS) {
                simplified++
            } else if (ch in TRADITIONAL_ONLY_CHARS) {
                traditional++
            }
        }
        return when {
            traditional > simplified -> "zh-Hant"
            else -> "zh-Hans"
        }
    }

    private fun normalizeFrameworkTag(locale: Locale): String {
        val language = locale.language
        return if (language.isEmpty()) "und" else language
    }

    private fun linguaTag(language: Language): String =
        when (language) {
            Language.ENGLISH -> "en"
            Language.FRENCH -> "fr"
            Language.GERMAN -> "de"
            Language.SPANISH -> "es"
            Language.PORTUGUESE -> "pt"
            Language.ITALIAN -> "it"
            Language.DUTCH -> "nl"
            Language.RUSSIAN -> "ru"
            Language.UKRAINIAN -> "uk"
            Language.BULGARIAN -> "bg"
            else -> language.name.lowercase(Locale.ROOT)
        }

    private fun linguaDetect(
        text: String,
        languages: List<Language>,
    ): Language? {
        val detector =
            linguaDetectors.getOrPut(languages) {
                LanguageDetectorBuilder
                    .fromLanguages(*languages.toTypedArray())
                    .withMinimumRelativeDistance(0.0)
                    .build()
            }
        return runCatching { detector.detectLanguageOf(text) }.getOrNull()
    }

    private val linguaDetectors = mutableMapOf<List<Language>, com.github.pemistahl.lingua.api.LanguageDetector>()

    private fun Char.isHan(): Boolean =
        code in 0x4E00..0x9FFF ||
            code in 0x3400..0x4DBF ||
            code in 0xF900..0xFAFF

    private fun Char.isHiragana(): Boolean = code in 0x3040..0x309F

    private fun Char.isKatakana(): Boolean = code in 0x30A0..0x30FF

    private fun Char.isHangul(): Boolean =
        code in 0xAC00..0xD7AF ||
            code in 0x1100..0x11FF ||
            code in 0x3130..0x318F

    private fun Char.isCyrillic(): Boolean = code in 0x0400..0x04FF

    private fun Char.isLatin(): Boolean =
        code in 0x0041..0x005A ||
            code in 0x0061..0x007A

    companion object {
        private const val MAX_SAMPLE_LETTERS = 2000

        private val LATIN_LANGUAGES =
            listOf(
                Language.ENGLISH,
                Language.FRENCH,
                Language.GERMAN,
                Language.SPANISH,
                Language.PORTUGUESE,
                Language.ITALIAN,
                Language.DUTCH,
            )

        private val CYRILLIC_LANGUAGES =
            listOf(
                Language.RUSSIAN,
                Language.UKRAINIAN,
                Language.BULGARIAN,
            )

        private val SIMPLIFIED_ONLY_CHARS = "们这国为发对长门问过还说让认识谁谢钱铁页飞马万与后里无进开时东点".toSet()
        private val TRADITIONAL_ONLY_CHARS = "們這國為發對長門問過還說讓認識誰謝錢鐵頁飛馬萬與後裡無進開時東點".toSet()
    }
}
