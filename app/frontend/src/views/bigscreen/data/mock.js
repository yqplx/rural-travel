/* ============================================================
 * 大屏 mock 数据层
 * 内容来源：用户提供的界面截图（2026-09-04）
 *  - 截图①  KPI：今日游客 8743 / AI生成内容 1280 / 智能导览 5372 / 游客满意度 96%
 *  - 截图②  今日实时客流趋势(8~18时) · 游客画像分布(亲子35/青年28/情侣20/银发17)
 *           AI多模态内容生产量(讲解文案320/宣传配图300/短视频脚本280/讲解语音380)
 *  - 截图③  景点热度TOP5(古城墙/蓟辽督师府/钟鼓楼/兴城文庙/祖氏石坊)
 *           内容质量五维评估(覆盖率/一致性/可读性/完读率/好评率) · 实时AI交互动态日志
 * 其余指标（拥挤度仪表盘/重规划/问答溯源占比等）按同一口径补齐。
 * ============================================================ */

/* 客流分时（今日实时客流趋势） */
export const flowHourly = [
  { h: '08时', v: 620 }, { h: '09时', v: 1580 }, { h: '10时', v: 3120 },
  { h: '11时', v: 4680 }, { h: '12时', v: 5240 }, { h: '13时', v: 6890 },
  { h: '14时', v: 8420 }, { h: '15时', v: 7310 }, { h: '16时', v: 5860 },
  { h: '17时', v: 3480 }, { h: '18时', v: 1740 }
]

/* 游客画像（年龄结构 环形 + 兴趣标签 雷达） */
export const ageMix = [
  { name: '亲子家庭', value: 35 }, { name: '青年深度', value: 28 },
  { name: '情侣出游', value: 20 }, { name: '银发老年', value: 17 }
]
export const interestRadar = {
  dims: ['历史建筑', '非遗手艺', '美食体验', '亲子研学', '自然风光'],
  today: [82, 64, 71, 58, 66],
  lastWeek: [74, 60, 68, 52, 61]
}

/* 景点拥挤度（仪表盘 + 告警滚动条） */
export const crowding = {
  average: 62,
  list: [
    { name: '明清一条街', level: 80, state: '拥挤' },
    { name: '古城墙景区', level: 74, state: '拥挤' },
    { name: '钟鼓楼', level: 58, state: '适中' },
    { name: '蓟辽督师府', level: 46, state: '适中' },
    { name: '兴城文庙', level: 32, state: '畅通' },
    { name: '祖氏石坊', level: 28, state: '畅通' }
  ]
}

/* 点位热力（地图点位：x/y 为舞台坐标百分比，v=热度） */
export const mapPoints = [
  { name: '古城墙', x: 42, y: 46, v: 92 }, { name: '蓟辽督师府', x: 55, y: 38, v: 84 },
  { name: '钟鼓楼', x: 50, y: 55, v: 78 }, { name: '兴城文庙', x: 62, y: 60, v: 66 },
  { name: '祖氏石坊', x: 45, y: 64, v: 61 }, { name: '明清一条街', x: 58, y: 48, v: 88 },
  { name: '海韵广场', x: 33, y: 52, v: 45 }, { name: '温泉度假村', x: 70, y: 42, v: 40 },
  { name: '首山栈道', x: 74, y: 66, v: 35 }, { name: '古码头', x: 36, y: 70, v: 38 }
]

/* 游览路线完成率 */
export const routeStats = { planned: 1280, finished: 1114, rate: 87 }

/* —— 屏2：游客体验与导览效果 —— */
export const visitRank = [
  { name: '古城墙', v: 2846 }, { name: '蓟辽督师府', v: 2310 },
  { name: '钟鼓楼', v: 1974 }, { name: '兴城文庙', v: 1580 },
  { name: '祖氏石坊', v: 1326 }, { name: '明清一条街', v: 1190 }
]
export const voiceStats = { played: 2847, finished: 2618, finishRate: 92 }
export const qaStats = { total: 1264, grounded: 986, llm: 214, hallucination: 64 }
export const rePlan = { today: 42, week: [23, 31, 28, 35, 40, 37, 42] }
export const satisfactionDist = [
  { name: '5星', v: 72 }, { name: '4星', v: 21 }, { name: '3星', v: 5 },
  { name: '2星', v: 1.4 }, { name: '1星', v: 0.6 }
]
export const satisfactionTrend = [
  { d: '08-29', v: 91 }, { d: '08-30', v: 92 }, { d: '08-31', v: 94 },
  { d: '09-01', v: 93 }, { d: '09-02', v: 95 }, { d: '09-03', v: 96 }, { d: '09-04', v: 96 }
]
export const avgDurationHours = 3.6
export const qaWordCloud = [
  { name: '门票', value: 186 }, { name: '开放时间', value: 152 }, { name: '宁远大捷', value: 141 },
  { name: '袁崇焕', value: 132 }, { name: '带孩子', value: 118 }, { name: '停车场', value: 104 },
  { name: '小吃', value: 96 }, { name: '拍照机位', value: 88 }, { name: '满族刺绣', value: 82 },
  { name: '古城墙', value: 78 }, { name: '讲解', value: 72 }, { name: '厕所', value: 66 },
  { name: '海鲜', value: 61 }, { name: '公交', value: 54 }, { name: '夜市', value: 48 },
  { name: '温泉', value: 44 }, { name: '历史', value: 40 }, { name: '住宿', value: 36 }
]

