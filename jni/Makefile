DUKTAPE_SOURCES = duktape.c poll.c tcpSocket.c httpRequest.c jsDuktapeWrapper.c

ifeq ($(OS),Windows_NT)
    # TODO: test for windows
    #JAVA_HOME = $(shell readlink -f /usr/bin/javac | sed "s:bin/javac::")include
else
    UNAME_S := $(shell uname -s)
    ifeq ($(UNAME_S),Linux)
        JAVA_HOME = $(shell readlink -f /usr/bin/javac | sed "s:bin/javac::")include
        JAVA_LINUX = $(JAVA_HOME)/linux
    endif
    ifeq ($(UNAME_S),Darwin)
        JAVA_HOME = $(shell /usr/libexec/java_home)/include
	JAVA_OSX = $(JAVA_HOME)/darwin
        # below is for java 1.6, the osx default. but you should use java 1.7
	#JAVA_HOME = /System/Library/Frameworks/JavaVM.framework/Versions/A/Headers
    endif
endif

CC	= gcc
CCOPTS = -pedantic -std=c99 -Wall -fstrict-aliasing -shared -fpic
#CCOPTS	= -Os -pedantic -std=c99 -Wall -fstrict-aliasing -fomit-frame-pointer -shared -fpic
CCOPTS += -I$(JAVA_HOME)
ifdef JAVA_LINUX
  CCOPTS += -I$(JAVA_LINUX)  # for combined sources
endif
ifdef JAVA_OSX
  CCOPTS += -I$(JAVA_OSX)   # include darwin dir
endif
CCLIBS	= -lm
DEFINES =


all: libjsDuktapeJni.so

test: test.c
	gcc -o $@ -std=c99 -O2 -Wall -Wextra -Isrc/ duktape.c $< -lm

# For debugging, use -O0 -g -ggdb, and don't add -fomit-frame-pointer
libjsDuktapeJni.so: $(addprefix src/, $(DUKTAPE_SOURCES)) 
	$(CC) -o $@ $(DEFINES) $(CCOPTS) $^ $(CCLIBS)
	
clean:
	@rm -rf *.so

