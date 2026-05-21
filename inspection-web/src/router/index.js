import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import LoginView from '../views/LoginView.vue'
import LayoutView from '../views/LayoutView.vue'
import DashboardView from '../views/dashboard/DashboardView.vue'
import TaskView from '../views/task/TaskView.vue'
import InspectionView from '../views/inspection/InspectionView.vue'
import DataView from '../views/data/DataView.vue'
import ReportView from '../views/report/ReportView.vue'
import RegulationView from '../views/regulation/RegulationView.vue'
import WarningView from '../views/warning/WarningView.vue'
import OperationLogView from '../views/system/OperationLogView.vue'
import InstitutionView from '../views/institution/InstitutionView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView },
    {
      path: '/',
      component: LayoutView,
      redirect: '/dashboard',
      children: [
        {
          path: '/dashboard',
          component: DashboardView,
          meta: { roles: ['ADMIN', 'OPERATOR', 'INSPECTOR'] }
        },
        { path: '/task', component: TaskView, meta: { roles: ['ADMIN', 'OPERATOR', 'INSPECTOR'] } },
        { path: '/inspection', component: InspectionView, meta: { roles: ['ADMIN', 'OPERATOR', 'INSPECTOR'] } },
        { path: '/data', component: DataView, meta: { roles: ['ADMIN', 'OPERATOR', 'INSPECTOR'] } },
        { path: '/report', component: ReportView, meta: { roles: ['ADMIN', 'OPERATOR', 'INSPECTOR'] } },
        { path: '/regulation', component: RegulationView, meta: { roles: ['ADMIN', 'OPERATOR', 'INSPECTOR'] } },
        { path: '/warning', component: WarningView, meta: { roles: ['ADMIN', 'OPERATOR', 'INSPECTOR'] } },
        { path: '/operation-log', component: OperationLogView, meta: { roles: ['ADMIN', 'OPERATOR'] } },
        { path: '/institution', component: InstitutionView, meta: { roles: ['ADMIN'] } }
      ]
    }
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.path !== '/login' && !auth.token) {
    return '/login'
  }
  if (to.path === '/login' && auth.token) {
    return '/dashboard'
  }
  const roles = to.meta?.roles
  if (roles && !roles.includes(auth.user?.role || 'OPERATOR')) {
    return '/dashboard'
  }
  return true
})

export default router
