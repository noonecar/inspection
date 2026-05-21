import request from '../utils/request'

export const fetchOverview = () => request.get('/api/reports/overview')
export const fetchReports = (params) => request.get('/api/reports', { params })
export const fetchReportStats = (params) => request.get('/api/reports/stats', { params })
export const createReport = (data) => request.post('/api/reports', data)
export const deleteReport = (id) => request.delete(`/api/reports/${id}`)
export const fetchReportDetail = (id) => request.get(`/api/reports/${id}`)
export const exportReport = (params) => request.get('/api/reports/export', { params, responseType: 'blob' })
