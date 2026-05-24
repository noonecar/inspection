<template>
  <div class="page-card">
    <div class="page-title">任务统筹</div>

    <div class="toolbar mb-12">
      <div class="toolbar-left">
        <el-button v-if="canEdit" type="primary" @click="openDialog()">新建任务</el-button>
        <el-dropdown v-if="canEdit" @command="handleBatchCommand">
          <el-button :disabled="!selectedIds.length">批量操作</el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="assign">分配检查员</el-dropdown-item>
              <el-dropdown-item command="status">更新状态</el-dropdown-item>
              <el-dropdown-item command="remind">任务催办</el-dropdown-item>
              <el-dropdown-item command="cancel">任务撤销</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button @click="handleExport">导出任务列表</el-button>
      </div>

      <div class="toolbar-right">
        <el-input v-model="query.keyword" placeholder="任务名称/责任人/备注" clearable class="basic-filter-input" />
        <el-select v-model="query.taskType" placeholder="任务类型" clearable class="basic-filter-select">
          <el-option label="台站检查任务" value="台站检查任务" />
          <el-option label="频率检查任务" value="频率检查任务" />
        </el-select>
        <el-select v-model="query.checkCategory" placeholder="检查细分类" clearable class="basic-filter-select-wide">
          <el-option v-for="item in availableCategories" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable class="basic-filter-select">
          <el-option label="待审核" value="待审核" />
          <el-option label="进行中" value="进行中" />
          <el-option label="待复检" value="待复检" />
          <el-option label="已完成" value="已完成" />
          <el-option label="已取消" value="已取消" />
        </el-select>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="onReset">重置</el-button>
      </div>
    </div>

    <el-table :data="tableData" stripe border @selection-change="onSelectionChange">
      <el-table-column type="selection" width="48" />
      <el-table-column prop="name" label="任务名称" min-width="220" show-overflow-tooltip />
      <el-table-column prop="taskType" label="任务类型" width="120" />
      <el-table-column prop="checkCategory" label="检查细分类" min-width="140" />
      <el-table-column prop="inspectionMode" label="检查方式" width="100" />
      <el-table-column prop="stationName" label="检查对象" min-width="180" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="95" />
      <el-table-column prop="assignee" label="责任检查员" width="180">
        <template #default="scope">
          {{ formatAssignees(scope.row.assignee) }}
        </template>
      </el-table-column>
      <el-table-column label="截止时间" width="120">
        <template #default="scope">{{ formatDateOnly(scope.row.dueDate) }}</template>
      </el-table-column>
      <el-table-column label="最近催办" width="120">
        <template #default="scope">{{ formatDateOnly(scope.row.remindedAt) }}</template>
      </el-table-column>
      <el-table-column v-if="showAction" label="操作" width="220" fixed="right">
        <template #default="scope">
          <div class="action-buttons">
            <el-button v-if="canEdit" size="small" text type="primary" @click="openDialog(scope.row)">编辑</el-button>
            <el-button
              v-if="canEdit && scope.row.status === '待审核'"
              size="small"
              text
              type="success"
              @click="auditTask(scope.row, true)"
            >
              审核通过
            </el-button>
            <el-button
              v-if="canEdit && scope.row.status === '待审核'"
              size="small"
              text
              type="warning"
              @click="auditTask(scope.row, false)"
            >
              驳回
            </el-button>
            <el-button v-if="canDelete" size="small" text type="danger" @click="remove(scope.row.id)">删除</el-button>
            <el-button size="small" text type="info" @click="openHistory(scope.row)">历史记录</el-button>
          </div>
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

    <el-dialog v-model="visible" :title="form.id ? '编辑任务' : '新建任务'" width="680px">
      <el-form :model="form" label-width="116px">
        <el-form-item label="任务名称"><el-input v-model="form.name" /></el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="任务类型">
              <el-select v-model="form.taskType" placeholder="请选择">
                <el-option label="台站检查任务" value="台站检查任务" />
                <el-option label="频率检查任务" value="频率检查任务" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="检查细分类">
              <el-select v-model="form.checkCategory" placeholder="请选择">
                <el-option v-for="item in checkCategories" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="检查方式">
              <el-select v-model="form.inspectionMode" placeholder="请选择">
                <el-option label="日常检查" value="日常检查" />
                <el-option label="重点检查" value="重点检查" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="检查机构">
              <el-select v-model="form.institutionId" filterable clearable placeholder="请选择机构" style="width: 100%">
                <el-option v-for="item in filteredInstitutionOptions" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="检查对象">
              <el-select v-model="form.stationIds" multiple filterable placeholder="请选择台站" style="width: 100%">
                <el-option v-for="item in filteredStationOptions" :key="item.stationId" :label="item.name" :value="item.stationId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="责任检查员">
              <el-select v-model="form.assignees" multiple filterable placeholder="请选择检查员" style="width: 100%">
                <el-option v-for="item in inspectorOptions" :key="item.username" :label="item.realName || item.username" :value="item.username" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="备注"><el-input v-model="form.description" type="textarea" rows="3" /></el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="截止时间"><el-date-picker v-model="form.dueDate" type="date" value-format="YYYY-MM-DD" :disabled-date="disabledPastDate" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchEditVisible" title="批量更新状态" width="420px">
      <el-form label-width="98px">
        <el-form-item label="状态">
          <el-select v-model="batchStatus" placeholder="请选择" clearable style="width: 100%">
            <el-option label="进行中" value="进行中" />
            <el-option label="待复检" value="待复检" />
            <el-option label="已完成" value="已完成" />
            <el-option label="已取消" value="已取消" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchEditVisible = false">取消</el-button>
        <el-button type="primary" @click="handleBatchStatus">确认更新</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="cancelVisible" title="批量撤销任务" width="500px">
      <el-form label-width="90px">
        <el-form-item label="撤销原因"><el-input v-model="cancelReason" type="textarea" rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelVisible = false">取消</el-button>
        <el-button type="danger" @click="handleBatchCancel">确认撤销</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="historyVisible" title="操作历史记录" width="560px">
      <el-timeline v-if="historyData.length" style="padding: 0 12px">
        <el-timeline-item
          v-for="item in historyData"
          :key="item.id"
          :color="getHistoryColor(item.actionType)"
          :timestamp="formatDateTime(item.createdAt)"
          placement="top"
        >
          <p style="margin: 0 0 4px"><strong>{{ item.actionType }}</strong></p>
          <p style="margin: 0 0 4px; color: #606266; font-size: 13px">{{ item.description }}</p>
          <p style="margin: 0; color: #909399; font-size: 12px">操作人：{{ item.operator }}</p>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无操作记录" />
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  batchAssignTasks,
  batchCancelTasks,
  batchRemindTasks,
  batchUpdateTaskStatus,
  auditTasks,
  createTask,
  deleteTask,
  fetchInspectors,
  fetchTasks,
  getTaskHistory,
  updateTask
} from '../../api/tasks'
import { fetchStations } from '../../api/stations'
import { useAuthStore } from '../../stores/auth'
import { fetchInstitutions, getInspectorsByInstitutions } from '../../api/institutions'

