#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#ifndef NO_SIGNAL
#include <signal.h>
#endif

#include "jsDuktapeWrapper.h"
#include "duktape.h"

/*
 * Inspired from Duktape eventLoop main.c
 */

#define DEBUG 0

extern void poll_register(duk_context *ctx);
extern void register_active_socket_list(duk_context *ctx);
extern void register_tcp_socket(duk_context *ctx);
extern void register_xml_http_request(duk_context *ctx);

void print_context(const char *prefix, duk_context *ctx) {
	duk_push_context_dump(ctx);
	printf("%s, %s\n", prefix, duk_to_string(ctx, -1));
	duk_pop(ctx);
}

int wrapped_compile_execute(duk_context *ctx) {
	int comp_flags = 0;

	// Compile input and place it into global _USERCODE
	duk_compile(ctx, comp_flags);
	duk_push_global_object(ctx);
	duk_insert(ctx, -2);  // [ ... global func ]
	duk_put_prop_string(ctx, -2, "_USERCODE");
	duk_pop(ctx);

	// Start a zero timer which will call _USERCODE from within
	// the event loop.
	fprintf(stderr, "set _USERCODE timer\n");
	fflush(stderr);
	duk_eval_string(ctx, "setTimeout(function() { _USERCODE(); }, 0);");
	duk_pop(ctx);

	// Finally, launch eventloop.  This call only returns after the
	//eventloop terminates.
	// At the moment, I just handle the Ecmascript version of loops
	fprintf(stderr, "calling EventLoop.run()\n");
	fflush(stderr);
	duk_eval_string(ctx, "EventLoop.run();");
	duk_pop(ctx);

	return 0;
}

void myFatal (duk_context *ctx, duk_errcode_t code, const char *msg) {
	printf("Error code: %d\n", code);
	printf("Error message: %s\n", msg);
	exit(-1);
}

void throwException(JNIEnv *env, const char* tag, const char *message) {
	jclass cls = (*env)->FindClass(env, "fi/helsinki/cs/iot/hub/jsengine/JavascriptEngineException");
	if (cls == 0) {
		printf("Could not find the class: %s\n", "fi/helsinki/cs/iot/hub/jsengine/JavascriptEngineException");
		return;
	}
	jmethodID mid = (*env)->GetMethodID(env, cls, "<init>", "(Ljava/lang/String;Ljava/lang/String;)V");
	if (mid == 0) {
		printf("Could not find the init method");
		return;
	}
	jstring jtag = (*env)->NewStringUTF(env, tag);
	jstring jmessage = (*env)->NewStringUTF(env, message);
	jthrowable e = (*env)->NewObject(env, cls, mid, jtag, jmessage);
	if (e == 0) {
		printf("Could not instanciate the throwable");
		return;
	}
	// Now throw the exception
	(*env)->Throw(env, e);
}

const char* loadScriptString(JNIEnv *env, duk_context *ctx, jstring script) {
	const char *nativeScript = (*env)->GetStringUTFChars(env, script, 0);

	if (duk_peval_string(ctx, nativeScript) != 0) {
		char str[80];
		sprintf(str, "Script error: %s\n", duk_safe_to_string(ctx, -1));
		throwException(env, "checkService", str);
		return NULL;
	}

	(*env)->ReleaseStringUTFChars(env, script, nativeScript);
	const char *res = duk_safe_to_string(ctx, -1);
	return res;
}

void loadScriptFile(JNIEnv *env, duk_context *ctx, jstring script) {
	const char *nativeScript = (*env)->GetStringUTFChars(env, script, 0);
	printf("Eval script file: %s\n", nativeScript);
	if (duk_peval_file(ctx, nativeScript) != 0) {
		char str[80];
		sprintf(str, "Script error: %s\n", duk_safe_to_string(ctx, -1));
		throwException(env, "checkService", str);
	}
	(*env)->ReleaseStringUTFChars(env, script, nativeScript);
	duk_pop(ctx);
}

int hasAdditionalFunctionality(JNIEnv *env, jobject obj, const char *function) {
	int res = 0;
	jclass objclass = (*env)->GetObjectClass(env, obj);
	if (objclass != NULL) {
		jmethodID mid = (*env)->GetMethodID(env, objclass, function, "()Z");
		if (mid != NULL) {
			jboolean response = (*env)->CallBooleanMethod(env, obj, mid);
			if(response == JNI_TRUE) {
				res = 1;
			}
		}
	}
	return res;
}

//Call to the homonymous function of DuktapeJavascriptEngineWrapper
int hasTcpSockets(JNIEnv *env, jobject obj) {
	return hasAdditionalFunctionality(env, obj, "hasTcpSockets");
}

//Call to the homonymous function of DuktapeJavascriptEngineWrapper
int hasHttpRequest(JNIEnv *env, jobject obj) {
	return hasAdditionalFunctionality(env, obj, "hasHttpRequest");
}

