<template>
  <el-dialog :model-value="modelValue" width="600px" align-center
             :show-close="false" :close-on-click-modal="false" class="pref-dialog"
             @update:model-value="v => !v && skip()">
    <template #header>
      <div class="pref-head">
        <div class="pref-title">🌾 找到属于你的乡野玩法</div>
        <div class="pref-sub">勾选你感兴趣的主题，首页推荐会更懂你</div>
      </div>
    </template>

    <div v-loading="loading" class="pref-grid">
      <button v-for="t in tags" :key="t.id" type="button"
              class="pref-chip" :class="{ active: selected.includes(t.id) }"
              @click="toggle(t.id)">
        <span class="pref-icon">{{ tagIcon(t.name) }}</span>
        <span>{{ t.name }}</span>
      </button>
    </div>

    <template #footer>
      <div class="pref-foot">
        <span class="rt-muted" style="font-size:.82rem">已选 {{ selected.length }} 项（可多选）</span>
        <div>
          <el-button text @click="skip">跳过</el-button>
          <el-button type="primary" round :disabled="!selected.length" :loading="saving" @click="save">
            保存偏好，为我推荐
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { tagApi, userApi } from '@/api'
import { auth } from '@/store/auth'

const props = defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue', 'saved'])

const loading = ref(true)
const saving = ref(false)
const tags = ref([])
const selected = ref([])

const ICONS = { 花海田园: '🌸', 梯田景观: '🌾', 徒步漫游: '🥾', 民俗体验: '🎏', 特色美食: '🍜', 古建古迹: '🏛️', 摄影胜地: '📷', 精品民宿: '🏡', 蔬果采摘: '🍓', 亲子好友: '🧒', 古镇夜景: '🏮', 红色研学: '🚩' }
const tagIcon = name => ICONS[name] || '🌿'

onMounted(async () => {
  try {
    tags.value = await tagApi.list()
    // 仅在已登录时拉取已存偏好,否则该接口会 401 触发跳转
    if (auth.isLogin && selected.value.length === 0) {
      const saved = await userApi.interests()
      selected.value = saved || []
    }
  } catch {}
  loading.value = false
})

function toggle(id) {
  const i = selected.value.indexOf(id)
  i >= 0 ? selected.value.splice(i, 1) : selected.value.push(id)
}

async function save() {
  saving.value = true
  try {
    await userApi.setInterests(selected.value)
    ElMessage.success('偏好已保存，首页推荐已为你更新')
    localStorage.setItem('rural_pref_done', '1')
    emit('saved')
    emit('update:modelValue', false)
  } catch {} finally { saving.value = false }
}

function skip() {
  localStorage.setItem('rural_pref_done', '1')
  emit('update:modelValue', false)
}
</script>

<style scoped>
.pref-head { text-align: center; padding: 4px 0 2px; }
.pref-title { font-family: "Noto Serif SC", serif; font-size: 1.3rem; font-weight: 700; color: var(--rt-green-dark); }
.pref-sub { color: var(--rt-muted); font-size: .88rem; margin-top: 6px; }
.pref-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; min-height: 120px; }
.pref-chip {
  display: flex; align-items: center; justify-content: center; gap: 8px;
  padding: 14px 8px; border-radius: 12px; border: 1.5px solid var(--rt-rule);
  background: var(--rt-bg); cursor: pointer; font-size: .95rem; color: var(--rt-ink);
  transition: all .2s; font-family: inherit;
}
.pref-chip:hover { border-color: var(--rt-green); transform: translateY(-2px); }
.pref-chip.active {
  border-color: var(--rt-green); background: var(--rt-green-light);
  color: var(--rt-green-dark); font-weight: 700;
  box-shadow: 0 4px 14px rgba(62, 124, 79, .18);
}
.pref-icon { font-size: 1.15rem; }
.pref-foot { display: flex; justify-content: space-between; align-items: center; }
@media (max-width: 520px) {
  .pref-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
