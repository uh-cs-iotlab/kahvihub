#include "duktape.h"
#include <jni.h>

extern void print_context(const char *prefix, duk_context *ctx);

void register_settimeout(duk_context *ctx, JNIEnv *env, jobject obj) {
	jclass objclass = (*env)->GetObjectClass(env, obj);
	if (objclass != NULL) {
		jfieldID jfield = (*env)->GetFieldID(env, objclass, "minifiedEventloopJs", "Ljava/lang/String;");
		if (jfield != NULL) {
			jstring jminifiedEventloopJs = (*env)->GetObjectField(env, obj, jfield);
			if (jminifiedEventloopJs != NULL) {
				const char *minifiedEventloopJs = (*env)->GetStringUTFChars(env, jminifiedEventloopJs, 0);
				duk_eval_string_noresult(ctx, minifiedEventloopJs);
				(*env)->ReleaseStringUTFChars(env, jminifiedEventloopJs, minifiedEventloopJs);
			}
		}
	}
}
