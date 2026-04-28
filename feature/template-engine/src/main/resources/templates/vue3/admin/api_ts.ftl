import request from '@/utils/request'
import type { ${entityPascal}Item, ${entityPascal}CreateBody, ${entityPascal}UpdateBody } from '@/types/${moduleName}'

export function list${entityPascal}(page?: number, size?: number) {
  return request({
    url: '/api/${restPath}',
    method: 'get',
    params: { page, size },
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
