<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  compareTaskReportVersions,
  fetchTaskReportVersion,
  fetchTaskReportVersions
} from '../../api/task'
import type {
  ReportVersion,
  ReportVersionCompare,
  ReportVersionFieldChange,
  ReportVersionItemChange
} from '../../types/task'
import { formatDateTime } from '../../utils/format'

const props = defineProps<{
  taskId: string
}>()

const text = {
  title: '报告历史版本',
  versionCount: '版本数',
  refresh: '刷新版本',
  versionNo: '版本',
  snapshotSource: '快照来源',
  createdAt: '创建时间',
  actions: '操作',
  viewDetail: '查看详情',
  compareTitle: '版本对比',
  fromVersion: '起始版本',
  toVersion: '目标版本',
  compare: '执行对比',
  compareResult: '对比结果',
  noVersions: '暂无历史版本',
  noVersionDetail: '版本不存在或不属于当前任务',
  noCompareResult: '请选择两个版本执行对比',
  sameVersionNoop: '同版本对比无变化',
  noChanges: '两个版本无差异',
  hasChanges: '检测到版本差异',
  detailTitle: '版本详情',
  reportSnapshot: '报告快照',
  rawSnapshot: '原始快照',
  reportFieldsChanged: '报告字段变化',
  sectionsAdded: '新增章节',
  sectionsRemoved: '移除章节',
  sectionsChanged: '章节字段变化',
  evidenceRefsAdded: '新增证据',
  evidenceRefsRemoved: '移除证据',
  evidenceRefsChanged: '证据字段变化',
  reviewFieldsChanged: '审核字段变化',
  emptyBucket: '暂无变化',
  emptyValue: '-',
  loadVersionsFailed: '报告版本加载失败',
  loadVersionsError: '报告版本加载异常',
  loadVersionFailed: '报告版本详情加载失败',
  loadVersionError: '报告版本详情加载异常',
  compareFailed: '报告版本对比失败',
  compareError: '报告版本对比异常'
} as const

const loadingVersions = ref(false)
const loadingDetail = ref(false)
const comparing = ref(false)
const versions = ref<ReportVersion[]>([])
const selectedVersion = ref<ReportVersion | null>(null)
const detailVisible = ref(false)
const fromVersionNo = ref<number | null>(null)
const toVersionNo = ref<number | null>(null)
const compareResult = ref<ReportVersionCompare | null>(null)
const compareMissing = ref(false)

const sortedVersions = computed(() => {
  return [...versions.value].sort((left, right) => (right.versionNo || 0) - (left.versionNo || 0))
})

const versionOptions = computed(() => {
  return sortedVersions.value.map((version) => ({
    label: `v${version.versionNo}`,
    value: version.versionNo
  }))
})

const selectedReportSnapshot = computed(() => {
  const snapshot = selectedVersion.value?.snapshot
  if (!snapshot || typeof snapshot !== 'object') return {}
  const report = (snapshot as Record<string, any>).report
  return report && typeof report === 'object' ? report as Record<string, any> : {}
})

const compareSummary = computed(() => {
  if (compareMissing.value) {
    return {
      title: text.noVersionDetail,
      type: 'warning' as const
    }
  }

  if (!compareResult.value) {
    return {
      title: text.noCompareResult,
      type: 'info' as const
    }
  }

  if (compareResult.value.sameVersion) {
    return {
      title: text.sameVersionNoop,
      type: 'success' as const
    }
  }

  return {
    title: compareResult.value.changed ? text.hasChanges : text.noChanges,
    type: compareResult.value.changed ? 'warning' as const : 'success' as const
  }
})

function displayValue(value: unknown) {
  if (value === undefined || value === null || value === '') {
    return text.emptyValue
  }
  if (Array.isArray(value)) {
    return value.length ? value.join('；') : text.emptyValue
  }
  if (typeof value === 'object') {
    try {
      return JSON.stringify(value, null, 2)
    } catch {
      return String(value)
    }
  }
  return String(value)
}

function stringifyJson(value: unknown) {
  if (value === undefined || value === null) {
    return text.emptyValue
  }
  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value)
  }
}

function formatPath(change: ReportVersionFieldChange) {
  return [change.path, change.field].filter(Boolean).join('.') || text.emptyValue
}

function formatItemKey(item: ReportVersionItemChange) {
  return item.key || text.emptyValue
}

