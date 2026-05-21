import request from '../utils/request'

export const fetchInstitutions = () => request.get('/api/institutions')
export const createInstitution = (data) => request.post('/api/institutions', data)
export const updateInstitution = (id, data) => request.put(`/api/institutions/${id}`, data)
export const deleteInstitution = (id) => request.delete(`/api/institutions/${id}`)
export const getInstitutionInspectors = (id) => request.get(`/api/institutions/${id}/inspectors`)
export const assignInspectorsToInstitution = (id, data) => request.post(`/api/institutions/${id}/inspectors`, data)
export const removeInspectorFromInstitution = (id, userId) => request.delete(`/api/institutions/${id}/inspectors/${userId}`)
export const getInspectorsByInstitutions = (institutionIds) => request.post('/api/institutions/inspectors-by-institutions', { institutionIds })
