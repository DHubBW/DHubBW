FROM eclipse-temurin:25-jdk

WORKDIR /app

COPY target/dhubbw-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=dev
ENV DB_URL=jdbc:h2:mem:testdb
ENV DB_USERNAME=sa
ENV DB_PASSWORD=password

ENTRYPOINT ["java","-jar","/app/app.jar"]