<template>
  <div class="bs-root">
    <div class="bs-stage" :style="stageStyle">
      <div class="bs-header">
        <div class="side left">
          <span>{{ countyName }}</span>
          <span class="bs-clock">{{ clock }}</span>
        </div>
        <div class="title">
          乡野智游 · 县域文旅 AI 指挥中心
          <small>RURAL CULTURAL-TOURISM AI COMMAND CENTER</small>
        </div>
        <div class="side right">
          <div class="bs-tabs">
            <span v-for="(s, i) in SCREENS" :key="i" :class="['bs-tab', current === i && 'on']"
                  :title="`${s.w}×${s.h}`" @click="switchTo(i)">{{ i + 1 }}·{{ s.tab }}</span>
          </div>
          <span style="cursor:pointer" title="自动轮播开关" @click="auto = !auto">
            {{ auto ? '⏸ 轮播中' : '▶ 轮播已停' }}
          </span>
          <span v-if="classic" class="bs-clock" style="cursor:pointer" title="切换到新版 1主+5副"
                @click="goV2">新版 →</span>
          <span v-else class="bs-clock" style="cursor:pointer" title="查看旧版三屏"
                @click="goClassic">旧版 →</span>
        </div>
      </div>

      <div class="bs-body">
        <component :is="SCREENS[current].comp" class="bs-screen" :key="curKey" />
      </div>

      <div class="bs-footer-tip">
        按 1~{{ SCREENS.length }} 切屏 · 空格暂停轮播 · 双击进入全屏 ·
        主屏 {{ SCREENS[0].w }}×{{ SCREENS[0].h }} 拼接屏 · 数据源：{{ sourceLabel }} · 传输：{{ transportKind }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/* ============ 大屏外壳 v2：1 主屏（3840×1080）+ 5 副屏（1920×1080） ============ */
import { computed, markRaw, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import './bigscreen.css'
import MainTwin from './screens/v2/MainTwin.vue'
import SubKgExplore from './screens/v2/SubKgExplore.vue'
import SubRecEffect from './screens/v2/SubRecEffect.vue'
import SubUgcSentiment from './screens/v2/SubUgcSentiment.vue'
import SubAgentChat from './screens/v2/SubAgentChat.vue'
import SubRevitalization from './screens/v2/SubRevitalization.vue'
import ScreenCommand from './screens/ScreenCommand.vue'
import ScreenExperience from './screens/ScreenExperience.vue'
import ScreenAsset from './screens/ScreenAsset.vue'
import { DATA_SOURCE } from './data/provider'
import { bindTransport } from './data/transport'

interface ScreenDef { tab: string; w: number; h: number; comp: any }

const route = useRoute()
const router = useRouter()
const classic = computed(() => route.query.classic === '1')

const V2: ScreenDef[] = [
  { tab: '指挥中心', w: 3840, h: 1080, comp: markRaw(MainTwin) },
  { tab: '知识图谱', w: 1920, h: 1080, comp: markRaw(SubKgExplore) },
  { tab: '推荐效果', w: 1920, h: 1080, comp: markRaw(SubRecEffect) },
  { tab: '多模态舆情', w: 1920, h: 1080, comp: markRaw(SubUgcSentiment) },
  { tab: 'AI Agent', w: 1920, h: 1080, comp: markRaw(SubAgentChat) },
  { tab: '振兴成效', w: 1920, h: 1080, comp: markRaw(SubRevitalization) }
]
const V1: ScreenDef[] = [
  { tab: '总览指挥', w: 1920, h: 1080, comp: markRaw(ScreenCommand) },
  { tab: '游客体验', w: 1920, h: 1080, comp: markRaw(ScreenExperience) },
  { tab: '内容资产', w: 1920, h: 1080, comp: markRaw(ScreenAsset) }
]
const SCREENS = computed<ScreenDef[]>(() => (classic.value ? V1 : V2))

const initial = Number(route.query.screen) || 1
const current = ref(Math.min(V2.length, Math.max(1, initial)) - 1)
const auto = ref(true)
const clock = ref('')
const scale = ref(1)
const transportKind = ref('mock')
const countyName = '梅城古镇 · 云溪县（ZJ-YX-01）'
const curKey = computed(() => (classic.value ? 'v1' : 'v2') + '-' + current.value)

const stageStyle = computed(() => {
  const s = SCREENS.value[current.value] || SCREENS.value[0]
  return {
    width: s.w + 'px',
    height: s.h + 'px',
    transform: `translate(-50%, -50%) scale(${scale.value})`
  }
})
const sourceLabel = computed(() => (DATA_SOURCE === 'mock' ? '演示数据（可切换真实接口）' : '实时接口'))

let rotateTimer: number | null = null
let clockTimer: number | null = null

function fit(): void {
  const s = SCREENS.value[current.value] || SCREENS.value[0]
  scale.value = Math.min(window.innerWidth / s.w, window.innerHeight / s.h)
}
function switchTo(i: number): void { current.value = i; fit() }
function toggleAuto(): void { auto.value = !auto.value }
function toggleFullscreen(): void {
  if (document.fullscreenElement) document.exitFullscreen()
  else document.documentElement.requestFullscreen?.()
}
function goClassic(): void { current.value = 0; router.push({ path: '/bigscreen', query: { classic: '1' } }) }
function goV2(): void { current.value = 0; router.push({ path: '/bigscreen' }) }

function onKey(e: KeyboardEvent): void {
  const n = SCREENS.value.length
  if (e.key >= '1' && e.key <= String(Math.min(9, n))) switchTo(Number(e.key) - 1)
  else if (e.code === 'Space') { e.preventDefault(); toggleAuto() }
}
function tickClock(): void {
  const d = new Date()
  const wd = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()]
  const p = (v: number) => String(v).padStart(2, '0')
  clock.value = `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} 星期${wd} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

onMounted(async () => {
  fit()
  window.addEventListener('resize', fit)
  window.addEventListener('keydown', onKey)
  window.addEventListener('dblclick', toggleFullscreen)
  tickClock()
  clockTimer = window.setInterval(tickClock, 1000)
  rotateTimer = window.setInterval(() => {
    if (auto.value) switchTo((current.value + 1) % SCREENS.value.length)
  }, 30000)
  // 传输层：未配置 VITE_WS_URL / VITE_MQTT_URL 时自动走 mock 总线
  try {
    const t = await bindTransport()
    transportKind.value = t.kind
  } catch { transportKind.value = 'mock' }
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', fit)
  window.removeEventListener('keydown', onKey)
  window.removeEventListener('dblclick', toggleFullscreen)
  rotateTimer && clearInterval(rotateTimer)
  clockTimer && clearInterval(clockTimer)
})
</script>

<style scoped>
.bs-screen { min-height: 0; }
</style>
