<template>
  <div class="page-card">
    <div class="page-title">法规标准</div>

    <el-space class="mb-12" wrap>
      <el-button @click="syncLibrary" v-if="canEdit">法规条款更新与同步</el-button>
      <el-button @click="downloadClauses">批量下载条款库</el-button>
      <el-button @click="downloadStandards">批量下载标准库</el-button>
    </el-space>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="法规文件" name="regulations">
        <el-space class="mb-12" wrap>
          <el-input v-model="regulationQuery.keyword" placeholder="法规标题/分类/版本" clearable style="width: 220px" />
          <el-select v-model="regulationQuery.category" placeholder="分类" clearable style="width: 140px">
            <el-option v-for="item in categoryOptions" :key="item" :label="item" :value="item" />
          </el-select>
          <el-input v-model="regulationQuery.version" placeholder="版本" clearable style="width: 120px" />
          <el-button type="primary" @click="loadRegulations">查询</el-button>
          <el-button v-if="canEdit" @click="openRegulationDialog()">新增法规</el-button>
        </el-space>

        <el-table :data="regulationTable" stripe border>
          <el-table-column prop="title" label="法规标题" min-width="260" show-overflow-tooltip />
          <el-table-column prop="category" label="分类" width="130" />
          <el-table-column prop="version" label="版本" width="110" />
          <el-table-column label="发布日期" width="120">
            <template #default="scope">{{ formatDateOnly(scope.row.issuedAt) }}</template>
          </el-table-column>
          <el-table-column label="文件地址" min-width="320">
            <template #default="scope">
              <el-link
                v-if="scope.row.fileUrl"
                class="file-url-link"
                :href="scope.row.fileUrl"
                target="_blank"
                type="primary"
              >
                {{ scope.row.fileUrl }}
              </el-link>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column v-if="showAction" label="操作" width="160" fixed="right">
            <template #default="scope">
              <el-button v-if="canEdit" size="small" text type="primary" @click="openRegulationDialog(scope.row)">编辑</el-button>
              <el-button v-if="canDelete" size="small" text type="danger" @click="removeRegulation(scope.row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="法规条款库" name="clauses">
        <el-space class="mb-12" wrap>
          <el-input v-model="clauseFilter.keyword" placeholder="输入即筛选：条款编号/关键词/内容" clearable style="width: 280px" />
          <el-select v-model="clauseFilter.clauseCategory" placeholder="条款分类" clearable style="width: 160px">
            <el-option v-for="item in clauseCategoryOptions" :key="item" :label="item" :value="item" />
          </el-select>
          <el-select v-model="clauseFilter.regulationId" placeholder="所属法规" clearable style="width: 220px">
            <el-option v-for="item in regulationTable" :key="item.id" :label="item.title" :value="item.id" />
          </el-select>
          <el-button @click="resetClauseFilter">重置筛选</el-button>
          <el-button v-if="canEdit" @click="openClauseDialog()">新增条款</el-button>
        </el-space>

        <div class="knowledge-layout">
          <div class="knowledge-tree-panel">
            <div class="tree-panel-title">法规条款库（共 {{ clauseListForTree.length }} 条）</div>
            <el-tree
              ref="clauseTreeRef"
              :data="clauseTreeData"
              node-key="key"
              default-expand-all
              highlight-current
              :expand-on-click-node="false"
              @node-click="onClauseNodeClick"
            />
          </div>

          <div class="knowledge-detail-panel">
            <template v-if="selectedClause">
              <div class="detail-header">
                <div>
                  <div class="detail-title">{{ selectedClause.articleNo }}</div>
                  <div class="detail-subtitle">{{ regulationTitleById(selectedClause.regulationId) }} / {{ selectedClause.chapterTitle || '未分章节' }}</div>
                </div>
                <el-button v-if="canEdit" type="primary" plain @click="openClauseDialog(selectedClause)">编辑条款</el-button>
              </div>

              <el-descriptions :column="1" border>
                <el-descriptions-item label="关键词">{{ selectedClause.keywordTag || '-' }}</el-descriptions-item>
                <el-descriptions-item label="条款内容">{{ selectedClause.content || '-' }}</el-descriptions-item>
              </el-descriptions>

              <div class="relation-title">关联检查标准（{{ relatedStandardsForSelectedClause.length }}）</div>
              <el-table :data="relatedStandardsForSelectedClause" size="small" border stripe>
                <el-table-column prop="itemCode" label="项号" width="90" />
                <el-table-column prop="itemName" label="检查项" min-width="180" show-overflow-tooltip />
                <el-table-column prop="objectType" label="检查对象" width="130" />
                <el-table-column label="操作" width="120">
                  <template #default="scope">
                    <el-button text type="primary" @click="locateStandard(scope.row.id)">定位到标准树</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </template>

            <el-empty v-else description="请选择左侧树中的具体条款" />
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="检查标准库" name="standards">
        <el-space class="mb-12" wrap>
          <el-input v-model="standardFilter.keyword" placeholder="输入即筛选：项号/检查项/法规条款" clearable style="width: 270px" />
          <el-select v-model="standardFilter.objectType" placeholder="检查对象" clearable style="width: 160px">
            <el-option label="无线电频率" value="无线电频率" />
            <el-option label="在用无线电台（站）" value="在用无线电台（站）" />
          </el-select>
          <el-button @click="resetStandardFilter">重置筛选</el-button>
          <el-button v-if="canEdit" @click="openStandardDialog()">新增标准</el-button>
        </el-space>

        <div class="knowledge-layout">
          <div class="knowledge-tree-panel">
            <div class="tree-panel-title">检查标准库（共 {{ standardListForTree.length }} 项）</div>
            <el-tree
              ref="standardTreeRef"
              :data="standardTreeData"
              node-key="key"
              default-expand-all
              highlight-current
              :expand-on-click-node="false"
              @node-click="onStandardNodeClick"
            />
          </div>

          <div class="knowledge-detail-panel">
            <template v-if="selectedStandard">
              <div class="detail-header">
                <div>
                  <div class="detail-title">{{ selectedStandard.itemCode }} · {{ selectedStandard.itemName }}</div>
                  <div class="detail-subtitle">{{ selectedStandard.objectType }} / {{ selectedStandard.stationType || '未指定台站类型' }}</div>
                </div>
                <el-button v-if="canEdit" type="primary" plain @click="openStandardDialog(selectedStandard)">编辑标准</el-button>
              </div>

              <el-descriptions :column="2" border>
                <el-descriptions-item label="关联法规条款">{{ selectedStandard.legalClause || '-' }}</el-descriptions-item>
                <el-descriptions-item label="检查方式">{{ selectedStandard.checkType || '-' }}</el-descriptions-item>
                <el-descriptions-item label="判定规则">{{ selectedStandard.judgmentRule || '-' }}</el-descriptions-item>
                <el-descriptions-item label="检查方法" :span="2">{{ selectedStandard.checkMethod || '-' }}</el-descriptions-item>
              </el-descriptions>

              <div class="relation-title">关联法规条款（{{ relatedClausesForSelectedStandard.length }}）</div>
              <el-table :data="relatedClausesForSelectedStandard" size="small" border stripe>
                <el-table-column prop="articleNo" label="条款编号" width="130" />
                <el-table-column prop="clauseCategory" label="条款分类" width="120" />
                <el-table-column label="所属法规" min-width="180" show-overflow-tooltip>
                  <template #default="scope">{{ regulationTitleById(scope.row.regulationId) }}</template>
                </el-table-column>
                <el-table-column prop="content" label="条款内容" min-width="180" show-overflow-tooltip />
                <el-table-column label="操作" width="120">
                  <template #default="scope">
                    <el-button text type="primary" @click="locateClause(scope.row.id)">定位到条款树</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </template>

            <el-empty v-else description="请选择左侧树中的具体检查项" />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="regulationVisible" title="法规信息" width="620px">
      <el-form :model="regulationForm" label-width="92px">
        <el-form-item label="法规标题"><el-input v-model="regulationForm.title" /></el-form-item>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="法规分类"><el-select v-model="regulationForm.category" placeholder="请选择"><el-option v-for="item in categoryOptions" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="版本"><el-input v-model="regulationForm.version" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="文件地址"><el-input v-model="regulationForm.fileUrl" /></el-form-item>
        <el-form-item label="发布日期"><el-date-picker v-model="regulationForm.issuedAt" type="date" value-format="YYYY-MM-DD" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="regulationVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRegulation">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="clauseVisible" title="法规条款" width="660px">
      <el-form :model="clauseForm" label-width="102px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="所属法规"><el-select v-model="clauseForm.regulationId" placeholder="请选择"><el-option v-for="item in regulationTable" :key="item.id" :label="item.title" :value="item.id" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="条款分类"><el-select v-model="clauseForm.clauseCategory" placeholder="请选择"><el-option v-for="item in clauseCategoryOptions" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="章节"><el-input v-model="clauseForm.chapterTitle" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="条款编号"><el-input v-model="clauseForm.articleNo" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="关键词"><el-input v-model="clauseForm.keywordTag" /></el-form-item>
        <el-form-item label="条款内容"><el-input v-model="clauseForm.content" type="textarea" rows="4" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="clauseVisible = false">取消</el-button>
        <el-button type="primary" @click="saveClause">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="standardVisible" title="检查标准" width="700px">
      <el-form :model="standardForm" label-width="106px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="检查对象"><el-select v-model="standardForm.objectType" placeholder="请选择"><el-option label="无线电频率" value="无线电频率" /><el-option label="在用无线电台（站）" value="在用无线电台（站）" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="台站类型"><el-input v-model="standardForm.stationType" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item label="项号"><el-input v-model="standardForm.itemCode" /></el-form-item></el-col>
          <el-col :span="16"><el-form-item label="检查项"><el-input v-model="standardForm.itemName" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="法规条款"><el-input v-model="standardForm.legalClause" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="检查方式"><el-select v-model="standardForm.checkType" placeholder="请选择"><el-option label="书面检查" value="书面检查" /><el-option label="现场核查" value="现场核查" /><el-option label="监测" value="监测" /><el-option label="检测" value="检测" /><el-option label="监测及分析计算" value="监测及分析计算" /><el-option label="提前核查" value="提前核查" /></el-select></el-form-item></el-col>
        </el-row>
        <el-form-item label="检查方法"><el-input v-model="standardForm.checkMethod" /></el-form-item>
        <el-form-item label="判定规则"><el-input v-model="standardForm.judgmentRule" /></el-form-item>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="风险等级"><el-select v-model="standardForm.severityLevel" placeholder="请选择"><el-option label="高" value="高" /><el-option label="中" value="中" /><el-option label="低" value="低" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="启用"><el-switch v-model="standardForm.enabled" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="standardVisible = false">取消</el-button>
        <el-button type="primary" @click="saveStandard">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createRegulation, deleteRegulation, fetchRegulations, updateRegulation } from '../../api/regulations'
