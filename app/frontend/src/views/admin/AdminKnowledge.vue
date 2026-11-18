<template>
  <div>
    <div class="rt-card" style="margin-bottom:16px">
      <el-form inline @submit.prevent>
        <el-form-item label="关键词"><el-input v-model="query.keyword" clearable style="width:180px" @change="load(1)" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="query.category" clearable placeholder="全部" style="width:130px" @change="load(1)">
            <el-option v-for="c in ['门票','交通','住宿','美食','行程','文化','通用']" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-button type="primary" @click="load(1)">查询</el-button>
        <el-button type="success" @click="openEdit()">+ 新增知识</el-button>
      </el-form>
    </div>
    <div class="rt-card">
      <el-table v-loading="loading" :data="list">
        <el-table-column prop="id" label="ID" width="55" />
        <el-table-column prop="question" label="问题" min-width="170" show-overflow-tooltip />
        <el-table-column prop="answer" label="答案" min-width="260" show-overflow-tooltip />
        <el-table-column prop="keywords" label="检索词" min-width="140" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="80" />
        <el-table-column label="状态" width="70">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column width="130" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="删除该知识条目?" @confirm="remove(row.id)">
              <template #reference><el-button size="small" text type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="query.size"
                     style="justify-content:center; margin-top:14px" @current-change="load" />
    </div>

    <el-dialog v-model="dlg" :title="form.id ? '编辑知识' : '新增知识'" width="520px">
      <el-form label-width="80px">
        <el-form-item label="问题" required><el-input v-model="form.question" /></el-form-item>
        <el-form-item label="答案" required><el-input v-model="form.answer" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="检索词"><el-input v-model="form.keywords" placeholder="逗号分隔,如:门票,票价,收费" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" style="width:100%">
            <el-option v-for="c in ['门票','交通','住宿','美食','行程','文化','通用']" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ keyword: '', category: null, current: 1, size: 10 })
const dlg = ref(false)
const form = reactive({ id: null, question: '', answer: '', keywords: '', category: '通用', sortOrder: 0, status: 1 })

onMounted(() => load(1))
async function load(p) {
  query.current = p
  loading.value = true
  try { const res = await adminApi.knowledge(query); list.value = res.records; total.value = res.total } catch {}
  loading.value = false
}
function openEdit(row) {
  Object.assign(form, row || { id: null, question: '', answer: '', keywords: '', category: '通用', sortOrder: 0, status: 1 })
  dlg.value = true
}
async function save() {
  if (!form.question || !form.answer) { ElMessage.warning('问题和答案不能为空'); return }
  try { await adminApi.saveKnowledge(form); ElMessage.success('已保存'); dlg.value = false; load(query.current) } catch {}
}
async function remove(id) {
  try { await adminApi.deleteKnowledge(id); ElMessage.success('已删除'); load(1) } catch {}
}
</script>
