<template>
  <div>
    <div class="rt-card" style="margin-bottom:16px">
      <el-form inline @submit.prevent>
        <el-form-item label="关键词"><el-input v-model="query.keyword" placeholder="用户名/昵称" clearable style="width:170px" @change="load(1)" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="query.role" clearable placeholder="全部" style="width:120px" @change="load(1)">
            <el-option label="管理员" value="ADMIN" /><el-option label="普通用户" value="USER" />
          </el-select>
        </el-form-item>
        <el-button type="primary" @click="load(1)">查询</el-button>
      </el-form>
    </div>
    <div class="rt-card">
      <el-table v-loading="loading" :data="list">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="province" label="省份" width="90" />
        <el-table-column label="角色" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.role === 'ADMIN' ? 'warning' : 'success'">{{ row.role === 'ADMIN' ? '管理员' : '用户' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="170" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="确定删除该用户?" @confirm="remove(row.id)">
              <template #reference><el-button size="small" text type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="query.size"
                     style="justify-content:center; margin-top:14px" @current-change="load" />
    </div>

    <el-dialog v-model="dlg" title="编辑用户" width="440px">
      <el-form label-width="90px">
        <el-form-item label="昵称"><el-input v-model="form.nickname" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="手机"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="省份"><el-input v-model="form.province" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" style="width:100%">
            <el-option label="普通用户" value="USER" /><el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="正常" inactive-text="禁用" />
        </el-form-item>
        <el-form-item label="重置密码"><el-input v-model="form.newPassword" placeholder="留空则不修改(至少6位)" show-password /></el-form-item>
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
const query = reactive({ keyword: '', role: null, current: 1, size: 10 })
const dlg = ref(false)
const form = reactive({ id: null, nickname: '', email: '', phone: '', province: '', role: 'USER', status: 1, newPassword: '' })

onMounted(() => load(1))
async function load(p) {
  query.current = p
  loading.value = true
  try { const res = await adminApi.users(query); list.value = res.records; total.value = res.total } catch {}
  loading.value = false
}
function openEdit(row) {
  Object.assign(form, { id: row.id, nickname: row.nickname, email: row.email, phone: row.phone, province: row.province, role: row.role, status: row.status, newPassword: '' })
  dlg.value = true
}
async function save() {
  try { await adminApi.updateUser(form.id, form); ElMessage.success('已保存'); dlg.value = false; load(query.current) } catch {}
}
async function remove(id) {
  try { await adminApi.deleteUser(id); ElMessage.success('已删除'); load(1) } catch {}
}
</script>
