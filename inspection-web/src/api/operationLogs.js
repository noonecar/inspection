import request from '../utils/request'

export const fetchOperationLogs = (params) => request.get('/api/operation-logs', { params })
export const fetchOperationLogSummary = () => request.get('/api/operation-logs/summary')
export const deleteOperationLog = (id) => request.delete(`/api/operation-logs/${id}`)
