# 使用官方的 OpenJDK 21 JDK 镜像作为构建器镜像
FROM openjdk:21-jdk as builder

# 设置工作目录
WORKDIR /app

# 复制整个项目到镜像中
COPY . .

# 打包应用
RUN mvn package

# 找出依赖的模块
RUN jdeps \
    --multi-release 21 \
    --print-module-deps \
    --ignore-missing-deps \
    --class-path "target/lib/*" \
    target/classes > /app/jdeps.txt

# 创建自定义的 JRE
RUN jlink \
    --add-modules $(cat /app/jdeps.txt) \
    --strip-debug \
    --no-header-files \
    --no-man-pages \
    --output /app/custom-jre

# 使用一个更小的基础镜像
FROM debian:stable-slim

# 设置工作目录
WORKDIR /app

# 从构建器镜像中复制自定义的 JRE 和应用
COPY --from=builder /app/custom-jre /app/custom-jre
COPY --from=builder /app/target/gpt-api-sync-1.0.0.jar /app/gpt-api-sync.jar

# 暴露端口
EXPOSE 7000

# 启动应用
CMD ["/app/custom-jre/bin/java", "-jar", "/app/gpt-api-sync.jar"]