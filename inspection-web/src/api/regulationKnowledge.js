import request from '../utils/request'

export const fetchRegulationClauses = (params) => request.get('/api/regulation-knowledge/clauses', { params })
export const createRegulationClause = (data) => request.post('/api/regulation-knowledge/clauses', data)
export const updateRegulationClause = (id, data) => request.put(`/api/regulation-knowledge/clauses/${id}`, data)

export const fetchInspectionStandards = (params) => request.get('/api/regulation-knowledge/standards', { params })
export const createInspectionStandard = (data) => request.post('/api/regulation-knowledge/standards', data)
export const updateInspectionStandard = (id, data) => request.put(`/api/regulation-knowledge/standards/${id}`, data)

export const syncRegulationLibrary = () => request.post('/api/regulation-knowledge/sync')
export const downloadRegulationClauses = () => request.get('/api/regulation-knowledge/download?type=clauses', { responseType: 'blob' })
export const downloadInspectionStandards = () => request.get('/api/regulation-knowledge/download?type=standards', { responseType: 'blob' })
