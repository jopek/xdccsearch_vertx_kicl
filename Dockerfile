FROM maven:3.5-jdk-8-alpine AS build

# create our src folder
ENV SOURCE_DIR /src
RUN mkdir $SOURCE_DIR
WORKDIR $SOURCE_DIR


# ===== build inside docker: =====
# selectively add the POM file
#COPY pom.xml $SOURCE_DIR

# download maven dependencies
#RUN mvn verify --fail-never

# copy our Vertical sources 
#COPY src $SOURCE_DIR/src

# package the thing to generate a Fat Jar
#RUN mvn package

#CMD java -jar target/xdcc-1.0-SNAPSHOT.jar
# ///// build inside docker: /////


# ===== copy prebuilt fat jar to docker: =====
COPY target/xdcc-1.0-SNAPSHOT.jar .

CMD java -jar xdcc-1.0-SNAPSHOT.jar
# ///// copy prebuilt fat jar to docker: /////
