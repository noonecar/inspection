import request from '../utils/request'

export const uploadFile = (file, category = 'evidence') => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('category', category)
  return request.post('/api/files/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