//Call to the homonymous function of DuktapeJavascriptEngineWrapper
int hasEventLoop(JNIEnv *env, jobject obj) {
	return hasAdditionalFunctionality(env, obj, "hasEventLoop");
}

int needJNIEnv(int hasTcpSockets, int hasHttpRequest) {
	return hasTcpSockets || hasHttpRequest;
}

duk_ret_t native_check_exit_requested(duk_context *ctx) {
	(void) duk_get_global_string(ctx, "JNIEnv");
	JNIEnv *env = (JNIEnv *)duk_require_pointer(ctx, -1);

	(void) duk_get_global_string(ctx, "DuktapeJavascriptEngineWrapper");
	jobject obj = (jobject) duk_require_pointer(ctx, -1);

	int res = 0;

	jclass objclass = (*env)->GetObjectClass(env, obj);
	if (objclass != NULL) {
		jmethodID mid = (*env)->GetMethodID(env, objclass, "needToStopAllEvents", "()Z");
		if (mid != NULL) {
			jboolean response = (*env)->CallBooleanMethod(env, obj, mid);
			if(response == JNI_TRUE) {
				res = 1;
			}
		}
	}
	duk_pop(ctx);  // pop global
	duk_push_boolean(ctx, res);
	return 1;
}

duk_ret_t make_config_persistant(duk_context *ctx) {
	const char *configuration = duk_require_string(ctx, 0);

	(void) duk_get_global_string(ctx, "JNIEnv");
	JNIEnv *env = (JNIEnv *)duk_require_pointer(ctx, -1);

	(void) duk_get_global_string(ctx, "DuktapeJavascriptEngineWrapper");
	jobject obj = (jobject) duk_require_pointer(ctx, -1);

	int res = 0;

	jclass objclass = (*env)->GetObjectClass(env, obj);
	if (objclass != NULL) {
		jmethodID mid = (*env)->GetMethodID(env, objclass, "makePluginConfigurationPersistant", "(Ljava/lang/String;)Z");
		if (mid != NULL) {
			jstring jconf = NULL;
			if (configuration) {
				jconf = (*env)->NewStringUTF(env, configuration);
			}
			jboolean response = (*env)->CallBooleanMethod(env, obj, mid, jconf);
			if(response == JNI_TRUE) {
				res = 1;
			}
		}
	}
	duk_pop_3(ctx);  // pop global
	duk_push_boolean(ctx, res);
	return 1;

}

int loadEnvironment(JNIEnv *env, jobject obj, duk_context *ctx) {

	int has_tcp = hasTcpSockets(env, obj);
	int has_http = hasHttpRequest(env, obj);
	//int hasEventLoop = hasEventLoop(env, obj);

	if (needJNIEnv(has_tcp, has_http)) {
		//I need access to the JNIEnv in my native_request_send
		duk_push_global_object(ctx);
		duk_push_pointer(ctx, env);
		duk_put_prop_string(ctx, -2, "JNIEnv");
		duk_pop(ctx);  // pop global

		//I need access to the jobject obj
		duk_push_global_object(ctx);
		duk_push_pointer(ctx, obj);
		duk_put_prop_string(ctx, -2, "DuktapeJavascriptEngineWrapper");
		duk_pop(ctx);  // pop global
	}

	if (has_tcp) {
		register_tcp_socket(ctx);
	}

	if (has_http) {
		register_xml_http_request(ctx);
	}

	duk_push_global_object(ctx);
	duk_push_c_function(ctx, native_check_exit_requested, 0);
	duk_put_prop_string(ctx, -2, "checkNativeExitRequested");
	duk_pop(ctx);  // pop global

	duk_push_global_object(ctx);
	duk_push_c_function(ctx, make_config_persistant, 1);
	duk_put_prop_string(ctx, -2, "makeConfigurationPersistant");
	duk_pop(ctx);  // pop global

	/*jclass objclass = (*env)->GetObjectClass(env, obj);
	jmethodID mid = (*env)->GetMethodID(env, objclass, "getInitFiles", "()[Ljava/lang/String;");
	if (mid == NULL) {
		return -2;
	}
	jobjectArray result = (*env)->CallObjectMethod(env, obj, mid);
	if (result == NULL) {
		return -1;
	}
	jsize length = (*env)->GetArrayLength(env, (jobjectArray)result);
	for (int i = 0; i < length; i++) {
		jstring initScript = (jstring) (*env)->GetObjectArrayElement(env, (jobjectArray)result, i);
		loadScriptFile(env, ctx, initScript);
	}*/
	return 1;
}

