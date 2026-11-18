<template>
  <div class="rt-page">
    <!-- 用户信息头 -->
    <div class="rt-card" style="display:flex; align-items:center; gap:18px; margin-bottom:20px">
      <el-avatar :size="64" :src="user?.avatar">{{ user?.nickname?.[0] || '游' }}</el-avatar>
      <div>
        <h2 class="rt-serif" style="font-size:1.3rem">{{ user?.nickname || user?.username }}</h2>
        <p class="rt-muted" style="font-size:.88rem">
          账号:{{ user?.username }} · 角色:{{ user?.role === 'ADMIN' ? '管理员' : '普通用户' }}
        </p>
      </div>
    </div>

    <el-tabs v-model="tab">
      <!-- ============ 基本资料 ============ -->
      <el-tab-pane label="基本资料" name="info">
        <div class="rt-card" style="max-width:620px">
          <el-form :model="form" label-width="90px">
            <el-form-item label="昵称" required>
              <el-input v-model="form.nickname" maxlength="20" placeholder="请输入昵称" />
            </el-form-item>
            <el-form-item label="姓名" required>
              <el-input v-model="form.realName" maxlength="30" placeholder="请输入真实姓名" />
            </el-form-item>
            <el-form-item label="性别">
              <el-select v-model="form.gender" placeholder="请选择" clearable style="width:100%">
                <el-option label="男" value="男" />
                <el-option label="女" value="女" />
                <el-option label="保密" value="保密" />
              </el-select>
            </el-form-item>
            <el-form-item label="年龄">
              <el-input-number v-model="form.age" :min="1" :max="120" style="width:100%" />
            </el-form-item>
            <el-form-item label="生日">
              <el-date-picker v-model="form.birthday" type="date" value-format="YYYY-MM-DD"
                              placeholder="选择生日" style="width:100%" />
            </el-form-item>
            <el-form-item label="职业">
              <el-input v-model="form.occupation" maxlength="30" placeholder="如:学生" />
            </el-form-item>
            <el-form-item label="所在地区">
              <el-input v-model="form.region" maxlength="60" placeholder="如:辽宁省沈阳市" />
            </el-form-item>
            <el-form-item label="头像 URL">
              <el-input v-model="form.avatar" placeholder="头像图片地址(选填)" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="saveProfile">保存资料</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <!-- ============ 修改密码 ============ -->
      <el-tab-pane label="修改密码" name="pwd">
        <div class="rt-card" style="max-width:520px">
          <el-form ref="pwdRef" :model="pwd" :rules="pwdRules" label-width="100px">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="pwd.oldPassword" type="password" show-password placeholder="请输入原密码" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwd.newPassword" type="password" show-password placeholder="8-20 位,含字母和数字" />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirm">
              <el-input v-model="pwd.confirm" type="password" show-password placeholder="再次输入新密码" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="pwdSaving" @click="changePwd">修改密码</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <!-- ============ 兴趣标签 ============ -->
      <el-tab-pane label="兴趣标签" name="tags">
        <div class="rt-card" style="max-width:680px">
          <p class="rt-muted" style="margin-bottom:16px">选择你感兴趣的主题,将为你推荐更合心意的景区</p>
          <div class="tag-cloud" v-loading="tagsLoading">
            <button v-for="t in tags" :key="t.id" type="button"
                    class="tag-chip" :class="{ on: selectedTags.includes(t.id) }"
                    @click="toggleTag(t.id)">{{ t.name }}</button>
          </div>
          <el-button type="primary" style="margin-top:18px" :loading="tagsSaving"
                     :disabled="!selectedTags.length" @click="saveTags">保存兴趣标签</el-button>
        </div>
      </el-tab-pane>

      <!-- ============ 我的评分 ============ -->
      <el-tab-pane label="我的评分" name="rate">
        <div class="rt-card">
          <el-table :data="myRatings" style="width:100%">
            <el-table-column label="景区">
              <template #default="{ row }">
                <router-link :to="`/scenic/${row.scenicSpotId}`" style="color:var(--rt-green)">{{ row.scenicName }}</router-link>
              </template>
            </el-table-column>
            <el-table-column label="评分" width="160">
              <template #default="{ row }"><el-rate :model-value="row.score" disabled size="small" /></template>
            </el-table-column>
            <el-table-column prop="comment" label="评价" show-overflow-tooltip />
            <el-table-column prop="createdAt" label="时间" width="170" />
          </el-table>
          <el-empty v-if="!myRatings.length" description="还没有发表过评分" />
          <el-pagination background layout="prev, pager, next" :total="rateTotal" :page-size="10"
                         style="justify-content:center; margin-top:14px"
                         @current-change="p => loadRate(p)" />
        </div>
      </el-tab-pane>

      <!-- ============ 我的收藏 ============ -->
      <el-tab-pane label="我的收藏" name="fav">
        <div class="rt-grid">
          <div v-for="f in favorites" :key="f.id" style="position:relative">
            <ScenicCard :spot="f.scenicSpot" />
            <el-button size="small" type="danger" plain round style="position:absolute; top:10px; right:10px"
                       @click.stop="removeFav(f.scenicSpotId)">取消收藏</el-button>
          </div>
        </div>
        <el-empty v-if="!favorites.length" description="还没有收藏,去景区页逛逛吧" />
        <el-pagination background layout="prev, pager, next" :total="favTotal" :page-size="9"
                       style="justify-content:center; margin-top:18px"
                       @current-change="p => loadFav(p)" />
      </el-tab-pane>

      <!-- ============ 浏览记录 ============ -->
      <el-tab-pane label="浏览记录" name="history">
        <div class="rt-card">
          <div style="display:flex; justify-content:flex-end; margin-bottom:10px">
            <el-popconfirm title="确定清空全部浏览历史?" @confirm="clearHistory">
              <template #reference><el-button size="small" type="danger" plain :disabled="!history.length">清空历史</el-button></template>
            </el-popconfirm>
          </div>
          <el-table :data="history" style="width:100%">
            <el-table-column label="景区">
              <template #default="{ row }">
                <router-link :to="`/scenic/${row.scenicSpotId}`" style="color:var(--rt-green)">
                  {{ row.scenicSpot?.name || '景区#' + row.scenicSpotId }}
                </router-link>
              </template>
            </el-table-column>
            <el-table-column label="停留" width="110">
              <template #default="{ row }">{{ row.duration }} 秒</template>
            </el-table-column>
            <el-table-column prop="createdAt" label="浏览时间" width="180" />
            <el-table-column width="90">
              <template #default="{ row }">
                <el-button size="small" text type="danger" @click="removeHistory(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!history.length" description="暂无浏览记录" />
          <el-pagination background layout="prev, pager, next" :total="hisTotal" :page-size="10"
                         style="justify-content:center; margin-top:14px"
                         @current-change="p => loadHistory(p)" />
        </div>
      </el-tab-pane>

      <!-- ============ AI 设置(用自己的密钥) ============ -->
      <el-tab-pane label="AI 设置" name="ai">
        <div class="rt-card" style="max-width:640px">
          <el-alert type="info" :closable="false" show-icon style="margin-bottom:18px"
                    title="大模型增强使用你自己的密钥,不会消耗他人额度。未配置时自动降级为本地引擎,功能依然可用。" />

          <div v-if="aiCfg.configured" class="ai-key-box">
            <div>
              <div class="rt-muted" style="font-size:13px">当前密钥</div>
              <div class="ai-mask">{{ aiCfg.mask }}</div>
              <div class="rt-muted" style="font-size:12px">更新于 {{ aiCfg.updatedAt || '—' }}</div>
            </div>
            <el-popconfirm title="确定清除你的密钥?清除后将使用本地引擎。" @confirm="clearAi">
              <template #reference><el-button type="danger" plain size="small">清除</el-button></template>
            </el-popconfirm>
          </div>
          <el-alert v-else type="warning" :closable="false" show-icon style="margin-bottom:16px"
                    title="尚未配置密钥,当前使用本地引擎。填写后即可启用大模型增强。" />

          <el-form label-width="96px" style="margin-top:14px">
            <el-form-item label="API Key">
              <el-input v-model="aiForm.apiKey" type="password" show-password clearable
                        :placeholder="aiCfg.configured ? '已保存,留空则不更换;填写即覆盖' : '粘贴你的密钥,如 sk-xxxx'" />
            </el-form-item>
            <el-form-item label="接口地址">
              <el-input v-model="aiForm.baseUrl" clearable placeholder="https://api.deepseek.com/v1" />
            </el-form-item>
            <el-form-item label="模型名">
              <el-input v-model="aiForm.model" clearable placeholder="deepseek-chat" />
            </el-form-item>
          </el-form>
          <div style="display:flex; gap:10px">
            <el-button :loading="aiTesting" :disabled="!canTestAi" @click="testAi">测试连接</el-button>
            <el-button type="primary" :loading="aiSaving" @click="saveAi">保存配置</el-button>
          </div>
          <p class="rt-muted" style="font-size:12px; margin-top:12px; line-height:1.7">
            密钥经 AES 加密后存入数据库,服务端不会回显明文,也不会被其他用户使用。<br>
            支持任何 OpenAI 兼容厂商:DeepSeek、通义千问、智谱、硅基流动等。
          </p>
        </div>
      </el-tab-pane>

      <!-- ============ 账号安全 ============ -->
      <el-tab-pane label="账号安全" name="security">
        <div class="rt-card" style="max-width:520px">
          <el-alert type="warning" :closable="false" show-icon
                    title="注销账号将停用你的账户,无法登录;相关数据会保留。此操作需验证密码。"
                    style="margin-bottom:18px" />
          <el-input v-model="deactivatePwd" type="password" show-password
                    placeholder="请输入密码以确认注销" style="margin-bottom:16px" />
          <el-popconfirm title="确定要注销账号吗?注销后将无法登录!" width="240"
                         confirm-button-text="确定注销" cancel-button-text="再想想"
                         confirm-button-type="danger" @confirm="doDeactivate">
            <template #reference>
              <el-button type="danger" :disabled="!deactivatePwd" :loading="deactivating">确认注销账号</el-button>
            </template>
          </el-popconfirm>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { favoriteApi, historyApi, ratingApi, tagApi, userApi } from '@/api'
