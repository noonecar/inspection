<template>
  <div class="page-card">
    <div class="page-title">统计报表</div>

    <el-space class="mb-12" wrap>
      <el-input v-model="query.region" placeholder="区域（按地址关键字）" clearable style="width: 180px" />
      <el-select v-model="query.inspectionMode" placeholder="检查类型" clearable style="width: 130px">
        <el-option label="日常检查" value="日常检查" />
        <el-option label="重点检查" value="重点检查" />
      </el-select>
      <el-select v-model="query.violationLevel" placeholder="违规等级" clearable style="width: 120px">
        <el-option label="高" value="高" />
        <el-option label="中" value="中" />
        <el-option label="低" value="低" />
      </el-select>
      <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" />
      <el-button type="primary" @click="loadStats">统计</el-button>
      <el-button @click="resetQuery">重置</el-button>
      <el-button @click="handleExport('csv')">导出CSV</el-button>
      <el-button @click="handleExport('pdf')">导出PDF</el-button>
      <el-button v-if="canCreate" @click="openDialog">生成报表记录</el-button>
    </el-space>

    <el-row :gutter="16" class="mb-12" ref="summaryRowRef">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" style="border-left: 4px solid #409EFF">
          <div class="stat-title">检查记录总量</div>
          <div class="stat-value" style="color: #409EFF">{{ stats.recordTotal || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" style="border-left: 4px solid #F56C6C">
          <div class="stat-title">预警总量</div>
          <div class="stat-value" style="color: #F56C6C">{{ stats.warningTotal || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" style="border-left: 4px solid #67C23A">
          <div class="stat-title">已存报表</div>
          <div class="stat-value" style="color: #67C23A">{{ reports.length }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card" style="border-left: 4px solid #909399">
          <div class="stat-title">统计周期</div>
          <div class="stat-value" style="font-size: 16px; margin-top: 8px; color: #909399">{{ dateRangeText }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" ref="chartsRowOneRef">
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>检查结果分布（饼图）</template>
          <div ref="resultPieRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>任务状态分布（柱状图）</template>
          <div ref="typeBarRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt-12" ref="chartsRowTwoRef">
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>六类检查分布（折线图）</template>
          <div ref="categoryLineRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>违规等级分布（柱状图）</template>
          <div ref="violationBarRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-divider />

    <el-table :data="reports" stripe border ref="reportTableRef">
      <el-table-column prop="name" label="报表名称" min-width="220" />
      <el-table-column prop="reportType" label="报表类型" width="160" />
      <el-table-column label="生成时间" width="180">
        <template #default="scope">{{ formatDateTime(scope.row.generatedAt) }}</template>
      </el-table-column>
      <el-table-column prop="summary" label="摘要" min-width="240" show-overflow-tooltip />
      <el-table-column label="操作" width="160">
        <template #default="scope">
          <el-button size="small" text @click="viewSnapshot(scope.row)">查看</el-button>
          <el-button v-if="canDelete" size="small" text type="danger" @click="remove(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" title="生成报表" width="560px">
      <el-form :model="form" label-width="108px">
        <el-form-item label="报表名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="报表类型">
          <el-select v-model="form.reportType" placeholder="请选择">
            <el-option label="区域统计" value="区域统计" />
            <el-option label="时间周期统计" value="时间周期统计" />
            <el-option label="检查类型统计" value="检查类型统计" />
            <el-option label="违规等级统计" value="违规等级统计" />
            <el-option label="自定义配置统计" value="自定义配置统计" />
          </el-select>
        </el-form-item>
        <el-form-item label="自定义配置">
          <el-input v-model="form.summary" type="textarea" rows="3" placeholder="例如：区域=河北；周期=近3个月；重点检查=是" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="viewVisible" title="报表数据快照" width="600px">
      <template v-if="viewData">
        <div style="font-size:16px;font-weight:600;margin-bottom:16px">{{ viewData.name }}</div>
        <el-table :data="viewData.items" stripe border>
          <el-table-column prop="label" label="指标" width="200" />
          <el-table-column prop="value" label="数据" />
        </el-table>
      </template>
      <template #footer>
        <el-button @click="viewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import html2canvas from 'html2canvas'
import jsPDF from 'jspdf'
import { createReport, deleteReport, exportReport, fetchReportDetail, fetchReportStats, fetchReports } from '../../api/reports'
import { useAuthStore } from '../../stores/auth'

const resultPieRef = ref(null)
const typeBarRef = ref(null)
const categoryLineRef = ref(null)
const violationBarRef = ref(null)
const summaryRowRef = ref(null)
const chartsRowOneRef = ref(null)
const chartsRowTwoRef = ref(null)
const reports = ref([])
const stats = ref({})
const visible = ref(false)
const dateRange = ref([])

const query = ref({
  region: '',
  inspectionMode: '',
  violationLevel: '',
  startDate: '',
  endDate: ''
})

const form = ref({
  name: '',
  reportType: '',
  summary: ''
})

const viewVisible = ref(false)
const viewData = ref(null)

const auth = useAuthStore()
const role = computed(() => auth.user?.role || 'OPERATOR')
const canCreate = computed(() => ['ADMIN', 'OPERATOR'].includes(role.value))
const canDelete = computed(() => role.value === 'ADMIN')

const dateRangeText = computed(() => {
  if (!dateRange.value || !dateRange.value.length) {
    return '默认近6个月'
  }
  return `${dateRange.value[0]} 至 ${dateRange.value[1]}`
})

const toSeriesData = (obj = {}) => Object.entries(obj).map(([name, value]) => ({ name, value }))

const formatDateTime = (value) => {
  if (!value) return ''
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value
    if (!year || !month || !day) return ''
    return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')} ${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}:${String(second).padStart(2, '0')}`
  }
  return String(value).slice(0, 19).replace('T', ' ')
}

const renderCharts = () => {
  const resultData = toSeriesData(stats.value.resultDistribution)
  const typeData = toSeriesData(stats.value.taskStatusDistribution)
  const categoryData = toSeriesData(stats.value.checkCategoryDistribution)
  const violationData = toSeriesData(stats.value.violationDistribution)

  if (resultPieRef.value) {
    const chart = echarts.init(resultPieRef.value)
    chart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie',
        radius: ['35%', '70%'],
        data: resultData.map(item => ({
          ...item,
          itemStyle: {
            color: item.name === '合格' ? '#67C23A' : item.name === '不合格' ? '#F56C6C' : '#909399'
          }
        }))
      }]
    })
  }

  if (typeBarRef.value) {
    const chart = echarts.init(typeBarRef.value)
    const statusColorMap = {
      '进行中': '#409EFF',
      '待复检': '#E6A23C',
      '未开始': '#909399',
      '已完成': '#67C23A',
      '待审核': '#9B59B6'
    }
    chart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: typeData.map((item) => item.name) },
      yAxis: { type: 'value' },
      series: [{
        type: 'bar',
        data: typeData.map(item => ({
          value: item.value,
          itemStyle: { color: statusColorMap[item.name] || '#5b8ff9' }
        }))
      }]
    })
  }

  if (categoryLineRef.value) {
    const chart = echarts.init(categoryLineRef.value)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: categoryData.map((item) => item.name) },
      yAxis: { type: 'value' },
      series: [{ type: 'line', smooth: true, areaStyle: {}, data: categoryData.map((item) => item.value) }]
    })
  }

  if (violationBarRef.value) {
    const chart = echarts.init(violationBarRef.value)
    const levels = ['高', '中', '低']
    const vData = stats.value.violationDistribution || {}
    const unprocessedPrefixes = ['未处理', '处理中']
    const redData = levels.map(level =>
      unprocessedPrefixes.reduce((sum, s) => sum + (Number(vData[s + '_' + level]) || 0), 0)
    )
    const greenData = levels.map(level =>
      Number(vData['已处理_' + level] || 0)
    )
    const levelColorMap = { '高': '#F56C6C', '中': '#E6A23C', '低': '#f0c040' }
    chart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['未处理/处理中', '已处理'] },
      xAxis: { type: 'category', data: levels },
      yAxis: { type: 'value' },
      series: [
        {
          name: '未处理/处理中',
          type: 'bar',
          data: levels.map((level, i) => ({
            value: redData[i],
            itemStyle: { color: levelColorMap[level] }
          }))
        },
        { name: '已处理', type: 'bar', data: greenData, itemStyle: { color: '#67C23A' } }
      ]
    })
  }
}

