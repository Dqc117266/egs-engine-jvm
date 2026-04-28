import type { RouteRecordRaw } from 'vue-router'

const ${moduleName}Routes: RouteRecordRaw[] = [
  {
    path: '/${moduleName}',
    name: '${entityPascal}Admin',
    component: () => import('@/views/${moduleName}/index.vue'),
  },
]

export default ${moduleName}Routes
