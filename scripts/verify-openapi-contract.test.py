import importlib.util
import sys
import unittest
from pathlib import Path


sys.dont_write_bytecode = True
SCRIPT = Path(__file__).with_name("verify-openapi-contract.py")
SPEC = importlib.util.spec_from_file_location("verify_openapi_contract", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class VerifyOpenApiContractTest(unittest.TestCase):
    def setUp(self):
        self.exported = {
            "openapi": "3.1.0",
            "info": {"title": "实时文档", "version": "dev"},
            "servers": [{"url": "http://localhost:18080"}],
            "security": [{"BearerAuth": []}],
            "paths": {
                "/api/v1/widgets/{id}": {
                    "get": {
                        "operationId": "unstableLiveName",
                        "summary": "实时说明",
                        "parameters": [
                            {
                                "in": "path",
                                "name": "id",
                                "required": True,
                                "schema": {"type": "string"},
                            }
                        ],
                        "responses": {
                            "200": {
                                "description": "成功",
                                "content": {
                                    "application/json": {
                                        "schema": {
                                            "$ref": "#/components/schemas/Widget"
                                        }
                                    }
                                },
                            }
                        },
                        "security": [],
                    },
                    "post": {
                        "operationId": "unstableCreateName",
                        "requestBody": {
                            "required": True,
                            "content": {
                                "application/json": {
                                    "schema": {
                                        "$ref": "#/components/schemas/CreateWidget"
                                    }
                                }
                            },
                        },
                        "responses": {"204": {"description": "已创建"}},
                    },
                    "patch": {
                        "operationId": "unstablePatchName",
                        "summary": "实时更新说明",
                        "tags": ["Live Widgets"],
                        "responses": {
                            "202": {
                                "description": "已更新",
                                "content": {
                                    "application/json": {
                                        "schema": {
                                            "$ref": "#/components/schemas/Widget"
                                        }
                                    }
                                },
                            }
                        },
                    },
                },
                "/api/v1/internal": {
                    "get": {
                        "operationId": "getInternalStatus",
                        "summary": "实时内部状态",
                        "responses": {
                            "200": {
                                "description": "成功",
                                "content": {
                                    "application/json": {
                                        "schema": {
                                            "$ref": "#/components/schemas/InternalStatus"
                                        }
                                    }
                                },
                            }
                        },
                    }
                },
                "/api/legacy/status": {
                    "get": {
                        "operationId": "getLegacyStatus",
                        "responses": {
                            "200": {
                                "description": "成功",
                                "content": {
                                    "application/json": {
                                        "schema": {
                                            "$ref": "#/components/schemas/LegacyStatus"
                                        }
                                    }
                                },
                            }
                        },
                    }
                },
            },
            "components": {
                "schemas": {
                    "CreateWidget": {
                        "description": "创建参数",
                        "properties": {
                            "owner": {"$ref": "#/components/schemas/Owner"}
                        },
                        "type": "object",
                    },
                    "InternalStatus": {
                        "properties": {
                            "detail": {
                                "$ref": "#/components/schemas/InternalDetail"
                            }
                        },
                        "type": "object",
                    },
                    "InternalDetail": {"type": "string"},
                    "LegacyStatus": {"type": "integer"},
                    "Owner": {"type": "string"},
                    "Unused": {"type": "boolean"},
                    "Widget": {
                        "properties": {
                            "description": {"type": "string"},
                            "owner": {"$ref": "#/components/schemas/Owner"}
                        },
                        "type": "object",
                    },
                },
                "securitySchemes": {
                    "BearerAuth": {
                        "bearerFormat": "JWT",
                        "scheme": "bearer",
                        "type": "http",
                    },
                    "UnusedAuth": {"scheme": "basic", "type": "http"},
                },
            },
        }
        self.published = {
            "openapi": "3.1.0",
            "info": {"title": "稳定客户端", "version": "1.0.0"},
            "servers": [{"url": "/"}],
            "paths": {
                "/api/v1/widgets/{id}": {
                    "get": {
                        "operationId": "get_widget",
                        "summary": "保留的发布说明",
                        "tags": ["Widgets"],
                        "parameters": [],
                        "responses": {"201": {"description": "错误旧契约"}},
                        "security": [{"BearerAuth": []}],
                    },
                    "post": {
                        "operationId": "post_widget",
                        "tags": ["Widgets"],
                        "responses": {"201": {"description": "错误旧契约"}},
                    },
                }
            },
            "components": {
                "schemas": {"Widget": {"type": "integer"}},
                "securitySchemes": {
                    "BearerAuth": {"scheme": "bearer", "type": "http"}
                },
            },
        }

    def test_build_published_contract_uses_live_semantics_and_stable_metadata(self):
        result = MODULE.build_published_contract(self.exported, self.published)
        get_operation = result["paths"]["/api/v1/widgets/{id}"]["get"]
        post_operation = result["paths"]["/api/v1/widgets/{id}"]["post"]

        self.assertEqual("get_widget", get_operation["operationId"])
        self.assertEqual(["Widgets"], get_operation["tags"])
        self.assertEqual("保留的发布说明", get_operation["summary"])
        self.assertEqual(["200"], list(get_operation["responses"]))
        self.assertEqual("id", get_operation["parameters"][0]["name"])
        self.assertEqual([], get_operation["security"])
        self.assertTrue(post_operation["requestBody"]["required"])
        self.assertEqual(["204"], list(post_operation["responses"]))
        self.assertEqual([{"BearerAuth": []}], result["security"])
        self.assertEqual(
            [
                "CreateWidget",
                "InternalDetail",
                "InternalStatus",
                "Owner",
                "Widget",
            ],
            list(result["components"]["schemas"]),
        )
        self.assertNotIn("Unused", result["components"]["schemas"])
        self.assertEqual(
            ["BearerAuth"], list(result["components"]["securitySchemes"])
        )

    def test_build_published_contract_adds_all_live_v1_operations_only(self):
        result = MODULE.build_published_contract(self.exported, self.published)

        expected_operations = {
            operation
            for operation in MODULE.operations(self.exported)
            if MODULE.is_v1_route(operation[0])
        }
        self.assertEqual(expected_operations, MODULE.operations(result))
        self.assertIn("/api/v1/internal", result["paths"])
        self.assertIn("patch", result["paths"]["/api/v1/widgets/{id}"])
        self.assertNotIn("/api/legacy/status", result["paths"])

        patch_operation = result["paths"]["/api/v1/widgets/{id}"]["patch"]
        self.assertEqual("unstablePatchName", patch_operation["operationId"])
        self.assertEqual("实时更新说明", patch_operation["summary"])
        self.assertEqual(["Live Widgets"], patch_operation["tags"])
        self.assertEqual(["202"], list(patch_operation["responses"]))

        internal_operation = result["paths"]["/api/v1/internal"]["get"]
        self.assertEqual("getInternalStatus", internal_operation["operationId"])
        self.assertEqual(
            "#/components/schemas/InternalStatus",
            internal_operation["responses"]["200"]["content"][
                "application/json"
            ]["schema"]["$ref"],
        )
        self.assertIn("InternalDetail", result["components"]["schemas"])
        self.assertIn("InternalStatus", result["components"]["schemas"])
        self.assertNotIn("LegacyStatus", result["components"]["schemas"])

    def test_assert_published_contract_matches_detects_semantic_drift(self):
        with self.assertRaisesRegex(SystemExit, "语义不一致"):
            MODULE.assert_published_contract_matches(self.exported, self.published)

    def test_assert_published_contract_matches_accepts_synchronized_contract(self):
        synchronized = MODULE.build_published_contract(self.exported, self.published)
        MODULE.assert_published_contract_matches(self.exported, synchronized)

    def test_build_published_contract_rejects_operation_missing_from_backend(self):
        self.published["paths"]["/api/v1/missing"] = {
            "get": {"operationId": "missing", "responses": {"200": {}}}
        }

        with self.assertRaisesRegex(
            SystemExit, r"后端不存在的操作：[\s\S]*GET /api/v1/missing"
        ):
            MODULE.build_published_contract(self.exported, self.published)

    def test_build_published_contract_rejects_repeated_server_path_prefix(self):
        self.published["servers"] = [{"url": "/api/v1"}]

        with self.assertRaisesRegex(SystemExit, "重复前缀"):
            MODULE.build_published_contract(self.exported, self.published)

    def test_build_published_contract_accepts_root_server(self):
        result = MODULE.build_published_contract(self.exported, self.published)

        self.assertEqual([{"url": "/"}], result["servers"])

    def test_build_published_contract_accepts_missing_servers(self):
        del self.published["servers"]

        result = MODULE.build_published_contract(self.exported, self.published)

        self.assertNotIn("servers", result)

    def test_semantic_projection_ignores_documentation_noise(self):
        synchronized = MODULE.build_published_contract(self.exported, self.published)
        changed_docs = MODULE.deep_copy(synchronized)
        changed_docs["paths"]["/api/v1/widgets/{id}"]["get"]["summary"] = "新说明"
        changed_docs["components"]["schemas"]["Widget"]["description"] = "新说明"

        self.assertEqual(
            MODULE.semantic_projection(synchronized),
            MODULE.semantic_projection(changed_docs),
        )

    def test_semantic_projection_preserves_schema_property_named_description(self):
        synchronized = MODULE.build_published_contract(self.exported, self.published)
        changed_schema = MODULE.deep_copy(synchronized)
        changed_schema["components"]["schemas"]["Widget"]["properties"][
            "description"
        ]["type"] = "integer"

        self.assertNotEqual(
            MODULE.semantic_projection(synchronized),
            MODULE.semantic_projection(changed_schema),
        )


if __name__ == "__main__":
    unittest.main()
