<template>
  <el-tabs v-model="tab">
    <el-tab-pane label="虚拟用户" name="users">
      <div class="rt-card">
        <div style="display:flex; justify-content:space-between; margin-bottom:12px">
          <span class="rt-muted" style="font-size:.85rem">虚拟用户用于丰富推荐样本,帮助新用户获得更贴合的推荐</span>
          <el-button size="small" type="success" @click="openUser()">+ 新增虚拟用户</el-button>
        </div>
        <el-table :data="users">
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="name" label="名称" width="130" />
          <el-table-column label="兴趣标签" min-width="180">
            <template #default="{ row }">
              <span v-for="n in tagNames(row.interestTags)" :key="n" class="rt-tag">{{ n }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="画像描述" show-overflow-tooltip />
          <el-table-column width="130" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="openUser(row)">编辑</el-button>
              <el-popconfirm title="删除将同时清除其评分样本,确定?" @confirm="delUser(row.id)">
                <template #reference><el-button size="small" text type="danger">删除</el-button></template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination background layout="total, prev, pager, next" :total="userTotal" :page-size="10"
                       style="justify-content:center; margin-top:14px" @current-change="loadUsers" />
      </div>
    </el-tab-pane>
    <el-tab-pane label="虚拟评分样本" name="ratings">
      <div class="rt-card">
        <div style="display:flex; justify-content:space-between; margin-bottom:12px">
          <el-select v-model="filterUserId" clearable placeholder="按虚拟用户筛选" style="width:200px" @change="loadRatings(1)">
            <el-option v-for="u in allUsers" :key="u.id" :label="u.name" :value="u.id" />
          </el-select>
          <el-button size="small" type="success" @click="openRating()">+ 新增评分样本</el-button>
        </div>
        <el-table :data="ratings">
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="virtualUserName" label="虚拟用户" width="130" />
          <el-table-column label="景区" min-width="160">
            <template #default="{ row }">{{ spotName(row.scenicSpotId) }}</template>
          </el-table-column>
          <el-table-column label="评分" width="150">
            <template #default="{ row }"><el-rate :model-value="row.score" disabled size="small" /></template>
          </el-table-column>
          <el-table-column width="130" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="openRating(row)">编辑</el-button>
              <el-popconfirm title="删除该样本?" @confirm="delRating(row.id)">
                <template #reference><el-button size="small" text type="danger">删除</el-button></template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination background layout="total, prev, pager, next" :total="ratingTotal" :page-size="10"
                       style="justify-content:center; margin-top:14px" @current-change="loadRatings" />
      </div>
    </el-tab-pane>
  </el-tabs>

  <el-dialog v-model="userDlg" :title="userForm.id ? '编辑虚拟用户' : '新增虚拟用户'" width="440px">
    <el-form label-width="90px">
      <el-form-item label="名称"><el-input v-model="userForm.name" /></el-form-item>
      <el-form-item label="兴趣标签">
        <el-select v-model="userForm.tagIdList" multiple style="width:100%">
          <el-option v-for="t in tags" :key="t.id" :label="t.name" :value="t.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="画像描述"><el-input v-model="userForm.description" type="textarea" :rows="2" /></el-form-item>
    </el-form>
    <template #footer><el-button type="primary" @click="saveUser">保存</el-button></template>
  </el-dialog>

  <el-dialog v-model="ratingDlg" :title="ratingForm.id ? '编辑评分样本' : '新增评分样本'" width="440px">
    <el-form label-width="90px">
      <el-form-item label="虚拟用户">
        <el-select v-model="ratingForm.virtualUserId" style="width:100%">
          <el-option v-for="u in allUsers" :key="u.id" :label="u.name" :value="u.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="景区">
        <el-select v-model="ratingForm.scenicSpotId" filterable style="width:100%">
          <el-option v-for="s in spots" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="评分"><el-rate v-model="ratingForm.score" /></el-form-item>
    </el-form>
    <template #footer><el-button type="primary" @click="saveRating">保存</el-button></template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi, scenicApi } from '@/api'

const tab = ref('users')
const users = ref([])
const userTotal = ref(0)
const ratings = ref([])
const ratingTotal = ref(0)
const allUsers = ref([])
const tags = ref([])
const spots = ref([])
const filterUserId = ref(null)
const userDlg = ref(false)
const ratingDlg = ref(false)
const userForm = reactive({ id: null, name: '', tagIdList: [], description: '' })
const ratingForm = reactive({ id: null, virtualUserId: null, scenicSpotId: null, score: 5 })

onMounted(async () => {
  loadUsers(1); loadRatings(1)
  try { allUsers.value = await adminApi.allVirtualUsers() } catch {}
  try { tags.value = await adminApi.allTags() } catch {}
  try { spots.value = (await scenicApi.page({ size: 100 })).records } catch {}
})

async function loadUsers(p) {
  const res = await adminApi.virtualUsers({ current: p, size: 10 })
  users.value = res.records; userTotal.value = res.total
}
async function loadRatings(p) {
  const res = await adminApi.virtualRatings({ current: p, size: 10, virtualUserId: filterUserId.value })
  ratings.value = res.records; ratingTotal.value = res.total
}
function tagNames(wrapped) {
  const ids = (wrapped || '').split(',').filter(Boolean).map(Number)
  return tags.value.filter(t => ids.includes(t.id)).map(t => t.name)
}
function spotName(id) { return spots.value.find(s => s.id === id)?.name || '#' + id }
function openUser(row) {
  Object.assign(userForm, row
    ? { id: row.id, name: row.name, tagIdList: (row.interestTags || '').split(',').filter(Boolean).map(Number), description: row.description }
    : { id: null, name: '', tagIdList: [], description: '' })
  userDlg.value = true
}
async function saveUser() {
  if (!userForm.name) { ElMessage.warning('请输入名称'); return }
  const interestTags = userForm.tagIdList.length ? ',' + userForm.tagIdList.join(',') + ',' : null
  await adminApi.saveVirtualUser({ id: userForm.id, name: userForm.name, interestTags, description: userForm.description })
  ElMessage.success('已保存'); userDlg.value = false
  loadUsers(1); allUsers.value = await adminApi.allVirtualUsers()
}
async function delUser(id) {
  await adminApi.deleteVirtualUser(id); ElMessage.success('已删除')
  loadUsers(1); loadRatings(1); allUsers.value = await adminApi.allVirtualUsers()
}
function openRating(row) {
  Object.assign(ratingForm, row || { id: null, virtualUserId: null, scenicSpotId: null, score: 5 })
  ratingDlg.value = true
}
async function saveRating() {
  if (!ratingForm.virtualUserId || !ratingForm.scenicSpotId) { ElMessage.warning('请选择虚拟用户和景区'); return }
  await adminApi.saveVirtualRating(ratingForm)
  ElMessage.success('已保存,景区均分已刷新'); ratingDlg.value = false; loadRatings(1)
}
async function delRating(id) {
  await adminApi.deleteVirtualRating(id); ElMessage.success('已删除'); loadRatings(1)
}
</script>
