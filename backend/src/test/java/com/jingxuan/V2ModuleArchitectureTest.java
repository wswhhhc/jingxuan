package com.jingxuan;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

class V2ModuleArchitectureTest {

    private static final String ROOT_PACKAGE = "com.jingxuan.";

    private static final List<String> V2_MODULES = List.of(
            "identityaccess",
            "referencedata",
            "campaign",
            "portfolio",
            "evaluation",
            "communication",
            "moderation",
            "operationsreporting",
            "workflow"
    );

    private static final String[] V2_BASE_PACKAGES = V2_MODULES.stream()
            .map(ROOT_PACKAGE::concat)
            .toArray(String[]::new);

    private static final JavaClasses V2_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(V2_BASE_PACKAGES);

    /**
     * 当前 v2 Controller 仍需委托给旧实现。清单按具体 Controller 收口，迁移时只应删除条目；
     * 新 Controller 不会自动获得访问旧包的权限。
     */
    private static final Map<String, Set<String>> DOCUMENTED_CONTROLLER_BRIDGES;

    private static final Map<String, Set<String>> buildControllerBridges() {
        var map = new LinkedHashMap<String, Set<String>>();
        map.put("com.jingxuan.identityaccess.web.V1AuthController", Set.of("com.jingxuan.auth."));
        map.put("com.jingxuan.portfolio.web.V1AuditController", Set.of("com.jingxuan.modules.audit.", "com.jingxuan.modules.work."));
        map.put("com.jingxuan.portfolio.web.V1CommentController", Set.of("com.jingxuan.modules.comment.", "com.jingxuan.modules.work."));
        map.put("com.jingxuan.portfolio.web.V1CommentAdminController", Set.of("com.jingxuan.modules.comment."));
        map.put("com.jingxuan.portfolio.web.V1LikeController", Set.of("com.jingxuan.modules.adapter."));
        map.put("com.jingxuan.portfolio.web.V1PortfolioController", Set.of("com.jingxuan.modules.deleterequest.", "com.jingxuan.modules.work.", "com.jingxuan.workflow."));
        map.put("com.jingxuan.portfolio.web.V1PublicationController", Set.of("com.jingxuan.modules.publish."));
        map.put("com.jingxuan.portfolio.web.V1ShowcaseController", Set.of("com.jingxuan.modules.work.", "com.jingxuan.modules.adapter."));
        map.put("com.jingxuan.evaluation.web.V1ScoreController", Set.of("com.jingxuan.modules.score."));
        map.put("com.jingxuan.identityaccess.web.V1RegistrationController", Set.of("com.jingxuan.auth."));
        map.put("com.jingxuan.campaign.web.V1CampaignController", Set.of("com.jingxuan.modules.scorebatch."));
        map.put("com.jingxuan.referencedata.web.V1ReferenceDataController", Set.of("com.jingxuan.modules.dict."));
        return Collections.unmodifiableMap(map);
    }

    static {
        DOCUMENTED_CONTROLLER_BRIDGES = buildControllerBridges();
    }

