import request from '../utils/request'

export const fetchRegulations = (params) => request.get('/api/regulations', { params })
export const createRegulation = (data) => request.post('/api/regulations', data)
export const updateRegulation = (id, data) => request.put(`/api/regulations/${id}`, data)
export const deleteRegulation = (id) => request.delete(`/api/regulations/${id}`)
