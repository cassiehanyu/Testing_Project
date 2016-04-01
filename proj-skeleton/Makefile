JFLAGS = -g
JC = javac -J-Xmx128m
RM = rm
JVM = java
MAIN = pipair
.SUFFIXES: .java .class
.java.class:
		$(JC) $(JFLAGS) $*.java

CLASSES = \
		pipair.java

default: classes

run: $(classes)
	$(JVM) $(MAIN)

classes: $(CLASSES:.java=.class)

clean:
		$(RM) *.class