import {
  createInspectionStandard,
  createRegulationClause,
  downloadInspectionStandards,
  downloadRegulationClauses,
  fetchInspectionStandards,
  fetchRegulationClauses,
  syncRegulationLibrary,
  updateInspectionStandard,
  updateRegulationClause
} from '../../api/regulationKnowledge'
import { useAuthStore } from '../../stores/auth'

const activeTab = ref('regulations')
const categoryOptions = ['法律', '行政法规', '部门规章', '规范性文件', '国家标准', '行业标准']
const clauseCategoryOptions = ['频率许可', '频率使用事项', '台站执照', '维护要求', '重点检查', '分类检查', '过程记录', '监督管理']

const regulationQuery = reactive({ page: 1, size: 100, keyword: '', category: '', version: '' })
const clauseQuery = reactive({ page: 1, size: 1000, keyword: '', clauseCategory: '', regulationId: '' })
const standardQuery = reactive({ page: 1, size: 1000, keyword: '', objectType: '', severityLevel: '' })

const regulationTable = ref([])
const clauseTable = ref([])
const standardTable = ref([])

const clauseTreeRef = ref()
const standardTreeRef = ref()
const selectedClauseId = ref(null)
const selectedStandardId = ref(null)

const clauseFilter = reactive({ keyword: '', clauseCategory: '', regulationId: '' })
const standardFilter = reactive({ keyword: '', objectType: '' })

