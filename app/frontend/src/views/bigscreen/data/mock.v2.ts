/* ============================================================
 * 大屏 v2 mock 数据（1 主屏 + 5 副屏）
 * 说明：后端未就绪，所有数据在此集中定义；结构与真实 API 对齐，
 *      接 Kafka/Flink 实时流时只换 provider.ts 的取数实现。
 * ============================================================ */

/* 日志素材沿用 v1 mock（种子 + 滚动池） */
export { alertLogSeed, alertLogPool } from './mock'

/* ---------- 主屏：数字孪生地理与态势 ---------- */
/** 县域：安徽 · 黄山市 · 黟县（与设计方案一致） */
export const countyMeta = {
  name: '黟县',
  full: '安徽 · 黄山市 · 黟县',
  weather: '☀ 26°C',
  aqi: 42,
  aqiText: '优',
  lunar: '农历七月廿三'
}
/** 县域边界（简化环，经纬度） */
export const countyRing: [number, number][] = [
  [117.852, 30.021], [117.901, 30.048], [117.968, 30.052], [118.021, 30.031],
  [118.048, 29.988], [118.036, 29.941], [117.988, 29.912], [117.921, 29.906],
  [117.868, 29.931], [117.842, 29.978], [117.852, 30.021]
]

export interface Poi { id: string; name: string; type: string; lon: number; lat: number; heat: number; crowd: number }
/** 核心 POI（黟县及周边古村落景区） */
export const pois: Poi[] = [
  { id: 'e1', name: '查济古村', type: '景点', lon: 117.962, lat: 30.012, heat: 0.98, crowd: 0.82 },
  { id: 'e2', name: '宏村', type: '景点', lon: 117.981, lat: 30.001, heat: 0.94, crowd: 0.78 },
  { id: 'e3', name: '西递', type: '景点', lon: 117.992, lat: 29.941, heat: 0.90, crowd: 0.71 },
  { id: 'e4', name: '呈坎八卦村', type: '景点', lon: 118.101, lat: 29.921, heat: 0.82, crowd: 0.56 },
  { id: 'e5', name: '唐模水口', type: '景点', lon: 118.158, lat: 29.902, heat: 0.76, crowd: 0.48 },
  { id: 'e6', name: '江村', type: '景点', lon: 118.072, lat: 30.051, heat: 0.71, crowd: 0.42 },
  { id: 'e7', name: '龙川景区', type: '景点', lon: 118.548, lat: 30.068, heat: 0.68, crowd: 0.39 },
  { id: 'e8', name: '上庄', type: '景点', lon: 118.512, lat: 30.104, heat: 0.62, crowd: 0.33 },
  { id: 'e9', name: '障山大峡谷', type: '景点', lon: 118.601, lat: 30.022, heat: 0.58, crowd: 0.28 },
  { id: 'e10', name: '冯村', type: '景点', lon: 117.952, lat: 29.968, heat: 0.52, crowd: 0.24 },
  { id: 'e11', name: '徽州刺绣', type: '非遗', lon: 117.972, lat: 29.996, heat: 0.44, crowd: 0.18 },
  { id: 'e12', name: '徽派木雕', type: '非遗', lon: 117.984, lat: 29.988, heat: 0.41, crowd: 0.16 },
  { id: 'e13', name: '徽墨制作', type: '非遗', lon: 118.088, lat: 30.021, heat: 0.46, crowd: 0.22 },
  { id: 'e14', name: '臭鳜鱼', type: '美食', lon: 117.978, lat: 30.004, heat: 0.88, crowd: 0.68 },
  { id: 'e15', name: '毛豆腐', type: '美食', lon: 117.968, lat: 29.998, heat: 0.79, crowd: 0.62 },
  { id: 'e16', name: '一品锅', type: '美食', lon: 117.990, lat: 29.995, heat: 0.72, crowd: 0.55 },
  { id: 'e17', name: '石头粿', type: '美食', lon: 117.964, lat: 30.008, heat: 0.66, crowd: 0.47 }
]
/** 历史 POI（保留结构，便于后续扩到 26+ 实体） */
export const poisLegacy: Poi[] = [
  { id: 'e1', name: '梅城古镇城门', type: '景点', lon: 120.1412, lat: 29.3505, heat: 0.92, crowd: 0.74 },
  { id: 'e2', name: '明清一条街', type: '景点', lon: 120.1498, lat: 29.3481, heat: 0.88, crowd: 0.80 },
  { id: 'e3', name: '云溪书院', type: '景点', lon: 120.1571, lat: 29.3533, heat: 0.70, crowd: 0.42 },
  { id: 'e4', name: '望江楼', type: '景点', lon: 120.1364, lat: 29.3602, heat: 0.66, crowd: 0.38 },
  { id: 'e5', name: '文昌阁', type: '景点', lon: 120.1625, lat: 29.3442, heat: 0.62, crowd: 0.40 },
  { id: 'e6', name: '千亩荷塘', type: '景点', lon: 120.1102, lat: 29.3371, heat: 0.72, crowd: 0.35 },
  { id: 'e7', name: '后山观景台', type: '景点', lon: 120.1908, lat: 29.3711, heat: 0.54, crowd: 0.22 },
  { id: 'e8', name: '溪畔花海', type: '景点', lon: 120.0934, lat: 29.3255, heat: 0.58, crowd: 0.30 },
  { id: 'e9', name: '古码头遗址', type: '景点', lon: 120.1287, lat: 29.3428, heat: 0.49, crowd: 0.28 },
  { id: 'e10', name: '石拱桥群', type: '景点', lon: 120.1449, lat: 29.3363, heat: 0.64, crowd: 0.44 },
  { id: 'e11', name: '古樟树群', type: '景点', lon: 120.1731, lat: 29.3262, heat: 0.47, crowd: 0.20 },
  { id: 'e12', name: '乡愁记忆馆', type: '景点', lon: 120.1555, lat: 29.3631, heat: 0.51, crowd: 0.33 },
  { id: 'e13', name: '云溪竹编', type: '非遗', lon: 120.1509, lat: 29.3402, heat: 0.44, crowd: 0.18 },
  { id: 'e14', name: '梅城剪纸', type: '非遗', lon: 120.1471, lat: 29.3455, heat: 0.40, crowd: 0.16 },
  { id: 'e15', name: '古法榨油', type: '非遗', lon: 120.1183, lat: 29.3301, heat: 0.38, crowd: 0.15 },
  { id: 'e16', name: '云溪米酒酿造', type: '非遗', lon: 120.1667, lat: 29.3384, heat: 0.46, crowd: 0.24 },
  { id: 'e17', name: '木版年画', type: '非遗', lon: 120.1398, lat: 29.3312, heat: 0.35, crowd: 0.12 },
  { id: 'e18', name: '舞龙灯', type: '非遗', lon: 120.1592, lat: 29.3586, heat: 0.52, crowd: 0.36 },
  { id: 'e19', name: '梅城酥饼', type: '美食', lon: 120.1523, lat: 29.3472, heat: 0.80, crowd: 0.65 },
  { id: 'e20', name: '云溪米酒', type: '美食', lon: 120.1651, lat: 29.3407, heat: 0.72, crowd: 0.58 },
  { id: 'e21', name: '溪鱼一锅鲜', type: '美食', lon: 120.1312, lat: 29.3389, heat: 0.69, crowd: 0.52 },
  { id: 'e22', name: '艾草青团', type: '美食', lon: 120.1433, lat: 29.3439, heat: 0.61, crowd: 0.47 },
  { id: 'e23', name: '手打麻糍', type: '美食', lon: 120.1488, lat: 29.3431, heat: 0.66, crowd: 0.55 },
  { id: 'e24', name: '笋干老鸭煲', type: '美食', lon: 120.1712, lat: 29.3512, heat: 0.58, crowd: 0.41 },
  { id: 'e25', name: '桂花糖藕', type: '美食', lon: 120.1245, lat: 29.3461, heat: 0.55, crowd: 0.39 },
  { id: 'e26', name: '农家腊味拼盘', type: '美食', lon: 120.1789, lat: 29.3445, heat: 0.63, crowd: 0.43 }
]

