#include "duktape.h"
#include <jni.h>

extern void print_context(const char *prefix, duk_context *ctx);

void register_settimeout(duk_context *ctx) {

	duk_push_global_object(ctx);

	(void) duk_get_global_string(ctx, "JNIEnv");
	JNIEnv *env = (JNIEnv *)duk_require_pointer(ctx, -1);
	jclass duktape_wrapper_jclass =
			(*env)->FindClass(env, "fi/helsinki/cs/iot/hub/jsengine/DuktapeJavascriptEngineWrapper");

	jfieldID jfield = (*env)->GetStaticFieldID(env, duktape_wrapper_jclass, "minifiedEventloopJs", "Ljava/lang/String;");
	if(jfield == NULL)
	{
		fprintf(stderr, "Could not get the field\n");
		fflush(stderr);
		return;
	}

	jstring jminifiedEventloopJs = (*env)->GetObjectField(env, duktape_wrapper_jclass, jfield);
	if (jminifiedEventloopJs != NULL) {
		const char *minifiedEventloopJs = (*env)->GetStringUTFChars(env, jminifiedEventloopJs, 0);
		duk_eval_string_noresult(ctx, minifiedEventloopJs);
		(*env)->ReleaseStringUTFChars(env, jminifiedEventloopJs, minifiedEventloopJs);
	}

	duk_pop(ctx);  // pop global

}