const auth = useAuthStore()
const role = computed(() => auth.user?.role || 'OPERATOR')
const canEdit = computed(() => ['ADMIN', 'OPERATOR'].includes(role.value))
const canDelete = computed(() => role.value === 'ADMIN')
const showAction = computed(() => canEdit.value || canDelete.value)

const regulationVisible = ref(false)
const clauseVisible = ref(false)
const standardVisible = ref(false)

const regulationForm = reactive({ id: null, title: '', category: '', version: '', fileUrl: '', issuedAt: '' })
const clauseForm = reactive({ id: null, regulationId: null, chapterTitle: '', articleNo: '', clauseCategory: '', keywordTag: '', content: '' })
const standardForm = reactive({ id: null, objectType: '', stationType: '', itemCode: '', itemName: '', legalClause: '', checkType: '', checkMethod: '', judgmentRule: '', severityLevel: '', enabled: true })

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

const normalizeText = (value) => String(value || '').replace(/\s+/g, '').trim()

const splitLegalClauseTokens = (value) => {
  const source = String(value || '').trim()
  if (!source) return []
  return source
    .split(/[，,；;、/\n]+/)
    .map((item) => item.trim())
    .filter(Boolean)
}

const cnNumberToInt = (input) => {
  const value = String(input || '').trim()
  if (!value) return Number.MAX_SAFE_INTEGER
  if (/^\d+$/.test(value)) return Number(value)
  const map = { 零: 0, 〇: 0, 一: 1, 二: 2, 三: 3, 四: 4, 五: 5, 六: 6, 七: 7, 八: 8, 九: 9 }
  if (value === '十') return 10
  if (value.includes('十')) {
    const [leftRaw, rightRaw] = value.split('十')
    const left = leftRaw ? (map[leftRaw] ?? 1) : 1
    const right = rightRaw ? (map[rightRaw] ?? 0) : 0
    return left * 10 + right
  }
  return map[value] ?? Number.MAX_SAFE_INTEGER
}

