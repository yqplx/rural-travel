<template>
  <div class="rt-page">
    <div class="gd-hero">
      <h1 class="rt-serif">先说你是谁，再谈去哪儿</h1>
      <p class="lede">
        同一座县城，带孩子的、独自来的、赶时间的、想躲清静的，走出来的路不该是同一条。
        填完左边这张表，系统会按你的兴趣、各处的人气与实时人流，在你给的时间预算内
        挑出一条"塞得下又值得走"的路线。
      </p>
      <div class="gd-policy-line">对应政策：智慧文旅发展 · 创新数字内容消费</div>
    </div>

    <div style="display:grid;grid-template-columns:minmax(280px,340px) 1fr;gap:24px;align-items:start"
         class="gd-plan-grid">

      <!-- ============ 左：游客画像 ============ -->
      <div class="rt-card" style="position:sticky;top:76px">
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span class="rt-section-title" style="margin:0">游客画像</span>
          <el-button v-if="auth.isLogin" size="small" text @click="infer">从浏览兴趣推断</el-button>
        </div>
        <p class="rt-muted" style="font-size:.8rem;margin:8px 0 18px;line-height:1.7">
          {{ auth.isLogin ? '画像存在你名下，换个设备登录还在。' : '未登录也能规划，但画像不会保存。' }}
        </p>

        <div class="gd-field">
          <label>同行的人</label>
          <div class="gd-picks">
            <span v-for="g in GROUPS" :key="g.v" :class="['gd-pick', form.groupType === g.v && 'on']"
                  @click="form.groupType = g.v">{{ g.n }}</span>
          </div>
          <p class="hint">独自出行讲得更细些，带小朋友则讲得更浅更生动——讲解会跟着同行的人自动调整。</p>
        </div>

        <div class="gd-field">
          <label>年龄段</label>
          <div class="gd-picks">
            <span v-for="a in AGES" :key="a" :class="['gd-pick', form.ageBand === a && 'on']"
                  @click="form.ageBand = a">{{ a }}</span>
          </div>
        </div>

        <div class="gd-field">
          <label>讲解深度 · {{ DEPTH_NAME[form.depthLevel] }}</label>
          <el-slider v-model="form.depthLevel" :min="1" :max="3" :step="1" show-stops
                     :marks="{ 1: '简约', 2: '标准', 3: '深度' }" />
          <p class="hint">这是你的知识背景水平。到现场后系统还会按停留和提问自动加深，不必一次选死。</p>
        </div>

        <div class="gd-field">
          <label>时间预算 · {{ form.planMinutes }} 分钟</label>
          <el-slider v-model="form.planMinutes" :min="60" :max="480" :step="30"
                     :marks="{ 120: '2h', 240: '4h', 360: '6h' }" />
        </div>

        <div class="gd-field">
          <label>走路节奏</label>
          <div class="gd-picks">
            <span v-for="p in PACES" :key="p.v" :class="['gd-pick', form.pace === p.v && 'on']"
                  @click="form.pace = p.v">{{ p.n }}</span>
          </div>
        </div>

        <div class="gd-field">
          <label>语音口音</label>
          <div class="gd-picks">
            <span v-for="d in ['普通话', '方言']" :key="d" :class="['gd-pick', form.preferDialect === d && 'on']"
                  @click="form.preferDialect = d">{{ d }}</span>
          </div>
        </div>

        <div class="gd-field">
          <label>兴趣标签（选得越准，路线越懂你）</label>
          <div class="gd-picks">
            <span v-for="t in tagOptions" :key="t" :class="['gd-pick', form.interests.includes(t) && 'on']"
                  @click="toggle(t)">{{ t }}</span>
          </div>
          <p class="hint" v-if="!tagOptions.length">正在读取县域标签…</p>
        </div>

        <div class="gd-field">
          <label>想看的类型</label>
          <div class="gd-picks">
            <span v-for="t in TYPES" :key="t.v" :class="['gd-pick', form.entityTypes.includes(t.v) && 'on']"
                  @click="toggleType(t.v)">{{ t.n }}</span>
          </div>
        </div>

        <div class="gd-field" style="display:flex;align-items:center;justify-content:space-between">
          <div>
            <label style="margin:0">避开拥挤</label>
            <p class="hint" style="margin:2px 0 0">开启后，太挤的点会自动往后排</p>
          </div>
          <el-switch v-model="avoidCrowd" />
        </div>

        <div class="gd-field" style="display:flex;align-items:center;justify-content:space-between">
          <div><label style="margin:0">最多站点</label></div>
          <el-input-number v-model="form.maxItems" :min="3" :max="10" size="small" />
        </div>

        <div style="display:flex;gap:8px;margin-top:20px">
          <el-button v-if="auth.isLogin" @click="saveProfile" style="flex:0 0 auto">保存画像</el-button>
          <el-button type="primary" style="flex:1" :loading="planning" @click="plan">规划路线</el-button>
        </div>
      </div>

      <!-- ============ 右：路线结果 ============ -->
      <div>
        <div v-if="!route" class="gd-empty">
          <div class="glyph">⤳</div>
          <div class="t">还没有路线</div>
          <div class="d">
            左边填完点「规划路线」。系统会从人气高又不太挤的点起步，
            一站一站地试"加上这站时间还够不够"，不够就自动换下一个——保证走得完。
          </div>
        </div>

        <template v-else>
          <div class="rt-card" style="padding:18px 22px">
            <div style="display:flex;justify-content:space-between;align-items:flex-start;gap:16px;flex-wrap:wrap">
              <div>
                <div class="rt-serif" style="font-size:1.3rem;font-weight:700">{{ route.routeName }}</div>
                <div class="rt-muted" style="font-size:.82rem;margin-top:4px">
                  {{ route.nodes.length }} 站 ·
                  用时 <b class="gd-num">{{ route.totalMinutes }}</b> / {{ route.planMinutes }} 分钟 ·
                  余 {{ route.remainMinutes }} 分钟
                </div>
              </div>
              <div style="text-align:right">
                <div class="rt-muted" style="font-size:.74rem">这条路线有多适合你</div>
                <div class="gd-num" style="font-size:2rem;font-weight:700;color:var(--rt-green-dark);line-height:1.1">
                  {{ fitLevel }}
                </div>
              </div>
            </div>
            <div class="gd-track" style="margin-top:14px">
              <i :style="{ width: usage + '%' }"></i>
            </div>
            <div style="display:flex;justify-content:space-between;font-size:.74rem;color:var(--rt-muted);margin-top:6px">
              <span>时间占用 {{ usage }}%</span>
              <span>{{ route.feasible ? '时间刚刚好，走得完' : '略超预算，已自动为你取舍' }}</span>
            </div>
          </div>

          <div class="gd-formula" style="margin-top:16px">
            <div class="sub" style="margin-top:2px">
              这条路线是为你算出来的：综合了你的兴趣匹配、各处的人气口碑、当下的拥挤程度和讲解内容的丰富度，
              并在你的时间预算内一站一站装配而成。换一份画像，同一张地图会走出另一条路。
            </div>
          </div>

          <div class="rt-card" style="margin-top:16px">
            <div class="gd-timeline">
              <div class="gd-stop" v-for="n in route.nodes" :key="n.seq"
                   :data-seq="n.seq" :class="n.entityType === 'FOOD' ? 'food' : ''">
                <div class="hd">
                  <img v-if="n.coverImage" class="stop-cover" :src="n.coverImage" :alt="n.entityName" loading="lazy" />
                  <span class="nm">{{ n.entityName }}</span>
                  <span class="gd-chip">{{ typeName(n.entityType) }}</span>
                  <span class="when">
                    第 {{ n.arriveMinute }} 分钟到 · 停 {{ n.stayMinutes }} 分
                    <template v-if="n.transferMin"> · 步行 {{ n.transferMin }} 分</template>
                  </span>
                </div>
                <div class="scores">
                  <span v-for="tag in stopTags(n)" :key="tag.t" :class="['gd-chip', tag.neg && 'neg']">{{ tag.t }}</span>
                </div>
                <div class="why">{{ n.reason }}</div>
              </div>
            </div>

            <hr class="gd-rule" />
            <div style="display:flex;gap:10px;flex-wrap:wrap">
              <el-button type="primary" plain @click="goLive">按这条路线开始导览 →</el-button>
              <el-button v-if="route.routeId" text @click="loadRoute(route.routeId)">重新载入已保存路线</el-button>
            </div>
            <p class="rt-muted" style="font-size:.78rem;margin-top:10px;line-height:1.7">
              {{ auth.isLogin && route.routeId ? '这条路线已存到你的账号里。' : '未登录时路线不落库，刷新即失。' }}
            </p>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { guideApi } from '@/api'
