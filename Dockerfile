FROM maven:3.6.0-jdk-8-alpine AS build

#Pass build args into env vars
ARG CI
ENV CI=$CI

ARG SONAR_HOST_URL
ENV SONAR_HOST_URL=$SONAR_HOST_URL

ARG SONAR_LOGIN
ENV SONAR_LOGIN=$SONAR_LOGIN

RUN if getent ahosts "sslhelp.doi.net" > /dev/null 2>&1; then \
		wget 'http://sslhelp.doi.net/docs/DOIRootCA2.cer' && \
		keytool -import -trustcacerts -file DOIRootCA2.cer -alias DOIRootCA2.cer -keystore $JAVA_HOME/jre/lib/security/cacerts -noprompt -storepass changeit; \
	fi

COPY pom.xml /build/pom.xml
WORKDIR /build

#download all maven dependencies (this will only re-run if the pom has changed)
RUN mvn -B dependency:go-offline

# copy git history into build image so that sonar can report trends over time
COPY .git /build
COPY src /build/src
ARG BUILD_COMMAND="mvn -B clean verify"
RUN ${BUILD_COMMAND}

FROM usgswma/wma-spring-boot-base:8-jre-slim-0.0.4

ENV serverPort=7509
ENV javaToRServiceEndpoint=https://javator.test.gov
ENV aqcuReportsWebserviceUrl=http://reports.test.gov
ENV aquariusServiceEndpoint=http://aquarius.test.gov
ENV aquariusServiceUser=changeme
ENV hystrixThreadTimeout=600000
ENV hystrixMaxQueueSize=200
ENV hystrixThreadPoolSize=10
ENV oauthResourceId=resource-id
ENV oauthResourceTokenKeyUri=https://example.gov/oauth/token_key
ENV HEALTHY_RESPONSE_CONTAINS='{"status":{"code":"UP","description":""}'

COPY --chown=1000:1000 --from=build /build/target/*.jar app.jar

HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -k "https://127.0.0.1:${serverPort}${serverContextPath}${HEALTH_CHECK_ENDPOINT}" | grep -q ${HEALTHY_RESPONSE_CONTAINS} || exit 1
