include ../common/base.mk
include $(COMMON)/java.mk

JAVA_FILE := src/main/java/com/example/App.java
JAR_FILE := target/my-app-1.0-SNAPSHOT.jar


default::
	which mvn

build:: $(JAR_FILE)

$(JAR_FILE): $(JAVA_FILE)
	mvn clean package

run:: build
	java -jar target/my-app-1.0-SNAPSHOT.jar

test clean package::
	mvn $@
