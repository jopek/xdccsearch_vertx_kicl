FROM maven:3.5-jdk-8-alpine AS build

# create our src folder
ENV SOURCE_DIR /src  
RUN mkdir $SOURCE_DIR  
WORKDIR $SOURCE_DIR

# selectively add the POM file
COPY pom.xml $SOURCE_DIR

# download maven dependencies
RUN mvn verify --fail-never

# copy our Vertical sources 
COPY src $SOURCE_DIR/src

# package the thing to generate a Fat Jar
RUN mvn package

