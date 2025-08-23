# ---- Build stage ----
FROM gradle:8.7-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle installDist --no-daemon

# ---- Run stage ----
FROM eclipse-temurin:17-jre
WORKDIR /opt/app
COPY --from=build /app/build/install/ticketseller-server /opt/app
EXPOSE 8080
CMD ["/opt/app/bin/ticketseller-server"]
