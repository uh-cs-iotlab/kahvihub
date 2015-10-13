## Makefile v1.0 for configuring and installing the kahvihub

DEBUG := true

## Kahvihub options:
NAME := kahvihub

# Port
PORT := 80
TEST_PORT := 8080

# Root folder to install kahvihub
ROOT_INSTALL := /usr/local/
TEST_ROOT_INSTALL := test/

# Root folder for kahvihub var files
ROOT_VAR := /var/local/
TEST_ROOT_VAR := test/var/

# User and group
HUB_USER := kahvihub
HUB_GROUP := kahvihub

# Where to place the conf files
ETCDIR := etc/kahvihub
# Where to place the libraries
LIBDIR := lib/kahvihub
# Log files for kahvihub
LOGDIR := log/kahvihub
# Database folder
DBDIR := db/kahvihub

# Compilation of Kahvihub
GRADLE := ./gradlew

.PHONY: embedded.test embedded.test.run embedded.clean

clean:
	$(GRADLE) clean

embedded.test:
	#Need to create a config file
	@echo "{\"name\": \"$(NAME)-test\", \"port\": $(TEST_PORT)," > embedded/$(NAME)-test.conf
	@echo "\"libdir\": \"$(TEST_ROOT_INSTALL)$(LIBDIR)\"," >> embedded/$(NAME)-test.conf
	@echo "\"logdir\": \"$(TEST_ROOT_VAR)$(LOGDIR)\"," >> embedded/$(NAME)-test.conf
	@echo "\"dbdir\": \"$(TEST_ROOT_VAR)$(DBDIR)\"," >> embedded/$(NAME)-test.conf
	@echo "\"dbname\": \"$(NAME)-test.db\"," >> embedded/$(NAME)-test.conf
	@echo "\"dbversion\": 1.0," >> embedded/$(NAME)-test.conf
	@echo "\"debug\": $(DEBUG)}" >> embedded/$(NAME)-test.conf
	@if [ ! -d "embedded/$(TEST_ROOT_INSTALL)$(LIBDIR)" ]; then mkdir -p embedded/$(TEST_ROOT_INSTALL)$(LIBDIR); fi
	@if [ ! -d "embedded/$(TEST_ROOT_VAR)$(LOGDIR)" ]; then mkdir -p embedded/$(TEST_ROOT_VAR)$(LOGDIR); fi
	@if [ ! -d "embedded/$(TEST_ROOT_VAR)$(DBDIR)" ]; then mkdir -p embedded/$(TEST_ROOT_VAR)$(DBDIR); fi
	$(GRADLE) embedded:build
	
embedded.test.run:
	$(GRADLE) -q embedded:run '-Pconf=-c,$(NAME)-test.conf'

test: embedded.test
run: embedded.test.run