JNIEXPORT jstring JNICALL Java_fi_helsinki_cs_iot_hub_jsengine_DuktapeJavascriptEngineWrapper_runScript
(JNIEnv *env, jobject thisObj, jstring script) {
	//duk_context *ctx = duk_create_heap_default();
	duk_context *ctx = duk_create_heap(NULL, NULL, NULL, NULL, &myFatal);
	if (!ctx) {
		printf("Failed to create a Duktape heap.\n");
		return NULL;
	}
	//print_context("Before loading the environment", ctx);
	loadEnvironment(env, thisObj, ctx);
	//print_context("After loading the environment", ctx);
	const char *res = loadScriptString(env, ctx, script);
	//print_context("After loading the script", ctx);
	duk_destroy_heap(ctx);
	return (*env)->NewStringUTF(env, res);
}



int library_has_prop(JNIEnv *env, duk_context *ctx, const char* function) {
	duk_push_string(ctx, function); // -> [ global nativeName "function" ]
	duk_bool_t res = duk_get_prop(ctx, -2);
	duk_pop(ctx);
	return res;
}

JNIEXPORT jboolean JNICALL Java_fi_helsinki_cs_iot_hub_jsengine_DuktapeJavascriptEngineWrapper_checkPlugin
(JNIEnv *env, jobject thisObj, jstring jpluginName, jstring jscript) {
	duk_context *ctx = duk_create_heap(NULL, NULL, NULL, NULL, &myFatal);
	if (!ctx) {
		throwException(env, "checkPlugin", "Failed to create a Duktape heap");
		return JNI_FALSE;
	}

	if (!loadScriptString(env, ctx, jscript)) {
		duk_destroy_heap(ctx);
		return JNI_FALSE;
	}

	const char *pluginName = (*env)->GetStringUTFChars(env, jpluginName, 0);
	duk_push_global_object(ctx);
	duk_push_string(ctx, pluginName);
	if(duk_get_prop(ctx, -2) == 0) {
		duk_pop_n(ctx, 2);
		char str[80];
		sprintf(str, "Plugin %s is unknown",pluginName);
		throwException(env, "checkPlugin", str);
		duk_destroy_heap(ctx);
		return JNI_FALSE;
	}

	char *methods[] = {"needConfiguration", "checkConfiguration", "configure", "isFeatureSupported",
			"isFeatureAvailable", "isFeatureReadable", "isFeatureWritable",
			"getNumberOfFeatures", "getFeatureDescription", "getFeatureValue", "postFeatureValue"};
	for (int i = 0; i < sizeof(methods)/sizeof(char*); i++) {
		int res;
		if ((res = library_has_prop(env, ctx, methods[i])) != 1) {
			char str[80];
			sprintf(str, "The plugin %s has no method %s", pluginName, methods[i]);
			throwException(env, "checkService", str);
			duk_destroy_heap(ctx);
			return JNI_FALSE;
		}
	}
	duk_destroy_heap(ctx);
	return JNI_TRUE;
}

JNIEXPORT jstring JNICALL Java_fi_helsinki_cs_iot_hub_jsengine_DuktapeJavascriptEngineWrapper_getLibraryOutput
(JNIEnv *env, jobject thisObj, jstring libraryName, jstring jscript, jstring jcommandToEvaluate) {
	duk_context *ctx = duk_create_heap(NULL, NULL, NULL, NULL, &myFatal);
	if (!ctx) {
		throwException(env, "getLibraryOutput", "Failed to create a Duktape heap");
		return NULL;
	}
	loadEnvironment(env, thisObj, ctx);
	if (!loadScriptString(env, ctx, jscript)) {
		duk_destroy_heap(ctx);
		return NULL;
	}
	const char *res = loadScriptString(env, ctx, jcommandToEvaluate);
	if (!res) {
		throwException(env, "getLibraryOutput", "I could not load the command for some reason!");
		duk_destroy_heap(ctx);
		return NULL;
	}
	duk_destroy_heap(ctx);
	//fprintf(stderr, "After: %s\n", res);
	return (*env)->NewStringUTF(env, res);
}

