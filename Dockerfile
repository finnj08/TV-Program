# ---------- Stage 1: bauen ----------
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build

# Zuerst nur die pom.xml: Abhängigkeiten werden gecacht, solange sie sich nicht ändern
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

# ---------- Stage 2: schlankes Laufzeit-Image ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

# Nicht als root laufen lassen
RUN useradd --system --uid 1001 app
USER app

COPY --from=build /build/target/*.jar app.jar

# Für kleine Container (z. B. 512 MB): Heap an Container-Limit koppeln, sparsamer GC
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC"

# Der Port kommt zur Laufzeit aus der Umgebungsvariable PORT (siehe application.properties)
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]