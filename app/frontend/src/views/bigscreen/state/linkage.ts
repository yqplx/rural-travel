/* ============================================================
 * 跨屏联动状态（6 屏共享）
 * 任何一屏改动 → broadcast → 其余屏通过 subscribe 收到并响应
 * ============================================================ */
import { reactive, readonly } from 'vue'

export interface LinkState {
  county: string
  dayIndex: number
  date: string
  entityId: string | null
  category: string | null
  playing: boolean
  speed: number
  source: 'mock' | 'ws' | 'mqtt'
}

const state = reactive<LinkState>({
  county: 'ZJ-YX-01',
  dayIndex: 29,
  date: '',
  entityId: null,
  category: null,
  playing: false,
  speed: 1,
  source: 'mock'
})

export interface LinkMsg {
  type: 'day' | 'entity' | 'category' | 'playing' | 'reset'
  payload: any
  from?: string
}

type Handler = (m: LinkMsg) => void
const handlers = new Set<Handler>()

/** 订阅跨屏消息 */
export function subscribe(fn: Handler): () => void {
  handlers.add(fn)
  return () => handlers.delete(fn)
}

/** 广播（同一页面内直接分发；多端场景交给 transport 走 WS/MQTT） */
export function broadcast(m: LinkMsg): void {
  applyLocal(m)
  handlers.forEach(h => h(m))
}

function applyLocal(m: LinkMsg): void {
  switch (m.type) {
    case 'day': state.dayIndex = m.payload.index; state.date = m.payload.date || state.date; break
    case 'entity': state.entityId = m.payload.id; break
    case 'category': state.category = m.payload.category; break
    case 'playing': state.playing = !!m.payload.playing; break
    case 'reset': state.entityId = null; state.category = null; break
  }
}

/* 语义化动作，屏幕里直接调用 */
export const link = {
  state: readonly(state),
  raw: state,
  setDay(index: number, date?: string) { broadcast({ type: 'day', payload: { index, date } }) },
  selectEntity(id: string | null) { broadcast({ type: 'entity', payload: { id } }) },
  filterCategory(category: string | null) { broadcast({ type: 'category', payload: { category } }) },
  setPlaying(playing: boolean) { broadcast({ type: 'playing', payload: { playing } }) }
}
