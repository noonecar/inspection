<template>
  <div class="page-card">
    <div class="page-title">违规预警</div>

    <el-row :gutter="16" class="mb-12">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" style="border-left: 4px solid #909399">
          <div class="stat-title">历史总预警数</div>
          <div class="stat-value" style="color: #909399">{{ summary.total }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" style="border-left: 4px solid #F56C6C">
          <div class="stat-title">未处理</div>
          <div class="stat-value" style="color: #F56C6C">{{ summary.pending }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" style="border-left: 4px solid #E6A23C">
          <div class="stat-title">处理中</div>
          <div class="stat-value" style="color: #E6A23C">{{ summary.processing }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" style="border-left: 4px solid #67C23A">
          <div class="stat-title">已处理</div>
          <div class="stat-value" style="color: #67C23A">{{ summary.done }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-space class="mb-12" wrap>
      <el-input v-model="query.keyword" placeholder="预警规则/预警内容" clearable style="width: 220px" />
      <!-- <el-select v-model="query.ruleName" placeholder="预警规则" clearable style="width: 260px">
        <el-option v-for="item in ruleOptions" :key="item" :label="item" :value="item" />
      </el-select> -->
      <el-select v-model="query.level" placeholder="等级" clearable style="width: 110px">
        <el-option label="高" value="高" />
        <el-option label="中" value="中" />
        <el-option label="低" value="低" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
        <el-option label="未处理" value="未处理" />
        <el-option label="处理中" value="处理中" />
        <el-option label="已处理" value="已处理" />
      </el-select>
      <el-input v-model="query.relatedTaskName" placeholder="关联任务名称" clearable style="width: 170px" />
      <el-button type="primary" @click="onSearch">查询</el-button>
      <el-button @click="onReset">重置</el-button>
      <el-button v-if="canEdit" @click="openDialog()">新增预警</el-button>
    </el-space>

    <el-table :data="tableData" stripe border :row-class-name="getRowClassName">
      <el-table-column prop="ruleName" label="预警规则" min-width="260" show-overflow-tooltip />
      <el-table-column label="等级" width="90">
        <template #default="scope">
          <el-tag
            :type="scope.row.level === '高' ? 'danger' : scope.row.level === '中' ? 'warning' : 'info'"
            :style="scope.row.level === '低' ? 'background: #fdf6ec; color: #e6a23c; border-color: #f5dab1;' : ''"
            size="small"
            effect="dark"
          >
            {{ scope.row.level }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="message" label="预警内容" min-width="240" show-overflow-tooltip />
      <el-table-column prop="status" label="处理状态" width="110">
        <template #default="scope">
          <el-tag :type="scope.row.status === '已处理' ? 'success' : scope.row.status === '处理中' ? 'warning' : 'danger'">
            {{ scope.row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="relatedTaskName" label="关联任务名称" min-width="160" show-overflow-tooltip />
      <el-table-column label="更新时间" width="120">
          <template #default="scope">{{ formatDate(scope.row.updatedAt) }}</template>
        </el-table-column>
      <el-table-column v-if="showAction" label="操作" width="280" fixed="right">
        <template #default="scope">
          <el-button v-if="canEdit" size="small" text type="primary" @click="openDialog(scope.row)">
            编辑
          </el-button>
          <el-button v-if="canEdit && scope.row.status !== '已处理'" size="small" text type="success" @click="resolveWarningItem(scope.row)">
            标记已处理
          </el-button>
          <el-button size="small" text type="info" @click="openHistory(scope.row)">
            历史
          </el-button>
          <el-button v-if="canDelete" size="small" text type="danger" @click="remove(scope.row.id)">
            删除
          </el-button>
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

    <el-dialog v-model="visible" title="预警信息" width="620px">
      <el-form :model="form" label-width="108px">
        <el-form-item label="预警规则">
          <el-select v-model="form.ruleName" placeholder="请选择" style="width: 100%" filterable allow-create @change="onRuleChange">
            <el-option v-for="item in ruleOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="预警等级">
              <el-select v-model="form.level" placeholder="请选择">
                <el-option label="高" value="高" />
                <el-option label="中" value="中" />
                <el-option label="低" value="低" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="预警内容">
          <el-input v-model="form.message" type="textarea" :rows="3" placeholder="填写触发事实、风险说明和建议措施" />
        </el-form-item>
        <el-form-item label="关联任务名称">
          <el-select v-model="form.relatedTaskId" filterable clearable placeholder="请选择关联任务" style="width: 100%">
            <el-option v-for="item in taskOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="historyVisible" title="预警变更历史" width="560px">
      <el-timeline v-if="historyData.length" style="padding: 0 12px">
        <el-timeline-item
          v-for="item in historyData"
          :key="item.id"
          :color="historyColor(item.level)"
          :timestamp="formatDate(item.createdAt)"
          placement="top"
        >
          <p style="margin: 0 0 4px">
            <el-tag :type="item.level === '高' ? 'danger' : item.level === '中' ? 'warning' : 'info'" size="small">
              {{ item.level }}
            </el-tag>
            <span style="margin-left: 8px; font-weight: 500">{{ item.status }}</span>
          </p>
          <p style="margin: 4px 0; color: #606266; font-size: 13px">
            <template v-if="item.status !== '已处理'">{{ item.message }}</template>
            <template v-else><span style="color: #c0c4cc; font-style: italic">该预警已处理，详细内容已归档</span></template>
          </p>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无变更记录" />
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createWarning, deleteWarning, fetchWarningHistory, fetchWarnings, resolveWarning, updateWarning } from '../../api/warnings'
import { fetchTasks } from '../../api/tasks'
import { useAuthStore } from '../../stores/auth'

const ruleOptions = [
  '监测发现异常需重点检查',
  '违反无线电管理法规并被责令改正或行政处罚',
  '未按要求报送年度无线电频率使用报告或报告真实性存疑',
  '频率占用费逾期未缴纳',
  '未进行定期维护导致性能指标不符合规定',
  '需增加频率使用需求'
]

const ruleLevelMap = {
  '监测发现异常需重点检查': '高',
  '违反无线电管理法规并被责令改正或行政处罚': '高',
  '未按要求报送年度无线电频率使用报告或报告真实性存疑': '中',
  '频率占用费逾期未缴纳': '中',
  '未进行定期维护导致性能指标不符合规定': '中',
  '需增加频率使用需求': '低'
}

const ruleMessageTemplate = {
  '监测发现异常需重点检查': '监测发现信号异常，可能存在有害干扰或未经批准擅自使用频率情况，需立即重点处置。',
  '违反无线电管理法规并被责令改正或行政处罚': '检查结果不合格且需整改，请尽快复核并跟踪整改情况。',
  '未按要求报送年度无线电频率使用报告或报告真实性存疑': '年度频率使用报告未按要求报送，请督促限期补报。',
  '频率占用费逾期未缴纳': '频率占用费未缴纳，违反频率资源有偿使用规定，建议催缴。',
  '未进行定期维护导致性能指标不符合规定': '专业技术人员配置或设备维护不符合要求，建议安排专项整改。',
  '需增加频率使用需求': '现有频率资源已无法满足使用需求，建议评估后提出增加频率申请。'
}

const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  ruleName: '',
  level: '',
  status: '',
  relatedTaskName: ''
})

const tableData = ref([])
const total = ref(0)
const visible = ref(false)
const taskNameMap = ref({})
const taskOptions = ref([])
const summary = ref({ total: 0, pending: 0, processing: 0, done: 0 })
  const historyVisible = ref(false)
  const historyData = ref([])
const auth = useAuthStore()
const route = useRoute()
const role = computed(() => auth.user?.role || 'OPERATOR')

const highlightWarningId = ref(null)
let highlightTimer = null

const applyHighlight = (id) => {
  const parsed = Number(id)
  if (!parsed || Number.isNaN(parsed)) return
  highlightWarningId.value = parsed
  if (highlightTimer) {
    clearTimeout(highlightTimer)
  }
  highlightTimer = setTimeout(() => {
    highlightWarningId.value = null
  }, 3000)
}

const getRowClassName = ({ row }) => {
  if (highlightWarningId.value && row?.id === highlightWarningId.value) {
    return 'warning-highlight'
  }
  return ''
}
const canEdit = computed(() => ['ADMIN', 'OPERATOR'].includes(role.value))
const canDelete = computed(() => role.value === 'ADMIN')
const showAction = computed(() => canEdit.value || canDelete.value)
const form = reactive({
  id: null,
  ruleName: '',
  level: '',
  message: '',
  status: '未处理',
  relatedTaskId: null
})

const onRuleChange = (ruleName) => {
  if (ruleName && ruleLevelMap[ruleName]) {
    form.level = ruleLevelMap[ruleName]
  }
  if (ruleName && ruleMessageTemplate[ruleName]) {
    form.message = ruleMessageTemplate[ruleName]
  }
}

const loadTaskOptions = async () => {
  const res = await fetchTasks({ page: 1, size: 1000 })
  if (res.success) {
    const records = Array.isArray(res.data) ? res.data : res.data.records
    const rows = records || []
    taskOptions.value = rows.map((item) => ({ id: item.id, name: item.name }))
    const map = {}
    rows.forEach((item) => {
      map[item.id] = item.name
    })
    taskNameMap.value = map
  }
}

const load = async () => {
  const res = await fetchWarnings(query)
  if (res.success) {
    const page = Array.isArray(res.data) ? { records: res.data, total: (res.data || []).length } : res.data || {}
    const records = page.records || []
    const rows = (records || []).map((item) => ({
      ...item,
      relatedTaskName: (item.relatedTaskId ? taskNameMap.value[item.relatedTaskId] : '') || item.taskName || ''
    }))
    // 前端只展示当前页的记录，分页总数使用后端返回的 total
    tableData.value = rows
    total.value = Number(page.total || rows.length)

    if (route.query.warningId) {
      applyHighlight(route.query.warningId)
    }

    // 为了展示各状态的汇总（而不是仅当前页），单独请求总数（size=1 以减少数据量）
    try {
      const [pendingRes, processingRes, doneRes] = await Promise.all([
        fetchWarnings({ ...query, page: 1, size: 1, status: '未处理' }),
        fetchWarnings({ ...query, page: 1, size: 1, status: '处理中' }),
        fetchWarnings({ ...query, page: 1, size: 1, status: '已处理' })
      ])
      summary.value = {
        total: Number(page.total || 0),
        pending: pendingRes.success ? Number(pendingRes.data?.total || 0) : 0,
        processing: processingRes.success ? Number(processingRes.data?.total || 0) : 0,
        done: doneRes.success ? Number(doneRes.data?.total || 0) : 0
      }
    } catch (e) {
      summary.value = {
        total: Number(page.total || 0),
        pending: rows.filter((item) => item.status === '未处理').length,
        processing: rows.filter((item) => item.status === '处理中').length,
        done: rows.filter((item) => item.status === '已处理').length
      }
    }
  }
}

const onSearch = () => {
  query.page = 1
  load()
}

const onReset = () => {
  Object.assign(query, {
    page: 1,
    size: 10,
    keyword: '',
    ruleName: '',
    level: '',
    status: '',
    relatedTaskName: ''
  })
  load()
}

const openDialog = (row) => {
  if (row) {
    Object.assign(form, {
      ...row,
      relatedTaskId: row.relatedTaskId || null
    })
  } else {
    Object.assign(form, { id: null, ruleName: '', level: '', message: '', status: '未处理', relatedTaskId: null })
  }
  visible.value = true
}

const save = async () => {
  if (!form.ruleName || !form.level || !form.message) {
    ElMessage.warning('请完整填写预警规则、等级与内容')
    return
  }
  if (form.id) {
    await updateWarning(form.id, {
      ruleName: form.ruleName,
      level: form.level,
      message: form.message,
      status: form.status,
      relatedTaskId: form.relatedTaskId
    })
    ElMessage.success('更新成功')
  } else {
    await createWarning({
      ruleName: form.ruleName,
      level: form.level,
      message: form.message,
      status: form.status,
      relatedTaskId: form.relatedTaskId
    })
    ElMessage.success('创建成功')
    query.page = 1
  }
  visible.value = false
  load()
}

const remove = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该预警记录吗？', '确认删除', { type: 'warning' })
    await deleteWarning(id)
    ElMessage.success('删除成功')
    load()
  } catch (e) {
    // 用户取消
  }
}

const resolveWarningItem = async (row) => {
  await resolveWarning(row.id)
  ElMessage.success('预警已标记为已处理')
  load()
}

const openHistory = async (row) => {
  const res = await fetchWarningHistory(row.id)
  if (res.success) {
    historyData.value = res.data || []
  }
  historyVisible.value = true
}

const historyColor = (level) => {
  if (level === '高') return '#F56C6C'
  if (level === '中') return '#E6A23C'
  return '#909399'
}

const formatDate = (value) => {
  if (!value) return ''
  return String(value).slice(0, 10)
}

onMounted(async () => {
  await loadTaskOptions()
  await load()
})

watch(
  () => route.query.warningId,
  (value) => {
    if (value) {
      applyHighlight(value)
    }
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

:deep(.warning-highlight) {
  background: rgba(59, 130, 246, 0.18) !important;
  transition: background 0.3s ease;
}
</style>
