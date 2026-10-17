# --- build: compila o jar com o Maven Wrapper do projeto ---
FROM eclipse-temurin:25-jdk AS build
WORKDIR /build

# Dependências primeiro: enquanto o pom.xml não mudar, esta camada fica em cache
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

COPY src src
RUN ./mvnw -B -ntp -DskipTests package && cp target/*.jar app.jar

# --- runtime: só o JRE e o jar, rodando sem root ---
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /build/app.jar app.jar
USER app

EXPOSE 8080
# Usa até 75% da memória do container (o padrão da JVM é 25%)
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
