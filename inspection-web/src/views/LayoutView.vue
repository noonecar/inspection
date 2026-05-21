<template>
  <el-container class="layout">
    <el-aside width="248px" class="sidebar">
      <div class="logo">
        <span>无线电监督检查</span>
        <el-button size="small" text class="custom-btn" @click="openCustomize">菜单定制</el-button>
      </div>
      <el-menu :default-active="active" router class="menu">
        <el-sub-menu index="fav" v-if="favoriteItems.length">
          <template #title>收藏夹</template>
          <el-menu-item
            v-for="item in favoriteItems"
            :key="item.path"
            :index="item.path"
          >
            {{ item.label }}
          </el-menu-item>
        </el-sub-menu>
        <el-menu-item
          v-for="item in visibleMenuItems"
          :key="item.path"
          :index="item.path"
          class="menu-item"
        >
          <span class="menu-label-wrapper">
            <span>{{ item.label }}</span>
            <span v-if="item.path === '/inspection' && inspectionBadgeCount > 0" class="inline-red-dot" />
          </span>
          <el-icon class="fav-icon" @click.stop="toggleFavorite(item)">
            <StarFilled v-if="isFavorite(item.path)" />
            <Star v-else />
          </el-icon>
        </el-menu-item>
      </el-menu>
      <div class="sidebar-footer">
        <div class="footer-line">举报电话：123-123-123</div>
        <div class="footer-line">
          举报网址：
          <a class="footer-link" href="https://www.tsinghua.edu.cn/">jubao.com</a>
        </div>
        <div class="footer-line">©2026 wl All rights reserved</div>
      </div>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-left">无线电频率使用与在用台站监督检查系统</div>
        <div class="header-right">
          <el-popover placement="bottom-end" width="360" trigger="click">
            <template #reference>
              <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" class="notify">
                <el-button text circle>
                  <el-icon><Bell /></el-icon>
                </el-button>
              </el-badge>
            </template>
            <div class="notify-list" @scroll="handleNotifyScroll">
              <div class="notify-header">
                <div class="notify-title">通知中心</div>
                <el-button text size="small" :disabled="!hasReadNotifications" @click="clearReadNotifications">
                  清空已读
                </el-button>
              </div>
              <div class="notify-summary">未读 {{ unreadCount }} 条 · 共 {{ notifications.length }} 条</div>
              <div v-if="!notifications.length" class="notify-empty">暂无消息</div>
              <div v-for="item in notifications" :key="item.id" class="notify-item" :class="{ 'is-unread': item.unread }">
                <div class="notify-head">
                  <div class="notify-title-text">{{ item.title }}</div>
                  <span v-if="item.unread" class="notify-dot" />
                </div>
                <div class="notify-text clickable" @click="jumpFromNotification(item)">{{ item.summary }}</div>
                <div class="notify-meta">
                  <el-tag size="small" type="info">{{ item.sourceLabel }}</el-tag>
                  <el-tag v-if="item.level" size="small" :type="item.level === '高' ? 'danger' : item.level === '中' ? 'warning' : 'info'">
                    {{ item.level }}
                  </el-tag>
                  <el-tag v-if="item.status" size="small" :type="item.status === '已处理' ? 'success' : item.status === '处理中' ? 'warning' : 'danger'">
                    {{ item.status }}
                  </el-tag>
                  <span class="notify-time">{{ item.timeLabel }}</span>
                  <span class="notify-relative">{{ item.relativeTime }}</span>
                </div>
              </div>
              <div v-if="notifyLoading" class="notify-loading">加载中...</div>
            </div>
          </el-popover>
          <div class="theme-toggle">
            <span class="theme-label">暗色</span>
            <el-switch v-model="darkMode" inline-prompt active-text="开" inactive-text="关" />
          </div>
          <el-tag type="info">当前角色：{{ roleLabel }}</el-tag>

          <el-button type="primary" text @click="handleLogout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>

  <el-drawer v-model="customizeVisible" title="菜单定制" size="360px">
    <div class="drawer-tip">拖拽排序，点击星标加入收藏夹</div>
    <draggable v-model="editableMenu" item-key="path" handle=".drag-handle">
      <template #item="{ element }">
        <div class="menu-editor-item">
          <el-icon class="drag-handle"><Sort /></el-icon>
          <span class="menu-name">{{ element.label }}</span>
          <el-icon class="fav-toggle" @click="toggleFavorite(element)">
            <StarFilled v-if="isFavorite(element.path)" />
            <Star v-else />
          </el-icon>
        </div>
      </template>
    </draggable>
    <template #footer>
      <el-button @click="customizeVisible = false">取消</el-button>
      <el-button type="primary" @click="saveCustomize">保存</el-button>
    </template>
  </el-drawer>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { getMe } from '../api/auth'
