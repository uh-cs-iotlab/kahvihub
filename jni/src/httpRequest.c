#include "duktape.h"
#include <jni.h>

extern void print_context(const char *prefix, duk_context *ctx);

duk_ret_t native_request_send(duk_context *ctx,
		const char *method, const char *url, const char *data) {

	(void) duk_get_global_string(ctx, "JNIEnv");
	JNIEnv *env = (JNIEnv *)duk_require_pointer(ctx, -1);
	jclass duktape_wrapper_jclass =
			(*env)->FindClass(env, "fi/helsinki/cs/iot/hub/jsengine/DuktapeJavascriptEngineWrapper");
	const char *signature =
			"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;";
	jmethodID perform_http_jmethodID =
			(*env)->GetStaticMethodID(env, duktape_wrapper_jclass, "performJavaHttpRequest", signature);
	jstring jmethod = (*env)->NewStringUTF(env, method);
	jstring jurl = (*env)->NewStringUTF(env, url);
	jstring jdata = NULL;
	if (data != NULL) {
		jdata = (*env)->NewStringUTF(env, data);
	}
	jstring json_response_jstring =
			(jstring) (*env)->CallStaticObjectMethod(env, duktape_wrapper_jclass, perform_http_jmethodID, jmethod, jurl, jdata);
	const char *response = (*env)->GetStringUTFChars(env, json_response_jstring, 0);
	duk_pop(ctx);  // pop global
	//print_context("After getting response", ctx);
	//printf("This is the response I got %s\n", response);
	if (response) {
		//Change the this.readyState variable to this.DONE
		duk_push_this(ctx); // [this]
		//print_context("After getting response, push this", ctx);
		duk_get_prop_string(ctx, -1, "DONE"); // [this this.DONE]
		//print_context("After getting response, get this.done", ctx);
		duk_put_prop_string(ctx, -2, "readyState"); // [this]
		//print_context("After getting response, change this.readyState", ctx);
		//Put the status to 200
		duk_push_int(ctx, 200);
		duk_put_prop_string(ctx, -2, "status"); // [this]
		//print_context("After getting response, change this.status to 200", ctx);
		//Put the response in responseText
		duk_push_string(ctx, response);
		duk_put_prop_string(ctx, -2, "responseText"); // [this]
		//print_context("After getting response, change this.responseText with response", ctx);

		//Call onreadystatechange if exists
		duk_get_prop_string(ctx, -1, "onreadystatechange"); // [this this.onreadystatechange]
		//print_context("After getting response, get this.onreadystatechange", ctx);
		if (duk_is_function(ctx, -1)) {
			duk_dup(ctx, -2); // [this this.onreadystatechange this]
			duk_remove(ctx, -3);
			//print_context("After getting response, move this before the function", ctx);
			duk_call_method(ctx, 0); // [this]
			//print_context("After getting response, call method", ctx);
		}
		else {
			duk_pop_2(ctx);
		}

	}
	//print_context("Nothing should be left here after the response is processed", ctx);
	(*env)->ReleaseStringUTFChars(env, json_response_jstring, response);
	return 1;
}

duk_ret_t xml_http_request_set_request_header(duk_context *ctx) {
	//const char *header = duk_require_string(ctx, 0);
	//const char *value = duk_require_string(ctx, 1);
	return 1;
}

duk_ret_t xml_http_request_abort(duk_context *ctx) {
	return 1;
}

duk_ret_t xml_http_request_get_response_header(duk_context *ctx) {
	//const char *header = duk_require_string(ctx, 0);
	return 1;
}

duk_ret_t xml_http_request_get_all_response_headers(duk_context *ctx) {
	return 1;
}

duk_ret_t xml_http_request_override_mime_type(duk_context *ctx) {
	//const char *mime = duk_require_string(ctx, 0);
	return 1;
}

duk_ret_t xml_http_request_send(duk_context *ctx) {
	//print_context("At the beginning of send", ctx);
	const char *data = duk_get_string(ctx, 0);
	duk_push_this(ctx); // [this]
	//print_context("Pushing this", ctx);
	duk_get_prop_string(ctx, -1, "settings"); // [this this.settings]
	//print_context("Get settings", ctx);
	duk_get_prop_string(ctx, -1, "method"); // [this this.settings this.settings.method]
	//print_context("Get settings.method", ctx);
	const char *method = duk_to_string(ctx, -1);
	duk_pop(ctx); // [this this.settings]
	duk_get_prop_string(ctx, -1, "url"); // [this this.settings this.settings.url]
	//print_context("Get settings.url", ctx);
	const char *url = duk_to_string(ctx, -1);
	duk_pop_n(ctx, 4); // [this]
	native_request_send(ctx, method, url, data);
	return 1;
}

