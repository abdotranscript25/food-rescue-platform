# ==========================================
# ÉTAPE 1 : Construction (Build)
# ==========================================
# On utilise une image Maven avec Java 21 pour compiler le code
FROM maven:3.9-eclipse-temurin-21 AS build

# On définit le dossier de travail dans le conteneur
WORKDIR /app

# On copie le fichier pom.xml et on télécharge les dépendances
COPY pom.xml .
RUN mvn dependency:go-offline

# On copie tout le code source
COPY src ./src

# On compile et on package l'application (sans les tests pour aller plus vite)
RUN mvn clean package -DskipTests

# ==========================================
# ÉTAPE 2 : Exécution (Runtime)
# ==========================================
# On utilise une image Java légère (JRE) pour exécuter l'application
FROM eclipse-temurin:21-jre-alpine

# On définit le dossier de travail
WORKDIR /app

# On copie le fichier .jar généré à l'étape 1 dans l'étape 2
COPY --from=build /app/target/*.jar app.jar

# On expose le port utilisé par Eureka
EXPOSE 8761

# La commande à exécuter au démarrage du conteneur
ENTRYPOINT ["java", "-jar", "app.jar"]