/* —— 屏3：AI 数字内容资产 —— */
export const kgStats = { entities: 26, relations: 90, scenic: 12, heritage: 6, food: 8 }
export const contentAssets = [
  { name: '讲解文案', value: 320 }, { name: '宣传配图', value: 300 },
  { name: '短视频脚本', value: 280 }, { name: '讲解语音', value: 380 }
]
export const qualityScoreDist = [
  { name: '0.9~1.0', v: 118 }, { name: '0.8~0.9', v: 96 }, { name: '0.7~0.8', v: 61 },
  { name: '0.6~0.7', v: 28 }, { name: '0.6以下', v: 9 }
]
export const qualityRadar = {
  dims: ['覆盖率', '一致性', '可读性', '完读率', '好评率'],
  value: [100, 88, 82, 90, 96]
}
export const regenStats = { today: 18, week: 96, total: 412 }
export const entityContentVisits = [
  { name: '古城墙', v: 1930 }, { name: '钟鼓楼', v: 1642 }, { name: '蓟辽督师府', v: 1518 },
  { name: '明清一条街', v: 1366 }, { name: '兴城文庙', v: 1104 }, { name: '祖氏石坊', v: 902 }
]
export const iterateVersions = [
  { name: '古城墙', v: 6 }, { name: '明清一条街', v: 5 }, { name: '钟鼓楼', v: 4 },
  { name: '蓟辽督师府', v: 4 }, { name: '兴城文庙', v: 3 }, { name: '祖氏石坊', v: 3 }
]
/* 知识图谱网络（抽样子集，类别：0景点 1非遗 2美食） */
export const kgGraph = {
  categories: ['景点', '非遗', '美食'],
  nodes: [
    { name: '古城墙', cat: 0, size: 34 }, { name: '钟鼓楼', cat: 0, size: 28 },
    { name: '蓟辽督师府', cat: 0, size: 26 }, { name: '兴城文庙', cat: 0, size: 22 },
    { name: '祖氏石坊', cat: 0, size: 20 }, { name: '明清一条街', cat: 0, size: 30 },
    { name: '满族刺绣', cat: 1, size: 18 }, { name: '全羊席', cat: 1, size: 16 },
    { name: '剪纸', cat: 1, size: 12 }, { name: '海杂鱼锅', cat: 2, size: 16 },
    { name: '缸炉烧饼', cat: 2, size: 14 }, { name: '海鲜火锅', cat: 2, size: 15 }
  ],
  links: [
    ['古城墙', '钟鼓楼'], ['古城墙', '祖氏石坊'], ['古城墙', '明清一条街'],
    ['钟鼓楼', '明清一条街'], ['蓟辽督师府', '明清一条街'], ['兴城文庙', '古城墙'],
    ['明清一条街', '满族刺绣'], ['满族剪纸', '剪纸'], ['明清一条街', '海杂鱼锅'],
    ['明清一条街', '缸炉烧饼'], ['古城墙', '海鲜火锅'], ['兴城文庙', '剪纸']
  ].map(([s, t]) => ({ source: s, target: t }))
}

/* 实时日志（屏1 拥堵告警 + 屏3 AI 动态，滚动追加） */
export const alertLogSeed = [
  { time: '10:30', tag: '信息', type: 'info', msg: '问答「宁远大捷」命中知识图谱，溯源回答' },
  { time: '10:28', tag: '导览', type: 'info', msg: '为 3 位亲子游客规划 4 小时避堵路线' },
  { time: '10:26', tag: '内容', type: 'ok', msg: '「全羊席」讲解完读率 92%，质量分上调' },
  { time: '10:24', tag: '预警', type: 'warn', msg: '古城墙区域人流上升，已自动优化路线' },
  { time: '10:22', tag: '问答', type: 'info', msg: '游客语音询问袁崇焕生平并溯源' },
  { time: '10:20', tag: '内容', type: 'ok', msg: '「满族刺绣」配图生成完成（第 3 版）' },
  { time: '10:18', tag: '预警', type: 'warn', msg: '检测到祖氏石坊讲解评分偏低，已重新生成' },
  { time: '10:15', tag: '信息', type: 'info', msg: '实时在园人数突破 8,700，接入分流建议' }
]
/* 滚动追加用素材池 */
export const alertLogPool = [
  { tag: '预警', type: 'warn', msg: '明清一条街拥挤度 0.80，超过预警线，已推送错峰建议' },
  { tag: '导览', type: 'info', msg: '银发游客团触发慢游路线，全程减少步行 800 米' },
  { tag: '内容', type: 'ok', msg: '「古城墙」深度版讲解重新生成完成（第 6 版）' },
  { tag: '问答', type: 'info', msg: '语音问答「几点关门」命中知识库并语音播报' },
  { tag: '信息', type: 'info', msg: '蓟辽督师府人流回落至适中区间' },
  { tag: '内容', type: 'warn', msg: '「缸炉烧饼」配图一致性偏低，已加入重生成队列' },
  { tag: '导览', type: 'ok', msg: '一条 7 站路线完成游览，游客评分 5 星' },
  { tag: '预警', type: 'warn', msg: '温泉度假村方向停车接近饱和，已更新引导话术' }
]
