/* ============================================================
 * 传输层：WebSocket / MQTT / Mock 三选一
 *
 *  现状：未配置 VITE_WS_URL 与 VITE_MQTT_URL → 走 MockBus（本地事件总线，
 *        行为与真实订阅一致：publish / subscribe / 定时推送）。
 *
 *  接后端：
 *    1) 实时流：后端 Kafka → Flink 计算 → 网关以 WebSocket 推送
 *       在 .env 写 VITE_WS_URL=ws://<host>/ws/bigscreen
 *    2) 设备/边缘侧：VITE_MQTT_URL=mqtt://<broker>:1883（走 mqtt.js）
 *       主题约定：rural/{county}/flow | /alert | /ugc | /agent
 *    3) 两种真实传输都失败时自动回落 MockBus，大屏不宕。
 * ============================================================ */
import { broadcast, type LinkMsg } from '../state/linkage'

export interface Transport {
  publish(topic: string, msg: LinkMsg): void
  subscribe(topic: string, fn: (m: LinkMsg) => void): () => void
  close(): void
  kind: 'ws' | 'mqtt' | 'mock'
}

/* ---------------- Mock 总线 ---------------- */
class MockBus implements Transport {
  kind = 'mock' as const
  private map = new Map<string, Set<(m: LinkMsg) => void>>()
  private timer: number | null = null

  publish(topic: string, msg: LinkMsg): void {
    this.map.get(topic)?.forEach(fn => fn(msg))
  }
  subscribe(topic: string, fn: (m: LinkMsg) => void): () => void {
    if (!this.map.has(topic)) this.map.set(topic, new Set())
    this.map.get(topic)!.add(fn)
    return () => this.map.get(topic)?.delete(fn)
  }
  /** 模拟边缘侧持续上报：每 8 秒推一条态势事件 */
  startHeartbeat(): void {
    if (this.timer) return
    this.timer = window.setInterval(() => {
      this.publish('rural/flow', { type: 'day', payload: { index: Math.floor(Math.random() * 30) } })
    }, 8000)
  }
  close(): void { if (this.timer) clearInterval(this.timer); this.timer = null; this.map.clear() }
}

/* ---------------- 真实 WebSocket ---------------- */
class WsTransport implements Transport {
  kind = 'ws' as const
  private ws: WebSocket
  private map = new Map<string, Set<(m: LinkMsg) => void>>()
  constructor(url: string) {
    this.ws = new WebSocket(url)
    this.ws.onmessage = (e) => {
      try {
        const { topic, msg } = JSON.parse(e.data)
        this.map.get(topic)?.forEach(fn => fn(msg))
      } catch { /* 忽略脏数据 */ }
    }
  }
  publish(topic: string, msg: LinkMsg): void {
    if (this.ws.readyState === WebSocket.OPEN) this.ws.send(JSON.stringify({ topic, msg }))
  }
  subscribe(topic: string, fn: (m: LinkMsg) => void): () => void {
    if (!this.map.has(topic)) this.map.set(topic, new Set())
    this.map.get(topic)!.add(fn)
    return () => this.map.get(topic)?.delete(fn)
  }
  close(): void { this.ws.close(); this.map.clear() }
}

/* ---------------- 真实 MQTT（mqtt.js，按环境变量懒加载） ---------------- */
async function createMqtt(url: string): Promise<Transport> {
  const mqtt = await import('mqtt')
  const client = mqtt.connect(url)
  const map = new Map<string, Set<(m: LinkMsg) => void>>()
  client.on('message', (topic: string, payload: Buffer) => {
    try {
      const msg = JSON.parse(payload.toString())
      map.get(topic)?.forEach(fn => fn(msg))
    } catch { /* 忽略脏数据 */ }
  })
  return {
    kind: 'mqtt',
    publish(topic, msg) { client.publish(topic, JSON.stringify(msg)) },
    subscribe(topic, fn) {
      if (!map.has(topic)) { map.set(topic, new Set()); client.subscribe(topic) }
      map.get(topic)!.add(fn)
      return () => map.get(topic)?.delete(fn)
    },
    close() { client.end(true); map.clear() }
  }
}

/* ---------------- 工厂 ---------------- */
let instance: Transport | null = null

export async function getTransport(): Promise<Transport> {
  if (instance) return instance
  const wsUrl = import.meta.env.VITE_WS_URL as string | undefined
  const mqttUrl = import.meta.env.VITE_MQTT_URL as string | undefined
  try {
    if (wsUrl) return (instance = new WsTransport(wsUrl))
    if (mqttUrl) return (instance = await createMqtt(mqttUrl))
  } catch (e) {
    console.warn('[bigscreen] 传输层连接失败，回落 mock：', e)
  }
  const bus = new MockBus()
  bus.startHeartbeat()
  return (instance = bus)
}

/** 让 transport 事件直接驱动跨屏联动（真实链路下同样生效） */
export async function bindTransport(): Promise<Transport> {
  const t = await getTransport()
  t.subscribe('rural/flow', (m) => broadcast(m))
  t.subscribe('rural/alert', (m) => broadcast(m))
  return t
}
