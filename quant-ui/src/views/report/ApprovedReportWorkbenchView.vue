<script setup lang="ts">
import ReportRevisionStatusTags from '../../components/report/ReportRevisionStatusTags.vue'
import ReportReviewStatsCards from '../../components/report/ReportReviewStatsCards.vue'
import { formatDateTime } from '../../utils/format'
import {
  getPriorityTagType,
  getPriorityText,
  getReviewStatusTagType,
  getReviewStatusText
} from '../../utils/task'
import { REPORT_REVIEW_STATUS } from '../../types/taskEnums'
import { REPORT_WORKBENCH_TABS, useReportWorkbench } from '../../utils/reportWorkbench'

const text = {
  title: '已通过报告',
  targetCode: '标的代码',
  targetCodePlaceholder: '如 01929',
  targetName: '标的名称',
  targetNamePlaceholder: '如 周大福',
  reviewedBy: '审核人',
  reviewedByPlaceholder: '如 admin',
  search: '查询',
  reset: '重置',
  empty: '暂无已通过报告',
  taskTitle: '任务标题',
  priority: '优先级',
  reviewStatus: '审核状态',
  revisionStatus: '修订情况',
  createdAt: '创建时间',
  reviewedAt: '审核时间',
  reviewComment: '审核意见',
  action: '操作',
  viewReport: '查看报告',
  workbench: '投研工作台',
  createTask: '发起研究',
  loadStatsFailed: '已通过统计加载失败',
  loadStatsError: '已通过统计加载异常',
  loadFailed: '已通过报告加载失败',
  loadError: '已通过报告加载异常'
} as const

const {
  loading,
  stats,
  pageData,
  query,
  handleSearch,
  handleReset,
  handlePageChange,
  handlePageSizeChange,
  goTab,
  goReport,
  goWorkbench,
  goCreateTask,
  canCreateTask
} = useReportWorkbench({
  mode: 'approved',
  loadStatsFailedText: text.loadStatsFailed,
  loadStatsErrorText: text.loadStatsError,
  loadFailedText: text.loadFailed,
  loadErrorText: text.loadError
})
</script>

<template>
  <div>
    <ReportReviewStatsCards :stats="stats" />

    <el-card style="margin-top: 16px;">
      <el-space>
        <el-button
          v-for="tab in REPORT_WORKBENCH_TABS"
          :key="tab.route"
          :type="tab.route === '/reports/approved' ? 'warning' : 'default'"
          @click="goTab(tab.route)"
        >
          {{ tab.label }}
        </el-button>
      </el-space>
    </el-card>

    <el-card style="margin-top: 16px;">
      <template #header>
        <div style="font-weight: 700;">{{ text.title }}</div>
      </template>

      <el-form inline>
        <el-form-item :label="text.targetCode">
          <el-input v-model="query.targetCode" :placeholder="text.targetCodePlaceholder" clearable />
        </el-form-item>

        <el-form-item :label="text.targetName">
          <el-input v-model="query.targetName" :placeholder="text.targetNamePlaceholder" clearable />
        </el-form-item>

        <el-form-item :label="text.reviewedBy">
          <el-input v-model="query.reportReviewedBy" :placeholder="text.reviewedByPlaceholder" clearable />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">{{ text.search }}</el-button>
          <el-button @click="handleReset">{{ text.reset }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 16px;">
      <el-table :data="pageData.records" v-loading="loading" border :empty-text="text.empty">
        <el-table-column prop="taskTitle" :label="text.taskTitle" min-width="180" />
        <el-table-column prop="targetCode" :label="text.targetCode" width="100" />
        <el-table-column prop="targetName" :label="text.targetName" width="120" />

        <el-table-column :label="text.priority" width="100">
          <template #default="{ row }">
            <el-tag :type="getPriorityTagType(row.priority)">
              {{ getPriorityText(row.priority) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column :label="text.reviewStatus" width="120">
          <template #default="{ row }">
            <el-tag :type="getReviewStatusTagType(row.reportReviewStatus || REPORT_REVIEW_STATUS.APPROVED)">
              {{ getReviewStatusText(row.reportReviewStatus || REPORT_REVIEW_STATUS.APPROVED) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column :label="text.revisionStatus" min-width="180">
          <template #default="{ row }">
            <ReportRevisionStatusTags
              :revised="row.revised"
              :summary-revised="row.summaryRevised"
              :highlights-revised="row.highlightsRevised"
              :risk-points-revised="row.riskPointsRevised"
            />
          </template>
        </el-table-column>

        <el-table-column :label="text.createdAt" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column prop="reportReviewedBy" :label="text.reviewedBy" width="120" />

        <el-table-column :label="text.reviewedAt" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.reportReviewedAt) }}
          </template>
        </el-table-column>

        <el-table-column prop="reportReviewComment" :label="text.reviewComment" min-width="180" show-overflow-tooltip />

        <el-table-column :label="text.action" width="290" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goReport(row.taskId)">{{ text.viewReport }}</el-button>
            <el-button link type="warning" @click="goWorkbench(row)">{{ text.workbench }}</el-button>
            <el-button v-if="canCreateTask" link type="warning" @click="goCreateTask(row)">{{ text.createTask }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
        <el-pagination
          background
          layout="total, prev, pager, next, sizes"
          :total="pageData.total"
          v-model:current-page="pageData.pageNum"
          v-model:page-size="pageData.pageSize"
          @current-change="handlePageChange"
          @size-change="handlePageSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>
