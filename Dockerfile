## ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-8 AS build
WORKDIR /build

# 先拷 pom，让依赖下载这一层能被缓存
COPY pom.xml ./
COPY sky-common/pom.xml sky-common/
COPY sky-pojo/pom.xml sky-pojo/
COPY sky-server/pom.xml sky-server/

# 再拷源码（application-dev.yml 已被 .dockerignore 排除，配置走环境变量）
COPY sky-common/src sky-common/src
COPY sky-pojo/src sky-pojo/src
COPY sky-server/src sky-server/src

RUN mvn -B clean package -Dmaven.test.skip=true

## ---------- 运行阶段 ----------
FROM eclipse-temurin:8-jre

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms256m -Xmx512m"

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /app
COPY --from=build /build/sky-server/target/sky-server-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
