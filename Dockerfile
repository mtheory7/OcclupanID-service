FROM amazoncorretto:23

WORKDIR /app

# Required by JavaCPP / OpenCV / nd4j-native on Linux
# RUN apt-get update && apt-get install -y --no-install-recommends libgomp1 && rm -rf /var/lib/apt/lists/*

COPY target/OcclupanID-service-1.0.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", \
  "-Xms256M", \
  "-Xmx1G", \
  "-Dorg.bytedeco.javacpp.maxbytes=2G", \
  "-Dorg.bytedeco.javacpp.maxphysicalbytes=3500M", \
  "-jar", "app.jar"]