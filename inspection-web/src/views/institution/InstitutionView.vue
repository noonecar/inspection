<template>
  <div class="page-card institution-page">
    <div class="page-title">人员管理</div>

    <!-- 顶部搜索 + 操作按钮 -->
    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="按机构名称、省份、城市或检查员姓名搜索"
        clearable
        class="search-input"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button type="primary" size="small" @click="openInstitutionDialog()">新增机构</el-button>
    </div>

    <!-- 左右分栏 -->
    <div class="content">
      <!-- 左侧：按省份分组的机构树 -->
      <div class="panel-left">
        <div class="institution-tree">
          <div v-for="prov in filteredProvinces" :key="prov.name" class="province-group">
            <div
              class="province-header"
              :class="{ expanded: prov.name === expandedProvince }"
              @click="toggleProvince(prov.name)"
            >
              <el-icon class="arrow-icon"><ArrowRight /></el-icon>
              <span class="province-name">{{ prov.name }}</span>
              <span class="province-count">{{ prov.institutions.length }} 个机构</span>
            </div>
            <div v-show="prov.name === expandedProvince" class="institution-list">
              <div
                v-for="inst in prov.institutions"
                :key="inst.id"
                class="institution-row"
                :class="{ active: selectedInstitution && selectedInstitution.id === inst.id }"
                @click="onInstitutionSelect(inst)"
              >
                <span class="inst-name">{{ inst.name }}</span>
                <span class="inst-city">{{ inst.city }}</span>
                <span class="inst-actions">
                  <el-button size="small" text type="primary" @click.stop="openInstitutionDialog(inst)">编辑</el-button>
                  <el-button size="small" text type="danger" @click.stop="deleteInst(inst)">删除</el-button>
                </span>
              </div>
            </div>
          </div>
          <el-empty v-if="filteredProvinces.length === 0" description="无匹配机构" :image-size="60" />
        </div>
      </div>

      <!-- 右侧：机构下检查员 -->
      <div class="panel-right">
        <div class="right-header" v-if="selectedInstitution">
          <div class="header-info">
            <span class="institution-name">{{ selectedInstitution.name }}</span>
            <span class="institution-city">{{ selectedInstitution.province }} {{ selectedInstitution.city }}</span>
          </div>
          <el-button type="primary" size="small" @click="openAddInspectorDialog()">添加检查员</el-button>
        </div>
        <div class="right-header" v-else>
          <span class="placeholder-text">请从左侧选择机构</span>
        </div>
        <el-table
          v-if="selectedInstitution"
          :data="institutionInspectors"
          stripe
          border
          class="fill-table"
        >
          <el-table-column prop="username" label="账号" min-width="140" show-overflow-tooltip />
          <el-table-column prop="realName" label="姓名" width="120" />
          <el-table-column label="操作" width="300" fixed="right">
            <template #default="scope">
              <el-button size="small" text type="primary" @click="openTaskOverview(scope.row)">任务情况</el-button>
              <el-button size="small" text type="warning" @click="resetPassword(scope.row)">重置密码</el-button>
              <el-button size="small" text type="danger" @click="removeInspector(scope.row)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="请选择机构查看检查员" />
      </div>
    </div>

    <!-- 新增/编辑机构对话框 -->
    <el-dialog v-model="instDialogVisible" :title="instForm.id ? '编辑机构' : '新增机构'" width="460px">
      <el-form :model="instForm" label-width="80px">
        <el-form-item label="省份">
          <el-select v-model="instForm.province" filterable placeholder="请选择省份" style="width: 100%">
            <el-option v-for="p in provinceList" :key="p" :label="p" :value="p" />
          </el-select>
        </el-form-item>
        <el-form-item label="机构名称"><el-input v-model="instForm.name" placeholder="例：成都市无线电监测站" /></el-form-item>
        <el-form-item label="城市"><el-input v-model="instForm.city" placeholder="例：成都市" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="instDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveInstitution">保存</el-button>
      </template>
    </el-dialog>

    <!-- 添加检查员对话框 -->
    <el-dialog v-model="addInspectorVisible" title="添加检查员" width="460px">
      <el-form :model="inspectorForm" label-width="80px">
        <el-form-item label="姓名">
          <el-input v-model="inspectorForm.realName" placeholder="请输入检查员姓名" @input="previewUsername" />
        </el-form-item>
        <el-form-item label="账号">
          <el-input v-model="inspectorForm.username" disabled :placeholder="usernamePreview || '输入姓名后自动生成'" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input value="123456" disabled />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addInspectorVisible = false">取消</el-button>
        <el-button type="primary" @click="saveNewInspector">确认添加</el-button>
      </template>
    </el-dialog>

    <!-- 检查员任务情况对话框 -->
    <el-dialog v-model="taskOverviewVisible" :title="taskOverviewInspectorName + ' — 任务及完成情况'" width="800px">
      <div v-if="taskOverview" class="task-overview">
        <!-- 统计卡片 -->
        <el-row :gutter="12" class="stat-row">
          <el-col :span="6">
            <div class="stat-card stat-total">
              <div class="stat-value">{{ taskOverview.total }}</div>
              <div class="stat-label">分配任务总数</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-card stat-pending">
              <div class="stat-value">{{ taskOverview.pending }}</div>
              <div class="stat-label">待审核</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-card stat-progress">
              <div class="stat-value">{{ taskOverview.inProgress }}</div>
              <div class="stat-label">进行中</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-card stat-completed">
              <div class="stat-value">{{ taskOverview.completed }}</div>
              <div class="stat-label">已完成</div>
            </div>
          </el-col>
        </el-row>
        <div class="stat-sub">检查记录数：{{ taskOverview.inspectionCount }} 条</div>

        <!-- 任务明细 -->
        <el-table :data="taskOverview.tasks" stripe border class="mt-16">
          <el-table-column prop="name" label="任务名称" min-width="180" show-overflow-tooltip />
          <el-table-column prop="taskType" label="类型" width="110" />
          <el-table-column prop="checkCategory" label="细分类" width="110" />
          <el-table-column label="状态" width="90">
            <template #default="scope">
              <el-tag :type="statusTagType(scope.row.status)" size="small">{{ scope.row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="截止" width="110">
            <template #default="scope">{{ scope.row.dueDate || '' }}</template>
          </el-table-column>
        </el-table>
      </div>
      <el-empty v-else description="加载中..." />
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight, Search } from '@element-plus/icons-vue'
import { pinyin } from 'pinyin-pro'
import {
  fetchInstitutions,
  createInstitution,
  updateInstitution,
  deleteInstitution,
  getInstitutionInspectors,
  assignInspectorsToInstitution,
  removeInspectorFromInstitution
} from '../../api/institutions'
import { fetchInspectors } from '../../api/tasks'
import { createInspectorWithInstitution, resetInspectorPassword, getInspectorTaskOverview } from '../../api/inspectors'

const institutionList = ref([])
const allInspectors = ref([])
const selectedInstitution = ref(null)
const institutionInspectors = ref([])
const inspectorInstitutionMap = ref({}) // 用户名 -> 机构名称

const searchKeyword = ref('')

const instDialogVisible = ref(false)
const instForm = reactive({ id: null, name: '', province: '', city: '' })

// 省份列表（去重）
const provinceList = computed(() => {
  const set = new Set()
  for (const inst of institutionList.value) {
    if (inst.province) set.add(inst.province)
  }
  return Array.from(set).sort()
})

const addInspectorVisible = ref(false)
const inspectorForm = reactive({ realName: '', username: '' })
const usernamePreview = ref('')

const taskOverviewVisible = ref(false)
const taskOverviewInspectorName = ref('')
const taskOverview = ref(null)

// 当前展开的省份
const expandedProvince = ref('')
function toggleProvince(name) {
  expandedProvince.value = expandedProvince.value === name ? '' : name
}

// 按省份分组 + 搜索过滤
const filteredProvinces = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase()

  // 先建立 机构ID -> 其下检查员姓名 的映射
  const instInspectorNames = {}
  allInspectors.value.forEach(ins => {
    const instName = inspectorInstitutionMap.value[ins.username]
    if (instName) {
      const inst = institutionList.value.find(i => i.name === instName)
      if (inst) {
        if (!instInspectorNames[inst.id]) instInspectorNames[inst.id] = []
        instInspectorNames[inst.id].push((ins.realName || '').toLowerCase())
      }
    }
  })

  // 按省份分组
  const provinceMap = {}
  for (const inst of institutionList.value) {
    const prov = inst.province || '未知'
    // 搜索过滤
    if (kw) {
      const matchName = inst.name.toLowerCase().includes(kw)
      const matchCity = inst.city.toLowerCase().includes(kw)
      const matchProv = prov.toLowerCase().includes(kw)
      const matchInspector = (instInspectorNames[inst.id] || []).some(n => n.includes(kw))
      if (!matchName && !matchCity && !matchProv && !matchInspector) continue
    }
    if (!provinceMap[prov]) provinceMap[prov] = []
    provinceMap[prov].push(inst)
  }

  // 排序：省份按拼音，省内按城市
  return Object.entries(provinceMap)
    .sort(([a], [b]) => a.localeCompare(b, 'zh'))
    .map(([name, institutions]) => ({
      name,
      institutions: institutions.sort((a, b) => (a.city || '').localeCompare(b.city || '', 'zh'))
    }))
})