const getChapterOrder = (chapterTitle) => {
  const match = String(chapterTitle || '').match(/^第([一二三四五六七八九十百零〇\d]+)章/)
  return match ? cnNumberToInt(match[1]) : Number.MAX_SAFE_INTEGER
}

const getArticleOrder = (articleNo) => {
  const match = String(articleNo || '').match(/^第([一二三四五六七八九十百零〇\d]+)条/)
  return match ? cnNumberToInt(match[1]) : Number.MAX_SAFE_INTEGER
}

const naturalItemCode = (value) => {
  const code = String(value || '')
  const match = code.match(/^([A-Za-z]+)-?(\d+)$/)
  if (!match) {
    return { prefix: code, index: Number.MAX_SAFE_INTEGER }
  }
  return { prefix: match[1], index: Number(match[2]) }
}

const toShortItemName = (value) => {
  const source = String(value || '').trim()
  if (!source) return ''
  const parts = source.split('/')
  if (parts.length <= 1) return source
  return parts[parts.length - 1].trim()
}

const regulationTitleById = (id) => {
  const regulation = regulationTable.value.find((item) => item.id === id)
  return regulation?.title || `法规#${id}`
}

const clauseListForTree = computed(() => {
  return clauseTable.value.filter((item) => {
    if (clauseFilter.regulationId && item.regulationId !== clauseFilter.regulationId) return false
    if (clauseFilter.clauseCategory && item.clauseCategory !== clauseFilter.clauseCategory) return false
    if (!clauseFilter.keyword) return true
    const key = clauseFilter.keyword.trim().toLowerCase()
    const text = [
      item.articleNo,
      item.keywordTag,
      item.content,
      item.chapterTitle,
      item.clauseCategory,
      regulationTitleById(item.regulationId)
    ]
      .filter(Boolean)
      .join('|')
      .toLowerCase()
    return text.includes(key)
  })
})

const standardListForTree = computed(() => {
  return standardTable.value.filter((item) => {
    if (standardFilter.objectType && item.objectType !== standardFilter.objectType) return false
    if (!standardFilter.keyword) return true
    const key = standardFilter.keyword.trim().toLowerCase()
    const text = [item.itemCode, item.itemName, item.legalClause, item.checkMethod, item.judgmentRule, item.stationType]
      .filter(Boolean)
      .join('|')
      .toLowerCase()
    return text.includes(key)
  })
})