const loadReports = async () => {
  const res = await fetchReports({ page: 1, size: 100 })
  if (res.success) {
    const records = Array.isArray(res.data) ? res.data : res.data.records
    reports.value = records || []
  }
}

const loadStats = async () => {
  query.value.startDate = dateRange.value?.[0] || ''
  query.value.endDate = dateRange.value?.[1] || ''
  const res = await fetchReportStats(query.value)
  if (res.success) {
    stats.value = res.data || {}
    await nextTick()
    renderCharts()
  }
}

const resetQuery = () => {
  dateRange.value = []
  query.value = { region: '', inspectionMode: '', violationLevel: '', startDate: '', endDate: '' }
  loadStats()
}

const openDialog = () => {
  form.value = { name: '', reportType: '', summary: '' }
  visible.value = true
}

const save = async () => {
  if (!form.value.name || !form.value.reportType) {
    ElMessage.warning('请填写报表名称和类型')
    return
  }
  form.value.dataSnapshot = JSON.stringify(stats.value)
  const filters = [
    '区域: ' + (query.value.region || '全部'),
    '检查类型: ' + (query.value.inspectionMode || '全部'),
    '违规等级: ' + (query.value.violationLevel || '全部'),
    '周期: ' + dateRangeText.value
  ].join('; ')
  form.value.summary = '筛选条件: ' + filters + ' | 检查记录: ' + (stats.value.recordTotal || 0) + ', 预警: ' + (stats.value.warningTotal || 0)
  await createReport(form.value)
  ElMessage.success('报表记录已生成')
  visible.value = false
  loadReports()
}

