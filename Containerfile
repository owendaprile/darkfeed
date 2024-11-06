FROM registry.access.redhat.com/ubi8/openjdk-21 AS build
WORKDIR /build
COPY --chown=jboss / .
RUN ./gradlew --no-daemon :app:installDist

FROM registry.access.redhat.com/ubi8/openjdk-21
COPY --from=build /build/app/build/install/app /app
ENTRYPOINT /app/bin/app