/** 典型游览动线（飞线 / 粒子流路径） */
export const routeFlows: { name: string; ids: string[] }[] = [
  { name: '古村经典线', ids: ['e1', 'e2', 'e3', 'e4', 'e10'] },
  { name: '非遗手作线', ids: ['e11', 'e12', 'e13', 'e2'] },
  { name: '舌尖寻味线', ids: ['e14', 'e15', 'e16', 'e17'] },
  { name: '山水慢游线', ids: ['e9', 'e8', 'e7', 'e6', 'e5'] }
]

/** 主屏 KPI（与设计方案口径一致） */
export const twinKpi = {
  visitors: 12580, income: 856320, conversion: 18.62,
  inPark: 3247, saturation: 68, alarms: 3, alarmDetail: '客流超载 1 · 天气 1 · 设备 1',
  devices: 256, sources: 12
}
/** 实时热门景点 TOP10 */
export const hotSpots: { name: string; v: number }[] = [
  { name: '查济古村', v: 9856 }, { name: '宏村', v: 8432 }, { name: '西递', v: 7215 },
  { name: '呈坎八卦村', v: 6109 }, { name: '唐模水口', v: 5876 }, { name: '江村', v: 4532 },
  { name: '龙川景区', v: 4208 }, { name: '上庄', v: 3987 }, { name: '障山大峡谷', v: 3654 },
  { name: '冯村', v: 3201 }
]
/** 游客来源地 TOP10（省份） */
export const sourceProvinces: { name: string; v: number }[] = [
  { name: '江苏', v: 2856 }, { name: '浙江', v: 2430 }, { name: '上海', v: 2105 },
  { name: '安徽本省', v: 1876 }, { name: '北京', v: 1432 }, { name: '广东', v: 1105 },
  { name: '山东', v: 926 }, { name: '河南', v: 743 }, { name: '湖北', v: 612 }, { name: '福建', v: 486 }
]
/** 游客画像：兴趣维度 + 年龄结构 */
export const visitorProfile = {
  interests: ['亲子', '情侣', '摄影', '研学', '徒步', '银发'],
  interestValue: [72, 66, 84, 58, 62, 41],
  ages: [
    { name: '18-25', v: 35 }, { name: '26-35', v: 26 },
    { name: '36-50', v: 20 }, { name: '50+', v: 19 }
  ]
}
/** 近 7 日客流（本期 / 上期） */
export const weekTrend: { d: string; cur: number; prev: number }[] = [
  { d: '08-29', cur: 8620, prev: 7410 }, { d: '08-30', cur: 9240, prev: 7850 },
  { d: '08-31', cur: 10180, prev: 8320 }, { d: '09-01', cur: 9420, prev: 8060 },
  { d: '09-02', cur: 11260, prev: 8940 }, { d: '09-03', cur: 12040, prev: 9480 },
  { d: '09-04', cur: 12580, prev: 9680 }
]

