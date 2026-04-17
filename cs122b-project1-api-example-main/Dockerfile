# ---------- Stage 1: Build WAR ----------
FROM maven:3.8.5-openjdk-11-slim AS builder

ARG MVN_PROFILE="default"
WORKDIR /app

COPY . .
RUN mvn clean package -P${MVN_PROFILE}

# ---------- Stage 2: Tomcat ----------
FROM tomcat:10-jdk11
WORKDIR /app

COPY --from=builder /app/target/fabflix.war /usr/local/tomcat/webapps/ROOT.war
COPY ./WebContent/META-INF/context.xml /usr/local/tomcat/conf/context.xml

EXPOSE 8080
CMD ["catalina.sh", "run"]