function resetCompareSelection() {
  const options = versionOptions.value
  if (options.length === 0) {
    fromVersionNo.value = null
    toVersionNo.value = null
    return
  }

  const latestOption = options[0]
  if (!latestOption) {
    fromVersionNo.value = null
    toVersionNo.value = null
    return
  }
  toVersionNo.value = latestOption.value
  fromVersionNo.value = options[1]?.value ?? latestOption.value
}

async function loadVersions() {
  if (!props.taskId) {
    versions.value = []
    resetCompareSelection()
    return
  }

  loadingVersions.value = true
  compareResult.value = null
  compareMissing.value = false
  selectedVersion.value = null
  try {
    const res = await fetchTaskReportVersions(props.taskId)
    if (res.success) {
      versions.value = res.data || []
      resetCompareSelection()
    } else {
      versions.value = []
      resetCompareSelection()
      ElMessage.error(res.message || text.loadVersionsFailed)
    }
  } catch (e: any) {
    versions.value = []
    resetCompareSelection()
    ElMessage.error(e?.message || text.loadVersionsError)
  } finally {
    loadingVersions.value = false
  }
}

async function openVersionDetail(versionNo: number) {
  loadingDetail.value = true
  selectedVersion.value = null
  detailVisible.value = true
  try {
    const res = await fetchTaskReportVersion(props.taskId, versionNo)
    if (res.success) {
      selectedVersion.value = res.data || null
      if (!res.data) {
        ElMessage.warning(text.noVersionDetail)
      }
    } else {
      ElMessage.error(res.message || text.loadVersionFailed)
    }
  } catch (e: any) {
    ElMessage.error(e?.message || text.loadVersionError)
  } finally {
    loadingDetail.value = false
  }
}

async function runCompare() {
  if (!fromVersionNo.value || !toVersionNo.value) {
    compareResult.value = null
    compareMissing.value = false
    return
  }

  comparing.value = true
  compareResult.value = null
  compareMissing.value = false
  try {
    const res = await compareTaskReportVersions(props.taskId, fromVersionNo.value, toVersionNo.value)
    if (res.success) {
      compareResult.value = res.data || null
      compareMissing.value = !res.data
      if (!res.data) {
        ElMessage.warning(text.noVersionDetail)
      }
    } else {
      ElMessage.error(res.message || text.compareFailed)
    }
  } catch (e: any) {
    ElMessage.error(e?.message || text.compareError)
  } finally {
    comparing.value = false
  }
}

watch(
  () => props.taskId,
  () => {
    loadVersions()
  },
  { immediate: true }
)
</script>

