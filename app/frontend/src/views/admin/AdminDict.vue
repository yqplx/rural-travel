<template>
  <div class="dict-grid">
    <div class="rt-card">
      <div style="display:flex; justify-content:space-between; margin-bottom:12px">
        <b>分类管理</b>
        <el-button size="small" type="success" @click="openCat()">+ 新增</el-button>
      </div>
      <el-table :data="cats" size="small">
        <el-table-column prop="icon" label="图标" width="60" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="sortOrder" label="排序" width="60" />
        <el-table-column label="状态" width="70">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column width="110">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="openCat(row)">编辑</el-button>
            <el-popconfirm title="删除该分类?" @confirm="delCat(row.id)">
              <template #reference><el-button size="small" text type="danger">删</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="rt-card">
      <div style="display:flex; justify-content:space-between; margin-bottom:12px">
        <b>标签管理</b>
        <el-button size="small" type="success" @click="openTag()">+ 新增</el-button>
      </div>
      <el-table :data="tagList" size="small">
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="sortOrder" label="排序" width="60" />
        <el-table-column label="状态" width="70">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column width="110">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="openTag(row)">编辑</el-button>
            <el-popconfirm title="删除该标签?" @confirm="delTag(row.id)">
              <template #reference><el-button size="small" text type="danger">删</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="catDlg" :title="catForm.id ? '编辑分类' : '新增分类'" width="380px">
      <el-form label-width="70px">
        <el-form-item label="名称"><el-input v-model="catForm.name" /></el-form-item>
        <el-form-item label="图标"><el-input v-model="catForm.icon" placeholder="emoji,如 🌾" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="catForm.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="catForm.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="saveCat">保存</el-button></template>
    </el-dialog>
    <el-dialog v-model="tagDlg" :title="tagForm.id ? '编辑标签' : '新增标签'" width="380px">
      <el-form label-width="70px">
        <el-form-item label="名称"><el-input v-model="tagForm.name" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="tagForm.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="tagForm.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="saveTag">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api'

const cats = ref([])
const tagList = ref([])
const catDlg = ref(false)
const tagDlg = ref(false)
const catForm = reactive({ id: null, name: '', icon: '', sortOrder: 0, status: 1 })
const tagForm = reactive({ id: null, name: '', sortOrder: 0, status: 1 })

onMounted(() => { loadCats(); loadTags() })
async function loadCats() { cats.value = await adminApi.categories({ size: 100 }).then(r => r.records) }
async function loadTags() { tagList.value = await adminApi.tags({ size: 100 }).then(r => r.records) }
function openCat(row) { Object.assign(catForm, row || { id: null, name: '', icon: '', sortOrder: 0, status: 1 }); catDlg.value = true }
function openTag(row) { Object.assign(tagForm, row || { id: null, name: '', sortOrder: 0, status: 1 }); tagDlg.value = true }
async function saveCat() {
  await adminApi.saveCategory(catForm); ElMessage.success('已保存'); catDlg.value = false; loadCats()
}
async function saveTag() {
  await adminApi.saveTag(tagForm); ElMessage.success('已保存'); tagDlg.value = false; loadTags()
}
async function delCat(id) { await adminApi.deleteCategory(id); ElMessage.success('已删除'); loadCats() }
async function delTag(id) { await adminApi.deleteTag(id); ElMessage.success('已删除'); loadTags() }
</script>

<style scoped>
.dict-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }
@media (max-width: 900px) { .dict-grid { grid-template-columns: 1fr; } }
</style>
