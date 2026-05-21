import request from '../utils/request'

export const fetchInspectionDetail = (category, params) => request.get(`/api/inspection-details/${category}`, { params })
export const saveInspectionDetail = (category, data) => request.post(`/api/inspection-details/${category}`, data)
