<template>
  <div class="rt-page" style="max-width:860px">
    <h1 class="rt-serif" style="font-size:1.6rem; text-align:center">📷 拍照识景</h1>
    <p class="rt-muted" style="text-align:center; margin:10px 0 26px">拍一张眼前的风景,小乡帮你认出这是哪里</p>

    <div class="rt-card">
      <el-upload drag :auto-upload="false" :show-file-list="false" accept="image/*" :on-change="onPick">
        <div v-if="!preview" style="padding:40px 0">
          <div style="font-size:3rem">🖼️</div>
          <p>点击或拖拽图片到这里</p>
          <p class="rt-muted" style="font-size:.8rem">支持 JPG/PNG,不超过 10MB</p>
        </div>
        <img v-else :src="preview" style="max-height:280px; max-width:100%; object-fit:contain" />
      </el-upload>
      <div style="text-align:center; margin-top:16px">
        <el-button type="primary" round :disabled="!file" :loading="recognizing" @click="recognize">
          {{ recognizing ? '识别中…' : '开始识别' }}
        </el-button>
        <el-button v-if="preview" round @click="reset">换一张</el-button>
      </div>
    </div>

    <div v-if="result" style="margin-top:26px">
      <el-alert :type="result.matched ? 'success' : 'warning'" :title="result.message" :closable="false" />
      <div v-if="result.matched && result.spot" style="margin-top:18px">
        <p class="rt-muted" style="margin-bottom:10px">置信度 {{ Math.round((result.confidence || 0) * 100) }}%</p>
        <ScenicCard :spot="result.spot" />
      </div>
      <template v-if="!result.matched && result.alternatives?.length">
        <h3 class="rt-section-title" style="font-size:1.1rem">你也许想去</h3>
        <div class="rt-grid">
          <ScenicCard v-for="s in result.alternatives" :key="s.id" :spot="s" />
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { aiApi } from '@/api'
import ScenicCard from '@/components/ScenicCard.vue'

const file = ref(null)
const preview = ref('')
const recognizing = ref(false)
const result = ref(null)

function onPick(f) {
  if (f.size > 10 * 1024 * 1024) { ElMessage.error('图片不能超过10MB'); return }
  file.value = f.raw
  preview.value = URL.createObjectURL(f.raw)
  result.value = null
}
async function recognize() {
  recognizing.value = true
  try { result.value = await aiApi.recognize(file.value) } catch {}
  recognizing.value = false
}
function reset() { file.value = null; preview.value = ''; result.value = null }
</script>