const FREQUENCY_CATEGORIES = ['地面频率使用', '卫星频率使用', '卫星通信网频率']
const STATION_CATEGORIES = ['地面台站', '空间电台', '卫星地球站']
const checkCategories = computed(() => {
  if (form.taskType === '频率检查任务') return FREQUENCY_CATEGORIES
  if (form.taskType === '台站检查任务') return STATION_CATEGORIES
  return [...FREQUENCY_CATEGORIES, ...STATION_CATEGORIES]
})

const availableCategories = computed(() => {
  if (query.taskType === '频率检查任务') return FREQUENCY_CATEGORIES
  if (query.taskType === '台站检查任务') return STATION_CATEGORIES
  return [...FREQUENCY_CATEGORIES, ...STATION_CATEGORIES]
})

const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  taskType: '',
  checkCategory: '',
  status: '',
  taskId: ''
})

const tableData = ref([])
const total = ref(0)
const selectedIds = ref([])
const stationMap = ref({})
const stationOptions = ref([])
const inspectorOptions = ref([])
const inspectorMap = ref({})
const institutionOptions = ref([])
const filteredStationOptions = computed(() => {
  if (!form.institutionId) return stationOptions.value
  const institution = institutionOptions.value.find(i => i.id === form.institutionId)
  if (!institution || !institution.city) return stationOptions.value
  return stationOptions.value.filter(s => s.address && s.address.includes(institution.city))
})

