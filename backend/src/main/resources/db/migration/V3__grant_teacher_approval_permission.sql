-- 教师自助注册审批必须由独立权限码控制，而非管理员菜单可见性。
INSERT INTO sys_menu (id, menu_name, parent_id, path, permission, type, icon, sort)
VALUES (20, '审批教师注册', 2, NULL, 'user:approve', 2, NULL, 4)
ON DUPLICATE KEY UPDATE
    menu_name = VALUES(menu_name),
    permission = VALUES(permission),
    deleted = 0;

INSERT INTO sys_role_menu (id, role_id, menu_id)
VALUES (120, 3, 20)
ON DUPLICATE KEY UPDATE
    deleted = 0;
