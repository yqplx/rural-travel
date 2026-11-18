import request from './request'

export const authApi = {
  login: data => request.post('/auth/login', data),
  register: data => request.post('/auth/register', data)
}

export const userApi = {
  profile: () => request.get('/users/profile'),
  update: data => request.put('/users/profile', data),
  interests: () => request.get('/users/interests'),
  setInterests: tagIds => request.put('/users/interests', { tagIds }),
  changePassword: data => request.put('/users/password', data),
  deactivate: password => request.post('/users/deactivate', { password }),
  aiConfig: () => request.get('/users/ai-config'),
  saveAiConfig: data => request.put('/users/ai-config', data),
  clearAiConfig: () => request.delete('/users/ai-config'),
  testAiConfig: data => request.post('/users/ai-config/test', data)
}

export const scenicApi = {
  page: params => request.get('/scenic-spots', { params }),
  detail: (id, params) => request.get(`/scenic-spots/${id}`, { params }),
  ratings: id => request.get(`/scenic-spots/${id}/ratings`),
  similar: id => request.get(`/scenic-spots/${id}/similar`),
  voiceGuide: id => request.get(`/scenic-spots/${id}/voice-guide`)
}

export const categoryApi = { list: () => request.get('/categories') }
export const tagApi = { list: () => request.get('/tags') }

export const favoriteApi = {
  add: scenicSpotId => request.post(`/favorites/${scenicSpotId}`),
  remove: scenicSpotId => request.delete(`/favorites/${scenicSpotId}`),
  page: params => request.get('/favorites', { params })
}

export const ratingApi = {
  submit: data => request.post('/ratings', data),
  my: params => request.get('/ratings/my', { params })
}

export const historyApi = {
  record: data => request.post('/browse-history', data),
  page: params => request.get('/browse-history', { params }),
  remove: id => request.delete(`/browse-history/${id}`),
  clear: () => request.delete('/browse-history')
}

export const recommendApi = {
  list: limit => request.get('/recommendations', { params: { limit } })
}

export const aiApi = {
  chat: message => request.post('/ai/chat', { message }),
  recognize: file => {
    const fd = new FormData()
    fd.append('file', file)
    return request.post('/ai/recognize', fd)
  }
}

// 方案三：面向县域文旅的 AI 多模态数字内容生成与智能导览
export const guideApi = {
  // S1 县域文旅知识图谱
  counties: () => request.get('/guide/counties'),
  graph: countyCode => request.get('/guide/graph', { params: { countyCode } }),
  entities: params => request.get('/guide/entities', { params }),
  entityDetail: id => request.get(`/guide/entities/${id}`),
  // S3 游客画像
  profile: () => request.get('/guide/profile'),
  saveProfile: data => request.put('/guide/profile', data),
  inferProfile: () => request.post('/guide/profile/infer'),
  // S4 路线规划
  planRoute: data => request.post('/guide/route/plan', data),
  myRoutes: () => request.get('/guide/routes'),
  routeDetail: id => request.get(`/guide/routes/${id}`),
  // S5 实时导览
  trigger: data => request.post('/guide/trigger', data),
  ask: data => request.post('/guide/ask', data),
  touch: (id, seconds) => request.post(`/guide/session/${id}/touch`, null, { params: { seconds } }),
  sessions: () => request.get('/guide/sessions'),
  // S6 反馈闭环
  feedback: data => request.post('/guide/feedback', data),
  myFeedback: () => request.get('/guide/feedback/my')
}

export const adminApi = {
  // 用户
  users: params => request.get('/admin/users', { params }),
  updateUser: (id, data) => request.put(`/admin/users/${id}`, data),
  deleteUser: id => request.delete(`/admin/users/${id}`),
  // 景区
  scenicPage: params => request.get('/admin/scenic-spots', { params }),
  scenicDetail: id => request.get(`/admin/scenic-spots/${id}`),
  scenicCreate: data => request.post('/admin/scenic-spots', data),
  scenicUpdate: (id, data) => request.put(`/admin/scenic-spots/${id}`, data),
  scenicDelete: id => request.delete(`/admin/scenic-spots/${id}`),
  // 分类标签
  categories: params => request.get('/admin/categories', { params }),
  saveCategory: data => data.id ? request.put(`/admin/categories/${data.id}`, data) : request.post('/admin/categories', data),
  deleteCategory: id => request.delete(`/admin/categories/${id}`),
  tags: params => request.get('/admin/tags', { params }),
  allTags: () => request.get('/admin/tags/all'),
  saveTag: data => data.id ? request.put(`/admin/tags/${data.id}`, data) : request.post('/admin/tags', data),
  deleteTag: id => request.delete(`/admin/tags/${id}`),
  allCategories: () => request.get('/admin/categories/all'),
  // 评分
  ratings: params => request.get('/admin/ratings', { params }),
  deleteRating: id => request.delete(`/admin/ratings/${id}`),
  // 虚拟样本
  virtualUsers: params => request.get('/admin/virtual-users', { params }),
  allVirtualUsers: () => request.get('/admin/virtual-users/all'),
  saveVirtualUser: data => data.id ? request.put(`/admin/virtual-users/${data.id}`, data) : request.post('/admin/virtual-users', data),
  deleteVirtualUser: id => request.delete(`/admin/virtual-users/${id}`),
  virtualRatings: params => request.get('/admin/virtual-ratings', { params }),
  saveVirtualRating: data => data.id ? request.put(`/admin/virtual-ratings/${data.id}`, data) : request.post('/admin/virtual-ratings', data),
  deleteVirtualRating: id => request.delete(`/admin/virtual-ratings/${id}`),
  // AI
  knowledge: params => request.get('/admin/knowledge', { params }),
  saveKnowledge: data => data.id ? request.put(`/admin/knowledge/${data.id}`, data) : request.post('/admin/knowledge', data),
  deleteKnowledge: id => request.delete(`/admin/knowledge/${id}`),
  analytics: () => request.get('/admin/analytics'),
  generate: data => request.post('/admin/content/generate', data),
  rebuildHashes: () => request.post('/admin/ai/rebuild-hashes'),
  // 日志
  logs: params => request.get('/admin/logs', { params }),

  // ===== 方案三：县域文旅多模态导览 =====
  // 知识图谱维护
  guideEntities: params => request.get('/admin/guide/entities', { params }),
  saveGuideEntity: data => request.post('/admin/guide/entities', data),
  deleteGuideEntity: id => request.delete(`/admin/guide/entities/${id}`),
  guideRelations: countyCode => request.get('/admin/guide/relations', { params: { countyCode } }),
  saveGuideRelation: data => request.post('/admin/guide/relations', data),
  deleteGuideRelation: id => request.delete(`/admin/guide/relations/${id}`),
  // 多模态内容量产
  produceContent: data => request.post('/admin/guide/content/produce', data),
  produceAllContent: (countyCode, data) => request.post('/admin/guide/content/produce-all', data, { params: { countyCode } }),
  guideContents: params => request.get('/admin/guide/contents', { params }),
  deleteGuideContent: id => request.delete(`/admin/guide/contents/${id}`),
  // 政策契合度
  guideMetrics: () => request.get('/admin/guide/metrics')
}