const filteredInstitutionOptions = computed(() => {
  if (!form.stationIds || form.stationIds.length === 0) return institutionOptions.value
  const selectedStations = stationOptions.value.filter(s => form.stationIds.includes(s.stationId))
  const cities = selectedStations.map(s => s.address || '').filter(a => a)
  if (cities.length === 0) return institutionOptions.value
  return institutionOptions.value.filter(i =>
    i.city && cities.some(c => c.includes(i.city))
  )
})

const visible = ref(false)
const batchEditVisible = ref(false)
const cancelVisible = ref(false)
const batchStatus = ref('')
const cancelReason = ref('')
const historyVisible = ref(false)
const historyData = ref([])

const auth = useAuthStore()
const route = useRoute()
const role = computed(() => auth.user?.role || 'OPERATOR')
const canEdit = computed(() => ['ADMIN', 'OPERATOR'].includes(role.value))
const canDelete = computed(() => role.value === 'ADMIN')
const showAction = computed(() => canEdit.value || canDelete.value)

const form = reactive({
  id: null,
  name: '',
  taskType: '台站检查任务',
  checkCategory: '地面台站',
  inspectionMode: '日常检查',
  stationId: null,
  stationIds: [],
  institutionId: null,
  status: '待审核',
  assignee: '',
  assignees: [],
  dueDate: '',
  description: ''
})

const disabledPastDate = (time) => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return time.getTime() < today.getTime()
}

const formatDateOnly = (value) => {
  if (!value) return ''
  if (Array.isArray(value)) {
    const [year, month, day] = value
    if (!year || !month || !day) return ''
    return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
  }
  const raw = String(value).replace('T', ' ')
  if (/^\d{4}-\d{2}-\d{2}/.test(raw)) {
    return raw.slice(0, 10)
  }
  const parts = raw.split(/[-/:,\s]/).filter(Boolean)
  if (parts.length >= 3 && parts[0].length === 4) {
    return `${parts[0]}-${String(parts[1]).padStart(2, '0')}-${String(parts[2]).padStart(2, '0')}`
  }
  return raw.slice(0, 10)
}

const loadStations = async () => {
  const res = await fetchStations({ page: 1, size: 1000 })
  if (!res.success) return
  const records = Array.isArray(res.data) ? res.data : res.data.records
  stationOptions.value = records || []
  const map = {}
  ;(records || []).forEach((item) => {
    map[item.stationId] = item.name
  })
  stationMap.value = map
}

const loadInspectors = async () => {
  const res = await fetchInspectors()
  if (res.success) {
    inspectorOptions.value = Array.isArray(res.data) ? res.data : []
    const map = {}
    inspectorOptions.value.forEach((item) => {
      map[item.username] = item.realName || ''
    })
    inspectorMap.value = map
  }
}

const loadInstitutions = async () => {
  const res = await fetchInstitutions()
  if (res.success) {
    institutionOptions.value = res.data || []
  }
}

const loadInspectorsByInstitution = async (institutionId) => {
  if (!institutionId) {
    await loadInspectors()
    return
  }
  const res = await getInspectorsByInstitutions([institutionId])
  if (res.success) {
    inspectorOptions.value = Array.isArray(res.data) ? res.data : []
  }
}

const formatAssignees = (assignee) => {
  if (!assignee) return ''
  const usernames = assignee.split(',').filter(Boolean)
  return usernames.map(u => inspectorMap.value[u] || u).join('、')
}

const formatStationNames = (item) => {
  if (!item) return ''
  let ids = []
  if (Array.isArray(item.stationIds)) {
    ids = item.stationIds
  } else if (typeof item.stationIds === 'string' && item.stationIds.trim()) {
    try {
      const parsed = JSON.parse(item.stationIds)
      if (Array.isArray(parsed)) ids = parsed
    } catch (error) {
      ids = []
    }
  }
  if (!ids.length && item.stationId) {
    ids = [item.stationId]
  }
  if (ids.length) {
    const names = ids
      .map(id => stationMap.value[id] || (id ? `台站-${id}` : ''))
      .filter(Boolean)
    return Array.from(new Set(names)).join('、')
  }
  return item.stationName || ''
}

const normalizeRow = (item) => ({
  ...item,
  stationName: stationMap.value[item.stationId] || item.stationName || (item.stationId ? `台站-${item.stationId}` : ''),
  assigneeName: inspectorMap.value[item.assignee] || ''
})