const clauseTreeData = computed(() => {
  const regulationMap = new Map()
  clauseListForTree.value.forEach((clause) => {
    if (!regulationMap.has(clause.regulationId)) {
      regulationMap.set(clause.regulationId, {
        key: `reg-${clause.regulationId}`,
        type: 'regulation',
        label: regulationTitleById(clause.regulationId),
        children: new Map()
      })
    }
    const regulationNode = regulationMap.get(clause.regulationId)
    const chapter = clause.chapterTitle || '未分章节'
    if (!regulationNode.children.has(chapter)) {
      regulationNode.children.set(chapter, {
        key: `reg-${clause.regulationId}-chapter-${chapter}`,
        type: 'chapter',
        label: chapter,
        children: []
      })
    }
    regulationNode.children.get(chapter).children.push({
      key: `clause-${clause.id}`,
      type: 'clause',
      label: `${clause.articleNo}`,
      payload: clause
    })
  })

  return Array.from(regulationMap.values())
    .sort((a, b) => a.label.localeCompare(b.label, 'zh-CN'))
    .map((regNode) => {
      const chapterList = Array.from(regNode.children.values())
        .sort((a, b) => {
          const delta = getChapterOrder(a.label) - getChapterOrder(b.label)
          return delta !== 0 ? delta : a.label.localeCompare(b.label, 'zh-CN')
        })
        .map((chapterNode) => {
          chapterNode.children.sort((a, b) => {
            const delta = getArticleOrder(a.payload.articleNo) - getArticleOrder(b.payload.articleNo)
            return delta !== 0 ? delta : a.payload.articleNo.localeCompare(b.payload.articleNo, 'zh-CN')
          })
          chapterNode.label = `${chapterNode.label} (${chapterNode.children.length})`
          return chapterNode
        })
      return {
        key: regNode.key,
        type: regNode.type,
        label: `${regNode.label} (${chapterList.reduce((sum, chapter) => sum + chapter.children.length, 0)})`,
        children: chapterList
      }
    })
})

const standardTreeData = computed(() => {
  const objectMap = new Map()
  standardListForTree.value.forEach((standard) => {
    const objectType = standard.objectType || '未指定对象'
    const stationType = standard.stationType || '未指定台站类型'

    if (!objectMap.has(objectType)) {
      objectMap.set(objectType, {
        key: `object-${objectType}`,
        type: 'object',
        label: objectType,
        children: new Map()
      })
    }
    const objectNode = objectMap.get(objectType)
    if (!objectNode.children.has(stationType)) {
      objectNode.children.set(stationType, {
        key: `object-${objectType}-station-${stationType}`,
        type: 'station',
        label: stationType,
        children: []
      })
    }
    const stationNode = objectNode.children.get(stationType)
    stationNode.children.push({
      key: `standard-${standard.id}`,
      type: 'standard',
      label: `${standard.itemCode} · ${toShortItemName(standard.itemName)}`,
      payload: standard
    })
  })

  return Array.from(objectMap.values())
    .sort((a, b) => a.label.localeCompare(b.label, 'zh-CN'))
    .map((objectNode) => {
      const stationList = Array.from(objectNode.children.values())
        .sort((a, b) => a.label.localeCompare(b.label, 'zh-CN'))
        .map((stationNode) => {
          stationNode.children.sort((a, b) => {
            const aCode = naturalItemCode(a.payload.itemCode)
            const bCode = naturalItemCode(b.payload.itemCode)
            if (aCode.prefix !== bCode.prefix) return aCode.prefix.localeCompare(bCode.prefix, 'zh-CN')
            return aCode.index - bCode.index
          })
          return {
            key: stationNode.key,
            type: stationNode.type,
            label: `${stationNode.label} (${stationNode.children.length})`,
            children: stationNode.children
          }
        })
      return {
        key: objectNode.key,
        type: objectNode.type,
        label: `${objectNode.label} (${stationList.reduce((sum, station) => sum + station.children.length, 0)})`,
        children: stationList
      }
    })
})