import { auth } from '@/store/auth'
import ScenicCard from '@/components/ScenicCard.vue'

const router = useRouter()
const tab = ref('info')
const user = ref(null)
const saving = ref(false)
const form = reactive({
  nickname: '', realName: '', gender: '', age: null, birthday: null,
  occupation: '', region: '', avatar: '', email: '', phone: '', province: ''
})

/* ---------- 基本资料 ---------- */
onMounted(async () => {
  await loadProfile()
  loadFav(1); loadRate(1); loadHistory(1); loadTags(); loadAi()
})

async function loadProfile() {
  try {
    user.value = await userApi.profile()
    Object.assign(form, {
      nickname: user.value.nickname, realName: user.value.realName,
      gender: user.value.gender, age: user.value.age, birthday: user.value.birthday,
      occupation: user.value.occupation, region: user.value.region,
      avatar: user.value.avatar, email: user.value.email,
      phone: user.value.phone, province: user.value.province
    })
  } catch {}
}

async function saveProfile() {
  if (!form.nickname) { ElMessage.warning('请输入昵称'); return }
  saving.value = true
  try {
    const u = await userApi.update(form)
    auth.setUser(u)
    user.value = u
    ElMessage.success('资料已更新')
  } catch {} finally { saving.value = false }
}

/* ---------- AI 设置(每人用自己的密钥) ---------- */
const aiCfg = ref({ configured: false, mask: null, baseUrl: '', model: '', updatedAt: null })
const aiForm = reactive({ apiKey: '', baseUrl: '', model: '' })
const aiSaving = ref(false)
const aiTesting = ref(false)
const canTestAi = computed(() => aiForm.apiKey.trim().length > 0 || aiCfg.value.configured)

