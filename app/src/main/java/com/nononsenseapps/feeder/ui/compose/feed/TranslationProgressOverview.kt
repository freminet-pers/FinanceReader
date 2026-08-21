package com.nononsenseapps.feeder.ui.compose.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nononsenseapps.feeder.R
import com.nononsenseapps.feeder.model.TranslationJob
import com.nononsenseapps.feeder.model.TranslationJobStatus

/**
 * 已翻译（正文）文章 ID 集合，供列表右侧「已翻译」小圆点使用。
 * 由 FeedListContent / FeedGridContent 在渲染前 provide；搜索等其它场景默认空集。
 */
val LocalTranslatedItemIds = compositionLocalOf<Set<Long>> { emptySet() }

/**
 * 列表顶部翻译进度概览：显示「正在翻译几篇 · 已完成几篇」，
 * 点击展开详情列表；无任务时隐藏。
 */
@Composable
fun TranslationProgressOverview(
    jobs: List<TranslationJob>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (jobs.isEmpty()) {
        return
    }
    val runningCount = jobs.count { it.status == TranslationJobStatus.RUNNING }
    val doneCount = jobs.count { it.status == TranslationJobStatus.DONE }
    val failedCount = jobs.count { it.status == TranslationJobStatus.FAILED }
    var expanded by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .clickable { expanded = !expanded }
                        .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text =
                            when {
                                runningCount > 0 && doneCount > 0 ->
                                    stringResource(R.string.translation_progress_summary, runningCount, doneCount)
                                runningCount > 0 ->
                                    stringResource(R.string.translation_progress_translating, runningCount)
                                else ->
                                    stringResource(R.string.translation_progress_done, doneCount)
                            },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (failedCount > 0) {
                        Text(
                            text = stringResource(R.string.translation_progress_failed, failedCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.translation_progress_hide_details),
                    )
                }
            }
            if (runningCount > 0) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider()
                    jobs
                        .sortedBy { it.status != TranslationJobStatus.RUNNING }
                        .takeLast(MAX_DETAIL_ITEMS)
                        .asReversed()
                        .forEach { job ->
                            TranslationJobDetailRow(job)
                        }
                }
            }
        }
    }
}

@Composable
private fun TranslationJobDetailRow(job: TranslationJob) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        val (statusText, statusColor) =
            when (job.status) {
                TranslationJobStatus.RUNNING ->
                    stringResource(R.string.translation_progress_status_translating) to MaterialTheme.colorScheme.primary
                TranslationJobStatus.DONE ->
                    stringResource(R.string.translation_progress_status_done) to MaterialTheme.colorScheme.onSurfaceVariant
                TranslationJobStatus.FAILED ->
                    stringResource(R.string.translation_progress_status_failed) to MaterialTheme.colorScheme.error
            }
        Text(
            text = job.title.ifBlank { "#${job.itemId}" },
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelSmall,
            color = statusColor,
        )
    }
}

/**
 * 已翻译文章小圆点：
 * - 已翻译且未读 → 主题主色（较亮）；
 * - 已翻译且已读 → 灰色。
 * 未翻译时不渲染。
 */
@Composable
fun TranslatedArticleDot(
    isTranslated: Boolean,
    isUnread: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!isTranslated) {
        return
    }
    val color =
        if (isUnread) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline
        }
    val description =
        if (isUnread) {
            stringResource(R.string.translated_article_unread)
        } else {
            stringResource(R.string.translated_article_read)
        }
    Box(
        modifier =
            modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
                .semantics {
                    contentDescription = description
                },
    )
}

private const val MAX_DETAIL_ITEMS = 20