/** 30 天态势序列（时间轴回放用） */
export interface DayStat { date: string; visitors: number; aqi: number; weather: string; income: number }
export const days30: DayStat[] = Array.from({ length: 30 }, (_, i) => {
  const d = new Date(2026, 7, 6 + i)
  const weekend = d.getDay() === 0 || d.getDay() === 6
  const base = weekend ? 9800 : 6200
  const jitter = Math.round(Math.sin(i / 3.1) * 900 + Math.random() * 700)
  const weather = ['晴', '多云', '阴', '小雨', '阵雨'][i % 5]
  return {
    date: `${d.getMonth() + 1}/${d.getDate()}`,
    visitors: base + jitter,
    aqi: 28 + Math.round(Math.sin(i / 4) * 18 + Math.random() * 12),
    weather,
    income: Math.round((base + jitter) * (118 + Math.random() * 40))
  }
})

/* ---------- 副屏1：知识图谱 ---------- */
export const kgSummary = {
  nodes: 58426, edges: 216308,
  categories: [
    { name: '景点', n: 12480 }, { name: '非遗', n: 8320 }, { name: '美食', n: 9610 },
    { name: '村落', n: 4120 }, { name: '人物', n: 7380 }, { name: '事件', n: 5240 },
    { name: '手艺', n: 4087 }
  ],
  growth: [3.2, 4.1, 5.0, 5.8, 6.9, 8.4, 9.6, 11.2, 13.8, 16.5, 19.4, 22.6]
}
/** G6 展示用抽样子图（真实规模下钻时才加载更多） */
export const kgSample = {
  nodes: [
    { id: 'root', label: '梅城古镇', cat: '景点', size: 46 },
    { id: 'e1', label: '古镇城门', cat: '景点', size: 30 },
    { id: 'e2', label: '明清一条街', cat: '景点', size: 34 },
    { id: 'e3', label: '云溪书院', cat: '景点', size: 26 },
    { id: 'e13', label: '云溪竹编', cat: '非遗', size: 24 },
    { id: 'e14', label: '梅城剪纸', cat: '非遗', size: 22 },
    { id: 'e17', label: '木版年画', cat: '非遗', size: 20 },
    { id: 'e16', label: '米酒酿造', cat: '手艺', size: 22 },
    { id: 'e19', label: '梅城酥饼', cat: '美食', size: 28 },
    { id: 'e20', label: '云溪米酒', cat: '美食', size: 24 },
    { id: 'e23', label: '手打麻糍', cat: '美食', size: 22 },
    { id: 'p1', label: '袁崇焕', cat: '人物', size: 26 },
    { id: 'v1', label: '云溪村', cat: '村落', size: 24 },
    { id: 'v2', label: '梅城村', cat: '村落', size: 22 },
    { id: 'v3', label: '荷塘村', cat: '村落', size: 20 },
    { id: 'ev1', label: '宁远大捷', cat: '事件', size: 24 }
  ],
  edges: [
    ['root', 'e1'], ['root', 'e2'], ['root', 'e3'], ['e2', 'e13'], ['e2', 'e14'],
    ['e2', 'e17'], ['e2', 'e19'], ['e2', 'e23'], ['e13', 'v1'], ['e14', 'v2'],
    ['e16', 'e20'], ['e16', 'v3'], ['e1', 'p1'], ['p1', 'ev1'], ['e3', 'p1'],
    ['v1', 'v2'], ['v2', 'v3'], ['root', 'v1'], ['root', 'v2'], ['e19', 'v2']
  ].map(([s, t]) => ({ source: s, target: t }))
}
/** 下钻：点击节点后的二级关系（模拟按需加载） */
export const kgDrill: Record<string, { nodes: { id: string; label: string; cat: string; size: number }[]; edges: { source: string; target: string }[] }> = {
  e2: {
    nodes: [
      { id: 'e2-a', label: '前店后坊格局', cat: '事件', size: 18 },
      { id: 'e2-b', label: '青石板街面', cat: '景点', size: 18 },
      { id: 'e2-c', label: '老字号店铺', cat: '景点', size: 18 },
      { id: 'e2-d', label: '夜游灯光秀', cat: '事件', size: 18 }
    ],
    edges: [['e2', 'e2-a'], ['e2', 'e2-b'], ['e2', 'e2-c'], ['e2', 'e2-d']].map(([s, t]) => ({ source: s, target: t }))
  }
}

