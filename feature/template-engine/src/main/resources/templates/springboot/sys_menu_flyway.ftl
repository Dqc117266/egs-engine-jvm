<#if ddlSql?has_content>
${ddlSql}

</#if>
-- EGS auto-generated (--with-admin / gen-from-backend): persists permissions + sidebar row for admin router API.
-- Apply: run Spring Boot (Flyway). Regenerating codegen overwrites this file per backend module slug.
-- Required so GET /api/admin/menus/router returns these routes (sidebar).
-- Insert is root-level menu only: parent_id NULL; order_num=${menuOrderNum} sorts among peers.
INSERT INTO permissions (name, description)
SELECT v.name, v.description
FROM (VALUES
    ('${moduleName}:list', 'List ${entityPascal} records'),
    ('${moduleName}:query', 'View ${entityPascal} detail'),
    ('${moduleName}:add', 'Create ${entityPascal}'),
    ('${moduleName}:edit', 'Update ${entityPascal}'),
    ('${moduleName}:remove', 'Delete ${entityPascal}')
) AS v(name, description)
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.name = v.name);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN (
    '${moduleName}:list', '${moduleName}:query', '${moduleName}:add', '${moduleName}:edit', '${moduleName}:remove'
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO sys_menus (
    parent_id, menu_name, order_num, path, component, route_name,
    menu_type, visible, status, perms, icon
)
SELECT
    NULL,
    '${entityPascal}',
    ${menuOrderNum},
    '${moduleName}',
    '${moduleName}/index',
    '${entityPascal}Admin',
    'C', TRUE, '0', '${moduleName}:list', 'Document'
WHERE NOT EXISTS (SELECT 1 FROM sys_menus m WHERE m.route_name = '${entityPascal}Admin');
