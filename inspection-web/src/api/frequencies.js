import request from '../utils/request'

export const fetchFrequencies = (params) => request.get('/api/frequencies', { params })
export const createFrequency = (data) => request.post('/api/frequencies', data)
export const updateFrequency = (id, data) => request.put(`/api/frequencies/${id}`, data)
export const deleteFrequency = (id) => request.delete(`/api/frequencies/${id}`)
export const batchCreateFrequencies = (data) => request.post('/api/frequencies/batch', data)
export const importFrequenciesCsv = (formData) => request.post('/api/frequencies/import-csv', formData, {
	headers: {
		'Content-Type': 'multipart/form-data'
	}
})
export const reviewFrequencies = (data) => request.post('/api/frequencies/review', data)
export const exportFrequencies = () => request.get('/api/frequencies/export', { responseType: 'blob' })
