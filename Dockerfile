# ---------- Build stage ----------
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw

RUN ./mvnw dependency:go-offline -B

COPY src ./src

RUN ./mvnw clean package -DskipTests


# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre

WORKDIR /app

# Provenance (V4 roadmap step 1): the deploy workflow passes the image tag,
# so stored runs record which code produced them.
ARG CODE_VERSION=local
ENV XNEWS_CODE_VERSION=${CODE_VERSION}

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]