/* ---------- 副屏2：推荐算法效果 ---------- */
/** 推荐转化漏斗（曝光 → 点击 → 收藏 → 转化） */
export const recFunnel = [
  { name: '曝光', v: 128450 }, { name: '点击', v: 32180 },
  { name: '收藏', v: 8920 }, { name: '转化', v: 3145 }
]
export const recTrend = Array.from({ length: 14 }, (_, i) => {
  const d = new Date(2026, 7, 22 + i)
  return {
    date: `${d.getMonth() + 1}/${d.getDate()}`,
    ctr: +(0.118 + i * 0.0022 + Math.random() * 0.004).toFixed(4),
    cvr: +(0.064 + i * 0.0015 + Math.random() * 0.003).toFixed(4),
    ndcg: +(0.712 + i * 0.0045 + Math.random() * 0.006).toFixed(3),
    diversity: +(0.428 + i * 0.0031 + Math.random() * 0.005).toFixed(3)
  }
})
export const abCompare = {
  groups: ['CTR', 'CVR', 'NDCG@10', '多样性', '停留时长'],
  base: [0.118, 0.064, 0.712, 0.428, 3.1],
  kgnn: [0.146, 0.083, 0.786, 0.517, 3.6],
  lift: ['+23.7%', '+29.7%', '+10.4%', '+20.8%', '+16.1%']
}
export const rlReward = Array.from({ length: 40 }, (_, i) => +(0.42 + i * 0.009 + Math.random() * 0.03).toFixed(3))
export const recChannels = [
  { name: '首页猜你喜欢', ctr: 0.142, cvr: 0.081 },
  { name: '行程规划推荐', ctr: 0.168, cvr: 0.094 },
  { name: '景点详情页关联', ctr: 0.121, cvr: 0.067 },
  { name: '语音导览插入', ctr: 0.156, cvr: 0.088 },
  { name: '离场挽留位', ctr: 0.094, cvr: 0.052 }
]

