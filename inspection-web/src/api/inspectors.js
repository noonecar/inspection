import request from '../utils/request'

export const createInspectorWithInstitution = (data) => request.post('/api/inspectors', data)
export const resetInspectorPassword = (id) => request.post(`/api/inspectors/${id}/reset-password`)
export const getInspectorTaskOverview = (id) => request.get(`/api/inspectors/${id}/task-overview`)