async function loadAi() {
  try {
    aiCfg.value = await userApi.aiConfig()
    aiForm.baseUrl = aiCfg.value.baseUrl || ''
    aiForm.model = aiCfg.value.model || ''
  } catch {}
}

async function saveAi() {
  aiSaving.value = true
  try {
    aiCfg.value = await userApi.saveAiConfig({
      apiKey: aiForm.apiKey.trim(),
      baseUrl: aiForm.baseUrl.trim(),
      model: aiForm.model.trim()
    })
    aiForm.apiKey = ''
    ElMessage.success(aiCfg.value.configured ? '密钥已保存并启用大模型增强' : '配置已保存')
  } catch {} finally { aiSaving.value = false }
}

async function clearAi() {
  try {
    await userApi.clearAiConfig()
    aiCfg.value = { configured: false, mask: null, baseUrl: '', model: '', updatedAt: null }
    aiForm.apiKey = ''
    ElMessage.success('密钥已清除,已切换回本地引擎')
  } catch {}
}

async function testAi() {
  aiTesting.value = true
  try {
    const ok = await userApi.testAiConfig({
      apiKey: aiForm.apiKey.trim(),
      baseUrl: aiForm.baseUrl.trim(),
      model: aiForm.model.trim()
    })
    ok ? ElMessage.success('连接成功,密钥可用') : ElMessage.error('连接失败,请检查密钥或接口地址')
  } catch {} finally { aiTesting.value = false }
}

