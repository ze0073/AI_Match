import request from './request'

export default {
  buildPersonGraph: (resumeId) => request.post('/graph/build/person', null, { params: { resumeId } }),
  buildJobGraph: (jobId) => request.post(`/graph/build/job/${jobId}`),
  getGraphData: () => request.get('/graph/data'),
  getMyGraph: () => request.get('/graph/my'),
  getGapGraph: (resumeId, jobId) => request.get('/graph/gap', { params: { resumeId, jobId } })
}