duk_ret_t xml_http_request_open(duk_context *ctx) {
	//print_context("Opening the XmlHttpRequest", ctx);
	const char *method = duk_require_string(ctx, 0);
	const char *url = duk_require_string(ctx, 1);
	const duk_bool_t async = duk_require_boolean(ctx, 2);
	const char *username = duk_get_string(ctx, 3);
	const char *password = duk_get_string(ctx, 4);

	//First call this.abort
	duk_push_this(ctx);
	duk_push_string(ctx, "abort");
	duk_call_prop(ctx, -2, 0);
	// stack is [this]
	//print_context("After this.abort in this.open", ctx);

	//create the setting obj
	duk_idx_t setting_idx = duk_push_object(ctx); // [this settings]
	duk_push_string(ctx, method);
	duk_put_prop_string(ctx, setting_idx, "method");
	duk_push_string(ctx, url);
	duk_put_prop_string(ctx, setting_idx, "url");
	duk_push_boolean(ctx, async);
	duk_put_prop_string(ctx, setting_idx, "async");
	if (username) {
		duk_push_string(ctx, username);
	}
	else {
		duk_push_null(ctx);
	}
	duk_put_prop_string(ctx, setting_idx, "username");
	if (password) {
		duk_push_string(ctx, password);
	}
	else {
		duk_push_null(ctx);
	}
	duk_put_prop_string(ctx, setting_idx, "password");
	//print_context("Should have created a setting object", ctx);
	duk_put_prop_string(ctx, -2, "settings"); // [this]
	//print_context("Should have added the object has the settings variable", ctx);
	duk_pop_n(ctx, 7);
	//print_context("End of Opening the XmlHttpRequest", ctx);
	return 1;
}

duk_ret_t xml_http_request(duk_context *ctx) {
	//print_context("creating the XmlHttpRequest", ctx);
	duk_push_object(ctx); // [obj]
	//print_context("pushing the XmlHttpRequest", ctx);

	//Now I will push the constant
	char *constant[] = {"UNSENT", "OPENED", "HEADERS_RECEIVED",
			"LOADING", "DONE"};
	for (int i = 0; i < 5; i++) {
		duk_push_int(ctx, i);
		duk_put_prop_string(ctx, -2, constant[i]);
		//print_context(constant[i], ctx);
	}

	duk_get_prop_string(ctx, -1, "UNSENT"); // [obj obj.UNSENT]
	duk_put_prop_string(ctx, -2, "readyState"); // [obj]
	//print_context("readyState should be set", ctx);

	//Inititialise onreadystatechange with null value
	duk_push_null(ctx);
	duk_put_prop_string(ctx, -2, "onreadystatechange");

	// check http://www.w3.org/TR/2014/WD-XMLHttpRequest-20140130/
	//Creating now the open function
	duk_push_c_function(ctx, xml_http_request_open, 5);
	duk_put_prop_string(ctx, -2, "open");

	duk_push_c_function(ctx, xml_http_request_set_request_header, 2);
	duk_put_prop_string(ctx, -2, "setRequestHeader"); // [this]

	duk_push_c_function(ctx, xml_http_request_abort, 0);
	duk_put_prop_string(ctx, -2, "abort"); // [this]

	duk_push_c_function(ctx, xml_http_request_get_response_header, 1);
	duk_put_prop_string(ctx, -2, "getResponseHeader"); // [this]

	duk_push_c_function(ctx, xml_http_request_get_all_response_headers, 0);
	duk_put_prop_string(ctx, -2, "getAllResponseHeaders"); // [this]

	duk_push_c_function(ctx, xml_http_request_override_mime_type, 1);
	duk_put_prop_string(ctx, -2, "overrideMimeType"); // [this]

	duk_push_string(ctx, "");
	duk_put_prop_string(ctx, -2, "responseText"); // [this]
	duk_push_string(ctx, "");
	duk_put_prop_string(ctx, -2, "responseXML"); // [this]
	duk_push_null(ctx);
	duk_put_prop_string(ctx, -2, "status"); // [this]
	duk_push_null(ctx);
	duk_put_prop_string(ctx, -2, "statusText"); // [this]

	//Last but not least, this.sent
	duk_push_c_function(ctx, xml_http_request_send, 1);
	duk_put_prop_string(ctx, -2, "send"); // [this]
	return 1;
}

void register_xml_http_request(duk_context *ctx) {
	//First I need to push a new function to the global object
	duk_push_global_object(ctx); // [global]
	// The constructor will be XMLHttpRequest()
	duk_push_c_function(ctx, xml_http_request, 0); // [global xml_http_request]
	duk_put_prop_string(ctx, -2, "XMLHttpRequest"); // [global]
	duk_pop(ctx);
}