const relationMap = computed(() => {
  const clauseIdToStandards = new Map()
  const standardIdToClauses = new Map()

  const normalizedClauseMap = clauseTable.value.map((clause) => ({
    ...clause,
    articleNoNormalized: normalizeText(clause.articleNo)
  }))

  const exactArticleMap = new Map()
  normalizedClauseMap.forEach((clause) => {
    if (!clause.articleNoNormalized) return
    if (!exactArticleMap.has(clause.articleNoNormalized)) {
      exactArticleMap.set(clause.articleNoNormalized, [])
    }
    exactArticleMap.get(clause.articleNoNormalized).push(clause)
  })

  standardTable.value.forEach((standard) => {
    const tokens = splitLegalClauseTokens(standard.legalClause)
    const matchedClauseMap = new Map()

    tokens.forEach((token) => {
      const normalizedToken = normalizeText(token)
      if (!normalizedToken) return

      const exact = exactArticleMap.get(normalizedToken) || []
      exact.forEach((clause) => {
        matchedClauseMap.set(clause.id, clause)
      })

      if (!exact.length) {
        normalizedClauseMap.forEach((clause) => {
          if (!clause.articleNoNormalized) return
          if (clause.articleNoNormalized.includes(normalizedToken) || normalizedToken.includes(clause.articleNoNormalized)) {
            matchedClauseMap.set(clause.id, clause)
          }
        })
      }
    })

    const matchedClauses = Array.from(matchedClauseMap.values())
    standardIdToClauses.set(standard.id, matchedClauses)

    matchedClauses.forEach((clause) => {
      if (!clauseIdToStandards.has(clause.id)) {
        clauseIdToStandards.set(clause.id, [])
      }
      clauseIdToStandards.get(clause.id).push(standard)
    })
  })

  return { clauseIdToStandards, standardIdToClauses }
})

const selectedClause = computed(() => clauseTable.value.find((item) => item.id === selectedClauseId.value) || null)
const selectedStandard = computed(() => standardTable.value.find((item) => item.id === selectedStandardId.value) || null)

const relatedStandardsForSelectedClause = computed(() => {
  if (!selectedClauseId.value) return []
  return relationMap.value.clauseIdToStandards.get(selectedClauseId.value) || []
})

const relatedClausesForSelectedStandard = computed(() => {
  if (!selectedStandardId.value) return []
  return relationMap.value.standardIdToClauses.get(selectedStandardId.value) || []
})

const loadRegulations = async () => {
  const res = await fetchRegulations(regulationQuery)
  if (res.success) {
    const records = Array.isArray(res.data) ? res.data : res.data.records
    regulationTable.value = records || []
  }
}

const loadClauses = async () => {
  const res = await fetchRegulationClauses(clauseQuery)
  if (res.success) {
    const records = Array.isArray(res.data) ? res.data : res.data.records
    clauseTable.value = records || []
    if (!selectedClauseId.value || !clauseTable.value.some((item) => item.id === selectedClauseId.value)) {
      selectedClauseId.value = clauseTable.value[0]?.id || null
    }
  }
}

const loadStandards = async () => {
  const res = await fetchInspectionStandards(standardQuery)
  if (res.success) {
    const records = Array.isArray(res.data) ? res.data : res.data.records
    standardTable.value = records || []
    if (!selectedStandardId.value || !standardTable.value.some((item) => item.id === selectedStandardId.value)) {
      selectedStandardId.value = standardTable.value[0]?.id || null
    }
  }
}

const resetClauseFilter = () => {
  Object.assign(clauseFilter, { keyword: '', clauseCategory: '', regulationId: '' })
}

const resetStandardFilter = () => {
  Object.assign(standardFilter, { keyword: '', objectType: '' })
}

const onClauseNodeClick = (node) => {
  if (node.type !== 'clause') return
  selectedClauseId.value = node.payload.id
}

const onStandardNodeClick = (node) => {
  if (node.type !== 'standard') return
  selectedStandardId.value = node.payload.id
}

const locateClause = async (clauseId) => {
  selectedClauseId.value = clauseId
  activeTab.value = 'clauses'
  await nextTick()
  clauseTreeRef.value?.setCurrentKey(`clause-${clauseId}`)
}

const locateStandard = async (standardId) => {
  selectedStandardId.value = standardId
  activeTab.value = 'standards'
  await nextTick()
  standardTreeRef.value?.setCurrentKey(`standard-${standardId}`)
}

watch(selectedClauseId, async (id) => {
  if (!id) return
  await nextTick()
  clauseTreeRef.value?.setCurrentKey(`clause-${id}`)
})

