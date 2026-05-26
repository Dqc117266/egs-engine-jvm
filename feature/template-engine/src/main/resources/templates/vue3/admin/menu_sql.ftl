-- EGS scaffold: reference snippet only (manual copy if needed).
-- For Spring Boot workspaces, **`web crud gen-from-backend` / `backend gen database --with-admin`**
-- also emits `backend/app/src/main/resources/db/migration/V*__sys_menu_${moduleName}.sql`
-- with idempotent INSERTs — start the server so Flyway applies it; then sidebar shows this module.
INSERT INTO sys_menus (
    parent_id, menu_name, order_num, path, component, route_name,
    menu_type, visible, status, perms, icon
) VALUES (
    NULL,
    '${entityPascal}',
    ${menuOrderNum},
    '${moduleName}',
    '${moduleName}/index',
    '${entityPascal}Admin',
    'C', TRUE, '0', '${moduleName}:list', 'Document');
