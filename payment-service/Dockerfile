# ==========================================
# ÉTAPE 1 : Build avec Maven
# ==========================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copier le pom.xml et télécharger les dépendances (mise en cache Docker)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copier le code source et compiler
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# ÉTAPE 2 : Runtime avec JRE léger
# ==========================================
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copier le JAR généré
COPY --from=build /app/target/*.jar app.jar

# Exposer le port (d'après votre application.yml)
EXPOSE 8086

# Lancer l'application
ENTRYPOINT ["java", "-jar", "app.jar"]