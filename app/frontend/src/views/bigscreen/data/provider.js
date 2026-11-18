/* ============================================================
 * 大屏数据源提供器（换源不动视图的关键）
 *
 * 现状：DATA_SOURCE = 'mock'，全部数据来自 ./mock.js（内容取自界面截图）
 *
 * 接真实接口的步骤（只改本文件，视图与配置零改动）：
 *   1. 把 DATA_SOURCE 改为 'api'
 *   2. 在 REAL_API 里把每个 key 指到真实端点（映射表见下）
 *   3. 若返回结构不同，在 adapt.xxx 里做字段映射，保持输出结构一致
 *
 * 字段 → 建议真实端点映射（乡野智游后端已具备其中大半）：
 *   kpi.visitors        GET /api/admin/analytics            （今日游客）
 *   kpi.aiContents      GET /api/admin/guide/metrics        （contentTotal）
 *   kpi.guideSessions   GET /api/admin/guide/metrics        （guideSessions）
 *   kpi.satisfaction    GET /api/admin/guide/metrics        （satisfaction×100）
 *   flowHourly          GET /api/admin/analytics            （24小时分布）
 *   ageMix/interest     GET /api/users/profile 统计          （画像分布）
 *   crowding.list       GET /api/guide/graph                （kg_entity.crowd）
 *   visitRank           GET /api/admin/analytics            （景点访问 TOP）
 *   qaStats             问答日志表（engine + references 统计溯源/幻觉占比）
 *   rePlan              GET /api/guide/routes               （重规划标记统计）
 *   kgStats/kgGraph     GET /api/guide/graph                （实体/关系/网络）
 *   contentAssets       GET /api/admin/guide/metrics        （byModality/byStyle）
 *   qualityScoreDist    GET /api/admin/guide/contents       （score_ema 分桶）
 *   entityContentVisits GET /api/admin/guide/contents       （exposure 聚合）
 *   日志流               GET /api/admin/logs + guide/sessions 轮询
 * ============================================================ */
import * as mock from './mock'

export const DATA_SOURCE = 'mock' // 'mock' | 'api'

/* 真实端点表：DATA_SOURCE='api' 时生效（key 与 mock 导出同名） */
const REAL_API = {
  kpi: '/api/admin/guide/metrics',
  flowHourly: '/api/admin/analytics',
  crowding: '/api/guide/graph',
  kgStats: '/api/guide/graph',
  kgGraph: '/api/guide/graph',
  contentAssets: '/api/admin/guide/metrics',
  qualityScoreDist: '/api/admin/guide/contents',
  visitRank: '/api/admin/analytics',
  qaStats: '/api/admin/logs?kind=qa',
  rePlan: '/api/guide/routes',
  satisfactionTrend: '/api/admin/analytics',
  alertLog: '/api/admin/logs?kind=stream'
}

/* 轮询间隔（毫秒）：实时数字 5s，图表 30s，日志 8s */
export const POLL = { realtime: 5000, charts: 30000, logs: 8000 }

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

/* 统一取数入口：视图只认这个函数 */
export async function fetchScreenData(key) {
  if (DATA_SOURCE === 'api' && REAL_API[key]) {
    // TODO: 接真实接口时在这里 fetch(REAL_API[key]) 并走 adapt 映射
    // 失败自动回落 mock，保证大屏永不空白
    try {
      const res = await fetch(REAL_API[key])
      const adapt = adaptMap[key]
      return adapt ? adapt(await res.json()) : await Promise.reject(new Error('no-adaptor'))
    } catch (e) {
      console.warn('[bigscreen] api fallback to mock:', key, e.message)
    }
  }
  await sleep(120) // 模拟网络延迟，保持与真实接口一致的生命周期
  return mock[key]
}

const adaptMap = {
  // 示例：kpi: (d) => ({ visitors: d.todayVisitors, ... })
}

/* 实时数字漂移（mock 专用：让大屏"活着"） */
export function drift(base, amp = 12, min = 0) {
  const v = base + Math.round((Math.random() - 0.45) * amp)
  return Math.max(min, v)
}

/**
 * 图表级动态刷新：注册一个定时回调，返回停止函数。
 * 组件里：onMounted(() => stop = poll(tick, POLL.charts))；onBeforeUnmount(() => stop())
 * 接真实接口后同一个 tick 换成重新拉数即可，视图不变。
 */
export function poll(fn, ms = POLL.charts) {
  const id = setInterval(fn, ms)
  return () => clearInterval(id)
}

/* 给一组数值做小幅随机波动，保持排序大致稳定 */
export function wobble(list, key = 'v', amp = 0.04, digits = 0) {
  return list.map(x => {
    const base = Number(x[key])
    const next = base + (Math.random() - 0.42) * base * amp * 2
    return { ...x, [key]: digits ? Number(next.toFixed(digits)) : Math.max(0, Math.round(next)) }
  })
}

/* 日志滚动：从素材池随机追加一条 */
export function nextLog(pool, now = new Date()) {
  const item = pool[Math.floor(Math.random() * pool.length)]
  const t = now
  return {
    ...item,
    time: `${String(t.getHours()).padStart(2, '0')}:${String(t.getMinutes()).padStart(2, '0')}`
  }
}