// 拼音转换 — 使用 pinyin-pro
// 城市拼音短码：完整拼音，避免不同城市首字母碰撞（如 淮北 huaibei vs 鹤壁 hebi）
const cityPinyinMap = {
  '成都市': 'chengdu', '自贡市': 'zigong', '攀枝花市': 'panzhihua', '德阳市': 'deyang',
  '绵阳市': 'mianyang', '广元市': 'guangyuan', '遂宁市': 'suining', '内江市': 'neijiang',
  '乐山市': 'leshan', '南充市': 'nanchong', '眉山市': 'meishan', '宜宾市': 'yibin',
  '广安市': 'guangan', '达州市': 'dazhou', '雅安市': 'yaan', '巴中市': 'bazhong',
  '资阳市': 'ziyang', '阿坝州': 'aba', '甘孜州': 'ganzi', '凉山州': 'liangshan',
  '泸州市': 'luzhou'
}

function toPinyin(name) {
  return pinyin(name, { toneType: 'none', type: 'array' }).join('')
}

function getCityCode(cityName) {
  if (cityPinyinMap[cityName]) return cityPinyinMap[cityName]
  const clean = cityName.replace(/[市区州盟县]$/, '')
  const arr = pinyin(clean, { toneType: 'none', type: 'array' })
  return arr.join('')
}

