<template>
  <div class="scr">
    <div class="kpis">
      <BsKpi label="今日规划请求" :value="requests" tone="cyan" delta="成功率 99.2%" />
      <BsKpi label="平均响应时延" :value="latency" suffix="ms" delta="P95 2.1s" />
      <BsKpi label="知识溯源率" :value="96.4" suffix="%" tone="cyan" delta="幻觉拦截 3.6%" />
      <BsKpi label="工具调用次数" :value="toolCalls" delta="本日累计" />
    </div>

    <BsPanel class="pa-ask" title="游客提问（实时）" en="live questions">
      <div class="ask" v-for="(q, i) in asks" :key="q" :class="{ on: i === cur }" @click="pick(i)">
        <span class="dot" /> {{ q }}
      </div>
    </BsPanel>

    <BsPanel class="pa-chain" title="Agent 思考链（流式）" en="chain of thought">
      <div class="chain">
        <div v-for="(s, i) in revealed" :key="i" class="step">
          <span class="idx">{{ i + 1 }}</span>
          <div class="body">
            <div class="lb">{{ s.label }} <i>{{ s.ms }}ms</i></div>
            <div class="dt">{{ s.detail }}</div>
          </div>
        </div>
        <div v-if="thinking" class="thinking">思考中<span class="dots"><i /><i /><i /></span></div>
      </div>
    </BsPanel>

    <BsPanel class="pa-ans" title="生成回答" en="answer">
      <div class="tools">
        <span class="chip" v-for="t in session.tools" :key="t">⚙ {{ t }}</span>
        <span class="chip meta">tokens {{ session.tokens }}</span>
        <span class="chip meta">耗时 {{ session.latency }}ms</span>
      </div>
      <div class="answer">{{ typed }}<span v-if="typing" class="caret" /></div>
    </BsPanel>
  </div>
</template>

<script setup lang="ts">
/* ============ 副屏4：LLM Agent 对话（思考链流式可视化） ============ */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import BsPanel from '../../components/BsPanel.vue'
import BsKpi from '../../components/BsKpi.vue'
import { agentSessions } from '../../data/mock.v2'
import { drift } from '../../data/provider'

const sessions = agentSessions
const asks = computed(() => sessions.map(s => s.ask))
const cur = ref(0)
const session = computed(() => sessions[cur.value])

const revealed = ref<any[]>([])
const typed = ref('')
const thinking = ref(false)
const typing = ref(false)
const requests = ref(1286)
const latency = ref(1830)
const toolCalls = ref(3742)

let timers: number[] = []
function clearAll(): void { timers.forEach(clearTimeout); timers = [] }

/** 播放一轮：逐步点亮思考链 → 流式吐字 */
function play(): void {
  clearAll()
  revealed.value = []
  typed.value = ''
  const s = session.value
  thinking.value = true
  let acc = 0
  s.steps.forEach((st, i) => {
    acc += 420 + Math.min(600, st.ms)
    timers.push(window.setTimeout(() => {
      revealed.value = [...revealed.value, st]
      if (i === s.steps.length - 1) thinking.value = false
    }, acc))
  })
  timers.push(window.setTimeout(() => {
    typing.value = true
    let k = 0
    const text = s.answer
    const step = Math.max(2, Math.round(text.length / 60))
    const t = window.setInterval(() => {
      k += step
      typed.value = text.slice(0, k)
      if (k >= text.length) { clearInterval(t); typing.value = false }
    }, 42)
    timers.push(t as unknown as number)
  }, acc + 260))
}

function pick(i: number): void { cur.value = i; play() }

let pollTimer: number | null = null
onMounted(() => {
  play()
  // 模拟持续有新的规划请求进来（真实环境由 WS 推送触发）
  pollTimer = window.setInterval(() => {
    requests.value = drift(requests.value, 14, 1000)
    latency.value = drift(latency.value, 120, 800)
    toolCalls.value = drift(toolCalls.value, 26, 3000)
  }, 4000)
})
onBeforeUnmount(() => { clearAll(); pollTimer && clearInterval(pollTimer) })
</script>

<style scoped>
.scr {
  height: 100%;
  display: grid;
  gap: 12px;
  grid-template-columns: 1fr 1.15fr;
  grid-template-rows: 88px 1fr 1.25fr;
  grid-template-areas: "kpi kpi" "ask chain" "ask ans";
  min-height: 0;
}
.kpis { grid-area: kpi; display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.pa-ask { grid-area: ask; }
.pa-chain { grid-area: chain; }
.pa-ans { grid-area: ans; }

.ask {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; color: #b9d4ea; padding: 8px 6px; cursor: pointer;
  border-bottom: 1px dashed rgba(74, 144, 197, .16); line-height: 1.5;
}
.ask .dot { width: 6px; height: 6px; border-radius: 50%; background: #4c7396; flex: 0 0 auto; }
.ask:hover { background: rgba(20, 52, 88, .5); }
.ask.on { background: rgba(53, 200, 232, .16); color: #eaf6ff; }
.ask.on .dot { background: #4fe0c8; box-shadow: 0 0 8px #4fe0c8; }

.chain { height: 100%; overflow: auto; padding-right: 4px; }
.step { display: flex; gap: 10px; margin-bottom: 10px; animation: fade .35s ease-out; }
.step .idx {
  flex: 0 0 22px; height: 22px; line-height: 20px; text-align: center; font-size: 12px;
  border: 1px solid rgba(79, 224, 200, .5); color: #4fe0c8; background: rgba(14, 42, 74, .6);
}
.step .lb { font-size: 13px; color: #eaf6ff; font-weight: 600; }
.step .lb i { font-style: normal; font-size: 11px; color: #5f86a8; margin-left: 6px; }
.step .dt { font-size: 12px; color: #8fb4d4; margin-top: 2px; line-height: 1.6; }
.thinking { font-size: 12px; color: #4fc3f7; padding-left: 32px; }
.dots i { display: inline-block; width: 4px; height: 4px; margin-left: 3px; border-radius: 50%; background: #4fc3f7; animation: blink 1s infinite; }
.dots i:nth-child(2) { animation-delay: .2s }
.dots i:nth-child(3) { animation-delay: .4s }
@keyframes blink { 0%, 100% { opacity: .2 } 50% { opacity: 1 } }
@keyframes fade { from { opacity: 0; transform: translateY(6px) } to { opacity: 1; transform: none } }

.tools { display: flex; gap: 6px; margin-bottom: 10px; flex-wrap: wrap; }
.chip {
  font-size: 12px; color: #9fc6e8; border: 1px solid rgba(79, 195, 247, .3);
  padding: 1px 8px; background: rgba(14, 42, 74, .45);
}
.chip.meta { color: #5f86a8; border-color: rgba(74, 144, 197, .2); }
.answer {
  font-size: 14px; line-height: 1.9; color: #cfe3f5; white-space: pre-wrap;
  min-height: 60px;
}
.caret {
  display: inline-block; width: 7px; height: 15px; background: #4fe0c8;
  vertical-align: -2px; margin-left: 2px; animation: blink .8s steps(1) infinite;
}
</style>
