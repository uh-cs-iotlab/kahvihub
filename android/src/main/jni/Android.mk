LOCAL_PATH := $(call my-dir)/../../../../jni/src
include $(CLEAR_VARS)

LOCAL_MODULE := jsDuktapeJni
LOCAL_SRC_FILES := duktape.c poll.c c_eventloop.c setTimeout.c socket.c tcpSocket.c httpRequest.c jsDuktapeWrapper.c
LOCAL_CFLAGS := -std=c99
include $(BUILD_SHARED_LIBRARY)