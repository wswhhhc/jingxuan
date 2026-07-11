# 阶段 1 兼容性核验

记录日期：2026-07-11

## 结论

- 后端阶段 1 目标版本可行：`Spring Boot 4.1.x + Java 25 + MyBatis-Plus Boot4 3.5.17 + Spring Modulith 2.1.x` 组合存在官方发布依据。
- 前端阶段 1 目标版本可行：当前 Node 24 LTS 线已稳定发布，适合作为 Vite 8 / Vue 3.5 工具链基线。
- 当前仓库仍停留在 `Spring Boot 3.2.5 + Java 17`，阶段 1 将是破坏性升级，不应与业务重构混在同一个增量里。

## 官方依据

- Spring Boot 官方系统要求页当前稳定版为 `4.1.0`，页面元数据标记 `version=4.1.0`。
- 同页要求：
  - Java `17` 到 `26`
  - Maven `3.6.3+`
- Maven Central 元数据：
  - `com.baomidou:mybatis-plus-spring-boot4-starter` 最新正式版 `3.5.17`
  - `org.springframework.modulith:spring-modulith-bom` 最新正式版 `2.1.0`
  - `org.springdoc:springdoc-openapi-starter-webmvc-ui` 最新正式版 `3.0.3`
- OpenJDK 项目页显示 JDK 25 已正式发布，可作为目标 LTS 线。
- Node 官方发布索引显示 Node 24 LTS 当前已发布到 `v24.18.0`，LTS 代号 `Krypton`。

## 建议落地顺序

1. 先完成阶段 1 的工程基础：Spring Boot 4.1、Java 25、Springdoc 3、Flyway、Modulith/ArchUnit 骨架。
2. 再迁移 OpenAPI 契约源与前端生成链路，避免接口和客户端同时手改。
3. 最后再进入业务模块拆分；不要在旧的 Adapter + Result 体系里混入新 `/api/v1` 契约。

## 来源

- https://docs.spring.io/spring-boot/system-requirements.html
- https://repo1.maven.org/maven2/com/baomidou/mybatis-plus-spring-boot4-starter/maven-metadata.xml
- https://repo1.maven.org/maven2/org/springframework/modulith/spring-modulith-bom/maven-metadata.xml
- https://repo1.maven.org/maven2/org/springdoc/springdoc-openapi-starter-webmvc-ui/maven-metadata.xml
- https://openjdk.org/projects/jdk/25/
- https://nodejs.org/dist/index.json
