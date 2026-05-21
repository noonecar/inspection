import request from '../utils/request'

export const fetchWarnings = (params) => request.get('/api/warnings', { params })
export const fetchWarningNotifications = (params) => request.get('/api/warnings/notifications', { params })
export const createWarning = (data) => request.post('/api/warnings', data)
export const updateWarning = (id, data) => request.put(`/api/warnings/${id}`, data)
export const deleteWarning = (id) => request.delete(`/api/warnings/${id}`)
export const resolveWarning = (id) => request.put(`/api/warnings/${id}/resolve`)
export const fetchWarningHistory = (taskId) => request.get(`/api/warnings/history/${taskId}`)
