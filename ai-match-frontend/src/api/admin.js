import request from './request'

export default {
  getUsers: (params) => request.get('/admin/users', { params }),
  updateStatus: (userId, status) => request.put('/admin/users/' + userId + '/status', null, { params: { status } }),
  getDashboardStats: () => request.get('/stats/dashboard')
}