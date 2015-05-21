## Makefile v1.0 for configuring and installing the kahvihub

## Kahvihub options:

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
LOGDIR := var/log/kahvihub
# Database folder
DBDIR := var/db/kahvihub

# Compilation of Kahvihub
GRADLE := gradle

.PHONY: all
all: core jni kahvihub


# Conf files

# Deamon files






