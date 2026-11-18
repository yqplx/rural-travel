<template>
  <div>
    <div class="gd-stats">
      <div class="gd-stat"><div class="k">内容总量</div><div class="v">{{ m.contentTotal ?? '—' }}</div></div>
      <div class="gd-stat"><div class="k">模态覆盖</div><div class="v">{{ m.modalityCovered ?? 0 }}<small>/4</small></div></div>
      <div class="gd-stat"><div class="k">实体覆盖率</div><div class="v">{{ pct(m.contentCoverage) }}<small>%</small></div></div>
      <div class="gd-stat"><div class="k">方言版本</div><div class="v">{{ m.dialectCount ?? 0 }}</div></div>
      <div class="gd-stat warn"><div class="k">AI 生成占比</div><div class="v">{{ pct(m.aiRatio) }}<small>%</small></div></div>
    </div>

    <!-- 生产面板 -->
    <div class="gd-work">
      <h3 style="display:flex;align-items:center;gap:10px">批量生产
        <el-tag type="success" effect="plain" size="small">一人 · 一天 · 全县</el-tag>
      </h3>
      <p class="hint">
        选好模态、风格、口音，点一次就把组合全部生成。已存在的组合会自动跳过，所以可以放心重跑——
        这正是"低成本内容量产"能落地的前提：不是每次都推倒重来。
      </p>

      <div class="gd-field">
        <label>生产范围</label>
        <div class="gd-picks" style="margin-bottom:12px">
          <span :class="['gd-pick', !scopeEntity && 'on']" @click="scopeEntity = null">全县域（{{ entityOptions.length }} 个实体）</span>
          <span :class="['gd-pick', !!scopeEntity && 'on']" @click="scopeEntity = entityOptions[0]?.id || 0">单个实体</span>
        </div>
        <el-select v-if="scopeEntity" v-model="scopeEntity" filterable placeholder="选择实体" style="width:260px">
          <el-option v-for="e in entityOptions" :key="e.id" :label="e.name" :value="e.id" />
        </el-select>
      </div>

      <div class="gd-field">
        <label>模态</label>
        <div class="gd-picks">
          <span v-for="t in MODALITIES" :key="t.v" :class="['gd-pick', batch.contentTypes.includes(t.v) && 'on']"
                @click="toggle(batch.contentTypes, t.v)">{{ t.n }}</span>
        </div>
      </div>

      <div class="gd-field">
        <label>风格</label>
        <div class="gd-picks">
          <span v-for="s in STYLES" :key="s" :class="['gd-pick', batch.styles.includes(s) && 'on']"
                @click="toggle(batch.styles, s)">{{ s }}</span>
        </div>
      </div>

      <div class="gd-field">
        <label>口音</label>
        <div class="gd-picks">
          <span v-for="d in DIALECTS" :key="d" :class="['gd-pick', batch.dialects.includes(d) && 'on']"
                @click="toggle(batch.dialects, d)">{{ d }}</span>
        </div>
      </div>

      <div class="gd-field" style="display:flex;align-items:center;justify-content:space-between;gap:16px;flex-wrap:wrap">
        <div>
          <label style="margin-bottom:2px">用大模型生成</label>
          <p class="hint" style="margin:0">
            {{ batch.useAi ? '调用你配置的 AI 接口生成，质量更高、耗时更长。' : '关闭则走本地模板，秒出，可离线演示。' }}
          </p>
        </div>
        <el-switch v-model="batch.useAi" />
      </div>

      <div style="display:flex;gap:10px;margin-top:18px;align-items:center;flex-wrap:wrap">
        <el-button type="primary" :loading="producing" @click="produce">
          {{ scopeEntity ? '为这个实体生产' : `为全县域 ${entityOptions.length} 个实体生产` }}
        </el-button>
        <span class="rt-muted" style="font-size:.8rem">
          本次将尝试 {{ batch.contentTypes.length }}×{{ batch.styles.length }}×{{ batch.dialects.length }} =
          {{ batch.contentTypes.length * batch.styles.length * batch.dialects.length }} 种组合
        </span>
      </div>

      <div class="gd-produce-out" v-if="out">
        <template v-if="out.sum">
          <div><span class="ok">新增 {{ out.sum.produced }} 条</span> ·
            <span class="skip">跳过 {{ out.sum.skipped }} 条（已存在）</span> ·
            覆盖 {{ out.count }} 个实体 · 引擎 {{ out.engine }}
          </div>
        </template>
        <div v-for="r in out.list" :key="r.entityId" style="font-size:.82rem">
          {{ r.entityName }}：
          <span class="ok">+{{ r.produced }}</span>
          <span class="skip"> / 跳过 {{ r.skipped }}</span>
          <template v-if="r.contentScoreAfter != null">
            · 内容完备度 g<sub>i</sub> {{ r.contentScoreBefore?.toFixed(2) }} →
            <b>{{ r.contentScoreAfter.toFixed(2) }}</b>
          </template>
        </div>
      </div>
    </div>

    <!-- 内容清单 -->
    <div class="gd-work">
      <h3>内容清单</h3>
      <p class="hint">按实体或模态筛选，可直接删除不合格内容——删完该实体的内容完备度 g<sub>i</sub> 会自动重算。</p>
      <div style="display:flex;gap:10px;flex-wrap:wrap;align-items:center">
        <el-select v-model="f.entityId" filterable clearable placeholder="按实体" style="width:200px" @change="loadContents">
          <el-option v-for="e in entityOptions" :key="e.id" :label="e.name" :value="e.id" />
        </el-select>
        <el-select v-model="f.contentType" clearable placeholder="按模态" style="width:140px" @change="loadContents">
          <el-option v-for="t in MODALITIES" :key="t.v" :label="t.n" :value="t.v" />
        </el-select>
        <el-button @click="loadContents">查询</el-button>
      </div>

      <el-table :data="contents" size="small" style="margin-top:14px" v-loading="cloading" max-height="520">
        <el-table-column label="所属实体" width="130">
          <template #default="{ row }">{{ nameOf(row.entityId) }}</template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column label="模态" width="78">
          <template #default="{ row }"><span class="rt-tag">{{ modName(row.contentType) }}</span></template>
        </el-table-column>
        <el-table-column prop="style" label="风格" width="70" />
        <el-table-column label="口音" width="74">
          <template #default="{ row }">
            <span :class="['rt-tag', row.dialect === '方言' && 'rt-tag-warn']">{{ row.dialect }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="depthLevel" label="档位" width="60" align="center" />
        <el-table-column label="质量分" width="80" align="right">
          <template #default="{ row }"><span class="gd-num">{{ row.scoreEma?.toFixed(2) }}</span></template>
        </el-table-column>
        <el-table-column label="曝光/完播" width="100" align="right">
          <template #default="{ row }"><span class="gd-num">{{ row.exposure }}/{{ row.finishCount }}</span></template>
        </el-table-column>
        <el-table-column label="引擎" width="90">
          <template #default="{ row }"><span class="rt-muted" style="font-size:.78rem">{{ row.engine }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="70" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="del(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination style="margin-top:14px;justify-content:flex-end"
        v-model:current-page="f.current" v-model:page-size="f.size"
        :total="ctotal" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="loadContents" />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'

const MODALITIES = [
  { v: 'TEXT', n: '图文文案' }, { v: 'AUDIO', n: '语音讲解' },
  { v: 'IMAGE', n: '宣传画面' }, { v: 'VIDEO', n: '视频脚本' }
]
const STYLES = ['清新', '活泼', '文艺']
const DIALECTS = ['普通话', '方言']
const modName = v => MODALITIES.find(m => m.v === v)?.n || v
const pct = v => Math.round((Number(v) || 0) * 100)

const countyCode = ref('ZJ-YX-01')
const m = ref({})
const entityOptions = ref([])
const scopeEntity = ref(null)
const producing = ref(false)
const out = ref(null)

const batch = reactive({ contentTypes: ['TEXT', 'AUDIO', 'VIDEO'], styles: ['清新'], dialects: ['普通话', '方言'], useAi: false })

const f = reactive({ current: 1, size: 10, entityId: null, contentType: '' })
const contents = ref([])
const ctotal = ref(0)
const cloading = ref(false)

const nameOf = id => entityOptions.value.find(e => e.id === id)?.name || `#${id}`
const toggle = (arr, v) => {
  const i = arr.indexOf(v)
  if (i >= 0) { if (arr.length > 1) arr.splice(i, 1) } else arr.push(v)
}

async function produce() {
  if (scopeEntity.value === 0 && entityOptions.value.length) scopeEntity.value = entityOptions.value[0].id
  if (!batch.contentTypes.length || !batch.styles.length || !batch.dialects.length) {
    ElMessage.warning('模态、风格、口音至少各选一项'); return
  }
  producing.value = true
  out.value = null
  try {
    const payload = { ...batch }
    if (scopeEntity.value) {
      const r = await adminApi.produceContent({ ...payload, entityId: scopeEntity.value })
      out.value = { list: [r], count: 1, engine: r.engine, sum: { produced: r.produced, skipped: r.skipped } }
    } else {
      const list = await adminApi.produceAllContent(countyCode.value, payload)
      out.value = {
        list, count: list.length, engine: list[0]?.engine,
        sum: { produced: list.reduce((s, x) => s + x.produced, 0), skipped: list.reduce((s, x) => s + x.skipped, 0) }
      }
    }
    ElMessage.success(`完成，新增 ${out.value.sum.produced} 条`)
    loadMetrics(); loadContents()
  } finally { producing.value = false }
}

async function del(row) {
  await ElMessageBox.confirm('删除这条内容？该实体的内容完备度会重算。', '确认', { type: 'warning' })
  await adminApi.deleteGuideContent(row.id)
  ElMessage.success('已删除')
  loadContents(); loadMetrics()
}

async function loadContents() {
  cloading.value = true
  try {
    const r = await adminApi.guideContents({ ...f })
    contents.value = r.records || []
    ctotal.value = r.total
  } finally { cloading.value = false }
}
async function loadMetrics() { m.value = await adminApi.guideMetrics().catch(() => ({})) }

onMounted(async () => {
  const r = await adminApi.guideEntities({ current: 1, size: 500, countyCode: countyCode.value }).catch(() => null)
  entityOptions.value = r?.records || []
  loadMetrics(); loadContents()
})
</script>

<style scoped>
.gd-field { margin-bottom: 18px; }
.gd-field > label { display: block; font-size: .82rem; font-weight: 600; margin-bottom: 8px; }
.gd-field .hint { font-size: .76rem; color: var(--rt-muted); line-height: 1.65; margin-top: 6px; }
.rt-tag-warn { background: var(--rt-orange-light); color: var(--rt-orange); }
</style>
