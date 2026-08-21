package com.nononsenseapps.feeder.model

import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

/** 单篇翻译任务的状态。 */
enum class TranslationJobStatus {
    /** 排队中或正在翻译。 */
    RUNNING,

    /** 翻译成功。 */
    DONE,

    /** 翻译失败。 */
    FAILED,
}

/** 单篇翻译任务（进度概览用）。 */
@Immutable
data class TranslationJob(
    val itemId: Long,
    val title: String,
    val status: TranslationJobStatus,
)

/**
 * 全局翻译进度与「已翻译文章」标记的单一事实来源。
 *
 * - 进度：任何翻译（列表卡片标题/摘要、文章正文）开始时 [startJob]，结束时 [finishJob]；
 *   供 Feed 列表顶部的小 UI 展示「正在翻译几篇、已完成几篇」与详情。
 * - 已翻译标记：正文翻译成功后 [markTranslated]，持久化到 SharedPreferences，
 *   供列表在文章右侧画「已翻译」小圆点（未读=亮色，已读=灰色）。
 *
 * 注意：[markTranslated] 只标记「正文翻译完成」的文章，不标记仅翻译了标题/摘要的卡片。
 */
class TranslationProgressStore(
    override val di: DI,
) : DIAware {
    private val sp: SharedPreferences by instance()

    private val _jobs = MutableStateFlow<List<TranslationJob>>(emptyList())
    val jobs: StateFlow<List<TranslationJob>> = _jobs.asStateFlow()

    private val _translatedItemIds =
        MutableStateFlow(
            sp
                .getStringSet(PREF_TRANSLATED_ITEM_IDS, emptySet())
                ?.mapNotNullTo(mutableSetOf()) { it.toLongOrNull() }
                ?: emptySet(),
        )
    val translatedItemIds: StateFlow<Set<Long>> = _translatedItemIds.asStateFlow()

    fun startJob(
        itemId: Long,
        title: String,
    ) {
        if (itemId <= 0) {
            return
        }
        _jobs.update { jobs ->
            // 同一文章先移除旧的 RUNNING 记录，避免重复计数
            val withoutDuplicate =
                jobs.filterNot { it.itemId == itemId && it.status == TranslationJobStatus.RUNNING }
            (withoutDuplicate + TranslationJob(itemId, title, TranslationJobStatus.RUNNING))
                .takeLast(MAX_JOBS)
        }
    }

    fun finishJob(
        itemId: Long,
        success: Boolean,
    ) {
        if (itemId <= 0) {
            return
        }
        _jobs.update { jobs ->
            jobs
                .map { job ->
                    if (job.itemId == itemId && job.status == TranslationJobStatus.RUNNING) {
                        job.copy(
                            status =
                                if (success) {
                                    TranslationJobStatus.DONE
                                } else {
                                    TranslationJobStatus.FAILED
                                },
                        )
                    } else {
                        job
                    }
                }.takeLast(MAX_JOBS)
        }
    }

    /** 记录某文章「正文已翻译」，并持久化（供小圆点标记）。 */
    fun markTranslated(itemId: Long) {
        if (itemId <= 0) {
            return
        }
        if (_translatedItemIds.value.contains(itemId)) {
            return
        }
        _translatedItemIds.update { it + itemId }
        sp
            .edit()
            .putStringSet(
                PREF_TRANSLATED_ITEM_IDS,
                _translatedItemIds.value.mapToStringSet(),
            ).apply()
    }

    fun isTranslated(itemId: Long): Boolean = itemId in _translatedItemIds.value

    /** 移除已结束（成功/失败）的任务，保留仍在运行的；用于列表顶部概览的关闭按钮。 */
    fun dismissFinishedJobs() {
        _jobs.update { jobs ->
            jobs.filter { it.status == TranslationJobStatus.RUNNING }
        }
    }

    companion object {
        private const val PREF_TRANSLATED_ITEM_IDS = "pref_translated_item_ids"

        /** 保留最近的任务数（概览详情用）。 */
        private const val MAX_JOBS = 50
    }
}

private fun Set<Long>.mapToStringSet(): Set<String> = mapTo(mutableSetOf()) { it.toString() }
