<template>
  <div class="gd-graph-shell">
    <svg class="gd-graph" :viewBox="`0 0 ${W} ${H}`" role="img" aria-label="县域文旅知识图谱">
      <!-- 县域环：地图的底子 -->
      <circle :cx="cx" :cy="cy" :r="R" fill="none" stroke="#EDE7D8" stroke-width="1" />
      <circle :cx="cx" :cy="cy" :r="R * 0.55" fill="none" stroke="#F2EEE4" stroke-width="1" stroke-dasharray="2 6" />

      <!-- 关系连线 -->
      <g>
        <path
          v-for="e in curved" :key="e.k"
          :class="['gd-edge', kindOf(e), edgeClass(e)]"
          :d="e.d"
        />
      </g>

      <!-- 路线：一条游走的墨线 -->
      <path v-if="routePath" class="gd-edge" :d="routePath"
            style="stroke:#26302A;stroke-width:2.4;opacity:.9;stroke-dasharray:none" />

      <!-- 讲解涟漪 -->
      <circle v-if="rippleAt != null && pos[rippleAt]" class="gd-ripple"
              :cx="pos[rippleAt].x" :cy="pos[rippleAt].y" r="8" />

      <!-- 节点 -->
      <g
        v-for="(n, i) in laid" :key="n.id"
        :class="['gd-node', nodeClass(n)]"
        @mouseenter="hover = n.id" @mouseleave="hover = null"
        @click="$emit('select', n)"
      >
        <circle class="halo" :cx="n.x" :cy="n.y" :r="rOf(n) * 2.4"
                :style="{ animationDelay: (i % 12) * 0.37 + 's' }" />
        <circle v-if="crowdRing(n) > 0" class="crowd-ring"
                :cx="n.x" :cy="n.y" :r="rOf(n) + 3.5" :stroke-width="crowdRing(n)" />
        <circle class="core" :cx="n.x" :cy="n.y" :r="rOf(n)" :fill="colorOf(n)" />
        <text class="label" :x="n.x" :y="n.y + rOf(n) + 13">{{ short(n.name) }}</text>
      </g>
    </svg>

    <div class="gd-legend">
      <span v-for="t in types" :key="t.k">
        <i class="dot" :style="{ background: t.c }"></i>{{ t.n }}
      </span>
      <span><i style="background:#C9D8C6"></i>步行接驳</span>
      <span><i style="background:#E8D6C2"></i>归属/搭配</span>
      <span><i style="background:#D8CFE4"></i>同主题</span>
      <span style="color:var(--rt-muted)">圆越大＝热度越高 · 橙环越粗＝越拥挤</span>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  nodes: { type: Array, default: () => [] },
  edges: { type: Array, default: () => [] },
  routeSeq: { type: Array, default: () => [] },   // 按路线顺序排列的 entityId
  activeId: { type: [Number, String], default: null },
  rippleAt: { type: [Number, String], default: null }
})
defineEmits(['select'])

const W = 760, H = 600, cx = 380, cy = 296, R = 232

const TYPES = [
  { k: 'SCENIC',   n: '景点', c: '#3E7C4F' },
  { k: 'HERITAGE', n: '非遗', c: '#C2662B' },
  { k: 'FOOD',     n: '美食', c: '#A9812F' },
  { k: 'VILLAGE',  n: '村落', c: '#5B9468' },
  { k: 'PERSON',   n: '人物', c: '#8A7B5C' },
  { k: 'CRAFT',    n: '手艺', c: '#9C6B4F' }
]
const types = TYPES
const colorMap = Object.fromEntries(TYPES.map(t => [t.k, t.c]))
const colorOf = n => colorMap[n.entityType] || '#67715F'
const short = s => (s && s.length > 7 ? s.slice(0, 6) + '…' : s || '')

/* 半径＝热度（h_i） */
const rOf = n => 5 + (Number(n.heat) || 0) * 9
/* 拥挤环＝拥挤度（c_i），超过 0.6 才显性提示 */
const crowdRing = n => {
  const c = Number(n.crowd) || 0
  return c > 0.6 ? 1 + (c - 0.6) * 8 : 0
}

/* 分区环形布局：同类型聚成一弧，弧间留白，像县域分区 */
const laid = computed(() => {
  const list = props.nodes || []
  if (!list.length) return []
  const groups = TYPES.map(t => ({ t, items: list.filter(n => n.entityType === t.k) }))
    .filter(g => g.items.length)
  const rest = list.filter(n => !TYPES.some(t => t.k === n.entityType))
  if (rest.length) groups.push({ t: { k: 'OTHER', n: '其他', c: '#67715F' }, items: rest })

  const GAP = 1.6
  const slots = list.length + GAP * Math.max(0, groups.length - 1)
  const step = (Math.PI * 2) / slots
  let a = -Math.PI / 2

  const out = []
  for (const g of groups) {
    for (const n of g.items) {
      out.push({
        ...n,
        x: cx + Math.cos(a) * R,
        y: cy + Math.sin(a) * R
      })
      a += step
    }
    a += step * GAP
  }
  return out
})

const pos = computed(() => Object.fromEntries(laid.value.map(n => [n.id, n])))

/* 连线：控制点向圆心收，形成柔和的弧 */
const curved = computed(() => {
  const p = pos.value
  return (props.edges || [])
    .filter(e => p[src(e)] && p[dst(e)])
    .map(e => {
      const s = p[src(e)], t = p[dst(e)]
      const mx = (s.x + t.x) / 2, my = (s.y + t.y) / 2
      const qx = cx + (mx - cx) * 0.55, qy = cy + (my - cy) * 0.55
      return { ...e, k: `${src(e)}-${dst(e)}-${e.relationType}`, d: `M${s.x},${s.y} Q${qx},${qy} ${t.x},${t.y}` }
    })
})

/* 后端 GraphEdge 用 source/target，KgRelation 用 sourceId/targetId，两种都兼容 */
const src = e => e.source ?? e.sourceId
const dst = e => e.target ?? e.targetId

const kindOf = e =>
  e.relationType === 'TRANSFER' || e.relationType === 'NEARBY' ? 'transfer'
    : e.relationType === 'SAME_THEME' ? 'theme' : 'belong'

const hover = ref(null)

const linked = computed(() => {
  if (hover.value == null) return null
  const s = new Set([hover.value])
  ;(props.edges || []).forEach(e => {
    if (src(e) === hover.value) s.add(dst(e))
    if (dst(e) === hover.value) s.add(src(e))
  })
  return s
})

const nodeClass = n => {
  // 选中只做高亮，不压暗其他节点；压暗只在悬停聚焦时发生
  if (props.activeId != null && n.id === props.activeId) return 'on'
  if (linked.value && !linked.value.has(n.id)) return 'dim'
  return ''
}
const edgeClass = e => {
  if (hover.value == null) return ''
  return (src(e) === hover.value || dst(e) === hover.value) ? 'hot' : 'dim'
}

/* 路线墨线：按 seq 依次穿点 */
const routePath = computed(() => {
  const p = pos.value
  const seq = (props.routeSeq || []).map(id => p[id]).filter(Boolean)
  if (seq.length < 2) return ''
  return seq.map((n, i) => `${i ? 'L' : 'M'}${n.x},${n.y}`).join(' ')
})
</script>