import { auth } from '@/store/auth'

const router = useRouter()
const TAU = 0.75

const GROUPS = [
  { v: '独自出行', n: '独自' }, { v: '情侣', n: '情侣' }, { v: '朋友结伴', n: '朋友' },
  { v: '家庭亲子', n: '亲子' }, { v: '团队', n: '团队' }
]
const AGES = ['少年', '青年', '中年', '老年']
const PACES = [{ v: '慢游', n: '慢游' }, { v: '适中', n: '适中' }, { v: '紧凑', n: '紧凑' }]
const TYPES = [{ v: 'SCENIC', n: '景点' }, { v: 'HERITAGE', n: '非遗' }, { v: 'FOOD', n: '美食' }]
const DEPTH_NAME = { 1: '简约', 2: '标准', 3: '深度' }
const TYPE_NAME = { SCENIC: '景点', HERITAGE: '非遗', FOOD: '美食', VILLAGE: '村落', PERSON: '人物', CRAFT: '手艺' }
const typeName = k => TYPE_NAME[k] || k

const form = reactive({
  ageBand: '青年', groupType: '朋友结伴', interests: [], pace: '适中',
  preferDialect: '普通话', depthLevel: 2, planMinutes: 240, avoidCrowd: 1,
  entityTypes: ['SCENIC', 'HERITAGE', 'FOOD'], maxItems: 8
})
const avoidCrowd = ref(true)
const countyCode = ref('ZJ-YX-01')
const tagOptions = ref([])
const route = ref(null)
const planning = ref(false)

