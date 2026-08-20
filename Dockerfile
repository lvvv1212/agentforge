# ===== Stage 1: 构建 =====
FROM maven:3.9-eclipse-temurin-22 AS builder
WORKDIR /build

# 注入阿里云 Maven 镜像，解决构建容器连中央仓库（repo.maven.apache.org）超时
COPY maven-settings.xml /root/.m2/settings.xml

# 先拷 pom.xml 下载依赖（利用 Docker 层缓存，改代码不用重下依赖）
COPY pom.xml .
RUN mvn -B dependency:go-offline

# 再拷源码编译
COPY src ./src
RUN mvn -B package -DskipTests

# ===== Stage 2: 运行（轻量 JRE 镜像）=====
FROM eclipse-temurin:22-jre
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar

# 默认启用 redis profile（配合 docker-compose 中的 redis-stack），向量数据重启不丢
ENV SPRING_PROFILES_ACTIVE=redis
ENV REDIS_HOST=redis

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
