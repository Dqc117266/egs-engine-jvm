-- EGS scaffold: paste into Flyway/db migration for sys_menus compatible with Vue3 router [${moduleName}]
INSERT INTO sys_menus (
    parent_id, menu_name, order_num, path, component, route_name,
    menu_type, visible, status, perms, icon
) VALUES (
    NULL,
    '${entityPascal}',
    100,
    '${moduleName}',
    '${moduleName}/index',
    '${entityPascal}Admin',
    'C', TRUE, '0', '${moduleName}:list', 'Document');
