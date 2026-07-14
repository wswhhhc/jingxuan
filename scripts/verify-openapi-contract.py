import argparse
import json
from copy import deepcopy as deep_copy
from pathlib import Path
from urllib.parse import urlsplit

import yaml


HTTP_METHODS = {
    "delete",
    "get",
    "head",
    "options",
    "patch",
    "post",
    "put",
    "trace",
}
DOCUMENTATION_KEYS = {
    "description",
    "example",
    "examples",
    "externalDocs",
    "operationId",
    "servers",
    "summary",
    "tags",
    "title",
}
OPERATION_METADATA_KEYS = {
    "description",
    "externalDocs",
    "operationId",
    "summary",
    "tags",
}
PATH_METADATA_KEYS = {"description", "servers", "summary"}
NAMED_MAP_KEYS = {
    "$defs",
    "callbacks",
    "content",
    "dependentSchemas",
    "encoding",
    "headers",
    "links",
    "mapping",
    "parameters",
    "pathItems",
    "paths",
    "patternProperties",
    "properties",
    "requestBodies",
    "responses",
    "schemas",
    "securitySchemes",
    "webhooks",
}
ROOT = Path(__file__).resolve().parent.parent
EXPORTED = ROOT / "backend" / "target" / "openapi" / "openapi.json"
PUBLISHED = ROOT / "openapi" / "jingxuan-v1.yaml"


def stable_value(value):
    if isinstance(value, list):
        return [stable_value(item) for item in value]
    if isinstance(value, dict):
        return {
            key: stable_value(value[key])
            for key in sorted(value, key=lambda item: str(item))
        }
    return value


def strip_documentation(value, preserve_keys=False):
    if isinstance(value, list):
        return [strip_documentation(item) for item in value]
    if isinstance(value, dict):
        result = {}
        for key, item in sorted(value.items(), key=lambda entry: str(entry[0])):
            if not preserve_keys and key in DOCUMENTATION_KEYS:
                continue
            if key == "security" and isinstance(item, list):
                result[key] = [
                    strip_documentation(requirement, preserve_keys=True)
                    for requirement in item
                ]
            else:
                result[key] = strip_documentation(
                    item,
                    preserve_keys=isinstance(item, dict) and key in NAMED_MAP_KEYS,
                )
        return result
    return value


def operations(spec):
    return {
        (route, method.lower())
        for route, path_item in spec.get("paths", {}).items()
        for method in path_item
        if method.lower() in HTTP_METHODS
    }


def is_v1_route(route):
    return isinstance(route, str) and (
        route == "/api/v1" or route.startswith("/api/v1/")
    )


def path_operations(path_item):
    if not isinstance(path_item, dict):
        return {}
    return {
        method.lower(): operation
        for method, operation in path_item.items()
        if isinstance(method, str)
        and method.lower() in HTTP_METHODS
        and isinstance(operation, dict)
    }


def normalized_server_path(server_url):
    if not isinstance(server_url, str):
        return None

    path = urlsplit(server_url.strip()).path
    if not path:
        return "/"
    return f"/{path.lstrip('/')}".rstrip("/") or "/"


def validate_server_path_prefixes(spec):
    servers = spec.get("servers")
    paths = spec.get("paths")
    if not isinstance(servers, list) or not isinstance(paths, dict):
        return

    routes = sorted(route for route in paths if isinstance(route, str))
    for server in servers:
        if not isinstance(server, dict):
            continue
        server_url = server.get("url")
        server_path = normalized_server_path(server_url)
        if not server_path or server_path == "/":
            continue

        repeated_routes = [
            route
            for route in routes
            if route == server_path or route.startswith(f"{server_path}/")
        ]
        if repeated_routes:
            preview = ", ".join(repeated_routes[:3])
            suffix = "" if len(repeated_routes) <= 3 else " 等"
            raise SystemExit(
                "已发布 OpenAPI 的 servers.url 与 paths 存在重复前缀："
                f"servers.url={server_url!r}，路径 {preview}{suffix}。"
                "OpenAPI 客户端会拼接二者，请移除 servers 或将其设为根地址 /。"
            )


def component_identity(reference):
    if not isinstance(reference, str) or not reference.startswith("#/components/"):
        return None
    segments = [
        segment.replace("~1", "/").replace("~0", "~")
        for segment in reference[2:].split("/")
    ]
    if len(segments) < 3 or segments[0] != "components":
        return None
    return segments[1], segments[2], reference


