FROM eclipse-temurin:11-jre AS builder
WORKDIR /application
ARG JAR_FILE=ddm-rrm/target/ddm-rrm-*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM eclipse-temurin:11-jre
RUN apt-get update \
  && apt-get install -y --no-install-recommends \
    git \
  && rm -rf /var/lib/apt/lists/*
WORKDIR /application
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/snapshot-dependencies/ ./
COPY --from=builder application/application/ ./
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} org.springframework.boot.loader.JarLauncher ${0} ${@}"]