/* ---------- 副屏3：多模态 UGC 舆情 ---------- */
export const ugcSummary = { total: 48620, text: 31280, image: 12460, audio: 2880, video: 2000 }
export const sentiment = [
  { name: '正面', value: 68.4 }, { name: '中性', value: 24.1 }, { name: '负面', value: 7.5 }
]
export const ugcPlatform = [
  { name: '抖音', v: 16820 }, { name: '小红书', v: 12460 }, { name: '携程', v: 8910 },
  { name: '微博', v: 5620 }, { name: '视频号', v: 4810 }
]
/** 高频话题标签（徽派文化语境） */
export const ugcWords = [
  { name: '徽派建筑', value: 360 }, { name: '古村落', value: 312 }, { name: '非遗', value: 268 },
  { name: '美食', value: 246 }, { name: '民宿', value: 228 }, { name: '摄影', value: 214 },
  { name: '亲子', value: 186 }, { name: '徒步', value: 164 }, { name: '文创', value: 142 },
  { name: '出片', value: 128 }, { name: '门票', value: 116 }, { name: '停车难', value: 98 },
  { name: '商业化', value: 86 }, { name: '讲解详细', value: 74 }, { name: '晨雾', value: 62 }
]
/** 多模态识别标签（CLIP 图像 + Whisper 语音） */
export const ugcModalTags = [
  { name: 'CLIP 图像识别', v: 12460 }, { name: 'Whisper 语音转写', v: 2880 },
  { name: '文本情感', v: 31280 }, { name: '视频关键帧', v: 2000 }
]
export const ugcHot: { title: string; platform: string; sentiment: string; heat: number; time: string }[] = [
  { title: '梅城古镇夜游灯光秀太出片了', platform: '抖音', sentiment: '正面', heat: 98, time: '10:24' },
  { title: '周末明清一条街人多到走不动', platform: '微博', sentiment: '负面', heat: 87, time: '10:12' },
  { title: '带娃体验竹编，孩子玩到不想走', platform: '小红书', sentiment: '正面', heat: 82, time: '09:58' },
  { title: '酥饼排队 40 分钟，值不值', platform: '抖音', sentiment: '中性', heat: 76, time: '09:41' },
  { title: '语音讲解讲得比导游还细', platform: '携程', sentiment: '正面', heat: 71, time: '09:30' },
  { title: '停车场指示不清晰，绕了三圈', platform: '微博', sentiment: '负面', heat: 64, time: '09:12' }
]
export const ugcTrend = Array.from({ length: 24 }, (_, i) => ({
  h: `${String(i).padStart(2, '0')}:00`,
  pos: Math.round(40 + Math.sin(i / 3) * 22 + Math.random() * 12),
  neg: Math.round(6 + Math.max(0, Math.sin(i / 5)) * 8 + Math.random() * 4)
}))