def visit_component_references(value, visitor):
    if isinstance(value, list):
        for item in value:
            visit_component_references(item, visitor)
        return
    if not isinstance(value, dict):
        return

    identity = component_identity(value.get("$ref"))
    if identity:
        visitor(identity)
    for item in value.values():
        visit_component_references(item, visitor)


def visit_security_requirements(value, visitor):
    if isinstance(value, list):
        for item in value:
            visit_security_requirements(item, visitor)
        return
    if not isinstance(value, dict):
        return

    for key, item in value.items():
        if key == "security" and isinstance(item, list):
            for requirement in item:
                if isinstance(requirement, dict):
                    for name in requirement:
                        visitor(name)
        visit_security_requirements(item, visitor)


def select_reachable_components(exported, contract_root):
    selected = {}
    queue = []
    queued = set()

    def queue_component(identity):
        bucket, name, _ = identity
        key = (bucket, name)
        if key not in queued:
            queued.add(key)
            queue.append(identity)

    visit_component_references(contract_root, queue_component)
    visit_security_requirements(
        contract_root,
        lambda name: queue_component(
            ("securitySchemes", name, f"#/components/securitySchemes/{name}")
        ),
    )

    while queue:
        bucket, name, reference = queue.pop(0)
        component = exported.get("components", {}).get(bucket, {}).get(name)
        if component is None:
            raise SystemExit(f"后端实时 OpenAPI 引用的组件不存在：{reference}")
        copied = deep_copy(component)
        selected.setdefault(bucket, {})[name] = copied
        visit_component_references(copied, queue_component)

    return stable_value(selected)


def merge_operation(live_operation, published_operation):
    merged = {
        key: deep_copy(value)
        for key, value in live_operation.items()
        if key not in OPERATION_METADATA_KEYS and key != "servers"
    }
    for key in OPERATION_METADATA_KEYS:
        if key in published_operation:
            merged[key] = deep_copy(published_operation[key])
        elif key in live_operation:
            merged[key] = deep_copy(live_operation[key])
    for key, value in published_operation.items():
        if key.startswith("x-"):
            merged[key] = deep_copy(value)
    return stable_value(merged)


def build_published_contract(exported, published):
    exported_paths = exported.get("paths")
    published_paths = published.get("paths")
    if not isinstance(exported_paths, dict):
        raise SystemExit("后端导出的 OpenAPI 缺少 paths")
    if not isinstance(published_paths, dict) or not published_paths:
        raise SystemExit("已发布 OpenAPI 缺少 paths")
    validate_server_path_prefixes(published)

    missing = []
    for route, published_path_item in published_paths.items():
        if not isinstance(published_path_item, dict):
            continue
        live_path_item = exported_paths.get(route)
        live_operations = path_operations(live_path_item)
        for method in published_path_item:
            if (
                isinstance(method, str)
                and method.lower() in HTTP_METHODS
                and method.lower() not in live_operations
            ):
                missing.append((route, method.lower()))

    if missing:
        details = "\n".join(
            f"- {method.upper()} {route}" for route, method in sorted(missing)
        )
        raise SystemExit(f"已发布 OpenAPI 包含后端不存在的操作：\n{details}")

    merged_paths = {}
    for route in sorted(route for route in exported_paths if is_v1_route(route)):
        live_path_item = exported_paths[route]
        if not isinstance(live_path_item, dict):
            continue
        published_path_item = published_paths.get(route)
        if not isinstance(published_path_item, dict):
            published_path_item = {}

        merged_path_item = {}
        if "$ref" in live_path_item:
            merged_path_item["$ref"] = deep_copy(live_path_item["$ref"])
        if "parameters" in live_path_item:
            merged_path_item["parameters"] = deep_copy(live_path_item["parameters"])
        for key in PATH_METADATA_KEYS:
            if key in published_path_item:
                merged_path_item[key] = deep_copy(published_path_item[key])
            elif key in live_path_item:
                merged_path_item[key] = deep_copy(live_path_item[key])
        for key, value in live_path_item.items():
            if isinstance(key, str) and key.startswith("x-"):
                merged_path_item[key] = deep_copy(value)
        for key, value in published_path_item.items():
            if isinstance(key, str) and key.startswith("x-"):
                merged_path_item[key] = deep_copy(value)

        published_operations = path_operations(published_path_item)
        for method, live_operation in sorted(path_operations(live_path_item).items()):
            merged_path_item[method] = merge_operation(
                live_operation, published_operations.get(method, {})
            )
        merged_paths[route] = stable_value(merged_path_item)

    result = {
        "openapi": exported.get("openapi", published.get("openapi", "3.1.0")),
        "info": deep_copy(
            published.get(
                "info", {"title": "Jingxuan API", "version": "1.0.0"}
            )
        ),
    }
    if "servers" in published:
        result["servers"] = deep_copy(published["servers"])
    if "security" in exported:
        result["security"] = deep_copy(exported["security"])
    if "tags" in published:
        result["tags"] = deep_copy(published["tags"])
    for key, value in published.items():
        if key.startswith("x-"):
            result[key] = deep_copy(value)
    result["paths"] = stable_value(merged_paths)
    components = select_reachable_components(exported, result)
    if components:
        result["components"] = components
    return result


