import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { useAuthStore } from '../stores/auth'

const shouldNullifyEmpty = (key) => /(Id|Date|At|Until|Deadline)$/i.test(String(key || ''))

const normalizePayload = (value, parentKey = '') => {
  if (Array.isArray(value)) {
    return value.map((item) => normalizePayload(item, parentKey))
  }
  if (value && typeof value === 'object' && !(value instanceof FormData) && !(value instanceof Date)) {
    return Object.keys(value).reduce((acc, key) => {
      acc[key] = normalizePayload(value[key], key)
      return acc
    }, {})
  }
  if (value === '' && shouldNullifyEmpty(parentKey)) {
    return null
  }
  return value
}

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000
})

service.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  const method = String(config.method || '').toLowerCase()
  if (['post', 'put', 'patch'].includes(method) && config.data && !(config.data instanceof FormData)) {
    config.data = normalizePayload(config.data)
  }
  return config
})

service.interceptors.response.use(
  (res) => res.data,
  (error) => {
    const status = error?.response?.status
    const requestUrl = error?.config?.url || ''
    const isLoginRequest = requestUrl.includes('/api/auth/login')
    const message =
      error?.response?.data?.message ||
      error?.response?.data?.error ||
      error?.message ||
      '请求失败'
    if (status === 401 && !isLoginRequest) {
      const auth = useAuthStore()
      auth.logout()
      if (router.currentRoute.value.path !== '/login') {
        router.push('/login')
      }
      ElMessage.error('登录已过期，请重新登录')
    } else if (status === 401 && isLoginRequest) {
      ElMessage.error(message)
    } else {
      ElMessage.error(message)
    }
    return Promise.reject(error)
  }
)

export default service
