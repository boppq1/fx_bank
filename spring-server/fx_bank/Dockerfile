FROM azul/zulu-openjdk:21-jre-jammy
WORKDIR /app
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
ENV TZ=Asia/Seoul
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.config.location=file:/config/application.properties"]