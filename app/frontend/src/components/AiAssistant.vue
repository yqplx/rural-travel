<template>
  <div>
    <button class="ai-fab" title="智能助手小乡" @click="open = !open">{{ open ? '✕' : '🌿' }}</button>
    <div v-if="open" class="ai-panel">
      <div class="head">
        <span>🌿 智能助手 · 小乡</span>
        <span style="font-size:.75rem; opacity:.85">有问必答</span>
      </div>
      <div ref="msgBox" class="msgs">
        <div v-for="(m, i) in messages" :key="i" class="ai-msg" :class="m.role">
          <div class="bubble">
            {{ m.text }}
            <div v-if="m.spotId" style="margin-top:8px">
              <el-button size="small" type="primary" plain round @click="goSpot(m.spotId)">
                去看看「{{ m.spotName }}」
              </el-button>
            </div>
          </div>
        </div>
        <div v-if="thinking" class="ai-msg bot"><div class="bubble">小乡正在思考…</div></div>
      </div>
      <div v-if="suggestions.length" class="ai-chips">
        <span v-for="s in suggestions" :key="s" class="chip" @click="send(s)">{{ s }}</span>
      </div>
      <div class="ai-input">
        <el-input v-model="input" placeholder="问我门票/交通/美食/行程…" size="small"
                  @keyup.enter="send(input)" />
        <el-button type="primary" size="small" :disabled="thinking" @click="send(input)">发送</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, ref } from 'vue'
import { useRouter } from 'vue-router'
import { aiApi } from '@/api'

const router = useRouter()
const open = ref(false)
const input = ref('')
const thinking = ref(false)
const suggestions = ref(['景区门票怎么收费?', '最佳旅游季节是什么时候?', '当地有哪些特色美食?'])
const messages = ref([
  { role: 'bot', text: '你好呀,我是小乡 🌾 关于乡村旅行的门票、交通、住宿、美食、行程问题都可以问我。' }
])
const msgBox = ref(null)

async function send(text) {
  text = (text || '').trim()
  if (!text || thinking.value) return
  messages.value.push({ role: 'user', text })
  input.value = ''
  thinking.value = true
  scroll()
  try {
    const res = await aiApi.chat(text)
    messages.value.push({
      role: 'bot', text: res.answer,
      spotId: res.relatedSpotId, spotName: res.relatedSpotName
    })
    if (res.suggestions?.length) suggestions.value = res.suggestions
  } catch { /* 拦截器已提示 */ }
  thinking.value = false
  scroll()
}

function scroll() {
  nextTick(() => { if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight })
}
function goSpot(id) { open.value = false; router.push(`/scenic/${id}`) }
</script>