/* ---------- 修改密码 ---------- */
const pwdRef = ref()
const pwdSaving = ref(false)
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).{8,20}$/, message: '8-20位,需同时包含字母和数字', trigger: 'blur' }
  ],
  confirm: [{
    validator: (r, v, cb) => v === pwd.newPassword ? cb() : cb(new Error('两次输入的密码不一致')),
    trigger: 'blur'
  }]
}
async function changePwd() {
  await pwdRef.value.validate().catch(() => Promise.reject())
  pwdSaving.value = true
  try {
    await userApi.changePassword({ oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
    ElMessage.success('密码修改成功,下次登录请使用新密码')
    Object.assign(pwd, { oldPassword: '', newPassword: '', confirm: '' })
    pwdRef.value.resetFields()
  } catch {} finally { pwdSaving.value = false }
}

/* ---------- 兴趣标签 ---------- */
const tags = ref([])
const selectedTags = ref([])
const tagsLoading = ref(true)
const tagsSaving = ref(false)
async function loadTags() {
  try {
    tags.value = await tagApi.list()
    selectedTags.value = (await userApi.interests()) || []
  } catch {}
  tagsLoading.value = false
}
function toggleTag(id) {
  const i = selectedTags.value.indexOf(id)
  i >= 0 ? selectedTags.value.splice(i, 1) : selectedTags.value.push(id)
}
async function saveTags() {
  tagsSaving.value = true
  try {
    await userApi.setInterests(selectedTags.value)
    localStorage.setItem('rural_pref_done', '1')
    ElMessage.success('兴趣标签已保存,首页推荐已为你更新')
  } catch {} finally { tagsSaving.value = false }
}

/* ---------- 收藏/评分/历史 ---------- */
const favorites = ref([]); const favTotal = ref(0)
const myRatings = ref([]); const rateTotal = ref(0)
const history = ref([]); const hisTotal = ref(0)

async function loadFav(p) {
  try { const res = await favoriteApi.page({ current: p, size: 9 }); favorites.value = res.records; favTotal.value = res.total } catch {}
}
async function removeFav(id) {
  try { await favoriteApi.remove(id); ElMessage.success('已取消收藏'); loadFav(1) } catch {}
}
async function loadRate(p) {
  try { const res = await ratingApi.my({ current: p, size: 10 }); myRatings.value = res.records; rateTotal.value = res.total } catch {}
}
async function loadHistory(p) {
  try { const res = await historyApi.page({ current: p, size: 10 }); history.value = res.records; hisTotal.value = res.total } catch {}
}
async function removeHistory(id) {
  try { await historyApi.remove(id); loadHistory(1) } catch {}
}
async function clearHistory() {
  try { await historyApi.clear(); ElMessage.success('已清空'); loadHistory(1) } catch {}
}

/* ---------- 账号注销 ---------- */
const deactivatePwd = ref('')
const deactivating = ref(false)
async function doDeactivate() {
  deactivating.value = true
  try {
    await userApi.deactivate(deactivatePwd.value)
    ElMessage.success('账号已注销,感谢使用')
    auth.logout()
    router.push('/')
  } catch {} finally { deactivating.value = false }
}
</script>

<style scoped>
.tag-cloud { display: flex; flex-wrap: wrap; gap: 10px; }
.tag-chip {
  padding: 8px 18px; border-radius: 20px; border: 1.5px solid var(--rt-rule);
  background: var(--rt-bg); cursor: pointer; font-size: .9rem; color: var(--rt-ink);
  transition: all .2s; font-family: inherit;
}
.tag-chip:hover { border-color: var(--rt-green); }
.tag-chip.on {
  background: var(--rt-green-light); border-color: var(--rt-green);
  color: var(--rt-green-dark); font-weight: 700;
}
.ai-key-box {
  display: flex; align-items: center; justify-content: space-between; gap: 16px;
  padding: 14px 16px; border: 1px solid var(--rt-rule);
  border-radius: 12px; background: var(--rt-green-light);
}
.ai-mask {
  font-family: Consolas, monospace; font-size: 15px; color: var(--rt-ink);
  margin: 2px 0; letter-spacing: .5px;
}
</style>
