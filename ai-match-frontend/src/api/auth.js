import request from './request'

export default {
  login: (data) => request.post('/auth/login', data),
  register: (data) => request.post('/auth/register', data),
  getUserInfo: () => request.get('/auth/userinfo'),
  updatePassword: (oldPassword, newPassword) =>
    request.put('/auth/password', null, { params: { oldPassword, newPassword } })
}
