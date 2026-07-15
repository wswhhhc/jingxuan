package com.jingxuan.api;

import com.jingxuan.campaign.web.V1CampaignAdminController;
import com.jingxuan.campaign.web.V1CampaignController;
import com.jingxuan.communication.web.V1NoticeController;
import com.jingxuan.communication.web.V1NotificationController;
import com.jingxuan.evaluation.web.V1LeaderboardController;
import com.jingxuan.evaluation.web.V1MyScoreController;
import com.jingxuan.evaluation.web.V1PrizeController;
import com.jingxuan.evaluation.web.V1ScoreAdminController;
import com.jingxuan.evaluation.web.V1ScoreController;
import com.jingxuan.identityaccess.web.V1AuthController;
import com.jingxuan.identityaccess.web.V1MenuAdminController;
import com.jingxuan.identityaccess.web.V1RegistrationController;
import com.jingxuan.identityaccess.web.V1RoleAdminController;
import com.jingxuan.identityaccess.web.V1UserAdminController;
import com.jingxuan.identityaccess.web.V1UserApprovalController;
import com.jingxuan.moderation.web.V1ContentModerationController;
import com.jingxuan.moderation.web.V1SensitiveRuleController;
import com.jingxuan.operationsreporting.web.V1DashboardController;
import com.jingxuan.operationsreporting.web.V1LogController;
import com.jingxuan.portfolio.web.V1AuditController;
import com.jingxuan.portfolio.web.V1CommentAdminController;
import com.jingxuan.portfolio.web.V1CommentController;
import com.jingxuan.portfolio.web.V1LikeController;
import com.jingxuan.portfolio.web.V1PortfolioController;
import com.jingxuan.portfolio.web.V1PublicationController;
import com.jingxuan.portfolio.web.V1ShowcaseController;
import com.jingxuan.referencedata.web.V1ReferenceDataController;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 校验 v1 REST controller 的成功状态契约：
 * <ul>
 *   <li>每个 Controller 都有一个且唯一的 {@code @RequestMapping} 路径</li>
 *   <li>每个方法有一个 {@code @RequestMapping} 且包含恰好一种 HTTP method</li>
 *   <li>非 200 的方法必须用 {@code @ResponseStatus} 声明状态码（而非 ResponseEntity）</li>
 *   <li>204 方法必须返回 void</li>
 * </ul>
 */
class V1SuccessStatusContractTest {

    private static final List<Class<?>> V1_CONTROLLERS = List.of(
            V1AuthController.class,
            V1RegistrationController.class,
            V1CampaignController.class,
            V1CampaignAdminController.class,
            V1ReferenceDataController.class,
            V1ShowcaseController.class,
            V1PublicationController.class,
            V1PortfolioController.class,
            V1LikeController.class,
            V1CommentController.class,
            V1CommentAdminController.class,
            V1AuditController.class,
            V1ScoreController.class,
            V1ScoreAdminController.class,
            V1MyScoreController.class,
            V1LeaderboardController.class,
            V1PrizeController.class,
            V1NotificationController.class,
            V1NoticeController.class,
            V1MenuAdminController.class,
            V1RoleAdminController.class,
            V1UserAdminController.class,
            V1UserApprovalController.class,
            V1SensitiveRuleController.class,
            V1ContentModerationController.class,
            V1LogController.class,
            V1DashboardController.class
    );

    /** 跨 Controller 共享同一 HTTP method+path 是可接受的。 */
    @Test
    void nonOkNon204OperationsMustUseResponseStatus() {
        var violations = new ArrayList<String>();
        for (var controller : V1_CONTROLLERS) {
            var classMapping = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
            assertNotNull(classMapping, controller.getName() + " missing @RequestMapping");
            String basePath = singlePath(classMapping);

            for (var method : controller.getDeclaredMethods()) {
                var methodMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (methodMapping == null) continue;
                assertEquals(1, methodMapping.method().length,
                        () -> method + " must declare exactly one HTTP method");

                String fullPath = basePath + singlePath(methodMapping);
                var responseStatus = AnnotatedElementUtils.findMergedAnnotation(method, ResponseStatus.class);
                String op = methodMapping.method()[0] + " " + fullPath;

                if (responseStatus == null) {
                    // 无 @ResponseStatus → 默认为 HttpStatus.OK，允许使用任何返回类型
                    continue;
                }

                var code = responseStatus.code();
                if (code.value() == 200) continue; // @ResponseStatus(200) 是冗余但无害

                // 非 200 的 @ResponseStatus 不得被 ResponseEntity 覆盖
                if (ResponseEntity.class.isAssignableFrom(method.getReturnType())) {
                    violations.add(op + " has @ResponseStatus(" + code.value() + ") but returns ResponseEntity");
                }

                // 204 必须返回 void
                if (code.value() == 204 && method.getReturnType() != void.class) {
                    violations.add(op + " is 204 but return type is " + method.getReturnType().getSimpleName());
                }
            }
        }
        assertTrue(violations.isEmpty(), "v1 contract violations:\n  " + String.join("\n  ", violations));
    }

    /** 所有 Controller 都有且只有一个 @RequestMapping 路径片段。 */
    @Test
    void allControllersHaveSingleRequestMappingPath() {
        for (var controller : V1_CONTROLLERS) {
            var mapping = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
            assertNotNull(mapping, controller.getName() + " missing @RequestMapping");
            assertDoesNotThrow(() -> singlePath(mapping),
                    controller.getName() + " must have a single @RequestMapping path");
        }
    }

    /** 列出所有发现的操作，用于人工审查。 */
    @Test
    void reportAllDiscoveredOperations() {
        Map<String, String> ops = new LinkedHashMap<>();
        for (var controller : V1_CONTROLLERS) {
            var classMapping = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
            String basePath = singlePath(classMapping);

            for (var method : controller.getDeclaredMethods()) {
                var methodMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (methodMapping == null) continue;
                if (methodMapping.method().length == 0) continue;

                String fullPath = basePath + singlePath(methodMapping);
                String key = methodMapping.method()[0] + " " + fullPath;

                var responseStatus = AnnotatedElementUtils.findMergedAnnotation(method, ResponseStatus.class);
                String status = responseStatus == null ? "200" : String.valueOf(responseStatus.code().value());
                if (!ops.containsKey(key)) {
                    ops.put(key, status);
                }
            }
        }
        System.out.println("=== v1 discovered operations (" + ops.size() + ") ===");
        ops.forEach((op, status) -> System.out.println(status + " " + op));
        assertTrue(ops.size() > 50, "expected 50+ v1 operations, found " + ops.size());
    }

    private String singlePath(RequestMapping mapping) {
        String[] paths = mapping.path().length == 0 ? mapping.value() : mapping.path();
        if (paths.length == 0) return "";
        assertEquals(1, paths.length, mapping + " must declare exactly one path (got " + paths.length + ")");
        return paths[0];
    }
}
