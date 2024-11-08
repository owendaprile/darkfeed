FROM registry.access.redhat.com/ubi8/openjdk-21 AS build
WORKDIR /build
COPY --chown=jboss / .
RUN ./gradlew --no-daemon :darkfeed:installDist

FROM registry.access.redhat.com/ubi8/openjdk-21
COPY --from=build /build/darkfeed/build/install/darkfeed /app
ENTRYPOINT /app/bin/darkfeed
