<template>
  <div class="page-card">
    <div class="page-title">
      <span>工作台概览</span>
      <el-button size="small" text @click="toggleEdit">{{ editMode ? '完成' : '编辑工作台' }}</el-button>
    </div>

    <draggable v-model="cards" item-key="label" class="card-grid" :disabled="!editMode">
      <template #item="{ element }">
        <el-card class="stat-card" @click="goto(element.path)">
          <div class="stat-value" :style="{ color: element.color }">{{ element.value }}</div>
          <div class="stat-label">{{ element.label }}</div>
          <div class="stat-trend" :class="element.trend >= 0 ? 'up' : 'down'">
            {{ element.trend >= 0 ? '↑' : '↓' }}{{ Math.abs(element.trend) }} {{ element.trendLabel }}
          </div>
          <div v-if="editMode" class="drag-mask">拖拽调整</div>
        </el-card>
      </template>
    </draggable>

    <el-divider />

    <draggable v-model="widgets" item-key="key" class="widget-grid" :disabled="!editMode">
      <template #item="{ element }">
        <el-card class="widget-card">
          <template #header>
            <div class="widget-header">
              <span>{{ element.title }}</span>
              <el-tag size="small" type="info" v-if="editMode">拖拽排序</el-tag>
            </div>
          </template>

          <div v-if="element.key === 'progress'" class="gantt">
            <div class="gantt-item clickable" v-for="task in taskSchedule" :key="task.name" @click="gotoTaskByStatus(task.status)">
              <div class="gantt-info">
                <div class="gantt-title">{{ task.name }}</div>
                <el-tag :type="task.statusType" size="small">{{ task.status }}</el-tag>
              </div>
              <div class="gantt-meta">{{ task.start }} - {{ task.end }}</div>
              <div class="gantt-bar">
                <div class="gantt-progress" :style="{ width: task.progress + '%', background: task.color }"></div>
              </div>
            </div>
          </div>

          <div v-else-if="element.key === 'insights'" class="insights">
            <div class="insight-card clickable" v-for="item in insights" :key="item.title" @click="goto(item.path)">
              <div class="insight-title">{{ item.title }}</div>
              <div class="insight-value">{{ item.value }}</div>
              <div class="insight-desc">{{ item.desc }}</div>
            </div>
          </div>

          <div v-else class="tips">
            
          </div>
        </el-card>
      </template>
    </draggable>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchOverview } from '../../api/reports'
import { fetchTaskProgressSummary } from '../../api/tasks'
import { fetchWarnings } from '../../api/warnings'
import draggable from 'vuedraggable'

const router = useRouter()
const cards = ref([
  { label: '任务总数', value: 0, path: '/task', trend: 0, trendLabel: '较上周', color: '#2563eb' },
  { label: '检查记录', value: 0, path: '/inspection', trend: 0, trendLabel: '较上周', color: '#16a34a' },
  { label: '预警记录', value: 0, path: '/warning', trend: 0, trendLabel: '较上周', color: '#f59e0b' },
  { label: '台站数量', value: 0, path: '/data', trend: 0, trendLabel: '较上周', color: '#6366f1' }
])

const widgets = ref([
  { key: 'progress', title: '近期任务进度' },
  { key: 'insights', title: '运行洞察' }
])

const editMode = ref(false)

const taskSchedule = ref([])
const progressSummary = ref({ total: 0, reviewing: 0, completed: 0 })
const pendingWarningCount = ref(0)

const goto = (path) => router.push(path)
const gotoTaskByStatus = (status) => router.push({ path: '/task', query: { status } })

const toggleEdit = () => {
  editMode.value = !editMode.value
  if (!editMode.value) {
    localStorage.setItem('dashboardCards', JSON.stringify(cards.value.map((item) => item.label)))
    localStorage.setItem('dashboardWidgets', JSON.stringify(widgets.value.map((item) => item.key)))
  }
}

const insights = computed(() => {
  const total = Number(progressSummary.value.total || 0)
  const completed = Number(progressSummary.value.completed || 0)
  const reviewing = Number(progressSummary.value.reviewing || 0)
  const completionRate = total ? Math.round((completed * 100) / total) : 0

  return [
    {
      title: '待审核任务',
      value: `${reviewing}项`,
      desc: '点击进入检查执行模块处理',
      path: '/inspection'
    },
    {
      title: '仍存在预警',
      value: `${pendingWarningCount.value}项`,
      desc: '点击进入违规预警模块查看',
      path: '/warning'
    },
    {
      title: '任务完成率',
      value: `${completionRate}%`,
      desc: '点击进入统计报表查看趋势',
      path: '/report'
    }
  ]
})

