import request from './request'

export default {
  upload: (file) => {
    const fd = new FormData()
    fd.append('file', file)
    return request.post('/jobseeker/resume/upload', fd)
  },
  getDetail: (id) => request.get(`/jobseeker/resume/${id}`),
  update: (id, data) => request.put(`/jobseeker/resume/${id}`, data),
  getCapability: () => request.get('/jobseeker/resume/my-capability')
}
