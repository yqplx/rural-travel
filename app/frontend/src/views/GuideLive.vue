<template>
  <div class="rt-page">
    <div class="gd-hero">
      <h1 class="rt-serif">走到哪儿，讲到哪儿</h1>
      <p class="lede">
        靠近一个点，讲解自己开始。停留得久、问得多，讲得就深——
        讲解的详略是跟着你的节奏走的，不用你操心档位。
      </p>
    </div>

    <!-- 定位：真实定位优先，取不到就手动选点 -->
    <div class="rt-card" style="padding:16px 18px">
      <div style="display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap">
        <span class="rt-section-title" style="margin:0">我在这里</span>
        <div style="display:flex;gap:8px;align-items:center">
          <el-button size="small" :loading="locating" @click="useGeolocation">用我的位置</el-button>
          <el-select v-model="dialect" size="small" style="width:104px">
            <el-option label="普通话" value="普通话" /><el-option label="方言" value="方言" />
          </el-select>
        </div>
      </div>
      <p class="rt-muted" style="font-size:.78rem;margin:8px 0 12px">
        {{ geoText }}
      </p>
      <div class="gd-here">
        <button v-for="e in entities" :key="e.id" :class="{ on: here?.id === e.id }"
                @click="setHere(e)">{{ e.name }}</button>
      </div>
    </div>

    <!-- 讲解主体 -->
    <div v-if="live" style="margin-top:18px">
      <div class="gd-live-head" :style="coverStyle">
        <div class="place">{{ live.entityName }}</div>
        <div class="meta">
          {{ typeName(live.entityType) }} · 距您 {{ Math.round(live.distanceM) }} 米 ·
          {{ live.style }}风格 · {{ live.engine }}
        </div>
        <div class="depth-badge">
          <div class="lv">{{ live.depthName?.slice(0, 1) }}</div>
          <div class="tx">{{ live.depthName }}讲解 · 听一会儿可加深</div>
        </div>
      </div>

      <div style="display:grid;grid-template-columns:1fr 1fr;gap:18px;margin-top:18px" class="gd-live-grid">
        <!-- 左：讲解词 -->
        <div>
          <div :class="['gd-script', live.dialect === '方言' && 'dialect']">{{ live.body }}</div>

          <div style="display:flex;align-items:center;gap:12px;margin-top:14px;flex-wrap:wrap">
            <button :class="['gd-mic', speaking && 'playing']" @click="speak" :title="speaking ? '停止朗读' : '朗读讲解'">
              {{ speaking ? '■' : '▶' }}
            </button>
            <span class="rt-muted" style="font-size:.8rem">
              {{ voiceOk ? '预计朗读 ' + (live.durationSec || 0) + ' 秒' : '当前设备无中文语音，仅提供文稿' }}
            </span>
            <span v-if="live.degradeNote" class="rt-reason">{{ live.degradeNote }}</span>
          </div>

          <div class="gd-detail-math">
            讲解会随你的节奏自动加深：多停留一会儿、多问几个问题，下一次刷新的讲解就会更详细。
          </div>

          <!-- 停留：让档位自己往上走 -->
          <div style="display:flex;align-items:center;gap:14px;margin-top:16px;flex-wrap:wrap">
            <span class="gd-num" style="font-size:1.4rem;font-weight:700">{{ fmtClock(dwell) }}</span>
            <span class="rt-muted" style="font-size:.8rem">
              已停留 · 提问 {{ askCount }} 次
            </span>
            <el-button size="small" type="primary" plain :loading="busy" @click="syncDwell">听一会儿了，讲深一点</el-button>
          </div>
          <p class="rt-muted" style="font-size:.76rem;margin-top:8px;line-height:1.7">
            点一下按钮，系统会结合你在这里待的时长和提问次数，把讲解调到更合适的深度。
            真机上这一步由定位自动完成，此处手动同步是为了让你看得见变化。
          </p>
        </div>

        <!-- 右：图谱上的当前点 -->
        <div>
          <KnowledgeGraph
            :nodes="graphNodes" :edges="graphEdges"
            :active-id="live.entityId" :ripple-at="live.entityId"
            @select="e => { const t = entities.find(x => x.id === e.id); if (t) setHere(t) }"
          />
        </div>
      </div>

      <!-- 追问 -->
      <div class="rt-card" style="margin-top:18px">
        <span class="rt-section-title" style="margin-top:0">还想问点什么</span>
        <div style="display:flex;gap:8px;margin-bottom:12px">
          <el-input v-model="question" placeholder="比如：这地方几点关门？带孩子适合吗？" @keyup.enter="ask" />
          <el-button type="primary" :loading="asking" @click="ask">提问</el-button>
        </div>
        <div class="gd-picks" v-if="!qa.length">
          <span class="gd-pick" v-for="f in DEFAULT_Q" :key="f" @click="question = f; ask()">{{ f }}</span>
        </div>

        <div v-for="(q, i) in qa" :key="i" style="margin-top:14px">
          <div style="font-weight:600;font-size:.92rem">问：{{ q.question }}</div>
          <div style="margin-top:6px;line-height:1.9;font-size:.9rem;white-space:pre-wrap">{{ q.answer }}</div>
          <div class="rt-muted" style="font-size:.74rem;margin-top:6px">
            {{ q.engine }}<template v-if="q.references?.length"> · 参考：{{ q.references.join('、') }}</template>
          </div>
          <div class="gd-picks" style="margin-top:8px" v-if="q.followUps?.length">
            <span class="gd-pick" v-for="f in q.followUps" :key="f" @click="question = f; ask()">{{ f }}</span>
          </div>
          <hr class="gd-rule" v-if="i < qa.length - 1" style="margin:14px 0 0" />
        </div>
      </div>

      <!-- 反馈 -->
      <div class="rt-card" style="margin-top:18px">
        <span class="rt-section-title" style="margin-top:0">这段讲解，值几星</span>
        <p class="rt-muted" style="font-size:.82rem;line-height:1.7;margin-bottom:14px">
          你的评价会直接决定这个点今后的推荐位置：讲得好的会越来越靠前，讲得不好的会慢慢往后挪。
        </p>
        <el-rate v-model="score" :texts="['很一般','一般','还行','不错','很棒']" show-text />
        <div style="margin-top:12px">
          <el-input v-model="comment" type="textarea" :rows="2" placeholder="哪里讲得不够？（可不填）" />
        </div>
        <el-button type="primary" style="margin-top:12px" :loading="sending" @click="sendFeedback">提交反馈</el-button>

        <div v-if="fb" class="gd-formula" style="margin-top:16px">
          <div class="sub" style="margin-top:2px">
            已收到！你的评价已生效——{{ fb.entityName }} 的人气和这条讲解的质量分都会温和更新：
            好评会让它往后更靠前，差评会让它慢慢往后挪，不会一票否决。
          </div>
        </div>
      </div>
    </div>

    <div v-else class="gd-empty" style="margin-top:24px">
      <div class="glyph">⌖</div>
      <div class="t">选个位置开始</div>
      <div class="d">
        上面点一个地名，或者直接用真实定位。系统会在 120 米半径内找到最近的实体，
        命中就触发讲解——这一步对应专利里 S5 的位置触发。
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { guideApi } from '@/api'
import KnowledgeGraph from '@/components/KnowledgeGraph.vue'