const remove = async (id) => {
  await deleteReport(id)
  ElMessage.success('删除成功')
  loadReports()
}

const formatSnapshot = (snapshot) => {
  if (!snapshot) return []
  const lines = []
  const dist = snapshot.resultDistribution
  if (dist) {
    const parts = Object.entries(dist).map(([k, v]) => k + '=' + v)
    lines.push({ label: '检查结果分布', value: parts.join('  ') })
  }
  const taskDist = snapshot.taskStatusDistribution
  if (taskDist) {
    const parts = Object.entries(taskDist).map(([k, v]) => k + '=' + v)
    lines.push({ label: '任务状态分布', value: parts.join('  ') })
  }
  const catDist = snapshot.checkCategoryDistribution
  if (catDist) {
    const parts = Object.entries(catDist).map(([k, v]) => k + '=' + v)
    lines.push({ label: '六类检查分布', value: parts.join('  ') })
  }
  const violDist = snapshot.violationDistribution
  if (violDist) {
    const levels = ['高', '中', '低']
    const unprocessed = ['未处理', '处理中']
    const redParts = levels.map(l => {
      const count = unprocessed.reduce((s, p) => s + (Number(violDist[p + '_' + l]) || 0), 0)
      return l + '=' + count
    })
    const greenParts = levels.map(l => l + '=' + (Number(violDist['已处理_' + l]) || 0))
    lines.push({ label: '违规等级分布（未处理/处理中）', value: redParts.join('  ') })
    lines.push({ label: '违规等级分布（已处理）', value: greenParts.join('  ') })
  }
  lines.push({ label: '检查记录总量', value: String(snapshot.recordTotal || 0) })
  lines.push({ label: '预警总量', value: String(snapshot.warningTotal || 0) })
  return lines
}