watch(selectedStandardId, async (id) => {
  if (!id) return
  await nextTick()
  standardTreeRef.value?.setCurrentKey(`standard-${id}`)
})

const openRegulationDialog = (row) => {
  Object.assign(regulationForm, row || { id: null, title: '', category: '', version: '', fileUrl: '', issuedAt: '' })
  regulationVisible.value = true
}

const saveRegulation = async () => {
  if (!regulationForm.title || !regulationForm.category || !regulationForm.version) {
    ElMessage.warning('请完整填写法规信息')
    return
  }
  if (regulationForm.id) {
    await updateRegulation(regulationForm.id, regulationForm)
  } else {
    await createRegulation(regulationForm)
  }
  ElMessage.success('保存成功')
  regulationVisible.value = false
  loadRegulations()
}

const removeRegulation = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该法规吗？', '确认删除', { type: 'warning' })
    await deleteRegulation(id)
    ElMessage.success('删除成功')
    loadRegulations()
  } catch (e) {
    // 用户取消
  }
}

const openClauseDialog = (row) => {
  Object.assign(clauseForm, row || { id: null, regulationId: null, chapterTitle: '', articleNo: '', clauseCategory: '', keywordTag: '', content: '' })
  clauseVisible.value = true
}

const saveClause = async () => {
  if (!clauseForm.regulationId || !clauseForm.articleNo || !clauseForm.clauseCategory || !clauseForm.content) {
    ElMessage.warning('请完整填写条款信息')
    return
  }
  if (clauseForm.id) {
    await updateRegulationClause(clauseForm.id, clauseForm)
  } else {
    await createRegulationClause(clauseForm)
  }
  ElMessage.success('保存成功')
  clauseVisible.value = false
  await loadClauses()
}

const openStandardDialog = (row) => {
  Object.assign(standardForm, row || {
    id: null,
    objectType: '',
    stationType: '',
    itemCode: '',
    itemName: '',
    legalClause: '',
    checkType: '',
    checkMethod: '',
    judgmentRule: '',
    severityLevel: '',
    enabled: true
  })
  standardVisible.value = true
}

const saveStandard = async () => {
  if (!standardForm.objectType || !standardForm.itemCode || !standardForm.itemName) {
    ElMessage.warning('请完整填写标准信息')
    return
  }
  if (standardForm.id) {
    await updateInspectionStandard(standardForm.id, standardForm)
  } else {
    await createInspectionStandard(standardForm)
  }
  ElMessage.success('保存成功')
  standardVisible.value = false
  await loadStandards()
}

const syncLibrary = async () => {
  const res = await syncRegulationLibrary()
  if (res.success) {
    ElMessage.success('同步成功')
    await loadClauses()
    await loadStandards()
  }
}

const saveBlob = (blob, filename) => {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

const downloadClauses = async () => {
  const blob = await downloadRegulationClauses()
  saveBlob(blob, 'regulation-clauses.csv')
}

const downloadStandards = async () => {
  const blob = await downloadInspectionStandards()
  saveBlob(blob, 'inspection-standards.csv')
}

onMounted(async () => {
  await loadRegulations()
  await loadClauses()
  await loadStandards()
})
</script>

<style scoped>
.mb-12 {
  margin-bottom: 12px;
}

.knowledge-layout {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 12px;
  min-height: 540px;
  align-items: start;
}

.knowledge-tree-panel,
.knowledge-detail-panel {
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  padding: 12px;
  background: #fff;
  max-height: calc(100vh - 220px);
  overflow: auto;
}

.knowledge-tree-panel {
  overflow: auto;
}

.knowledge-detail-panel {
  position: sticky;
  top: 16px;
}

.tree-panel-title {
  margin-bottom: 8px;
  font-weight: 600;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.detail-title {
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
}

.detail-subtitle {
  margin-top: 2px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.relation-title {
  margin: 14px 0 8px;
  font-size: 14px;
  font-weight: 600;
}

.file-url-link {
  max-width: 100%;
  display: inline-block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1200px) {
  .knowledge-layout {
    grid-template-columns: 1fr;
    min-height: 0;
  }

  .knowledge-detail-panel {
    position: static;
    max-height: none;
  }
}
</style>