const load = async () => {
  const res = await fetchTasks(query)
  if (!res.success) return
  const records = Array.isArray(res.data) ? res.data : res.data.records
  tableData.value = (records || []).map(normalizeRow)
  total.value = Array.isArray(res.data) ? tableData.value.length : res.data.total
}

const onSelectionChange = (rows) => {
  selectedIds.value = rows.map((row) => row.id)
}

const onSearch = () => {
  query.page = 1
  load()
}

const onReset = () => {
  Object.assign(query, { page: 1, size: 10, keyword: '', taskType: '', checkCategory: '', status: '', taskId: '' })
  load()
}

const openDialog = (row) => {
  if (row) {
    const assignees = row.assignee && row.assignee.includes(',')
      ? row.assignee.split(',').filter(Boolean)
      : row.assignee ? [row.assignee] : []
    const stationIds = row.stationId ? [row.stationId] : []
    Object.assign(form, {
      id: row.id,
      name: row.name,
      taskType: row.taskType,
      checkCategory: row.checkCategory,
      inspectionMode: row.inspectionMode,
      stationId: row.stationId,
      stationIds,
      institutionId: row.institutionId || null,
      status: row.status,
      assignee: row.assignee,
      assignees,
      dueDate: row.dueDate,
      description: row.description
    })
    if (form.institutionId) {
      loadInspectorsByInstitution(form.institutionId)
    }
  } else {
    Object.assign(form, {
      id: null,
      name: '',
      taskType: '台站检查任务',
      checkCategory: '地面台站',
      inspectionMode: '日常检查',
      stationId: null,
      stationIds: [],
      institutionId: null,
      status: '待审核',
      assignee: '',
      assignees: [],
      dueDate: '',
      description: ''
    })
    inspectorOptions.value = []
  }
  visible.value = true
}

const save = async () => {
  if (!form.name || !form.taskType || !form.checkCategory || !form.inspectionMode || !form.stationIds.length || !form.assignees.length) {
    ElMessage.warning('请完整填写任务关键信息（需指定台站和检查员）')
    return
  }

  const payload = {
    name: form.name,
    taskType: form.taskType,
    checkCategory: form.checkCategory,
    inspectionMode: form.inspectionMode,
    stationId: form.stationIds[0],
    stationIds: JSON.stringify(form.stationIds),
    status: form.id ? form.status : '待审核',
    assignee: form.assignees.join(','),
    dueDate: form.dueDate || null,
    description: form.description
  }

  if (form.id) {
    await updateTask(form.id, payload)
    ElMessage.success('更新成功')
  } else {
    await createTask(payload)
    ElMessage.success('创建成功')
    query.page = 1
  }
  visible.value = false
  load()
}

const auditTask = async (row, approved) => {
  const actionLabel = approved ? '通过' : '驳回'
  try {
    if (!approved) {
      const { value } = await ElMessageBox.prompt('请输入驳回原因', '任务审核驳回', {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        inputPlaceholder: '例如：资料不完整或无需执行'
      })
      await auditTasks({ ids: [row.id], result: '驳回', reason: value || '审核未通过' })
    } else {
      await auditTasks({ ids: [row.id], result: '通过' })
      // 存储审核通过的任务 ID，用于在检查页面中高亮显示
      const newTaskIds = JSON.parse(sessionStorage.getItem('new-task-ids') || '[]')
      if (!newTaskIds.includes(row.id)) {
        newTaskIds.push(row.id)
        sessionStorage.setItem('new-task-ids', JSON.stringify(newTaskIds))
      }
    }
    ElMessage.success(`审核${actionLabel}成功`)
    load()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(`审核${actionLabel}失败`)
    }
  }
}

const requireSelection = () => {
  if (!selectedIds.value.length) {
    ElMessage.warning('请先选择任务')
    return false
  }
  return true
}

const handleBatchAssign = async () => {
  if (!requireSelection()) return
  try {
    const { value } = await ElMessageBox.prompt('请输入检查员账号', '批量分配检查员', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputPlaceholder: '例如 inspector'
    })
    if (!value || !String(value).trim()) {
      ElMessage.warning('请填写检查员账号')
      return
    }
    await batchAssignTasks({ ids: selectedIds.value, assignee: String(value).trim() })
    ElMessage.success('批量分配成功')
    load()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量分配失败')
    }
  }
}

