FROM docker
COPY --from=docker/buildx-bin:latest /buildx /usr/libexec/docker/cli-plugins/docker-buildx

# 서버를 구동시킬 자바를 받아옵니다.
FROM azul/zulu-openjdk:17-latest as build

# `JAR_FILE` 이라는 이름으로 build 한 jar 파일을 지정합니다.
ARG JAR_FILE=./build/libs/*.jar

# 지정한 jar 파일을 app.jar 라는 이름으로 Docker Container에 추가합니다.
ADD ${JAR_FILE} app.jar

# 서비스에서 해당 포트를 사용할 것이라고 암시합니다.
EXPOSE 8080

# app.jar 파일을 실행합니다.
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "/app.jar"]