function generateUsername(realName, cityName) {
  const namePinyin = toPinyin(realName)
  const cityCode = getCityCode(cityName)
  return namePinyin + '_' + cityCode
}

function previewUsername() {
  if (!inspectorForm.realName || !selectedInstitution.value) {
    usernamePreview.value = ''
    return
  }
  usernamePreview.value = generateUsername(inspectorForm.realName, selectedInstitution.value.city)
}

const loadInstitutions = async () => {
  const res = await fetchInstitutions()
  if (res.success) {
    institutionList.value = res.data || []
  }
}

const loadInspectors = async () => {
  const res = await fetchInspectors()
  if (res.success) {
    allInspectors.value = Array.isArray(res.data) ? res.data : []
  }
  const map = {}
  for (const inst of (institutionList.value || [])) {
    const res2 = await getInstitutionInspectors(inst.id)
    if (res2.success) {
      for (const insp of (res2.data || [])) {
        map[insp.username] = inst.name
      }
    }
  }
  inspectorInstitutionMap.value = map
}

const onInstitutionSelect = async (row) => {
  selectedInstitution.value = row
  if (row) {
    const res = await getInstitutionInspectors(row.id)
    if (res.success) {
      institutionInspectors.value = res.data || []
    }
  } else {
    institutionInspectors.value = []
  }
}

const openInstitutionDialog = (row) => {
  if (row) {
    Object.assign(instForm, { id: row.id, name: row.name, province: row.province || '', city: row.city })
  } else {
    Object.assign(instForm, { id: null, name: '', province: '', city: '' })
  }
  instDialogVisible.value = true
}

const saveInstitution = async () => {
  if (!instForm.name || !instForm.city || !instForm.province) {
    ElMessage.warning('请完整填写机构信息')
    return
  }
  if (instForm.id) {
    await updateInstitution(instForm.id, instForm)
    ElMessage.success('更新成功')
  } else {
    await createInstitution(instForm)
    ElMessage.success('创建成功')
  }
  instDialogVisible.value = false
  await loadInstitutions()
}

