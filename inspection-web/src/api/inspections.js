import request from '../utils/request'

export const fetchInspections = (params) => request.get('/api/inspections', { params })
export const deleteInspection = (id) => request.delete(`/api/inspections/${id}`)
export const exportInspections = () => request.get('/api/inspections/export', { responseType: 'blob' })
