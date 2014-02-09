#Authors : Bryce McAnally, Eugene Neff

CFLAGS = -g -Wall
cc = gcc

all: TCPmain TCPserver

TCPmain : TCPclient.o TCPmain.o
	$(cc) $(CFLAGS) -o TCPmain TCPclient.o TCPmain.o

TCPserver : TCPserver.c
	$(cc) $(CFLAGS) -o TCPserver TCPserver.c -pthread

TCPmain.o : TCPmain.c TCPclient.h
	$(cc) $(CFLAGS) -c TCPmain.c 

TCPclient.o : TCPclient.c TCPclient.h
	$(cc) $(CFLAGS) -c TCPclient.c

.PHONY : clean
clean:
	rm TCPmain TCPclient.o TCPmain.o TCPserver TCPserver.o *.class

JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = TCPclient.class

default: classes

classes: $(CLASSES:.java=.class)