/* ---------- 副屏4：LLM Agent 对话 ---------- */
export interface AgentStep { label: string; detail: string; ms: number }
export interface AgentSession {
  id: string; ask: string; answer: string;
  steps: AgentStep[]; tools: string[]; tokens: number; latency: number
}
export const agentSessions: AgentSession[] = [
  {
    id: 'a1',
    ask: '带 5 岁孩子明天玩 4 小时，避开人多的地方，要能吃到本地小吃',
    answer: '已为你安排「亲子·避峰美食线」共 5 站：千亩荷塘(60min) → 云溪竹编体验(50min) → 手打麻糍(40min) → 梅城酥饼(40min) → 云溪书院(50min)。全程步行 1.8km，预计人均消费 128 元，11:00 前完成可避开午间高峰。',
    steps: [
      { label: '意图解析', detail: '同行=亲子(5岁) · 时长=240min · 偏好=本地美食 · 约束=避堵', ms: 120 },
      { label: '知识图谱检索', detail: '召回 26 实体 → 按亲子友好度过滤 → 命中 14 个候选', ms: 340 },
      { label: '拥挤度预测', detail: '读取 10:00 实时人流 + 30 天同期曲线 → 标记 3 个高峰点位', ms: 260 },
      { label: '多目标求解', detail: '兴趣 0.62 / 热度 0.71 / 拥挤惩罚 −0.18 / 内容 0.94 → 目标 0.744', ms: 410 },
      { label: '行程装配', detail: '时间预算 240min → 装入 5 站（218min）→ 步行接驳 1.8km', ms: 180 },
      { label: '生成讲解与播报', detail: '生成亲子版讲解 5 段（方言可选）· 预计朗读 6 分 20 秒', ms: 520 }
    ],
    tools: ['图谱检索', '拥挤预测', '路线求解', '内容生成'], tokens: 1862, latency: 1830
  },
  {
    id: 'a2',
    ask: '宁远大捷发生在哪一年？和这里有什么关系？',
    answer: '宁远大捷发生于明天启六年（1626 年），是袁崇焕据守宁远城击退后金军的战役。本县梅城古镇城门与文昌阁一带留存相关碑刻与传说，云溪书院藏有《宁远纪事》抄本影印件，可到馆查阅。该结论来源于知识图谱实体「宁远大捷」与「袁崇焕」的关联路径，非模型臆测。',
    steps: [
      { label: '意图解析', detail: '类型=史实问答 · 需要溯源 · 拒绝臆测', ms: 90 },
      { label: '知识图谱检索', detail: '命中实体：宁远大捷 / 袁崇焕 / 云溪书院（置信度 0.94）', ms: 220 },
      { label: '证据抽取', detail: '抽取 3 条证据并标注出处（县志·碑刻·书院藏书目）', ms: 300 },
      { label: '受控生成', detail: '仅使用证据内事实生成，未命中部分标记「暂无记载」', ms: 380 }
    ],
    tools: ['图谱检索', '证据抽取', '受控生成'], tokens: 1140, latency: 990
  }
]