const deleteInst = async (row) => {
  await ElMessageBox.confirm(`确认删除机构 "${row.name}"？该机构下的检查员关联也将被删除。`, '删除确认', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteInstitution(row.id)
  ElMessage.success('删除成功')
  await loadInstitutions()
  if (selectedInstitution.value && selectedInstitution.value.id === row.id) {
    selectedInstitution.value = null
    institutionInspectors.value = []
  }
}

const openAddInspectorDialog = () => {
  inspectorForm.realName = ''
  inspectorForm.username = ''
  usernamePreview.value = ''
  addInspectorVisible.value = true
}

const saveNewInspector = async () => {
  if (!inspectorForm.realName || !inspectorForm.realName.trim()) {
    ElMessage.warning('请输入检查员姓名')
    return
  }
  const username = generateUsername(inspectorForm.realName.trim(), selectedInstitution.value.city)
  const realName = inspectorForm.realName.trim()
  try {
    await createInspectorWithInstitution({
      username,
      realName,
      institutionId: selectedInstitution.value.id
    })
    ElMessage.success('添加成功，账号：' + username)
    addInspectorVisible.value = false
    const res = await getInstitutionInspectors(selectedInstitution.value.id)
    if (res.success) {
      institutionInspectors.value = res.data || []
    }
    await loadInspectors()
  } catch (e) {
    ElMessage.error('添加失败，可能账号已存在')
  }
}

const removeInspector = async (row) => {
  await ElMessageBox.confirm(`确认将 "${row.realName || row.username}" 从该机构移除？`, '移除确认', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await removeInspectorFromInstitution(selectedInstitution.value.id, row.id)
  ElMessage.success('移除成功')
  const res = await getInstitutionInspectors(selectedInstitution.value.id)
  if (res.success) {
    institutionInspectors.value = res.data || []
  }
}

const resetPassword = async (row) => {
  await ElMessageBox.confirm(`确认将 "${row.realName || row.username}" 的密码重置为 123456？`, '重置密码', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await resetInspectorPassword(row.id)
  ElMessage.success('密码已重置为 123456')
}

const openTaskOverview = async (row) => {
  taskOverviewInspectorName.value = row.realName || row.username
  taskOverview.value = null
  taskOverviewVisible.value = true
  const res = await getInspectorTaskOverview(row.id)
  if (res.success) {
    taskOverview.value = res.data
  } else {
    ElMessage.error('获取任务情况失败')
  }
}

const statusTagType = (status) => {
  const map = {
    '待审核': 'warning',
    '进行中': '',
    '已完成': 'success',
    '已取消': 'info'
  }
  return map[status] || 'info'
}


onMounted(async () => {
  await loadInstitutions()
  await loadInspectors()
})
</script>

<style scoped>
.institution-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px);
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.search-input {
  max-width: 360px;
}

.content {
  display: flex;
  gap: 16px;
  flex: 1;
  min-height: 0;
}

.panel-left {
  width: 380px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* 机构树 */
.institution-tree {
  flex: 1;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.province-header {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  cursor: pointer;
  background: #fafafa;
  border-bottom: 1px solid #ebeef5;
  user-select: none;
  transition: background 0.15s;
}

.province-header:hover {
  background: #ecf5ff;
}

.province-header.expanded {
  background: #e8f4ff;
  border-bottom: none;
}

.arrow-icon {
  margin-right: 6px;
  transition: transform 0.2s;
  font-size: 12px;
  color: #909399;
}

.province-header.expanded .arrow-icon {
  transform: rotate(90deg);
}

.province-name {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
  flex: 1;
}

.province-count {
  font-size: 12px;
  color: #909399;
}

.institution-list {
  border-bottom: 1px solid #ebeef5;
}

.institution-row {
  display: flex;
  align-items: center;
  padding: 8px 12px 8px 36px;
  cursor: pointer;
  border-bottom: 1px solid #f2f2f2;
  transition: background 0.1s;
}

.institution-row:hover {
  background: #f5f7fa;
}

.institution-row.active {
  background: #ecf5ff;
}

.institution-row:last-child {
  border-bottom: none;
}

.inst-name {
  flex: 1;
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}

.inst-city {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
  margin-left: 8px;
  width: 70px;
  text-align: right;
}

.inst-actions {
  flex-shrink: 0;
  margin-left: 4px;
  display: flex;
  gap: 2px;
}

.panel-right {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.fill-table {
  flex: 1;
}

.fill-table :deep(.el-table__body-wrapper) {
  overflow-y: auto;
}

.right-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 6px;
  flex-shrink: 0;
}

.header-info {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.institution-name {
  font-weight: 600;
  font-size: 16px;
  color: #303133;
}

.institution-city {
  font-size: 13px;
  color: #909399;
}

.placeholder-text {
  font-size: 14px;
  color: #c0c4cc;
}

/* 任务情况对话框 */
.stat-row {
  margin-bottom: 8px;
}

.stat-card {
  text-align: center;
  padding: 16px 8px;
  border-radius: 6px;
  background: #f5f7fa;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.stat-total .stat-value { color: #409EFF; }
.stat-pending .stat-value { color: #E6A23C; }
.stat-progress .stat-value { color: #909399; }
.stat-completed .stat-value { color: #67C23A; }

.stat-sub {
  text-align: center;
  font-size: 14px;
  color: #606266;
  margin-bottom: 12px;
}

.mt-16 {
  margin-top: 16px;
}
</style>