<template>
  <el-card class="report-version-panel">
    <template #header>
      <div class="panel-header">
        <div>
          <div class="panel-title">{{ text.title }}</div>
          <div class="panel-subtitle">{{ text.compareTitle }}</div>
        </div>
        <div class="panel-actions">
          <el-tag type="info">{{ text.versionCount }} {{ sortedVersions.length }}</el-tag>
          <el-button :loading="loadingVersions" @click="loadVersions">{{ text.refresh }}</el-button>
        </div>
      </div>
    </template>

    <el-table
      v-loading="loadingVersions"
      :data="sortedVersions"
      border
      empty-text="暂无历史版本"
    >
      <el-table-column :label="text.versionNo" width="90">
        <template #default="{ row }">
          <el-tag type="primary" effect="plain">v{{ row.versionNo }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="snapshotSource" :label="text.snapshotSource" min-width="140">
        <template #default="{ row }">{{ row.snapshotSource || text.emptyValue }}</template>
      </el-table-column>
      <el-table-column :label="text.createdAt" min-width="180">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column :label="text.actions" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openVersionDetail(row.versionNo)">
            {{ text.viewDetail }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="compare-toolbar">
      <el-select
        v-model="fromVersionNo"
        :placeholder="text.fromVersion"
        style="width: 180px;"
      >
        <el-option
          v-for="option in versionOptions"
          :key="`from-${option.value}`"
          :label="option.label"
          :value="option.value"
        />
      </el-select>
      <el-select
        v-model="toVersionNo"
        :placeholder="text.toVersion"
        style="width: 180px;"
      >
        <el-option
          v-for="option in versionOptions"
          :key="`to-${option.value}`"
          :label="option.label"
          :value="option.value"
        />
      </el-select>
      <el-button
        type="primary"
        :loading="comparing"
        :disabled="!fromVersionNo || !toVersionNo"
        @click="runCompare"
      >
        {{ text.compare }}
      </el-button>
    </div>

    <el-alert
      :title="compareSummary.title"
      :type="compareSummary.type"
      :closable="false"
      show-icon
      class="compare-alert"
    />

    <template v-if="compareResult">
      <el-descriptions :column="3" border class="compare-meta">
        <el-descriptions-item :label="text.fromVersion">
          v{{ compareResult.fromVersionNo }}
        </el-descriptions-item>
        <el-descriptions-item :label="text.toVersion">
          v{{ compareResult.toVersionNo }}
        </el-descriptions-item>
        <el-descriptions-item :label="text.snapshotSource">
          {{ compareResult.fromVersion?.snapshotSource || text.emptyValue }} → {{ compareResult.toVersion?.snapshotSource || text.emptyValue }}
        </el-descriptions-item>
      </el-descriptions>

      <el-collapse class="diff-collapse">
        <el-collapse-item :title="text.reportFieldsChanged" name="report-fields">
          <el-empty
            v-if="!compareResult.reportFieldsChanged?.length"
            :description="text.emptyBucket"
          />
          <el-table v-else :data="compareResult.reportFieldsChanged" border>
            <el-table-column label="Path" min-width="180">
              <template #default="{ row }">{{ formatPath(row) }}</template>
            </el-table-column>
            <el-table-column label="From" min-width="220">
              <template #default="{ row }">
                <pre class="value-pre">{{ displayValue(row.fromValue) }}</pre>
              </template>
            </el-table-column>
            <el-table-column label="To" min-width="220">
              <template #default="{ row }">
                <pre class="value-pre">{{ displayValue(row.toValue) }}</pre>
              </template>
            </el-table-column>
          </el-table>
        </el-collapse-item>

        <el-collapse-item :title="text.sectionsAdded" name="sections-added">
          <el-empty v-if="!compareResult.sectionsAdded?.length" :description="text.emptyBucket" />
          <el-table v-else :data="compareResult.sectionsAdded" border>
            <el-table-column label="Key" width="180">
              <template #default="{ row }">{{ formatItemKey(row) }}</template>
            </el-table-column>
            <el-table-column label="Value">
              <template #default="{ row }"><pre class="value-pre">{{ displayValue(row.value) }}</pre></template>
            </el-table-column>
          </el-table>
        </el-collapse-item>

        <el-collapse-item :title="text.sectionsRemoved" name="sections-removed">
          <el-empty v-if="!compareResult.sectionsRemoved?.length" :description="text.emptyBucket" />
          <el-table v-else :data="compareResult.sectionsRemoved" border>
            <el-table-column label="Key" width="180">
              <template #default="{ row }">{{ formatItemKey(row) }}</template>
            </el-table-column>
            <el-table-column label="Value">
              <template #default="{ row }"><pre class="value-pre">{{ displayValue(row.value) }}</pre></template>
            </el-table-column>
          </el-table>
        </el-collapse-item>

        <el-collapse-item :title="text.sectionsChanged" name="sections-changed">
          <el-empty v-if="!compareResult.sectionsChanged?.length" :description="text.emptyBucket" />
          <el-table v-else :data="compareResult.sectionsChanged" border>
            <el-table-column label="Path" min-width="180">
              <template #default="{ row }">{{ formatPath(row) }}</template>
            </el-table-column>
            <el-table-column label="From" min-width="220">
              <template #default="{ row }"><pre class="value-pre">{{ displayValue(row.fromValue) }}</pre></template>
            </el-table-column>
            <el-table-column label="To" min-width="220">
              <template #default="{ row }"><pre class="value-pre">{{ displayValue(row.toValue) }}</pre></template>
            </el-table-column>
          </el-table>
        </el-collapse-item>

        <el-collapse-item :title="text.evidenceRefsAdded" name="evidence-added">
          <el-empty v-if="!compareResult.evidenceRefsAdded?.length" :description="text.emptyBucket" />
          <el-table v-else :data="compareResult.evidenceRefsAdded" border>
            <el-table-column label="Key" width="180">
              <template #default="{ row }">{{ formatItemKey(row) }}</template>
            </el-table-column>
            <el-table-column label="Value">
              <template #default="{ row }"><pre class="value-pre">{{ displayValue(row.value) }}</pre></template>
            </el-table-column>
          </el-table>
        </el-collapse-item>

        <el-collapse-item :title="text.evidenceRefsRemoved" name="evidence-removed">
          <el-empty v-if="!compareResult.evidenceRefsRemoved?.length" :description="text.emptyBucket" />
          <el-table v-else :data="compareResult.evidenceRefsRemoved" border>
            <el-table-column label="Key" width="180">
              <template #default="{ row }">{{ formatItemKey(row) }}</template>
            </el-table-column>
            <el-table-column label="Value">
              <template #default="{ row }"><pre class="value-pre">{{ displayValue(row.value) }}</pre></template>
            </el-table-column>
          </el-table>
        </el-collapse-item>

        <el-collapse-item :title="text.evidenceRefsChanged" name="evidence-changed">
          <el-empty v-if="!compareResult.evidenceRefsChanged?.length" :description="text.emptyBucket" />
          <el-table v-else :data="compareResult.evidenceRefsChanged" border>
            <el-table-column label="Path" min-width="180">
              <template #default="{ row }">{{ formatPath(row) }}</template>
            </el-table-column>
            <el-table-column label="From" min-width="220">
              <template #default="{ row }"><pre class="value-pre">{{ displayValue(row.fromValue) }}</pre></template>
            </el-table-column>
            <el-table-column label="To" min-width="220">
              <template #default="{ row }"><pre class="value-pre">{{ displayValue(row.toValue) }}</pre></template>
            </el-table-column>
          </el-table>
        </el-collapse-item>

        <el-collapse-item :title="text.reviewFieldsChanged" name="review-fields">
          <el-empty v-if="!compareResult.reviewFieldsChanged?.length" :description="text.emptyBucket" />
          <el-table v-else :data="compareResult.reviewFieldsChanged" border>
            <el-table-column label="Path" min-width="180">
              <template #default="{ row }">{{ formatPath(row) }}</template>
            </el-table-column>
            <el-table-column label="From" min-width="220">
              <template #default="{ row }"><pre class="value-pre">{{ displayValue(row.fromValue) }}</pre></template>
            </el-table-column>
            <el-table-column label="To" min-width="220">
              <template #default="{ row }"><pre class="value-pre">{{ displayValue(row.toValue) }}</pre></template>
            </el-table-column>
          </el-table>
        </el-collapse-item>
      </el-collapse>
    </template>

    <el-drawer
      v-model="detailVisible"
      :title="text.detailTitle"
      size="54%"
    >
      <div v-loading="loadingDetail">
        <template v-if="selectedVersion">
          <el-descriptions :column="1" border>
            <el-descriptions-item :label="text.versionNo">
              v{{ selectedVersion.versionNo }}
            </el-descriptions-item>
            <el-descriptions-item :label="text.snapshotSource">
              {{ selectedVersion.snapshotSource || text.emptyValue }}
            </el-descriptions-item>
            <el-descriptions-item :label="text.createdAt">
              {{ formatDateTime(selectedVersion.createdAt) }}
            </el-descriptions-item>
          </el-descriptions>

          <div class="drawer-section-title">{{ text.reportSnapshot }}</div>
          <el-descriptions :column="1" border>
            <el-descriptions-item
              v-for="(value, key) in selectedReportSnapshot"
              :key="key"
              :label="String(key)"
            >
              <pre class="value-pre">{{ displayValue(value) }}</pre>
            </el-descriptions-item>
          </el-descriptions>

          <div class="drawer-section-title">{{ text.rawSnapshot }}</div>
          <pre class="snapshot-pre">{{ stringifyJson(selectedVersion.snapshot) }}</pre>
        </template>

        <el-empty v-else :description="text.noVersionDetail" />
      </div>
    </el-drawer>
  </el-card>
</template>

<style scoped>
.report-version-panel {
  margin-top: 16px;
}

.panel-header,
.panel-actions,
.compare-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.panel-actions,
.compare-toolbar {
  flex-wrap: wrap;
}

.panel-title {
  color: #142033;
  font-size: 18px;
  font-weight: 900;
}

.panel-subtitle {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.compare-toolbar {
  justify-content: flex-start;
  margin-top: 16px;
}

.compare-alert,
.compare-meta,
.diff-collapse {
  margin-top: 16px;
}

.value-pre,
.snapshot-pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  overflow-wrap: anywhere;
  font-family: inherit;
  line-height: 1.6;
}

.snapshot-pre {
  padding: 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  background: var(--el-fill-color-light);
  font-family: ui-monospace, SFMono-Regular, Consolas, 'Liberation Mono', monospace;
  font-size: 12px;
}

.drawer-section-title {
  margin: 18px 0 10px;
  color: #142033;
  font-weight: 900;
}

@media (max-width: 760px) {
  .panel-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
