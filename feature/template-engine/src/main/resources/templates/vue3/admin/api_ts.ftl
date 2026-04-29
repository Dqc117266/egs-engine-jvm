import request from '@/utils/request'
import type { ${entityPascal}CreateBody, ${entityPascal}UpdateBody } from '@/types/${moduleName}'

/**
 * Pagination query aligns with Vue example pages (`pageNum` / `pageSize`);
 * translates to backend Spring `page` (0-based) and `size` query params.
 */
export function list${entityPascal}(query?: Record<string, unknown>) {
  const q = query ?? {}
  const pageNum = q.pageNum != null ? Number(q.pageNum) : 1
  const pageSize = q.pageSize != null ? Number(q.pageSize) : 10
  const params: Record<string, unknown> = {
    page: Math.max(0, pageNum - 1),
    size: pageSize,
  }
  if (q.name != null && q.name !== '') {
    params.name = q.name
  }
  return request({
    url: '/api/${restPath}',
    method: 'get',
    params,
  })
}

export function get${entityPascal}All() {
  return request({ url: '/api/${restPath}/all', method: 'get' })
}

export function count${entityPascal}() {
  return request({ url: '/api/${restPath}/count', method: 'get' })
}

export function get${entityPascal}(id: number) {
  return request({ url: '/api/${restPath}/' + id, method: 'get' })
}

export function create${entityPascal}(data: ${entityPascal}CreateBody) {
  return request({ url: '/api/${restPath}', method: 'post', data })
}

export function update${entityPascal}(id: number, data: ${entityPascal}UpdateBody) {
  return request({ url: '/api/${restPath}/' + id, method: 'put', data })
}

export function delete${entityPascal}(id: number) {
  return request({ url: '/api/${restPath}/' + id, method: 'delete' })
}