JNIEXPORT jboolean JNICALL Java_fi_helsinki_cs_iot_hub_jsengine_DuktapeJavascriptEngineWrapper_checkService
(JNIEnv *env, jobject thisObj, jstring serviceName, jstring serviceScript) {
	/*duk_context *ctx = duk_create_heap(NULL, NULL, NULL, NULL, &myFatal);
	if (!ctx) {
		throwException(env, "checkService", "Failed to create a Duktape heap");
		return JNI_FALSE;
	}

	int res;
	if ((res = loadEnvironment(env, thisObj, ctx)) != 0) {
		char str[80];
		sprintf(str, "Could not load the environment (%d)", res);
		throwException(env, "checkService", str);
		duk_destroy_heap(ctx);
		return JNI_FALSE;
	}

	res = loadScriptString(env, ctx, serviceScript);
	if (res != 0) {
		fprintf(stderr, "I could not load the script for some reason!");
		throwException(env, "checkService", "I could not load the script for some reason!");
		duk_destroy_heap(ctx);
		return JNI_FALSE;
	}

	res = serviceHasFunction(env, ctx, serviceName, "needConfiguration");
	if (res <= 0) {
		char str[80];
		sprintf(str, "The service has no method needConfiguration (%d)", res);
		fprintf(stderr, "The service has no method needConfiguration (%d)\n", res);
		throwException(env, "checkService", str);
		duk_destroy_heap(ctx);
		return JNI_FALSE;
	}

	res = serviceHasFunction(env, ctx, serviceName, "run");
	if (res <= 0) {
		char str[80];
		sprintf(str, "The service has no method run (%d)", res);
		fprintf(stderr, "The service has no method run (%d)\n", res);
		throwException(env, "checkService", str);
		duk_destroy_heap(ctx);
		return JNI_FALSE;
	}

	duk_destroy_heap(ctx);*/
	return JNI_FALSE;
}


JNIEXPORT jboolean JNICALL Java_fi_helsinki_cs_iot_hub_jsengine_DuktapeJavascriptEngineWrapper_needConfiguration
(JNIEnv *env, jobject thisObj, jstring jserviceName, jstring jserviceScript) {
	/*duk_context *ctx = duk_create_heap(NULL, NULL, NULL, NULL, &myFatal);
	if (!ctx) {
		throwException(env, "checkService", "Failed to create a Duktape heap");
	}

	int res;
	if ((res = loadEnvironment(env, thisObj, ctx)) != 0) {
		char str[80];
		sprintf(str, "Could not load the environment (%d)", res);
		throwException(env, "checkService", str);
	}
	loadScriptString(env, ctx, jserviceScript);

	const char *serviceName = (*env)->GetStringUTFChars(env, jserviceName, 0);
	char eval_string[80];
	sprintf(eval_string, "%s.needConfiguration(this);", serviceName);

	jboolean returnVal = JNI_FALSE;
	if (duk_peval_string(ctx, eval_string) != 0) {
		char str[80];
		sprintf(str, "Error 3: %s\n", duk_safe_to_string(ctx, -1));
		throwException(env, "checkService", str);
		returnVal = JNI_FALSE;
	}
	else if (duk_get_boolean(ctx, -1)) {
		returnVal = JNI_TRUE;
	}
	duk_pop(ctx);  //
	(*env)->ReleaseStringUTFChars(env, jserviceName, serviceName);
	duk_destroy_heap(ctx);
	return returnVal;*/
	return JNI_FALSE;
}


JNIEXPORT void JNICALL Java_fi_helsinki_cs_iot_hub_jsengine_DuktapeJavascriptEngineWrapper_run
(JNIEnv *env, jobject thisObj, jstring jserviceName, jstring jserviceScript, jstring jserviceConf) {
	duk_context *ctx = duk_create_heap(NULL, NULL, NULL, NULL, &myFatal);
	if (!ctx) {
		throwException(env, "checkService", "Failed to create a Duktape heap");
	}

	poll_register(ctx);

	int res;
	if ((res = loadEnvironment(env, thisObj, ctx)) != 0) {
		char str[80];
		sprintf(str, "Could not load the environment (%d)", res);
		throwException(env, "checkService", str);
	}
	loadScriptString(env, ctx, jserviceScript);

	const char *serviceName = (*env)->GetStringUTFChars(env, jserviceName, 0);
	const char *serviceConf = (*env)->GetStringUTFChars(env, jserviceConf, 0);

	char eval_string[1024];
	sprintf(eval_string, "%s.config = %s;", serviceName, serviceConf);
	//fprintf(stderr, "%s\n", eval_string);
	if (duk_peval_string(ctx, eval_string) != 0) {
		char str[80];
		sprintf(str, "Script error: %s\n", duk_safe_to_string(ctx, -1));
		throwException(env, "set config", str);
	}

	memset(&eval_string[0], 0, sizeof(eval_string));
	sprintf(eval_string, "%s.run(this);", serviceName);

	duk_push_string(ctx, eval_string); // Push the code
	duk_push_string(ctx, serviceName); // Supposedly the filename

	duk_int_t rc = duk_safe_call(ctx, wrapped_compile_execute, 2 /*nargs*/, 1 /*nret*/);
	if (rc != DUK_EXEC_SUCCESS) {
		char str[80];
		sprintf(str, "Error 3: %s\n", duk_safe_to_string(ctx, -1));
		throwException(env, "checkService", str);
	}

	duk_pop(ctx);  // ignore result
	(*env)->ReleaseStringUTFChars(env, jserviceName, serviceName);
	(*env)->ReleaseStringUTFChars(env, jserviceName, serviceConf);
	duk_destroy_heap(ctx);
}
