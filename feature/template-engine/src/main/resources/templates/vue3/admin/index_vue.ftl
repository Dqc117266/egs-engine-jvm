<#--

  RuoYi-style admin CRUD (see admin/src/views/example/index.vue):

  app-container + query form + toolbar + pagination + zh copy + permission hooks.

-->

<#function zhLabel nm>
  <#if nm == 'id'><#return "序号"/>
  <#elseif nm == 'name'><#return "名称"/>
  <#elseif nm == 'description'><#return "描述"/>
  <#elseif nm == 'status'><#return "状态"/>
  <#elseif nm == 'instruction'><#return "步骤说明"/>
  <#elseif nm == 'stepOrder'><#return "步骤序号"/>
  <#elseif nm == 'foodItemId'><#return "关联食物"/>
  <#elseif nm == 'createdAt'><#return "创建时间"/>
  <#elseif nm == 'updatedAt'><#return "更新时间"/>
  <#elseif nm == 'createTime'><#return "创建时间"/>
  <#elseif nm == 'updateTime'><#return "更新时间"/>
  <#else><#return nm/>
  </#if>
</#function>

<#macro defaultValue fc>
  <#if fc.tsType == 'string'><#if fc.nullable>null<#else>''</#if><#elseif fc.tsType == 'number'>0<#elseif fc.tsType == 'boolean'>false<#else>null</#if>
</#macro>


<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <#if nameSearch>
      <el-form-item label="${zhLabel('name')}" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入${zhLabel('name')}" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      </#if>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['${moduleName}:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['${moduleName}:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="${entityCamel}List" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
<#list tableColumns as col>
  <#if col.kotlinName == 'status' && col.tsType == 'number'>
      <el-table-column label="${zhLabel(col.kotlinName)}" align="center" prop="${col.kotlinName}">
        <template #default="scope">
          <el-tag :type="scope.row.${col.kotlinName} === 1 ? 'success' : 'danger'">{{ scope.row.${col.kotlinName} === 1 ? '正常' : '停用' }}</el-tag>
        </template>
      </el-table-column>
  <#else>
      <el-table-column label="${zhLabel(col.kotlinName)}" align="center" prop="${col.kotlinName}"<#if col.kotlinName == 'instruction' || col.kotlinName == 'description'> :show-overflow-tooltip="true"<#elseif col.kotlinName == pkField || col.kotlinName=='id'> width="<#if col.kotlinType=='Long'>80<#else>100</#if>"</#if> />
  </#if>
</#list>
      <el-table-column label="操作" align="center" width="200">
        <template #default="scope">
          <el-button type="text" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['${moduleName}:edit']">修改</el-button>
          <el-button type="text" icon="Delete" style="color:red" @click="handleDelete(scope.row)" v-hasPermi="['${moduleName}:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
<#list formColumns as fc>
        <el-form-item label="${zhLabel(fc.kotlinName)}" prop="${fc.kotlinName}">
  <#if fc.tsType == 'string'>
          <#if fc.kotlinName=='description' || fc.kotlinName=='instruction' || (fc.kotlinName?lower_case)?contains('description')>
          <el-input v-model="form.${fc.kotlinName}" type="textarea" :rows="4" placeholder="请输入${zhLabel(fc.kotlinName)}" />
          <#else>
          <el-input v-model="form.${fc.kotlinName}" placeholder="请输入${zhLabel(fc.kotlinName)}" />
          </#if>
  <#elseif fc.tsType == 'number'>
          <#if fc.kotlinType == 'Int'>
          <el-input-number v-model="form.${fc.kotlinName}" :min="0" controls-position="right" style="width: 100%" />
          <#else>
          <el-input v-model.number="form.${fc.kotlinName}" placeholder="请输入${zhLabel(fc.kotlinName)}" />
          </#if>
  <#elseif fc.tsType == 'boolean'>
          <el-switch v-model="form.${fc.kotlinName}" />
  <#else>
          <el-input v-model="form.${fc.kotlinName}" placeholder="请输入${zhLabel(fc.kotlinName)}" />
  </#if>
        </el-form-item>
