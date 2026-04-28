import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { ${entityPascal}Item } from '@/types/${moduleName}'
import { list${entityPascal} } from '@/api/${moduleName}'

export const use${entityPascal}Store = defineStore('${moduleName}', () => {
  const rows = ref<${entityPascal}Item[]>([])
  const total = ref(0)
  const loading = ref(false)

  async function loadPage(page = 0, size = 20) {
    loading.value = true
    try {
      const res: any = await list${entityPascal}(page, size)
      const payload = res.data ?? res
      rows.value =
        payload?.records ?? payload?.data?.records ?? payload?.content ?? []
      total.value =
        payload?.total ?? payload?.data?.total ?? rows.value.length ?? 0
    } finally {
      loading.value = false
    }
  }

  return { rows, total, loading, loadPage }
})