import { fetchWarningNotifications } from '../api/warnings'
import { fetchTasks } from '../api/tasks'
import { Bell, Sort, Star, StarFilled } from '@element-plus/icons-vue'
import draggable from 'vuedraggable'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const menuItems = ref([
  { label: '工作台', path: '/dashboard', roles: ['ADMIN', 'OPERATOR', 'INSPECTOR'] },
  { label: '人员管理', path: '/institution', roles: ['ADMIN'] },
  { label: '数据管理', path: '/data', roles: ['ADMIN', 'OPERATOR', 'INSPECTOR'] },
  { label: '任务统筹', path: '/task', roles: ['ADMIN', 'OPERATOR', 'INSPECTOR'] },
  { label: '检查执行', path: '/inspection', roles: ['ADMIN', 'OPERATOR', 'INSPECTOR'] },
  { label: '违规预警', path: '/warning', roles: ['ADMIN', 'OPERATOR', 'INSPECTOR'] },
  { label: '统计报表', path: '/report', roles: ['ADMIN', 'OPERATOR', 'INSPECTOR'] },
  { label: '法规标准', path: '/regulation', roles: ['ADMIN', 'OPERATOR', 'INSPECTOR'] },
  { label: '操作日志', path: '/operation-log', roles: ['ADMIN', 'OPERATOR'] }
])

const role = computed(() => auth.user?.role || 'OPERATOR')
const userKey = computed(() => auth.user?.username || 'guest')
const roleLabel = computed(() => {
  const map = {
    ADMIN: '管理员',
    OPERATOR: '普通操作员',
    INSPECTOR: '检查员',
  }
  return map[role.value] || '用户'
})

const active = computed(() => route.path)

const customizeVisible = ref(false)
const editableMenu = ref([])
const favorites = ref([])
const menuOrder = ref([])

const storageKey = (key) => `${key}:${userKey.value}`

const notifications = ref([])
const notifyReadIds = ref([])
const notifyDismissedIds = ref([])
const notifyTaskIds = ref([])
const notifyTaskCursor = ref(0)
const notifyLimit = ref(10)
const notifyLoading = ref(false)
let notifyPollTimer = null

const pad2 = (value) => String(value).padStart(2, '0')

const parseTimeString = (value) => {
  if (!value) return 0
  const match = String(value).match(/^(\d{4})[-/](\d{1,2})[-/](\d{1,2})[ T](\d{1,2}):(\d{1,2})(?::(\d{1,2}))?$/)
  if (match) {
    const [, y, m, d, h, mi, s] = match
    return new Date(Number(y), Number(m) - 1, Number(d), Number(h), Number(mi), Number(s || 0)).getTime()
  }
  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? 0 : parsed.getTime()
}

const toTimestamp = (value) => {
  if (!value) return 0
  if (typeof value === 'number') return value
  if (value instanceof Date) return value.getTime()
  if (Array.isArray(value)) {
    const [y, m, d, h = 0, mi = 0, s = 0] = value
    return new Date(Number(y), Number(m) - 1, Number(d), Number(h), Number(mi), Number(s)).getTime()
  }
  return parseTimeString(value)
}

const formatAbsoluteTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())} ${pad2(date.getHours())}:${pad2(date.getMinutes())}`
}

const formatRelativeTime = (timestamp) => {
  if (!timestamp) return ''
  const diff = Date.now() - timestamp
  if (diff < 0) return '刚刚'
  const seconds = Math.floor(diff / 1000)
  if (seconds < 60) return '刚刚'
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  if (days < 7) return `${days}天前`
  const months = Math.floor(days / 30)
  if (months < 12) return `${months}个月前`
  return `${Math.floor(months / 12)}年前`
}

const loadNotifyStorage = () => {
  notifyReadIds.value = JSON.parse(localStorage.getItem(storageKey('notifyRead')) || '[]')
  notifyDismissedIds.value = JSON.parse(localStorage.getItem(storageKey('notifyDismissed')) || '[]')
  notifyTaskIds.value = JSON.parse(localStorage.getItem(storageKey('notifyTaskIds')) || '[]')
  notifyTaskCursor.value = Number(localStorage.getItem(storageKey('notifyTaskCursor')) || 0)
}

const persistNotifyStorage = () => {
  localStorage.setItem(storageKey('notifyRead'), JSON.stringify(notifyReadIds.value))
  localStorage.setItem(storageKey('notifyDismissed'), JSON.stringify(notifyDismissedIds.value))
  localStorage.setItem(storageKey('notifyTaskIds'), JSON.stringify(notifyTaskIds.value))
  localStorage.setItem(storageKey('notifyTaskCursor'), String(notifyTaskCursor.value || 0))
}

const orderedMenuItems = computed(() => {
  const order = menuOrder.value
  if (!order.length) return menuItems.value
  const map = new Map(menuItems.value.map((item) => [item.path, item]))
  const ordered = order.map((path) => map.get(path)).filter(Boolean)
  const rest = menuItems.value.filter((item) => !order.includes(item.path))
  return [...ordered, ...rest]
})

const visibleMenuItems = computed(() =>
  orderedMenuItems.value.filter((item) => item.roles.includes(role.value))
)

const favoriteItems = computed(() =>
  visibleMenuItems.value.filter((item) => favorites.value.includes(item.path))
)

const unreadCount = computed(() => notifications.value.filter((item) => item.unread).length)
const hasReadNotifications = computed(() => notifications.value.some((item) => !item.unread))

const isFavorite = (path) => favorites.value.includes(path)

const toggleFavorite = (item) => {
  if (isFavorite(item.path)) {
    favorites.value = favorites.value.filter((p) => p !== item.path)
  } else {
    favorites.value = [...favorites.value, item.path]
  }
  localStorage.setItem(storageKey('menuFavorites'), JSON.stringify(favorites.value))
}

const openCustomize = () => {
  editableMenu.value = [...visibleMenuItems.value]
  customizeVisible.value = true
}

const saveCustomize = () => {
  const order = editableMenu.value.map((item) => item.path)
  menuOrder.value = order
  localStorage.setItem(storageKey('menuOrder'), JSON.stringify(order))
  customizeVisible.value = false
}

const inspectionBadgeCount = ref(0)
let badgePollTimer = null

const loadInspectionBadgeCount = async () => {
  if (!auth.token) {
    inspectionBadgeCount.value = 0
    return
  }
  // 检查 sessionStorage 中是否有新审核通过的任务 ID
  const newTaskIds = JSON.parse(sessionStorage.getItem('new-task-ids') || '[]')
  inspectionBadgeCount.value = newTaskIds.length
}

window.clearInspectionBadge = () => {
  // 仅在用户访问检查页面时清除侧边栏徽章
  inspectionBadgeCount.value = 0
}

const startBadgePoll = () => {
  stopBadgePoll()
  badgePollTimer = setInterval(loadInspectionBadgeCount, 30000)
}

const stopBadgePoll = () => {
  if (badgePollTimer) {
    clearInterval(badgePollTimer)
    badgePollTimer = null
  }
}

const darkMode = ref(false)

const loadUserProfile = async () => {
  if (!auth.token) return
  try {
    const res = await getMe()
    if (res.success) {
      auth.setAuth(auth.token, {
        username: res.data.username,
        realName: res.data.realName,
        role: res.data.role
      })
    }
  } catch (e) {
    // 忽略
  }
}

const loadMenuSettings = () => {
  favorites.value = JSON.parse(localStorage.getItem(storageKey('menuFavorites')) || '[]')
  menuOrder.value = JSON.parse(localStorage.getItem(storageKey('menuOrder')) || '[]')
}

const loadNotifications = async () => {
  if (!auth.token || notifyLoading.value) {
    notifications.value = []
    return
  }
  notifyLoading.value = true
  try {
    const readSet = new Set(notifyReadIds.value)
    const dismissedSet = new Set(notifyDismissedIds.value)

    const warningRes = await fetchWarningNotifications({ limit: notifyLimit.value })
    const warningItems = warningRes.success
      ? (warningRes.data || []).map((item) => {
          const timestamp = toTimestamp(item.time)
          return {
            id: `warning-${item.id}`,
            type: 'warning',
            sourceLabel: '预警',
            title: '预警通知',
            summary: item.text || '',
            level: item.level || '',
            status: item.status || '',
            time: item.time || '',
            timestamp,
            timeLabel: formatAbsoluteTime(timestamp),
            relativeTime: formatRelativeTime(timestamp),
            path: item.path || '/warning',
            relatedTaskId: item.relatedTaskId || null,
            warningId: item.id
          }
        })
      : []

    let taskItems = []
    if (role.value === 'INSPECTOR') {
      const taskRes = await fetchTasks({ page: 1, size: 200 })
      const taskRecords = Array.isArray(taskRes.data) ? taskRes.data : taskRes.data?.records || []
      const activeTasks = taskRecords.filter((task) => ['进行中', '待复检'].includes(task.status || ''))
      const storedTaskIds = new Set(notifyTaskIds.value)
      const activeTaskIds = new Set()
      let latestCursor = notifyTaskCursor.value

      activeTasks.forEach((task) => {
        activeTaskIds.add(task.id)
        const taskTime = toTimestamp(task.updatedAt || task.createdAt)
        if (taskTime > latestCursor) {
          latestCursor = taskTime
        }
        if (taskTime > notifyTaskCursor.value) {
          storedTaskIds.add(task.id)
        }
      })

      const filteredTaskIds = Array.from(storedTaskIds).filter((id) => activeTaskIds.has(id))
      notifyTaskIds.value = filteredTaskIds
      notifyTaskCursor.value = latestCursor
      persistNotifyStorage()

      taskItems = activeTasks
        .filter((task) => storedTaskIds.has(task.id))
        .map((task) => {
          const timestamp = toTimestamp(task.updatedAt || task.createdAt)
          const summary = `任务名称：${task.name || '未命名'} · 检查类别：${task.checkCategory || '-'} · 检查方式：${task.inspectionMode || '-'}`
          return {
            id: `task-${task.id}`,
            type: 'task',
            sourceLabel: '任务',
            title: '新任务通知',
            summary,
            level: '',
            status: task.status || '',
            time: task.updatedAt || task.createdAt || '',
            timestamp,
            timeLabel: formatAbsoluteTime(timestamp),
            relativeTime: formatRelativeTime(timestamp),
            path: '/inspection',
            relatedTaskId: task.id,
            taskId: task.id
          }
        })
    }

    const merged = [...warningItems, ...taskItems]
    const unique = new Map()
    merged.forEach((item) => {
      if (!unique.has(item.id)) {
        unique.set(item.id, item)
      }
    })

    notifications.value = Array.from(unique.values())
      .filter((item) => !dismissedSet.has(item.id))
      .map((item) => ({
        ...item,
        unread: !readSet.has(item.id)
      }))
      .sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0))
  } catch (e) {
    notifications.value = []
  } finally {
    notifyLoading.value = false
  }
}

const markNotificationRead = (item) => {
  if (!item) return
  if (!notifyReadIds.value.includes(item.id)) {
    notifyReadIds.value = [...notifyReadIds.value, item.id]
    persistNotifyStorage()
  }
  notifications.value = notifications.value.map((entry) =>
    entry.id === item.id ? { ...entry, unread: false } : entry
  )
}

const clearReadNotifications = () => {
  const dismissedSet = new Set(notifyDismissedIds.value)
  notifications.value.forEach((item) => {
    if (!item.unread) {
      dismissedSet.add(item.id)
    }
  })
  notifyDismissedIds.value = Array.from(dismissedSet)
  persistNotifyStorage()
  notifications.value = notifications.value.filter((item) => item.unread)
}

const handleNotifyScroll = (event) => {
  const target = event.target
  if (!target || notifyLoading.value) return
  const nearBottom = target.scrollTop + target.clientHeight >= target.scrollHeight - 16
  if (nearBottom) {
    notifyLimit.value += 10
    loadNotifications()
  }
}

const startNotifyPoll = () => {
  stopNotifyPoll()
  notifyPollTimer = setInterval(loadNotifications, 60000)
}

const stopNotifyPoll = () => {
  if (notifyPollTimer) {
    clearInterval(notifyPollTimer)
    notifyPollTimer = null
  }
}

const jumpFromNotification = (item) => {
  if (!item) return
  markNotificationRead(item)
  if (item.type === 'task' && item.taskId) {
    const newTaskIds = JSON.parse(sessionStorage.getItem('new-task-ids') || '[]')
    if (!newTaskIds.includes(item.taskId)) {
      newTaskIds.push(item.taskId)
      sessionStorage.setItem('new-task-ids', JSON.stringify(newTaskIds))
    }
    router.push({ path: '/inspection' })
    return
  }
  if (item.path === '/task' && item.relatedTaskId) {
    router.push({ path: '/task', query: { taskId: item.relatedTaskId } })
    return
  }
  if (item.type === 'warning' && item.warningId) {
    router.push({ path: '/warning', query: { warningId: item.warningId } })
    return
  }
  router.push(item.path || '/warning')
}

watch(userKey, () => {
  loadMenuSettings()
  loadNotifyStorage()
  loadNotifications()
})

onMounted(() => {
  const saved = localStorage.getItem('theme')
  darkMode.value = saved === 'dark'
  document.documentElement.setAttribute('data-theme', darkMode.value ? 'dark' : 'light')
  loadUserProfile()
  loadMenuSettings()
  loadNotifyStorage()
  loadNotifications()
  loadInspectionBadgeCount()
  startBadgePoll()
  startNotifyPoll()
})

onUnmounted(() => {
  stopBadgePoll()
  stopNotifyPoll()
})

watch(darkMode, (val) => {
  document.documentElement.setAttribute('data-theme', val ? 'dark' : 'light')
  localStorage.setItem('theme', val ? 'dark' : 'light')
})

const handleLogout = () => {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  height: 100%;
}

.sidebar {
  background: var(--sidebar-bg);
  color: #fff;
  padding: 16px 0;
  display: flex;
  flex-direction: column;
}

.logo {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  padding: 0 20px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.custom-btn {
  color: #a5b4fc;
}

.menu {
  flex: 1;
}

.menu-badge {
  display: inline-block;
}

.menu-badge-right {
  display: inline-flex;
  align-items: center;
}

.menu-label-wrapper {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.inline-red-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  background-color: #f56c6c;
  border-radius: 50%;
  vertical-align: middle;
  flex-shrink: 0;
}

:deep(.el-menu) {
  border-right: none;
  background: transparent;
}

:deep(.el-menu-item) {
  color: var(--sidebar-text);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

:deep(.el-menu-item.is-active) {
  background: rgba(96, 165, 250, 0.2);
  color: #fff;
}

.fav-icon {
  font-size: 14px;
  color: #94a3b8;
}

.sidebar-footer {
  padding: 12px 20px 0;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.6;
}

.footer-line {
  margin-bottom: 4px;
}

.footer-link {
  color: #a5b4fc;
  text-decoration: none;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--header-bg);
  box-shadow: var(--shadow);
}

.header-left {
  font-weight: 600;
  color: var(--text-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.notify {
  margin-right: 4px;
}

.notify-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 360px;
  overflow-y: auto;
  padding-right: 4px;
}

.notify-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.notify-title {
  font-weight: 600;
  color: var(--text-primary);
}

.notify-summary {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: -6px;
}

.notify-item {
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 8px 10px;
  background: var(--card-bg);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.notify-item.is-unread {
  border-color: rgba(37, 99, 235, 0.45);
  box-shadow: 0 0 0 1px rgba(37, 99, 235, 0.2);
}

.notify-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 4px;
}

.notify-title-text {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.notify-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #f97316;
  flex-shrink: 0;
}

.notify-text {
  font-size: 13px;
  color: var(--text-primary);
}

.clickable {
  cursor: pointer;
}

.notify-time {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.notify-relative {
  font-size: 12px;
  color: var(--text-secondary);
}

.notify-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  margin-top: 6px;
}

.notify-empty {
  color: var(--text-secondary);
  font-size: 13px;
}

.notify-loading {
  font-size: 12px;
  color: var(--text-secondary);
  text-align: center;
  padding: 4px 0;
}

.theme-toggle {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  border-radius: 999px;
  background: var(--primary-soft);
  color: var(--text-primary);
}

.theme-label {
  font-size: 12px;
}

.main {
  padding: 20px;
}

.drawer-tip {
  color: var(--text-secondary);
  font-size: 12px;
  margin-bottom: 12px;
}

.menu-editor-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 10px;
  margin-bottom: 10px;
  background: var(--card-bg);
}

.drag-handle {
  cursor: grab;
  color: var(--text-secondary);
}

.menu-name {
  flex: 1;
}

.fav-toggle {
  cursor: pointer;
  color: var(--warning);
}
</style>