onMounted(async () => {
  const storedCards = JSON.parse(localStorage.getItem('dashboardCards') || '[]')
  if (storedCards.length) {
    cards.value = storedCards
      .map((label) => cards.value.find((item) => item.label === label))
      .filter(Boolean)
      .concat(cards.value.filter((item) => !storedCards.includes(item.label)))
  }
  const storedWidgets = JSON.parse(localStorage.getItem('dashboardWidgets') || '[]')
  if (storedWidgets.length) {
    widgets.value = storedWidgets
      .map((key) => widgets.value.find((item) => item.key === key))
      .filter(Boolean)
      .concat(widgets.value.filter((item) => !storedWidgets.includes(item.key)))
  }
  try {
    const [overviewRes, progressRes, pendingRes, processingRes] = await Promise.all([
      fetchOverview(),
      fetchTaskProgressSummary(),
      fetchWarnings({ page: 1, size: 1, status: '未处理' }),
      fetchWarnings({ page: 1, size: 1, status: '处理中' })
    ])

    const pendingWarningTotal = (pendingRes.success ? Number(pendingRes.data?.total || 0) : 0) + (processingRes.success ? Number(processingRes.data?.total || 0) : 0)

    if (overviewRes.success) {
      const data = {
        任务总数: overviewRes.data.taskCount,
        检查记录: overviewRes.data.recordCount,
        预警记录: overviewRes.data.warningCount,
        台站数量: overviewRes.data.stationCount
      }
      const prev = JSON.parse(localStorage.getItem('overviewSnapshot') || '{}')
      cards.value = cards.value.map((item) => {
        const current = data[item.label] ?? item.value
        const delta = prev[item.label] ? current - prev[item.label] : 0
        return { ...item, value: current, trend: delta }
      })
      localStorage.setItem('overviewSnapshot', JSON.stringify(data))
    }

    if (progressRes.success) {
      const data = progressRes.data || {}
      progressSummary.value = {
        total: Number(data.total || 0),
        reviewing: Number(data.reviewing || 0),
        completed: Number(data.completed || 0)
      }
      const total = Number(data.total || 0)
      const toRate = (count) => {
        if (!total) return 0
        return Math.max(0, Math.min(100, Math.round((Number(count || 0) * 100) / total)))
      }
      taskSchedule.value = [
        {
          name: '进行中任务',
          start: '本周',
          end: `${data.ongoing || 0}项`,
          status: '进行中',
          statusType: 'success',
          progress: toRate(data.ongoing),
          color: '#22c55e'
        },
        {
          name: '待审核任务',
          start: '本周',
          end: `${data.reviewing || 0}项`,
          status: '待审核',
          statusType: 'info',
          progress: toRate(data.reviewing),
          color: '#3b82f6'
        },
        {
          name: '已完成任务',
          start: '本周',
          end: `${data.completed || 0}项`,
          status: '已完成',
          statusType: 'success',
          progress: toRate(data.completed),
          color: '#2563eb'
        }
      ]
    }

    // 工作台上的“未处理预警”统计按“未处理 + 处理中”聚合
    pendingWarningCount.value = pendingWarningTotal
  } catch (e) {
    // 忽略
  }
})
</script>

<style scoped>
.stat-card {
  text-align: center;
  position: relative;
  cursor: pointer;
  border-radius: 16px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: var(--primary);
}

.stat-label {
  margin-top: 6px;
  color: var(--text-secondary);
}

.stat-trend {
  margin-top: 8px;
  font-size: 12px;
}

.stat-trend.up {
  color: var(--success);
}

.stat-trend.down {
  color: var(--danger);
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow);
}

.drag-mask {
  position: absolute;
  inset: 0;
  border-radius: 16px;
  background: rgba(15, 23, 42, 0.08);
  color: var(--text-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.widget-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.widget-card {
  border-radius: 16px;
}

.widget-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.gantt {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.gantt-item {
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 12px;
  background: var(--card-bg);
}

.clickable {
  cursor: pointer;
}

.gantt-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
  color: var(--text-primary);
}

.gantt-meta {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.gantt-bar {
  height: 8px;
  background: var(--primary-soft);
  border-radius: 999px;
  overflow: hidden;
}

.gantt-progress {
  height: 100%;
  border-radius: 999px;
}

.insights {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.insight-card {
  padding: 12px;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: var(--card-bg);
}

.insight-title {
  font-size: 12px;
  color: var(--text-secondary);
}

.insight-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 6px 0;
}

.insight-desc {
  font-size: 12px;
  color: var(--text-secondary);
}

.tips {
  color: var(--text-secondary);
  font-size: 13px;
}

@media (max-width: 1200px) {
  .card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .widget-grid {
    grid-template-columns: 1fr;
  }
}
</style>
