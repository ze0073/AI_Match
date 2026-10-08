import request from './request'

export default {
  getSkillTree: () => request.get('/skill/tree'),
  getByCategory: (category) => request.get('/skill/category/' + category),
  search: (keyword) => request.get('/skill/search', { params: { keyword } }),
  add: (data) => request.post('/skill', data),
  update: (id, data) => request.put('/skill/' + id, data),
  delete: (id) => request.delete('/skill/' + id)
}