const handleBatchStatus = async () => {
  if (!requireSelection()) return
  if (!batchStatus.value) {
    ElMessage.warning('请选择状态')
    return
  }
  await batchUpdateTaskStatus({ ids: selectedIds.value, status: batchStatus.value })
  ElMessage.success('批量更新成功')
  batchEditVisible.value = false
  load()
}

const handleBatchRemind = async () => {
  if (!requireSelection()) return
  await ElMessageBox.confirm('将向已分配的检查员发送催办弹窗，是否继续？', '任务催办', {
    confirmButtonText: '确认催办',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await batchRemindTasks({ ids: selectedIds.value })
  ElMessage.success('催办弹窗已发送')
  load()
}

const handleBatchCancel = async () => {
  await batchCancelTasks({ ids: selectedIds.value, cancelReason: cancelReason.value || '任务调整撤销' })
  ElMessage.success('批量撤销成功')
  cancelVisible.value = false
  load()
}

const handleBatchCommand = (command) => {
  if (!requireSelection()) return
  if (command === 'assign') {
    handleBatchAssign()
    return
  }
  if (command === 'status') {
    batchStatus.value = ''
    batchEditVisible.value = true
    return
  }
  if (command === 'remind') {
    handleBatchRemind()
    return
  }
  if (command === 'cancel') {
    cancelReason.value = ''
    cancelVisible.value = true
  }
}

const handleExport = async () => {
  const res = await fetchTasks({ page: 1, size: 10000 })
  if (!res.success) {
    ElMessage.error('导出失败')
    return
  }
  const allData = Array.isArray(res.data) ? res.data : res.data.records
  const headers = ['任务名称', '任务类型', '检查细分类', '检查方式', '检查对象', '状态', '责任检查员', '截止时间', '最近催办']
  const rows = (allData || []).map((item) => [
    item.name || '',
    item.taskType || '',
    item.checkCategory || '',
    item.inspectionMode || '',
    formatStationNames(item),
    item.status || '',
    formatAssignees(item.assignee),
    formatDateOnly(item.dueDate),
    formatDateOnly(item.remindedAt)
  ])
  const csv = [headers, ...rows]
    .map((line) => line.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(','))
    .join('\n')
  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8;' })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = 'task-list.csv'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
  ElMessage.success(`已导出 ${rows.length} 条任务`)
}

const remove = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该任务吗？', '确认删除', { type: 'warning' })
    await deleteTask(id)
    ElMessage.success('删除成功')
    load()
  } catch (e) {
    // 用户取消
  }
}

const openHistory = async (row) => {
  const res = await getTaskHistory(row.id)
  if (res.success) {
    historyData.value = res.data || []
  }
  historyVisible.value = true
}

const getHistoryColor = (actionType) => {
  const map = {
    '新建': '#409EFF',
    '审核通过': '#67C23A',
    '审核驳回': '#E6A23C',
    '状态更新': '#909399',
    '任务催办': '#409EFF',
    '任务撤销': '#F56C6C',
    '分配检查员': '#409EFF',
    '删除': '#F56C6C',
    '编辑': '#909399',
    '检查执行': '#67C23A'
  }
  return map[actionType] || '#909399'
}

const formatDateTime = (value) => {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 19)
}

onMounted(async () => {
  if (route.query.taskId) {
    query.taskId = String(route.query.taskId)
  }
  await Promise.allSettled([loadStations(), loadInspectors(), loadInstitutions()])
  await load()
})

watch(
  () => form.taskType,
  () => {
    if (!checkCategories.value.includes(form.checkCategory)) {
      form.checkCategory = ''
    }
  }
)

watch(
  () => form.institutionId,
  (val) => {
    if (visible.value) {
      loadInspectorsByInstitution(val)
      form.assignees = []
      if (val) {
        const institution = institutionOptions.value.find(i => i.id === val)
        if (institution && institution.city) {
          form.stationIds = form.stationIds.filter(sid => {
            const station = stationOptions.value.find(s => s.stationId === sid)
            return station && station.address && station.address.includes(institution.city)
          })
        } else {
          form.stationIds = []
        }
      }
    }
  }
)

watch(
  () => form.status,
  () => {
  }
)
</script>

<style scoped>
.mb-12 {
  margin-bottom: 12px;
}

.mt-12 {
  margin-top: 12px;
}

.action-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 2px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.basic-filter-input {
  width: 260px;
}

.basic-filter-select {
  width: 130px;
}

.basic-filter-select-wide {
  width: 170px;
}
</style>
