<template>
  <div>
    <div class="rt-card" style="margin-bottom:16px">
      <el-form inline @submit.prevent>
        <el-form-item label="名称"><el-input v-model="query.keyword" clearable style="width:160px" @change="load(1)" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="query.categoryId" clearable placeholder="全部" style="width:130px" @change="load(1)">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width:110px" @change="load(1)">
            <el-option label="上架" :value="1" /><el-option label="下架" :value="0" />
          </el-select>
        </el-form-item>
        <el-button type="primary" @click="load(1)">查询</el-button>
        <el-button type="success" @click="openEdit()">+ 新增景区</el-button>
      </el-form>
    </div>
    <div class="rt-card">
      <el-table v-loading="loading" :data="list" style="width:100%">
        <el-table-column label="景区" min-width="180">
          <template #default="{ row }">
            <div style="display:flex; align-items:center; gap:10px">
              <img :src="row.coverImage" style="width:56px; height:36px; object-fit:cover; border-radius:6px" />
              <b>{{ row.name }}</b>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="100" />
        <el-table-column label="标签" min-width="140">
          <template #default="{ row }"><span v-for="t in row.tagNames" :key="t" class="rt-tag">{{ t }}</span></template>
        </el-table-column>
        <el-table-column label="价格" width="80">
          <template #default="{ row }">{{ row.ticketPrice > 0 ? '¥' + row.ticketPrice : '免费' }}</template>
        </el-table-column>
        <el-table-column prop="avgScore" label="评分" width="70" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '上架' : '下架' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="确定删除该景区?" @confirm="remove(row.id)">
              <template #reference><el-button size="small" text type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="query.size"
                     style="justify-content:center; margin-top:14px"
                     @current-change="load" />
    </div>

    <el-dialog v-model="dlg" :title="form.id ? '编辑景区' : '新增景区'" width="640px" top="6vh">
      <el-form :model="form" label-width="90px">
        <el-form-item label="名称" required><el-input v-model="form.name" maxlength="80" /></el-form-item>
        <el-form-item label="分类" required>
          <el-select v-model="form.categoryId" style="width:100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="form.tagIds" multiple style="width:100%">
            <el-option v-for="t in tags" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="省市">
          <el-input v-model="form.province" placeholder="省" style="width:48%" />
          <el-input v-model="form.city" placeholder="市" style="width:48%; margin-left:4%" />
        </el-form-item>
        <el-form-item label="详细地址"><el-input v-model="form.address" /></el-form-item>
        <el-form-item label="介绍"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="特色亮点"><el-input v-model="form.features" placeholder="逗号分隔" /></el-form-item>
        <el-form-item label="游览贴士"><el-input v-model="form.tips" /></el-form-item>
        <el-form-item label="门票价格" required><el-input-number v-model="form.ticketPrice" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="开放时间"><el-input v-model="form.openTime" /></el-form-item>
        <el-form-item label="封面图"><el-input v-model="form.coverImage" placeholder="图片URL" /></el-form-item>
        <el-form-item label="图集">
          <div style="width:100%">
            <div v-for="(img, i) in form.images" :key="i" style="display:flex; gap:8px; margin-bottom:6px">
              <el-input v-model="form.images[i]" placeholder="图片URL" />
              <el-button text type="danger" @click="form.images.splice(i, 1)">删</el-button>
            </div>
            <el-button size="small" @click="form.images.push('')">+ 添加图片</el-button>
          </div>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="上架" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
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
const categories = ref([])
const tags = ref([])
const query = reactive({ keyword: '', categoryId: null, status: null, current: 1, size: 10 })
const dlg = ref(false)
const saving = ref(false)
const blank = { id: null, name: '', categoryId: null, tagIds: [], province: '', city: '', address: '', description: '', features: '', tips: '', ticketPrice: 0, openTime: '', coverImage: '', images: [], status: 1 }
const form = reactive({ ...blank })

onMounted(async () => {
  try { categories.value = await adminApi.allCategories() } catch {}
  try { tags.value = await adminApi.allTags() } catch {}
  load(1)
})

async function load(p) {
  query.current = p
  loading.value = true
  try { const res = await adminApi.scenicPage(query); list.value = res.records; total.value = res.total } catch {}
  loading.value = false
}

async function openEdit(row) {
  Object.assign(form, { ...blank, images: [] })
  if (row) {
    const d = await adminApi.scenicDetail(row.id)
    Object.assign(form, {
      id: d.id, name: d.name, categoryId: d.categoryId,
      tagIds: (d.tagIds || '').split(',').filter(Boolean).map(Number),
      province: d.province, city: d.city, address: d.address, description: d.description,
      features: d.features, tips: d.tips, ticketPrice: d.ticketPrice, openTime: d.openTime,
      coverImage: d.coverImage, images: (d.images || []).map(i => i.imageUrl), status: d.status
    })
  }
  dlg.value = true
}

async function save() {
  if (!form.name || !form.categoryId) { ElMessage.warning('请填写名称并选择分类'); return }
  saving.value = true
  try {
    if (form.id) await adminApi.scenicUpdate(form.id, form)
    else await adminApi.scenicCreate(form)
    ElMessage.success('已保存')
    dlg.value = false
    load(query.current)
  } catch {} finally { saving.value = false }
}

async function remove(id) {
  try { await adminApi.scenicDelete(id); ElMessage.success('已删除'); load(1) } catch {}
}
</script>