const usage = computed(() => route.value
  ? Math.min(100, Math.round(route.value.totalMinutes / route.value.planMinutes * 100)) : 0)

/* 路线契合度：把总分翻译成客户能懂的词（总分满分约 1 分/站，7 站可达 4+） */
const fitLevel = computed(() => {
  const r = route.value
  if (!r || !r.nodes?.length) return '—'
  const avg = r.totalScore / r.nodes.length
  if (avg >= 0.75) return '非常合适'
  if (avg >= 0.62) return '相当合适'
  if (avg >= 0.5) return '值得一走'
  return '休闲一看'
})

/* 单站标签：不露算法数值，把四个分数维度（均为 0~1 的原始值）翻译成人话 */
function stopTags(n) {
  const tags = []
  const p = Number(n.scorePreference) || 0
  const h = Number(n.scoreHeat) || 0
  const c = Number(n.scoreCrowd) || 0
  const g = Number(n.scoreContent) || 0
  if (p >= 0.8) tags.push({ t: '正合你的兴趣' })
  else if (p >= 0.5) tags.push({ t: '挺合兴趣' })
  else tags.push({ t: '顺路一看' })
  if (h >= 0.75) tags.push({ t: '人气旺' })
  else if (h >= 0.55) tags.push({ t: '口碑不错' })
  else tags.push({ t: '小众清净' })
  if (c >= 0.7) tags.push({ t: '此刻较挤', neg: true })
  else if (c >= 0.45) tags.push({ t: '人流适中' })
  else tags.push({ t: '当前不挤' })
  if (g >= 0.9) tags.push({ t: '讲解内容齐全' })
  else if (g >= 0.5) tags.push({ t: '内容较全' })
  else tags.push({ t: '内容整理中', neg: true })
  return tags
}