    /** 已存在的根级 Mapper 过渡注入。任何新增注入都必须先迁移到模块内部。 */
    private static final Map<String, Set<String>> DOCUMENTED_MAPPER_BRIDGES = Map.of(
            "com.jingxuan.referencedata.internal.application.ReferenceDataCommandService", Set.of(
                    "com.jingxuan.mapper.TagMapper",
                    "com.jingxuan.mapper.WorkTagMapper"
            ),
            "com.jingxuan.referencedata.internal.application.ReferenceDataQueryService", Set.of(
                    "com.jingxuan.mapper.TagMapper"
            ),
            "com.jingxuan.campaign.internal.application.CampaignCommandService", Set.of(
                    "com.jingxuan.mapper.StudentTaskMapper"
            ),
            "com.jingxuan.campaign.internal.application.CampaignQueryService", Set.of(
                    "com.jingxuan.mapper.ScoreBatchMapper",
                    "com.jingxuan.mapper.SysDictMapper",
                    "com.jingxuan.mapper.SysUserMapper"
            ),
            "com.jingxuan.identityaccess.internal.application.UserApprovalService", Set.of(
                    "com.jingxuan.mapper.SysUserMapper"
            ),
            "com.jingxuan.identityaccess.internal.application.UserDeletionService", Set.of(
                    "com.jingxuan.mapper.DeleteRequestMapper",
                    "com.jingxuan.mapper.StudentTaskMapper",
                    "com.jingxuan.mapper.SysNotificationMapper",
                    "com.jingxuan.mapper.SysUserMapper",
                    "com.jingxuan.mapper.WorkMapper",
                    "com.jingxuan.mapper.WorkMemberMapper"
            ),
            "com.jingxuan.identityaccess.internal.application.UserAdminQueryService", Set.of(
                    "com.jingxuan.mapper.SysDictMapper",
                    "com.jingxuan.mapper.SysRoleMapper"
            )
    );

    @Test
    void v2ControllersMayOnlyUseTheirOwnApiAndApplicationLayers() {
        classes()
                .that().areAnnotatedWith(RestController.class)
                .should(new ControllerDependencyCondition())
                .because("Controller 只能调用本模块应用用例、api 契约和无业务逻辑的 web 边界适配器")
                .check(V2_CLASSES);
    }

    @Test
    void v2ModulesMustNotDependOnAnotherModulesInternalPackages() {
        classes()
                .should(new NoCrossModuleInternalDependencyCondition())
                .because("模块只能引用其他模块公开的 api，internal 是模块私有实现")
                .check(V2_CLASSES);
    }

    @Test
    void v2ModulesMustNotInjectMappersOwnedByAnotherModule() {
        classes()
                .should(new NoCrossModuleMapperInjectionCondition())
                .because("跨模块数据访问必须经过公开用例或 api，不能直接注入 Mapper")
                .check(V2_CLASSES);
    }

    private static final class ControllerDependencyCondition extends ArchCondition<JavaClass> {

        private ControllerDependencyCondition() {
            super("only depend on the same module's api/application layers or web boundary adapters");
        }

        @Override
        public void check(JavaClass controller, ConditionEvents events) {
            String sourceModule = requiredV2Module(controller);

            controller.getDirectDependenciesFromSelf().stream()
                    .filter(dependency -> !isAllowedControllerDependency(sourceModule, dependency))
                    .forEach(dependency -> events.add(SimpleConditionEvent.violated(
                            dependency,
                            dependency.getDescription() + "；请改由 " + sourceModule
                                    + " 应用用例或 web 边界适配器承接"
                    )));
        }
    }

    private static final class NoCrossModuleInternalDependencyCondition extends ArchCondition<JavaClass> {

        private NoCrossModuleInternalDependencyCondition() {
            super("not depend on another v2 module's internal packages");
        }

        @Override
        public void check(JavaClass source, ConditionEvents events) {
            String sourceModule = requiredV2Module(source);

            source.getDirectDependenciesFromSelf().stream()
                    .filter(dependency -> isOtherModuleInternal(sourceModule, dependency.getTargetClass()))
                    .forEach(dependency -> events.add(SimpleConditionEvent.violated(
                            dependency,
                            dependency.getDescription() + "；跨模块只能依赖 api"
                    )));
        }
    }

    private static final class NoCrossModuleMapperInjectionCondition extends ArchCondition<JavaClass> {

        private NoCrossModuleMapperInjectionCondition() {
            super("not inject mappers owned by another module");
        }

