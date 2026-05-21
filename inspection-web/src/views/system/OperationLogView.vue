<template>
  <div class="page-card">
    <div class="page-title">操作日志</div>

    <el-space class="mb-12" wrap>
      <el-input v-model="query.operator" placeholder="操作人" clearable style="width: 150px" />
      <el-select v-model="query.moduleName" placeholder="模块" clearable style="width: 180px">
        <el-option label="任务统筹" value="任务统筹" />
        <el-option label="检查执行" value="检查执行" />
        <el-option label="数据管理-台站" value="数据管理-台站" />
        <el-option label="数据管理-频率" value="数据管理-频率" />
      </el-select>
      <el-select v-model="query.operationType" placeholder="操作类型" clearable style="width: 130px">
        <el-option label="新增" value="新增" />
        <el-option label="修改" value="修改" />
        <el-option label="删除" value="删除" />
        <el-option label="审核" value="审核" />
      </el-select>
      <el-date-picker
        v-model="timeRange"
        type="datetimerange"
        range-separator="至"
        start-placeholder="开始时间"
        end-placeholder="结束时间"
        value-format="YYYY-MM-DDTHH:mm:ss"
      />
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </el-space>

    <el-row :gutter="16" class="mb-12">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" style="border-left: 4px solid #409EFF">
          <div class="stat-title">日志总量</div>
          <div class="stat-value" style="color: #409EFF">{{ summary.total || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" style="border-left: 4px solid #67C23A">
          <div class="stat-title">新增</div>
          <div class="stat-value" style="color: #67C23A">{{ getTypeCount('新增') }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" style="border-left: 4px solid #E6A23C">
          <div class="stat-title">修改</div>
          <div class="stat-value" style="color: #E6A23C">{{ getTypeCount('修改') }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" style="border-left: 4px solid #F56C6C">
          <div class="stat-title">审核/删除</div>
          <div class="stat-value" style="color: #F56C6C">{{ getTypeCount('审核') + getTypeCount('删除') }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-table :data="tableData" stripe border>
      <el-table-column prop="operationTime" label="操作时间" min-width="170">
        <template #default="scope">{{ formatDateTime(scope.row.operationTime) }}</template>
      </el-table-column>
      <el-table-column prop="operator" label="操作人" width="110" />
      <el-table-column prop="operatorRole" label="角色" width="110" />
      <el-table-column prop="moduleName" label="模块" width="160" />
      <el-table-column prop="operationType" label="操作类型" width="90" />
      <el-table-column prop="targetId" label="对象ID" width="90" />
      <el-table-column prop="description" label="描述" min-width="260" show-overflow-tooltip />
      <el-table-column v-if="canDelete" label="操作" width="100" fixed="right">
        <template #default="scope">
          <el-button size="small" text type="danger" @click="remove(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="mt-12"
      background
      layout="total, prev, pager, next"
      :total="total"
      v-model:current-page="query.page"
      :page-size="query.size"
      @current-change="load"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { deleteOperationLog, fetchOperationLogs, fetchOperationLogSummary } from '../../api/operationLogs'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
const role = computed(() => auth.user?.role || 'OPERATOR')
const canDelete = computed(() => role.value === 'ADMIN')

const tableData = ref([])
const total = ref(0)
const summary = ref({})
const timeRange = ref([])

const query = reactive({
  page: 1,
  size: 10,
  operator: '',
  moduleName: '',
  operationType: '',
  startTime: '',
  endTime: ''
})

const formatDateTime = (value) => {
  if (!value) return ''
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value
    if (!year || !month || !day) return ''
    return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')} ${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}:${String(second).padStart(2, '0')}`
  }
  return String(value).slice(0, 19).replace('T', ' ')
}

const getTypeCount = (type) => Number(summary.value?.byOperationType?.[type] || 0)

const loadSummary = async () => {
  const res = await fetchOperationLogSummary()
  if (res.success) {
    summary.value = res.data || {}
  }
}

const load = async () => {
  query.startTime = timeRange.value?.[0] || ''
  query.endTime = timeRange.value?.[1] || ''
  const res = await fetchOperationLogs(query)
  if (res.success) {
    const records = Array.isArray(res.data) ? res.data : res.data.records
    tableData.value = records || []
    total.value = Array.isArray(res.data) ? tableData.value.length : res.data.total
  }
}

const search = () => {
  query.page = 1
  load()
}

const reset = () => {
  timeRange.value = []
  Object.assign(query, { page: 1, size: 10, operator: '', moduleName: '', operationType: '', startTime: '', endTime: '' })
  load()
}

const remove = async (id) => {
  await deleteOperationLog(id)
  ElMessage.success('删除成功')
  await load()
  await loadSummary()
}

onMounted(async () => {
  await loadSummary()
  await load()
})
</script>

<style scoped>
.mb-12 {
  margin-bottom: 12px;
}

.mt-12 {
  margin-top: 12px;
}

.stat-card {
  border-radius: 8px;
}

.stat-title {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
}
</style>
