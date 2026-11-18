<template>
  <div class="content-grid">
    <div class="rt-card">
      <b>创作参数</b>
      <el-form label-position="top" style="margin-top:14px">
        <el-form-item label="内容类型">
          <el-radio-group v-model="form.type">
            <el-radio-button value="grass">种草文案</el-radio-button>
            <el-radio-button value="guide">游玩攻略</el-radio-button>
            <el-radio-button value="slogan">宣传标语</el-radio-button>
            <el-radio-button value="video">短视频脚本</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="目标景区">
          <el-select v-model="form.scenicSpotId" filterable clearable placeholder="不选则取热门景区" style="width:100%">
            <el-option v-for="s in spots" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="文案风格">
          <el-radio-group v-model="form.style">
            <el-radio-button value="fresh">清新</el-radio-button>
            <el-radio-button value="lively">活泼</el-radio-button>
            <el-radio-button value="literary">文艺</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-button type="primary" style="width:100%" :loading="generating" @click="generate">
          {{ generating ? '生成中…' : '一键生成' }}
        </el-button>
      </el-form>
    </div>
    <div class="rt-card">
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:12px">
        <b>{{ result?.title || '生成结果' }}</b>
        <div>
          <el-tag v-if="result" size="small" :type="result.engine === 'AI增强' ? 'warning' : 'success'">{{ result?.engine }}</el-tag>
          <el-button v-if="result" size="small" style="margin-left:8px" @click="copy">复制全文</el-button>
        </div>
      </div>
      <div v-if="result" class="result-box">{{ result.content }}</div>
      <el-empty v-else description="选择左侧参数,点击一键生成" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi, scenicApi } from '@/api'

const spots = ref([])
const generating = ref(false)
const result = ref(null)
const form = reactive({ type: 'grass', scenicSpotId: null, style: 'fresh', length: 'medium' })

onMounted(async () => {
  try { spots.value = (await scenicApi.page({ size: 100 })).records } catch {}
})
async function generate() {
  generating.value = true
  try { result.value = await adminApi.generate(form) } catch {}
  generating.value = false
}
async function copy() {
  try { await navigator.clipboard.writeText(result.value.content); ElMessage.success('已复制,可直接粘贴到小红书/公众号') }
  catch { ElMessage.warning('复制失败,请手动选择文本') }
}
</script>

<style scoped>
.content-grid { display: grid; grid-template-columns: 340px 1fr; gap: 18px; }
@media (max-width: 900px) { .content-grid { grid-template-columns: 1fr; } }
.result-box { white-space: pre-wrap; line-height: 1.9; font-size: .92rem; background: var(--rt-bg); border-radius: 10px; padding: 18px; max-height: 60vh; overflow-y: auto; }
</style>
