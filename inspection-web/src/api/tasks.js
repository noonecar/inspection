import request from '../utils/request'

export const fetchTasks = (params) => request.get('/api/tasks', { params })
export const createTask = (data) => request.post('/api/tasks', data)
export const updateTask = (id, data) => request.put(`/api/tasks/${id}`, data)
export const deleteTask = (id) => request.delete(`/api/tasks/${id}`)
export const batchAssignTasks = (data) => request.post('/api/tasks/batch-assign', data)
export const batchUpdateTaskStatus = (data) => request.post('/api/tasks/batch-status', data)
export const batchRemindTasks = (data) => request.post('/api/tasks/batch-remind', data)
export const batchCancelTasks = (data) => request.post('/api/tasks/batch-cancel', data)
export const auditTasks = (data) => request.post('/api/tasks/audit', data)
export const fetchTaskProgressSummary = () => request.get('/api/tasks/progress-summary')
export const fetchInspectors = () => request.get('/api/tasks/inspectors')
export const getTaskHistory = (taskId) => request.get(`/api/tasks/${taskId}/history`)