/* ---------- 副屏5：AHP-TOPSIS 乡村振兴成效 ---------- */
export interface AhpDim { name: string; weight: number; score: number; indicators: string[] }
export const ahpDims: AhpDim[] = [
  { name: '产业兴旺', weight: 0.284, score: 86.4, indicators: ['文旅综合收入', '经营主体数量', '农产品加工转化率', '电商销售额'] },
  { name: '生态宜居', weight: 0.196, score: 91.2, indicators: ['空气质量优良率', '水体达标率', '垃圾处理率', '村容整治覆盖率'] },
  { name: '乡风文明', weight: 0.148, score: 84.6, indicators: ['非遗传承人数量', '文化活���场次', '村史馆覆盖率'] },
  { name: '治理有效', weight: 0.156, score: 88.1, indicators: ['数字化治理覆盖率', '矛盾纠纷化解率', '智慧安防点位'] },
  { name: '生活富裕', weight: 0.216, score: 82.7, indicators: ['农村居民人均可支配收入', '村集体经济收入', '本地就业人数', '返乡创业人数'] }
]
export const topsisRank: { town: string; score: number; dPos: number; dNeg: number; closeness: number }[] = [
  { town: '梅城村', score: 88.6, dPos: 0.062, dNeg: 0.284, closeness: 0.821 },
  { town: '云溪村', score: 86.2, dPos: 0.078, dNeg: 0.266, closeness: 0.773 },
  { town: '荷塘村', score: 84.1, dPos: 0.091, dNeg: 0.248, closeness: 0.732 },
  { town: '石桥村', score: 81.7, dPos: 0.104, dNeg: 0.231, closeness: 0.690 },
  { town: '后山村', score: 78.3, dPos: 0.126, dNeg: 0.208, closeness: 0.623 },
  { town: '樟树村', score: 75.9, dPos: 0.142, dNeg: 0.192, closeness: 0.575 }
]
export const composite = { score: 86.42, rank: 3, of: 128, delta: +2.14 }
export const indicatorRadar = ahpDims.map(d => ({ name: d.name, value: d.score, max: 100 }))
/** 20+ 明细指标（用于仪表盘墙） */
export const indicators: { name: string; dim: string; value: number; unit: string; target: number; yoy: string }[] = [
  { name: '文旅综合收入', dim: '产业兴旺', value: 4.86, unit: '亿元', target: 5.2, yoy: '+18.4%' },
  { name: '经营主体数量', dim: '产业兴旺', value: 1268, unit: '家', target: 1400, yoy: '+12.6%' },
  { name: '农产品加工转化率', dim: '产业兴旺', value: 72.4, unit: '%', target: 75, yoy: '+3.1%' },
  { name: '电商销售额', dim: '产业兴旺', value: 1.24, unit: '亿元', target: 1.5, yoy: '+26.8%' },
  { name: '空气质量优良率', dim: '生态宜居', value: 94.6, unit: '%', target: 92, yoy: '+2.2%' },
  { name: '水体达标率', dim: '生态宜居', value: 96.2, unit: '%', target: 95, yoy: '+1.4%' },
  { name: '垃圾处理率', dim: '生态宜居', value: 99.1, unit: '%', target: 98, yoy: '+0.6%' },
  { name: '村容整治覆盖率', dim: '生态宜居', value: 88.4, unit: '%', target: 90, yoy: '+6.8%' },
  { name: '非遗传承人数量', dim: '乡风文明', value: 46, unit: '人', target: 50, yoy: '+9.5%' },
  { name: '文化活动场次', dim: '乡风文明', value: 312, unit: '场', target: 300, yoy: '+14.2%' },
  { name: '村史馆覆盖率', dim: '乡风文明', value: 76.8, unit: '%', target: 80, yoy: '+11.0%' },
  { name: '数字治理覆盖率', dim: '治理有效', value: 92.3, unit: '%', target: 95, yoy: '+8.4%' },
  { name: '矛盾化解率', dim: '治理有效', value: 97.6, unit: '%', target: 96, yoy: '+1.8%' },
  { name: '智慧安防点位', dim: '治理有效', value: 468, unit: '处', target: 520, yoy: '+22.6%' },
  { name: '居民人均可支配收入', dim: '生活富裕', value: 38620, unit: '元', target: 40000, yoy: '+7.9%' },
  { name: '村集体经济收入', dim: '生活富裕', value: 86.4, unit: '万元', target: 100, yoy: '+15.3%' },
  { name: '本地就业人数', dim: '生活富裕', value: 6420, unit: '人', target: 6800, yoy: '+5.6%' },
  { name: '返乡创业人数', dim: '生活富裕', value: 386, unit: '人', target: 450, yoy: '+31.2%' },
  { name: '游客满意度', dim: '产业兴旺', value: 96.0, unit: '%', target: 95, yoy: '+2.0%' },
  { name: '导览服务覆盖率', dim: '治理有效', value: 100, unit: '%', target: 100, yoy: '+0.0%' },
  { name: '多模态内容覆盖率', dim: '乡风文明', value: 100, unit: '%', target: 100, yoy: '+18.0%' },
  { name: '数字内容产出量', dim: '产业兴旺', value: 520, unit: '条', target: 600, yoy: '+42.0%' }
]
export const revTrend = Array.from({ length: 12 }, (_, i) => ({
  m: `${i + 1}月`,
  score: +(72 + i * 1.3 + Math.random() * 1.2).toFixed(1),
  income: +(2.4 + i * 0.22 + Math.random() * 0.1).toFixed(2)
}))