const route = useRoute()
const TYPE_NAME = { SCENIC: '景点', HERITAGE: '非遗', FOOD: '美食', VILLAGE: '村落', PERSON: '人物', CRAFT: '手艺' }
const typeName = k => TYPE_NAME[k] || k
const DEFAULT_Q = ['这里建议玩多久？', '有什么典故吗？', '附近还能去哪？', '带孩子来合适吗？']

const entities = ref([])
const graph = ref(null)
const here = ref(null)
const live = ref(null)
const dialect = ref('普通话')
const countyCode = ref('ZJ-YX-01')
const locating = ref(false)
const busy = ref(false)
const geoText = ref('户外定位可能不准，也可以直接在上面点一个地名。')

const dwell = ref(0)
const askCount = ref(0)
let timer = null

const question = ref('')
const qa = ref([])
const asking = ref(false)
const score = ref(5)
const comment = ref('')
const sending = ref(false)
const fb = ref(null)

const graphNodes = computed(() => graph.value?.nodes || [])
const graphEdges = computed(() => graph.value?.edges || [])
const voiceOk = computed(() => typeof window !== 'undefined' && 'speechSynthesis' in window)

/* 讲解卡底图：有封面就用，叠一层暗化保证白字可读 */
const coverStyle = computed(() => {
  const c = live.value?.coverImage || here.value?.coverImage
  if (!c) return {}
  return {
    backgroundImage: `linear-gradient(100deg, rgba(31,52,38,.92) 0%, rgba(31,52,38,.72) 46%, rgba(31,52,38,.30) 100%), url(${c})`,
    backgroundSize: 'cover', backgroundPosition: 'center'
  }
})

