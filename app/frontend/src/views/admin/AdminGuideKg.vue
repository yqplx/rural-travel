<template>
  <div>
    <div class="gd-stats">
      <div class="gd-stat"><div class="k">实体</div><div class="v">{{ page.total }}</div></div>
      <div class="gd-stat"><div class="k">关系</div><div class="v">{{ relations.length }}</div></div>
      <div class="gd-stat"><div class="k">景点</div><div class="v">{{ countOf('SCENIC') }}</div></div>
      <div class="gd-stat"><div class="k">非遗</div><div class="v">{{ countOf('HERITAGE') }}</div></div>
      <div class="gd-stat"><div class="k">美食</div><div class="v">{{ countOf('FOOD') }}</div></div>
    </div>

    <div class="rt-card" style="padding:16px 18px">
      <div style="display:flex;gap:10px;flex-wrap:wrap;align-items:center">
        <el-input v-model="q.keyword" placeholder="搜名称或简介" clearable style="width:200px" @keyup.enter="load()" />
        <el-select v-model="q.entityType" placeholder="类型" clearable style="width:130px" @change="load()">
          <el-option v-for="t in TYPES" :key="t.v" :label="t.n" :value="t.v" />
        </el-select>
        <el-button @click="load()">查询</el-button>
        <el-button type="primary" plain @click="openEntity()">新增实体</el-button>
        <el-button text @click="openRelation()">新增关系</el-button>
      </div>

      <el-table :data="records" style="margin-top:14px" v-loading="loading" size="small">
        <el-table-column prop="name" label="名称" min-width="130" />
        <el-table-column label="类型" width="80">
          <template #default="{ row }"><span class="rt-tag">{{ typeName(row.entityType) }}</span></template>
        </el-table-column>
        <el-table-column prop="summary" label="简介" min-width="220" show-overflow-tooltip />
        <el-table-column label="热度 h" width="86" align="right">
          <template #default="{ row }"><span class="gd-num">{{ row.heat?.toFixed(2) }}</span></template>
        </el-table-column>
        <el-table-column label="拥挤 c" width="86" align="right">
          <template #default="{ row }">
            <span class="gd-num" :style="{ color: row.crowd > 0.75 ? 'var(--rt-orange)' : 'inherit' }">{{ row.crowd?.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="内容 g" width="86" align="right">
          <template #default="{ row }"><span class="gd-num">{{ row.contentScore?.toFixed(2) }}</span></template>
        </el-table-column>
        <el-table-column label="停留 w" width="80" align="right">
          <template #default="{ row }"><span class="gd-num">{{ row.visitMinutes }}′</span></template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEntity(row)">编辑</el-button>
            <el-button link type="danger" @click="delEntity(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination style="margin-top:14px;justify-content:flex-end"
        v-model:current-page="q.current" v-model:page-size="q.size"
        :total="page.total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="load()" />
    </div>

    <!-- 关系 -->
    <div class="rt-card" style="margin-top:16px;padding:16px 18px">
      <span class="rt-section-title" style="margin-top:0">关系边（{{ relations.length }}）</span>
      <p class="rt-muted" style="font-size:.8rem;line-height:1.7;margin-bottom:12px">
        关系边决定路线能不能接得上：TRANSFER 是步行接驳（带 t<sub>i,j</sub> 分钟），
        BELONGS_TO 是"这道菜归哪个景"，SAME_THEME 把同主题的点串起来。
      </p>
      <el-table :data="relations" size="small" max-height="380">
        <el-table-column label="起点" width="140">
          <template #default="{ row }">{{ nameOf(row.sourceId) }}</template>
        </el-table-column>
        <el-table-column label="关系" width="120">
          <template #default="{ row }"><span class="rt-tag">{{ relName(row.relationType) }}</span></template>
        </el-table-column>
        <el-table-column label="终点" width="140">
          <template #default="{ row }">{{ nameOf(row.targetId) }}</template>
        </el-table-column>
        <el-table-column label="步行" width="80" align="right">
          <template #default="{ row }"><span class="gd-num">{{ row.walkMinutes ?? '—' }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button link type="danger" @click="delRelation(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 实体编辑 -->
    <el-dialog v-model="dlg" :title="form.id ? '编辑实体' : '新增实体'" width="620px">
      <el-form :model="form" label-width="92px" size="small">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.entityType" style="width:100%">
            <el-option v-for="t in TYPES" :key="t.v" :label="t.n" :value="t.v" />
          </el-select>
        </el-form-item>
        <el-form-item label="一句话简介"><el-input v-model="form.summary" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="详细介绍"><el-input v-model="form.detail" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="标签">
          <el-input v-model="tagText" placeholder="逗号分隔，如：古村落,摄影" />
        </el-form-item>
        <el-form-item label="经纬度">
          <div style="display:flex;gap:8px;width:100%">
            <el-input v-model.number="form.lng" placeholder="经度" />
            <el-input v-model.number="form.lat" placeholder="纬度" />
          </div>
        </el-form-item>
        <el-form-item label="建议停留">
          <el-input-number v-model="form.visitMinutes" :min="5" :max="300" :step="5" /> 分钟
        </el-form-item>
        <el-form-item label="开放时间"><el-input v-model="form.openTime" placeholder="全天开放" /></el-form-item>
        <el-form-item label="热度 / 拥挤">
          <div style="display:flex;gap:12px;width:100%;align-items:center">
            <el-slider v-model="form.heat" :min="0" :max="1" :step="0.01" style="flex:1" />
            <span class="gd-num" style="width:38px">{{ form.heat?.toFixed(2) }}</span>
            <el-slider v-model="form.crowd" :min="0" :max="1" :step="0.01" style="flex:1" />
            <span class="gd-num" style="width:38px">{{ form.crowd?.toFixed(2) }}</span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEntity">保存</el-button>
      </template>
    </el-dialog>

    <!-- 关系编辑 -->
    <el-dialog v-model="rdlg" title="新增关系" width="460px">
      <el-form :model="rform" label-width="80px" size="small">
        <el-form-item label="起点">
          <el-select v-model="rform.sourceId" filterable style="width:100%">
            <el-option v-for="e in all" :key="e.id" :label="e.name" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关系">
          <el-select v-model="rform.relationType" style="width:100%">
            <el-option v-for="r in RELS" :key="r.v" :label="r.n" :value="r.v" />
          </el-select>
        </el-form-item>
        <el-form-item label="终点">
          <el-select v-model="rform.targetId" filterable style="width:100%">
            <el-option v-for="e in all" :key="e.id" :label="e.name" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="步行分钟">
          <el-input-number v-model="rform.walkMinutes" :min="0" :max="120" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rdlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRelation">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api'

const TYPES = [
  { v: 'SCENIC', n: '景点' }, { v: 'HERITAGE', n: '非遗' }, { v: 'FOOD', n: '美食' },
  { v: 'VILLAGE', n: '村落' }, { v: 'PERSON', n: '人物' }, { v: 'CRAFT', n: '手艺' }
]
const RELS = [
  { v: 'TRANSFER', n: '步行接驳' }, { v: 'NEARBY', n: '邻近' },
  { v: 'BELONGS_TO', n: '归属' }, { v: 'RECOMMEND_WITH', n: '搭配推荐' }, { v: 'SAME_THEME', n: '同主题' }
]
const TYPE_NAME = Object.fromEntries(TYPES.map(t => [t.v, t.n]))
const typeName = k => TYPE_NAME[k] || k
const relName = k => RELS.find(r => r.v === k)?.n || k

const countyCode = ref('ZJ-YX-01')
const q = reactive({ current: 1, size: 10, keyword: '', entityType: '' })
const page = ref({ total: 0 })
const records = ref([])
const all = ref([])
const relations = ref([])
const loading = ref(false)
const saving = ref(false)
const dlg = ref(false)
const rdlg = ref(false)
const tagText = ref('')

const blank = () => ({
  id: null, countyCode: countyCode.value, entityType: 'SCENIC', name: '', summary: '', detail: '',
  tags: [], lng: null, lat: null, visitMinutes: 30, crowd: 0.4, heat: 0.5, openTime: '全天开放', status: 1
})
const form = ref(blank())
const rform = ref({ sourceId: null, targetId: null, relationType: 'TRANSFER', walkMinutes: 5, status: 1 })

const countOf = t => all.value.filter(e => e.entityType === t).length
const nameOf = id => all.value.find(e => e.id === id)?.name || `#${id}`

async function load() {
  loading.value = true
  try {
    const r = await adminApi.guideEntities({ ...q, countyCode: countyCode.value })
    page.value = { total: r.total }
    records.value = r.records || []
    all.value = await adminApi.guideEntities({ current: 1, size: 500, countyCode: countyCode.value })
      .then(x => x.records || []).catch(() => [])
    relations.value = await adminApi.guideRelations(countyCode.value).catch(() => [])
  } finally { loading.value = false }
}

function openEntity(row) {
  form.value = row ? { ...row } : blank()
  tagText.value = Array.isArray(form.value.tags) ? form.value.tags.join(',') : (form.value.tags || '')
  dlg.value = true
}
async function saveEntity() {
  saving.value = true
  try {
    const payload = {
      ...form.value,
      countyCode: countyCode.value,
      tags: tagText.value.split(/[,，]/).map(s => s.trim()).filter(Boolean)
    }
    await adminApi.saveGuideEntity(payload)
    ElMessage.success('已保存')
    dlg.value = false
    load()
  } finally { saving.value = false }
}
async function delEntity(row) {
  await ElMessageBox.confirm(`删除「${row.name}」？其关联关系与内容也会一并移除。`, '确认', { type: 'warning' })
  await adminApi.deleteGuideEntity(row.id)
  ElMessage.success('已删除')
  load()
}

function openRelation() { rform.value = { sourceId: null, targetId: null, relationType: 'TRANSFER', walkMinutes: 5, status: 1 }; rdlg.value = true }
async function saveRelation() {
  if (!rform.value.sourceId || !rform.value.targetId) { ElMessage.warning('请选择起点和终点'); return }
  saving.value = true
  try {
    await adminApi.saveGuideRelation(rform.value)
    ElMessage.success('已新增')
    rdlg.value = false
    load()
  } finally { saving.value = false }
}
async function delRelation(row) {
  await adminApi.deleteGuideRelation(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
