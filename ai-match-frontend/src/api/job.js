import request from './request'

export default {
  searchWithScore: (params) => request.get('/job/search-with-score', { params }),
  search: (params) => request.get('/job/search', { params }),
  getDetail: (id) => request.get(`/job/${id}`),
  getCapability: (id) => request.get(`/job/${id}/capability`),
  upload: (file) => {
    const fd = new FormData()
    fd.append('file', file)
    return request.post('/job/upload', fd)
  },
  delete: (id) => request.delete('/job/' + id),
  update: (id, data) => request.put(`/job/${id}`, data)
}