        @Override
        public void check(JavaClass source, ConditionEvents events) {
            Set<JavaClass> injectedMapperTypes = new HashSet<>();
            source.getFields().stream()
                    .map(field -> field.getRawType())
                    .filter(V2ModuleArchitectureTest::isMapperType)
                    .forEach(injectedMapperTypes::add);
            source.getCodeUnits().stream()
                    .flatMap(codeUnit -> codeUnit.getRawParameterTypes().stream())
                    .filter(V2ModuleArchitectureTest::isMapperType)
                    .forEach(injectedMapperTypes::add);

            injectedMapperTypes.stream()
                    .filter(mapper -> !isAllowedMapperInjection(source, mapper))
                    .forEach(mapper -> events.add(SimpleConditionEvent.violated(
                            source,
                            source.getName() + " 注入了跨模块 Mapper " + mapper.getName()
                    )));
        }
    }

    private static boolean isAllowedControllerDependency(String sourceModule, Dependency dependency) {
        JavaClass target = dependency.getTargetClass();
        String targetName = target.getName();

        if (!targetName.startsWith(ROOT_PACKAGE)
                || targetName.equals(dependency.getOriginClass().getName())
                || isSharedPlatformType(targetName)) {
            return true;
        }

        Optional<String> targetModule = v2ModuleOf(target);
        if (targetModule.isPresent()) {
            return sourceModule.equals(targetModule.get())
                    && isAllowedSameModuleControllerTarget(sourceModule, target);
        }

        return DOCUMENTED_CONTROLLER_BRIDGES
                .getOrDefault(dependency.getOriginClass().getName(), Set.of())
                .stream()
                .anyMatch(targetName::startsWith);
    }

    private static boolean isAllowedMapperInjection(JavaClass source, JavaClass mapper) {
        String sourceModule = requiredV2Module(source);
        Optional<String> mapperModule = v2ModuleOf(mapper);
        if (mapperModule.isPresent()) {
            return sourceModule.equals(mapperModule.get());
        }
        return DOCUMENTED_MAPPER_BRIDGES
                .getOrDefault(source.getName(), Set.of())
                .contains(mapper.getName());
    }

    private static boolean isOtherModuleInternal(String sourceModule, JavaClass target) {
        Optional<String> targetModule = v2ModuleOf(target);
        return targetModule.isPresent()
                && !sourceModule.equals(targetModule.get())
                && isInPackage(target, ROOT_PACKAGE + targetModule.get() + ".internal");
    }

    private static boolean isAllowedSameModuleControllerTarget(String module, JavaClass target) {
        String modulePackage = ROOT_PACKAGE + module;
        return isInPackage(target, modulePackage + ".api")
                || isInPackage(target, modulePackage + ".application")
                || isInPackage(target, modulePackage + ".internal.application")
                || (isInPackage(target, modulePackage + ".web")
                && !target.isAnnotatedWith(RestController.class));
    }

    private static boolean isSharedPlatformType(String typeName) {
        return typeName.startsWith("com.jingxuan.api.")
                || typeName.startsWith("com.jingxuan.common.")
                || typeName.startsWith("com.jingxuan.exception.")
                || typeName.startsWith("com.jingxuan.security.")
                || typeName.startsWith("com.jingxuan.workflow.");
    }

    private static boolean isMapperType(JavaClass type) {
        return type.getName().startsWith(ROOT_PACKAGE) && type.getSimpleName().endsWith("Mapper");
    }

    private static boolean isInPackage(JavaClass type, String packageName) {
        return type.getPackageName().equals(packageName)
                || type.getPackageName().startsWith(packageName + ".");
    }

    private static String requiredV2Module(JavaClass type) {
        return v2ModuleOf(type).orElseThrow(() -> new IllegalArgumentException(
                type.getName() + " 不属于 v2 九模块"
        ));
    }

    private static Optional<String> v2ModuleOf(JavaClass type) {
        String packageName = type.getPackageName();
        return V2_MODULES.stream()
                .filter(module -> packageName.equals(ROOT_PACKAGE + module)
                        || packageName.startsWith(ROOT_PACKAGE + module + "."))
                .findFirst();
    }
}