const resolveSnapshotData = async (row) => {
  if (row.dataSnapshot) {
    try {
      return JSON.parse(row.dataSnapshot)
    } catch {
      return null
    }
  }
  try {
    const res = await fetchReportDetail(row.id)
    if (res.success && res.data && res.data.dataSnapshot) {
      return JSON.parse(res.data.dataSnapshot)
    }
  } catch {
    // 忽略
  }
  return null
}

const viewSnapshot = async (row) => {
  const snapshot = await resolveSnapshotData(row)
  if (!snapshot) {
    ElMessage.warning('该报表记录没有统计数据快照')
    return
  }
  viewData.value = { name: row.name, items: formatSnapshot(snapshot) }
  viewVisible.value = true
}

const createPdfLayoutState = (pdf) => {
  const margin = 10
  const gap = 6
  const pageWidth = pdf.internal.pageSize.getWidth()
  const pageHeight = pdf.internal.pageSize.getHeight()
  return {
    margin,
    gap,
    pageWidth,
    pageHeight,
    contentWidth: pageWidth - margin * 2,
    cursorY: margin
  }
}

const addCanvasToPdf = (pdf, canvas, layoutState) => {
  if (!canvas || !layoutState) {
    return
  }

  let sourceY = 0
  const pxPerMm = canvas.width / layoutState.contentWidth

  while (sourceY < canvas.height) {
    const remainMm = layoutState.pageHeight - layoutState.margin - layoutState.cursorY
    if (remainMm < 12) {
      pdf.addPage()
      layoutState.cursorY = layoutState.margin
      continue
    }

    const sliceHeightPx = Math.min(Math.floor(remainMm * pxPerMm), canvas.height - sourceY)
    if (sliceHeightPx <= 0) {
      pdf.addPage()
      layoutState.cursorY = layoutState.margin
      continue
    }

    const piece = document.createElement('canvas')
    piece.width = canvas.width
    piece.height = sliceHeightPx
    const ctx = piece.getContext('2d')
    ctx?.drawImage(canvas, 0, sourceY, canvas.width, sliceHeightPx, 0, 0, canvas.width, sliceHeightPx)

    const pieceHeightMm = sliceHeightPx / pxPerMm
    pdf.addImage(piece, 'JPEG', layoutState.margin, layoutState.cursorY, layoutState.contentWidth, pieceHeightMm, undefined, 'FAST')

    piece.width = 0
    piece.height = 0
    sourceY += sliceHeightPx
    layoutState.cursorY += pieceHeightMm + layoutState.gap

    if (sourceY < canvas.height) {
      pdf.addPage()
      layoutState.cursorY = layoutState.margin
    }
  }
}

const EXPORT_CANVAS_OPTIONS = {
  scale: 1.35,
  useCORS: true,
  backgroundColor: '#ffffff'
}

