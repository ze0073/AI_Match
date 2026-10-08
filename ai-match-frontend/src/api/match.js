import request from './request'

export default {
  matchPersonToJobs: (resumeId) => request.post('/match/person-to-jobs', null, { params: { resumeId } }),
  matchJobToCandidates: (jobId) => request.post(`/match/job-to-candidates/${jobId}`),
  getRecommendations: (limit = 10) => request.get('/match/recommendations', { params: { limit } }),
  getRecommendationsForJob: (jobId, limit = 10) =>
    request.get(`/match/recommendations/job/${jobId}`, { params: { limit } }),
  getDetail: (id) => request.get(`/match/${id}`)
}