def semantic_projection(spec):
    projection = {
        "openapi": spec.get("openapi"),
        "paths": spec.get("paths", {}),
    }
    if "security" in spec:
        projection["security"] = spec["security"]
    if "components" in spec:
        projection["components"] = spec["components"]
    return strip_documentation(projection)


def semantic_mismatch_details(expected, published):
    details = []
    expected_paths = expected.get("paths", {})
    published_paths = published.get("paths", {})
    for route, expected_path_item in expected_paths.items():
        published_path_item = published_paths.get(route, {})
        for method in sorted(HTTP_METHODS):
            if method not in expected_path_item:
                continue
            if strip_documentation(expected_path_item[method]) != strip_documentation(
                published_path_item.get(method, {})
            ):
                details.append(f"{method.upper()} {route}")
    if strip_documentation(expected.get("security")) != strip_documentation(
        published.get("security")
    ):
        details.append("全局 security")
    if strip_documentation(expected.get("components", {})) != strip_documentation(
        published.get("components", {})
    ):
        details.append("递归依赖 components")
    return details


def assert_published_contract_matches(exported, published):
    expected = build_published_contract(exported, published)
    if semantic_projection(expected) != semantic_projection(published):
        mismatches = semantic_mismatch_details(expected, published)
        preview = "\n".join(f"- {item}" for item in mismatches[:20])
        suffix = "" if len(mismatches) <= 20 else f"\n- 另有 {len(mismatches) - 20} 项"
        raise SystemExit(
            "已发布 OpenAPI 与后端实时规格语义不一致，"
            "请运行 npm run api:generate 并提交更新：\n"
            f"{preview}{suffix}"
        )
    return expected


def write_published_contract(exported, published):
    synchronized = build_published_contract(exported, published)
    serialized = yaml.safe_dump(
        synchronized,
        allow_unicode=True,
        default_flow_style=False,
        sort_keys=False,
        width=4096,
    )
    PUBLISHED.write_text(serialized, encoding="utf-8", newline="\n")
    return synchronized


def main():
    parser = argparse.ArgumentParser(
        description="同步或校验用于 Orval 的已发布 V1 OpenAPI 语义契约"
    )
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--check", action="store_true", help="校验已发布规格")
    mode.add_argument("--write", action="store_true", help="用实时语义更新已发布规格")
    args = parser.parse_args()

    exported = json.loads(EXPORTED.read_text(encoding="utf-8"))
    published = yaml.safe_load(PUBLISHED.read_text(encoding="utf-8"))
    if args.write:
        synchronized = write_published_contract(exported, published)
        print(
            "已同步用于 Orval 的 V1 OpenAPI："
            f"{len(operations(synchronized))} 个操作，"
            f"{len(synchronized.get('components', {}).get('schemas', {}))} 个递归依赖 Schema。"
        )
        return

    synchronized = assert_published_contract_matches(exported, published)
    exported_v1_count = len(
        {
            operation
            for operation in operations(exported)
            if operation[0] == "/api/v1" or operation[0].startswith("/api/v1/")
        }
    )
    print(
        "OpenAPI 语义契约校验通过："
        f"已发布 {len(operations(synchronized))} 个操作与后端实时规格一致；"
        f"后端当前共 {exported_v1_count} 个 V1 操作。"
    )


if __name__ == "__main__":
    main()