const createPdfHeaderContainer = () => {
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  const timestamp = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`

  const line1 = `导出时间: ${timestamp}`
  const line2 = `筛选条件: 区域=${query.value.region || '全部'}; 检查类型=${query.value.inspectionMode || '全部'}; 违规等级=${query.value.violationLevel || '全部'}`
  const line3 = `统计周期: ${dateRange.value?.[0] || '默认近6个月'} 至 ${dateRange.value?.[1] || '当前'}`

  const container = document.createElement('div')
  container.style.position = 'fixed'
  container.style.left = '-10000px'
  container.style.top = '0'
  container.style.width = '1200px'
  container.style.background = '#ffffff'
  container.style.color = '#111111'
  container.style.padding = '28px 32px 20px'
  container.style.boxSizing = 'border-box'
  container.style.fontFamily = '"Microsoft YaHei", "PingFang SC", "Noto Sans SC", sans-serif'

  const title = document.createElement('div')
  title.textContent = '无线电监督检查统计报表'
  title.style.fontSize = '34px'
  title.style.fontWeight = '700'
  title.style.marginBottom = '14px'

  const intro = document.createElement('div')
  intro.style.fontSize = '18px'
  intro.style.lineHeight = '1.8'
  intro.textContent = [line1, line2, line3].join('\n')
  intro.style.whiteSpace = 'pre-wrap'

  container.appendChild(title)
  container.appendChild(intro)
  return container
}

const renderHeaderCanvas = async () => {
  const container = createPdfHeaderContainer()
  document.body.appendChild(container)
  try {
    return await html2canvas(container, EXPORT_CANVAS_OPTIONS)
  } finally {
    document.body.removeChild(container)
  }
}

const releaseCanvas = (canvas) => {
  if (!canvas) {
    return
  }
  canvas.width = 0
  canvas.height = 0
}

const resolveExportElement = (target) => {
  if (!target) {
    return null
  }
  if (target instanceof HTMLElement) {
    return target
  }
  if (target.$el instanceof HTMLElement) {
    return target.$el
  }
  if (target.$?.subTree?.el instanceof HTMLElement) {
    return target.$.subTree.el
  }
  return null
}

const exportPdfWithLayout = async () => {
  await nextTick()
  await new Promise((resolve) => requestAnimationFrame(resolve))

  const summaryRowEl = resolveExportElement(summaryRowRef.value)
  const chartRowOneEl = resolveExportElement(chartsRowOneRef.value)
  const chartRowTwoEl = resolveExportElement(chartsRowTwoRef.value)

  if (!summaryRowEl || !document.body.contains(summaryRowEl)) {
    throw new Error('统计区域尚未渲染完成，请稍后重试')
  }

  const pdf = new jsPDF('p', 'mm', 'a4')
  pdf.setProperties({ title: '统计报表' })
  const layoutState = createPdfLayoutState(pdf)

  const headerCanvas = await renderHeaderCanvas()
  addCanvasToPdf(pdf, headerCanvas, layoutState)
  releaseCanvas(headerCanvas)

  const summaryCanvas = await html2canvas(summaryRowEl, EXPORT_CANVAS_OPTIONS)
  addCanvasToPdf(pdf, summaryCanvas, layoutState)
  releaseCanvas(summaryCanvas)

  if (chartRowOneEl && document.body.contains(chartRowOneEl)) {
    const chart1 = await html2canvas(chartRowOneEl, EXPORT_CANVAS_OPTIONS)
    addCanvasToPdf(pdf, chart1, layoutState)
    releaseCanvas(chart1)
  }

  if (chartRowTwoEl && document.body.contains(chartRowTwoEl)) {
    const chart2 = await html2canvas(chartRowTwoEl, EXPORT_CANVAS_OPTIONS)
    addCanvasToPdf(pdf, chart2, layoutState)
    releaseCanvas(chart2)
  }

  pdf.save('statistics-report.pdf')
}

const handleExport = async (format) => {
  try {
    if (format === 'pdf') {
      await exportPdfWithLayout()
      ElMessage.success('PDF导出成功')
      return
    }

    query.value.startDate = dateRange.value?.[0] || ''
    query.value.endDate = dateRange.value?.[1] || ''
    const blob = await exportReport({
      format,
      region: query.value.region,
      inspectionMode: query.value.inspectionMode,
      violationLevel: query.value.violationLevel,
      startDate: query.value.startDate,
      endDate: query.value.endDate
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = format === 'pdf' ? 'statistics-report.pdf' : 'statistics-report.csv'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('CSV导出成功')
  } catch (error) {
    ElMessage.error(error?.message || '导出失败，请重试')
  }
}

onMounted(async () => {
  await loadReports()
  await loadStats()
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

.chart-card {
  border-radius: 14px;
}

.chart {
  width: 100%;
  height: 320px;
}
</style>
