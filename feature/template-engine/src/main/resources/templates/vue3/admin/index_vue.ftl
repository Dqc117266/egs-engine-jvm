<template>
  <div class="egs-crud-${moduleName}">
    <el-card>
      <template #header>${entityPascal}</template>
      <el-table :data="store.rows" v-loading="store.loading">
<#list listColumns as col>
        <el-table-column prop="${col.kotlinName}" label="${col.kotlinName}" />
</#list>
        <el-table-column fixed="right" label="Ops" width="160">
          <template #default="{ row }">
            <el-button link type="primary" @click="onEdit(row)">Edit</el-button>
            <el-button link type="danger" @click="onDelete(row)">Del</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination layout="prev, pager, next" :total="store.total" @current-change="onPage" />
    </el-card>
    <el-dialog v-model="dialogOpen" title="${entityPascal}">
      <el-form>
<#list formColumns as col>
        <el-form-item label="${col.kotlinName}">
          <el-input v-model.number="editRow.${col.kotlinName}" />
        </el-form-item>
</#list>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">Cancel</el-button>
        <el-button type="primary" @click="save">Save</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { ${entityPascal}Item } from '@/types/${moduleName}'
import { use${entityPascal}Store } from '@/stores/${moduleName}'
import { create${entityPascal}, update${entityPascal}, delete${entityPascal} } from '@/api/${moduleName}'
import { ElMessage } from 'element-plus'

const store = use${entityPascal}Store()
store.loadPage(0, 20)

const dialogOpen = ref(false)
const editRow = ref<Record<string, any>>({})
function onPage(p: number) {
  store.loadPage(Math.max(0, (p ?? 1) - 1), 20)
}

function onEdit(row: ${entityPascal}Item) {
  editRow.value = { ...(row as any) }
  dialogOpen.value = true
}
async function onDelete(row: ${entityPascal}Item) {
  await delete${entityPascal}((row as any).${pkField})
  ElMessage.success('Deleted')
  store.loadPage(0, 20)
}
async function save() {
  try {
    if (editRow.value.${pkField}) {
      await update${entityPascal}(editRow.value.${pkField}, editRow.value)
    } else {
      await create${entityPascal}(editRow.value as any)
    }
    dialogOpen.value = false
    store.loadPage(0, 20)
  } catch (_e) {
    ElMessage.error('Save failed')
  }
}
</script>
