import request from '../utils/request'

export const fetchStations = (params) => request.get('/api/stations', { params })
export const createStation = (data) => request.post('/api/stations', data)
export const updateStation = (id, data) => request.put(`/api/stations/${id}`, data)
export const deleteStation = (id) => request.delete(`/api/stations/${id}`)
export const reviewStations = (data) => request.post('/api/stations/review', data)
export const importStationsCsv = (formData) => request.post('/api/stations/import-csv', formData, {
	headers: {
		'Content-Type': 'multipart/form-data'
	}
})
export const exportStations = () => request.get('/api/stations/export', { responseType: 'blob' })
export const fetchStationDetail = (id) => request.get(`/api/stations/${id}/detail`)