const fmtClock = s => `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`

/* ---------- 语音 ---------- */
const speaking = ref(false)
function speak() {
  if (!voiceOk.value) { ElMessage.info('当前设备没有可用的语音合成'); return }
  if (speaking.value) { window.speechSynthesis.cancel(); speaking.value = false; return }
  const u = new SpeechSynthesisUtterance(live.value.body)
  u.lang = 'zh-CN'; u.rate = 0.95
  u.onend = () => { speaking.value = false }
  speaking.value = true
  window.speechSynthesis.speak(u)
}

/* ---------- 触发 ---------- */
async function trigger(extra = {}) {
  busy.value = true
  try {
    live.value = await guideApi.trigger({
      lng: here.value.lng, lat: here.value.lat,
      entityId: here.value.id,
      dialect: dialect.value,
      dwellSeconds: dwell.value,
      askCount: askCount.value,
      ...extra
    })
    if (!live.value?.triggered) ElMessage.warning('附近 120 米内没有可讲解的点')
  } finally { busy.value = false }
}

function setHere(e) {
  here.value = e
  dwell.value = 0; askCount.value = 0; qa.value = []; fb.value = null
  stopTimer(); startTimer()
  trigger()
}

function startTimer() { stopTimer(); timer = setInterval(() => dwell.value++, 1000) }
function stopTimer() { if (timer) { clearInterval(timer); timer = null } }

async function syncDwell() {
  if (!live.value?.sessionId) return
  await guideApi.touch(live.value.sessionId, dwell.value)
  await trigger()
  ElMessage.success('已按当前停留时长重算档位')
}

/* ---------- 追问 ---------- */
async function ask() {
  const q = question.value.trim()
  if (!q) return
  asking.value = true
  try {
    const r = await guideApi.ask({
      question: q, entityId: live.value?.entityId, sessionId: live.value?.sessionId, dialect: dialect.value
    })
    qa.value.unshift(r)
    askCount.value++
    question.value = ''
  } finally { asking.value = false }
}

/* ---------- 反馈 ---------- */
async function sendFeedback() {
  sending.value = true
  try {
    fb.value = await guideApi.feedback({
      entityId: live.value.entityId, contentId: live.value.contentId,
      score: score.value, comment: comment.value || null
    })
    ElMessage.success('已收到，谢谢你的评价！')
  } finally { sending.value = false }
}

/* ---------- 定位 ---------- */
function useGeolocation() {
  if (!('geolocation' in navigator)) { geoText.value = '当前浏览器不支持定位，请在上面手动选点。'; return }
  locating.value = true
  navigator.geolocation.getCurrentPosition(async pos => {
    geoText.value = `已获取位置：${pos.coords.latitude.toFixed(5)}, ${pos.coords.longitude.toFixed(5)}（精度 ±${Math.round(pos.coords.accuracy)} 米）`
    locating.value = false
    busy.value = true
    try {
      live.value = await guideApi.trigger({
        lng: pos.coords.longitude, lat: pos.coords.latitude,
        dwellSeconds: dwell.value, askCount: askCount.value, dialect: dialect.value
      })
      if (live.value?.entityId) {
        here.value = entities.value.find(e => e.id === live.value.entityId) || null
        dwell.value = 0; askCount.value = 0; qa.value = []; fb.value = null
        stopTimer(); startTimer()
      } else ElMessage.warning('你所在的位置附近没有已收录的点')
    } finally { busy.value = false }
  }, err => {
    locating.value = false
    geoText.value = '定位失败（' + (err.code === 1 ? '未授权' : '不可用') + '），请在上面手动选一个地名。'
  }, { enableHighAccuracy: true, timeout: 8000 })
}

watch(dialect, () => { if (here.value) trigger() })

onMounted(async () => {
  const cs = await guideApi.counties().catch(() => [])
  if (cs?.length) countyCode.value = cs[0].code
  entities.value = await guideApi.entities({ countyCode: countyCode.value }).catch(() => [])
  graph.value = await guideApi.graph(countyCode.value).catch(() => null)

  const qid = route.query.entityId
  const target = qid ? entities.value.find(e => String(e.id) === String(qid)) : null
  if (target) setHere(target)
})
onUnmounted(() => { stopTimer(); if (voiceOk.value) window.speechSynthesis.cancel() })
</script>

<style scoped>
@media (max-width: 980px) {
  .gd-live-grid { grid-template-columns: 1fr !important; }
}
</style>
