<template>
  <div class="rt-page">
    <div class="gd-hero">
      <h1 class="rt-serif">{{ county?.name || '县域' }}文旅知识图谱</h1>
      <p class="lede">
        景点、非遗、美食不是三条互不相干的清单，而是一张互相牵着的网：走两步能到哪、哪道菜该配哪个景、
        哪几项非遗同出一源。这张网就是后面所有内容生产和路线规划的底座。
      </p>
      <div class="gd-policy-line">对应政策：智慧文旅发展 · 乡村振兴数智赋能</div>
    </div>

    <div class="gd-stats" v-if="graph">
      <div class="gd-stat"><div class="k">实体总数</div><div class="v">{{ stat?.entityCount ?? nodes.length }}</div></div>
      <div class="gd-stat"><div class="k">关系边数</div><div class="v">{{ stat?.relationCount ?? edges.length }}</div></div>
      <div class="gd-stat">
        <div class="k">景点 · 非遗 · 美食</div>
        <div class="v">{{ stat?.scenicCount ?? 0 }}<small>·</small>{{ stat?.heritageCount ?? 0 }}<small>·</small>{{ stat?.foodCount ?? 0 }}</div>
      </div>
      <div class="gd-stat"><div class="k">内容覆盖率</div><div class="v">{{ pct(stat?.contentCoverage) }}<small>%</small></div></div>
      <div class="gd-stat warn"><div class="k">平均连接度</div><div class="v">{{ avgDegree }}</div></div>
    </div>

    <div class="rt-card" style="padding:0">
      <div style="display:flex;gap:8px;flex-wrap:wrap;padding:14px 16px 0">
        <span :class="['gd-pick', !filter && 'on']" @click="filter = ''">全部</span>
        <span v-for="t in TYPE_TABS" :key="t.k"
              :class="['gd-pick', filter === t.k && 'on']" @click="filter = t.k">
          {{ t.n }}（{{ countOf(t.k) }}）
        </span>
      </div>
      <KnowledgeGraph
        :nodes="shownNodes" :edges="shownEdges"
        :active-id="cur?.id" @select="cur = $event"
      />
    </div>

    <div style="display:grid;grid-template-columns:1fr;gap:18px;margin-top:18px">
      <div class="rt-card" v-if="cur">
        <div style="display:flex;justify-content:space-between;align-items:flex-start;gap:16px;flex-wrap:wrap">
          <div>
            <div style="font-size:1.15rem;font-weight:700">{{ cur.name }}</div>
            <div class="rt-muted" style="font-size:.82rem;margin-top:4px">
              {{ typeName(cur.entityType) }} · 建议停留 {{ cur.visitMinutes }} 分钟 · 关联 {{ cur.degree ?? 0 }} 条关系
            </div>
          </div>
          <el-button size="small" text type="primary" @click="goLive(cur)">从这里开始导览 →</el-button>
        </div>
        <p style="margin-top:12px;line-height:1.9;font-size:.92rem">{{ cur.summary }}</p>
        <div style="margin-top:12px">
          <span class="rt-tag" v-for="t in tagList(cur)" :key="t">{{ t }}</span>
        </div>
        <div class="gd-formula">
          <div class="sub" style="margin-top:2px">
            {{ curStatus(cur) }}
          </div>
          <div class="weights">
            <span>{{ cur.heat >= 0.8 ? '🔥 人气很旺' : cur.heat >= 0.6 ? '👍 人气不错' : '🌿 清静小众' }}</span>
            <span>{{ cur.crowd >= 0.75 ? '🧭 此刻较挤，建议错峰' : cur.crowd >= 0.5 ? '🙂 人流适中' : '😊 现在人少' }}</span>
            <span>{{ cur.contentScore >= 1 ? '📖 讲解内容齐全' : '📝 讲解内容整理中' }}</span>
          </div>
          <div class="sub" style="margin-top:8px">
            想知道它会不会排进你的路线？<router-link to="/guide/plan" style="color:var(--rt-green)">填一份你的偏好</router-link>，每个点的排序都会不一样。
          </div>
        </div>
      </div>
      <div class="gd-empty" v-else>
        <div class="glyph">◎</div>
        <div class="t">点一个节点看看</div>
        <div class="d">
          圆越大说明这个点越热，橙色外环越粗说明现在越挤。
          鼠标停在任意节点上，会亮出它和谁相连——这就是路线规划时"下一站去哪"的依据。
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { guideApi } from '@/api'
import KnowledgeGraph from '@/components/KnowledgeGraph.vue'

const router = useRouter()
const TYPE_TABS = [
  { k: 'SCENIC', n: '景点' }, { k: 'HERITAGE', n: '非遗' },
  { k: 'FOOD', n: '美食' }, { k: 'VILLAGE', n: '村落' }
]
const TYPE_NAME = { SCENIC: '景点', HERITAGE: '非遗', FOOD: '美食', VILLAGE: '村落', PERSON: '人物', CRAFT: '手艺' }
const typeName = k => TYPE_NAME[k] || k
const pct = v => Math.round((Number(v) || 0) * 100)

const countyCode = ref('ZJ-YX-01')
const graph = ref(null)
const filter = ref('')
const cur = ref(null)

const county = computed(() => graph.value?.counties?.[0] || null)
const stat = computed(() => graph.value?.stat || null)
const nodes = computed(() => graph.value?.nodes || [])
const edges = computed(() => graph.value?.edges || [])
const countOf = k => nodes.value.filter(n => n.entityType === k).length

const shownNodes = computed(() => filter.value ? nodes.value.filter(n => n.entityType === filter.value) : nodes.value)
const shownEdges = computed(() => {
  const ids = new Set(shownNodes.value.map(n => n.id))
  return edges.value.filter(e => ids.has(e.sourceId) && ids.has(e.targetId))
})

const avgDegree = computed(() => {
  if (!nodes.value.length) return '0'
  return (edges.value.length * 2 / nodes.value.length).toFixed(1)
})

const tagList = n => Array.isArray(n.tags) ? n.tags : (n.tags ? String(n.tags).split(',') : [])

/* 客户端不展示算法数值，把热度/拥挤/内容翻译成一句人话 */
const curStatus = n => {
  const h = Number(n.heat) || 0, c = Number(n.crowd) || 0
  const parts = []
  parts.push(h >= 0.8 ? '这是眼下最受欢迎的点之一' : h >= 0.6 ? '这个点口碑一直在涨' : '这个点还比较小众')
  if (c >= 0.75) parts.push('最近这个时段人偏多，建议早点来或换个时段')
  else if (c >= 0.5) parts.push('当前人流适中')
  else parts.push('现在来会很清净')
  parts.push('是否排进你的行程，取决于你填的偏好')
  return parts.join('；') + '。'
}

function goLive(n) {
  router.push({ path: '/guide/live', query: { entityId: n.id } })
}

async function load() {
  graph.value = await guideApi.graph(countyCode.value)
  if (!cur.value && nodes.value.length) cur.value = nodes.value[0]
}

onMounted(async () => {
  const cs = await guideApi.counties().catch(() => [])
  if (cs?.length) countyCode.value = cs[0].code
  load()
})
watch(filter, () => { cur.value = null })
</script>