function toggle(t) {
  const i = form.interests.indexOf(t)
  i >= 0 ? form.interests.splice(i, 1) : form.interests.push(t)
}
function toggleType(v) {
  const i = form.entityTypes.indexOf(v)
  if (i >= 0) { if (form.entityTypes.length > 1) form.entityTypes.splice(i, 1) }
  else form.entityTypes.push(v)
}

async function plan() {
  planning.value = true
  try {
    route.value = await guideApi.planRoute({
      countyCode: countyCode.value,
      planMinutes: form.planMinutes,
      avoidCrowd: avoidCrowd.value ? 1 : 0,
      entityTypes: form.entityTypes,
      maxItems: form.maxItems
    })
    if (!route.value?.nodes?.length) ElMessage.warning('这个时间预算下没有可行路线，试着放宽时长')
  } finally { planning.value = false }
}

async function saveProfile() {
  await guideApi.saveProfile({ ...form, avoidCrowd: avoidCrowd.value ? 1 : 0 })
  ElMessage.success('画像已保存')
}
async function infer() {
  const p = await guideApi.inferProfile()
  Object.assign(form, {
    ageBand: p.ageBand || form.ageBand, groupType: p.groupType || form.groupType,
    interests: p.interests || [], pace: p.pace || form.pace,
    preferDialect: p.preferDialect || '普通话', depthLevel: p.depthLevel || 2,
    planMinutes: p.planMinutes || 240
  })
  avoidCrowd.value = p.avoidCrowd !== false
  ElMessage.success('已按你的浏览兴趣推断画像')
}
async function loadRoute(id) { route.value = await guideApi.routeDetail(id) }

function goLive() {
  const first = route.value?.nodes?.[0]
  router.push({ path: '/guide/live', query: { entityId: first?.entityId, routeId: route.value?.routeId || '' } })
}

onMounted(async () => {
  const cs = await guideApi.counties().catch(() => [])
  if (cs?.length) countyCode.value = cs[0].code
  // 兴趣标签直接取自实体标签，保证与打分偏好项可比
  const ents = await guideApi.entities({ countyCode: countyCode.value }).catch(() => [])
  const set = new Set()
  ;(ents || []).forEach(e => String(e.tags || '').split(',').forEach(t => t && set.add(t)))
  tagOptions.value = [...set].slice(0, 14)
  if (auth.isLogin) {
    const p = await guideApi.profile().catch(() => null)
    if (p) {
      Object.assign(form, {
        ageBand: p.ageBand || form.ageBand, groupType: p.groupType || form.groupType,
        interests: p.interests || [], pace: p.pace || form.pace,
        preferDialect: p.preferDialect || '普通话', depthLevel: p.depthLevel || 2,
        planMinutes: p.planMinutes || 240
      })
      avoidCrowd.value = p.avoidCrowd !== false
    }
  }
  plan()
})
</script>

<style scoped>
.gd-field { margin-bottom: 18px; }
.gd-field > label {
  display: block; font-size: .82rem; font-weight: 600; color: var(--rt-ink); margin-bottom: 8px;
}
.gd-field .hint { font-size: .76rem; color: var(--rt-muted); line-height: 1.65; margin-top: 6px; }
.stop-cover {
  width: 54px; height: 38px; object-fit: cover; border-radius: 6px;
  border: 1px solid var(--rt-rule); flex-shrink: 0; background: var(--rt-bg2);
}
@media (max-width: 900px) {
  .gd-plan-grid { grid-template-columns: 1fr !important; }
  .rt-card[style*="sticky"] { position: static !important; }
}
</style>