</#list>
      </el-form>
      <template #footer>
        <el-button @click="cancel">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="${entityPascal}">
import { reactive, ref, onMounted } from 'vue'
import type { FormInstance } from 'element-plus'
import { ElMessageBox, ElMessage } from 'element-plus'
import {
  list${entityPascal},
  get${entityPascal},
  create${entityPascal},
  update${entityPascal},
  delete${entityPascal},
} from '@/api/${moduleName}'
import type { ${entityPascal}Item, ${entityPascal}CreateBody } from '@/types/${moduleName}'

const ${entityCamel}List = ref<${entityPascal}Item[]>([])
const total = ref(0)
const loading = ref(false)
const showSearch = ref(true)
const open = ref(false)
const title = ref('')
const multiple = ref(true)
const ids = ref<number[]>([])
const formRef = ref<FormInstance>()

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
<#if nameSearch>
  name: undefined as string | undefined,
</#if>
})

const form = ref<Partial<${entityPascal}Item> & Record<string, unknown>>({})
const rules = {
<#list requiredFormColumns as fc>
  '${fc.kotlinName}': [{ required: true, message: '${zhLabel(fc.kotlinName)}不能为空', trigger: 'blur' }]<#sep>,</#sep>
</#list>
}

function unwrapPagePayload(payload: any) {
  const inner = payload?.data !== undefined ? payload.data : payload
  const rows = inner?.records ?? inner?.list ?? []
  const t = inner?.total ?? 0
  return { rows, total: t }
}

function getList() {
  loading.value = true
  list${entityPascal}(queryParams as any)
    .then((res: any) => {
      loading.value = false
      const data = unwrapPagePayload(res)
      ${entityCamel}List.value = data.rows as ${entityPascal}Item[]
      total.value = Number(data.total) || 0
    })
    .catch(() => {
      loading.value = false
    })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  <#if nameSearch>
  queryParams.name = undefined
  </#if>
  handleQuery()
}

function handleSelectionChange(selection: ${entityPascal}Item[]) {
  ids.value = selection.map(item => item.${pkField} as number)
  multiple.value = !selection.length
}

function resetFormModel() {
  form.value = {
    ${pkField}: undefined as never,
    <#list formColumns as fc>
    ${fc.kotlinName}: <@defaultValue fc /><#sep>,</#sep>
    </#list>
  } as Partial<${entityPascal}Item>
}

function handleAdd() {
  resetFormModel()
  title.value = '添加${entityTitleZh}'
  open.value = true
}

async function handleUpdate(row: ${entityPascal}Item) {
  resetFormModel()
  const res = await get${entityPascal}((row.${pkField} as number)!)
  const inner = (res as any)?.data !== undefined ? (res as any).data : res
  form.value = { ...inner }
  title.value = '修改${entityTitleZh}'
  open.value = true
}

async function handleDelete(row?: ${entityPascal}Item) {
  const ids_ = row ? [row.${pkField}] : ids.value
  if (!ids_ || !(ids_.length > 0)) {
    ElMessage.warning('请选择要删除的数据')
    return
  }
  try {
    await ElMessageBox.confirm('是否确认删除该条记录？', '提示', { type: 'warning' })
  } catch {
    return
  }
  for (const id of ids_) {
    await delete${entityPascal}(id as number)
  }
  ElMessage.success('删除成功')
  getList()
}

function cancel() {
  open.value = false
  resetFormModel()
}

async function submitForm() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  const f = form.value as Record<string, unknown>
  if (f.${pkField} !== undefined && f.${pkField} !== null && f.${pkField} !== '') {
    await update${entityPascal}(f.${pkField} as number, f)
    ElMessage.success('修改成功')
  } else {
    const body = { ...f } as Record<string, unknown>
    delete body.${pkField}
    await create${entityPascal}(body as ${entityPascal}CreateBody)
    ElMessage.success('新增成功')
  }
  open.value = false
  getList()
}

onMounted(() => getList())
</script>
