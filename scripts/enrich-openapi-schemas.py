import yaml
"""在 openapi/export 后给 YAML 补上关键 response schema。"""

FIXES = {
    "/api/v1/auth/login": {"200": {"schema": "V1LoginResponse", "desc": "登录成功"}},
    "/api/v1/auth/refresh": {"200": {"schema": "V1LoginResponse", "desc": "令牌已轮换"}},
    "/api/v1/auth/me": {"200": {"schema": "V1UserInfo", "desc": "当前用户信息"}},
    "/api/v1/me/works": {"200": {"schema": "V1WorkSummary", "desc": "我的作品列表"}},
    "/api/v1/showcase/works": {"200": {"schema": "V1WorkSummary", "desc": "公开展示作品列表"}},
    "/api/v1/showcase/works/{id}": {"200": {"schema": "V1WorkDetail", "desc": "作品详情"}},
}

with open("openapi/jingxuan-v1.yaml", encoding="utf-8") as f:
    d = yaml.safe_load(f)

sc = d.get("components", {}).get("schemas", {})
changed = False

for path, mf in FIXES.items():
    if path not in d.get("paths", {}):
        continue
    for method in ("get", "post", "put", "delete"):
        if method not in d["paths"][path]:
            continue
        responses = d["paths"][path][method].setdefault("responses", {})
        for code, info in mf.items():
            sn = info["schema"]
            if sn not in sc:
                continue
            existing = responses.get(code, {})
            if not existing.get("content"):
                responses[code] = {
                    "description": info["desc"],
                    "content": {
                        "application/json": {
                            "schema": {"$ref": "#/components/schemas/" + sn}
                        }
                    }
                }
                changed = True

if changed:
    with open("openapi/jingxuan-v1.yaml", "w", encoding="utf-8") as f:
        yaml.dump(d, f, default_flow_style=False, sort_keys=False, allow_unicode=True, width=4096)
    print("schema fixes applied")
else:
    print("no changes needed")
