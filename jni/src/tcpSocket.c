#include "duktape.h"
#include <jni.h>

extern void print_context(const char *prefix, duk_context *ctx);

duk_ret_t tcp_socket_onreceive(duk_context *ctx) {
	//Dummy onreceive(code, msg)
	duk_pop(ctx);
	return 1;
}

duk_ret_t tcp_socket_onerror(duk_context *ctx) {
	//Dummy onerror(code, msg)
	duk_pop_2(ctx);
	return 1;
}

//This method should create a Java TCP socket on DuktapeJavascriptEngineWrapper and return a unique identifier
int jni_tcp_socket_connect(duk_context *ctx, const char *address, int port) {

	(void) duk_get_global_string(ctx, "JNIEnv");
	JNIEnv *env = (JNIEnv *)duk_require_pointer(ctx, -1);

	(void) duk_get_global_string(ctx, "DuktapeJavascriptEngineWrapper");
	jobject obj = (jobject) duk_require_pointer(ctx, -1);

	jint response = -1;

	jclass objclass = (*env)->GetObjectClass(env, obj);
	if (objclass != NULL) {
		jmethodID mid = (*env)->GetMethodID(env, objclass, "tcpSocketConnect", "(Ljava/lang/String;I)I");
		if (mid != NULL) {
			jstring jaddress = NULL;
			if (address) {
				jaddress = (*env)->NewStringUTF(env, address);
			}
			response = (*env)->CallIntMethod(env, obj, mid, jaddress, port);
		}
	}
	duk_pop_2(ctx);  // pop global
	return response;
}

duk_ret_t tcp_socket_connect(duk_context *ctx) {
	const char *address = duk_require_string(ctx, 0);
	const int port = duk_require_int(ctx, 1);

	//First call this.close()
	duk_push_this(ctx);
	duk_push_string(ctx, "close");
	duk_call_prop(ctx, -2, 0);

	int id_socket = jni_tcp_socket_connect(ctx, address, port);
	if (id_socket < 0) {
		//print_context("Could not connect to the tcp socket", ctx);
		duk_pop_n(ctx, 4);
		//print_context("Could not connect to the tcp socket, but I just make sure that everything is now clean", ctx);
		return 0;
	}

	//create the settings obj
	duk_idx_t setting_idx = duk_push_object(ctx);
	duk_push_string(ctx, address);
	duk_put_prop_string(ctx, setting_idx, "address");
	duk_push_int(ctx, port);
	duk_put_prop_string(ctx, setting_idx, "port");
	duk_push_int(ctx, id_socket);
	duk_put_prop_string(ctx, setting_idx, "id");

	duk_put_prop_string(ctx, -2, "settings");
	duk_pop(ctx);

	//Change the status to connected
	duk_get_prop_string(ctx, -1, "CONNECTED");
	duk_put_prop_string(ctx, -2, "status");
	duk_pop_3(ctx);
	return 1;
}

//This method should send a msg to the Java TCP socket on DuktapeJavascriptEngineWrapper with the unique identifier
int jni_tcp_socket_send(duk_context *ctx, int id_socket, const char *msg) {
	(void) duk_get_global_string(ctx, "JNIEnv");
	JNIEnv *env = (JNIEnv *)duk_require_pointer(ctx, -1);

	(void) duk_get_global_string(ctx, "DuktapeJavascriptEngineWrapper");
	jobject obj = (jobject) duk_require_pointer(ctx, -1);

	jclass objclass = (*env)->GetObjectClass(env, obj);
	if (objclass != NULL) {
		jmethodID mid = (*env)->GetMethodID(env, objclass, "tcpSocketSend", "(ILjava/lang/String;)Ljava/lang/String;");
		if (mid != NULL) {

			jstring jmsg = NULL;
			if (msg) {
				jmsg = (*env)->NewStringUTF(env, msg);
			}

			jstring jresponse = (jstring) (*env)->CallObjectMethod(env, obj, mid, id_socket, jmsg);
			if ((*env)->ExceptionCheck(env)) {
				//duk_pop_2(ctx);
				print_context("Got an exception while sending a message", ctx);
				return 0;
			}
			if (jresponse != NULL) {
				const char *response = (*env)->GetStringUTFChars(env, jresponse, 0);
				//Now it is time to call the onreceive
				duk_push_this(ctx); // [this]
				duk_get_prop_string(ctx, -1, "onreceive"); // [this this.onreceive]
				duk_swap(ctx, -1, -2);
				duk_push_string(ctx, response);
				duk_call_method(ctx, 1);
				duk_pop(ctx);
				(*env)->ReleaseStringUTFChars(env, jresponse, response);
			}
			duk_pop(ctx);
			return 1;
		}
	}
	duk_pop_2(ctx);  // pop global
	print_context("Fail to send a message, checking stack", ctx);
	return 0;
}

duk_ret_t tcp_socket_send(duk_context *ctx) {
	const char *msg = duk_require_string(ctx, 0);
	duk_push_this(ctx); // [this]

	//Check if the socket is connected
	duk_get_prop_string(ctx, -1, "status");
	duk_int_t status = duk_to_int(ctx, -1);
	duk_pop(ctx);
	duk_get_prop_string(ctx, -1, "CONNECTED");
	duk_int_t connected_status = duk_to_int(ctx, -1);
	duk_pop(ctx);
	// Get out if the socket is not connected
	if (status != connected_status) {
		//print_context("Could not send because the tcp socket is not connected", ctx);
		duk_pop_n(ctx, 2);
		//print_context("Could not send because the tcp socket is not connected, but I just make sure that everything is now clean", ctx);
		return 0;
	}

	duk_get_prop_string(ctx, -1, "settings");
	duk_get_prop_string(ctx, -1, "id");
	duk_int_t id_socket = duk_to_int(ctx, -1);
	duk_ret_t res = jni_tcp_socket_send(ctx, id_socket, msg);
	duk_pop_n(ctx, 5);
	return res;
}

//This method should close the Java TCP socket on DuktapeJavascriptEngineWrapper with the unique identifier
int jni_tcp_socket_close(duk_context *ctx, int id_socket) {
	(void) duk_get_global_string(ctx, "JNIEnv");
	JNIEnv *env = (JNIEnv *)duk_require_pointer(ctx, -1);

	(void) duk_get_global_string(ctx, "DuktapeJavascriptEngineWrapper");
	jobject obj = (jobject) duk_require_pointer(ctx, -1);

	jclass objclass = (*env)->GetObjectClass(env, obj);
	if (objclass != NULL) {
		jmethodID mid = (*env)->GetMethodID(env, objclass, "tcpSocketClose", "(I)Z");
		if (mid != NULL) {
			jboolean jresponse = (*env)->CallBooleanMethod(env, obj, mid, id_socket);
			if (jresponse != JNI_TRUE) {
				//Now it is time to call the onreceive
				duk_push_this(ctx); // [this]
				duk_get_prop_string(ctx, -1, "onerror"); // [this this.onreceive]
				duk_swap(ctx, -1, -2);
				duk_push_int(ctx, 0);
				duk_push_string(ctx, "Could not close the socket");
				duk_call_method(ctx, 2);
				duk_pop(ctx);
			}
			duk_pop_2(ctx);
			return 1;
		}
	}
	duk_pop_2(ctx);  // pop global
	print_context("Fail to close, checking stack", ctx);
	return 0;
}

duk_ret_t tcp_socket_close(duk_context *ctx) {
	duk_push_this(ctx); // [this]

	//Check if the socket is disconnected
	duk_get_prop_string(ctx, -1, "status");
	duk_int_t status = duk_to_int(ctx, -1);
	duk_pop(ctx);
	duk_get_prop_string(ctx, -1, "DISCONNECTED");
	duk_int_t disconnected_status = duk_to_int(ctx, -1);
	duk_pop(ctx);
	// Get out if the socket is not disconnected
	if (status != disconnected_status) {
		//make the clean up
		duk_get_prop_string(ctx, -1, "settings");
		duk_get_prop_string(ctx, -1, "id");
		duk_int_t id_socket = duk_to_int(ctx, -1);
		duk_pop_2(ctx);
		int res;
		if ((res = jni_tcp_socket_close(ctx, id_socket))) {
			duk_push_null(ctx);
			duk_put_prop_string(ctx, -2, "settings");
		}
		duk_pop(ctx);
		return res;
	}
	duk_pop(ctx);
	return 1;
}


duk_ret_t tcp_socket(duk_context *ctx) {
	//Push the object to the stack
	duk_push_object(ctx);

	//Now I will push the constant
	char *constant[] = {"DISCONNECTED", "CONNECTED"};
	for (int i = 0; i < sizeof(constant)/sizeof(char*); i++) {
		duk_push_int(ctx, i);
		duk_put_prop_string(ctx, -2, constant[i]);
	}

	//Set the status to disconnected
	duk_get_prop_string(ctx, -1, "DISCONNECTED");
	duk_put_prop_string(ctx, -2, "status");

	//Register the function connect (address, port)
	duk_push_c_function(ctx, tcp_socket_connect, 2);
	duk_put_prop_string(ctx, -2, "connect");

	//Register the function send (msg)
	duk_push_c_function(ctx, tcp_socket_send, 1);
	duk_put_prop_string(ctx, -2, "send");

	//Register the function onreceive (msg)
	duk_push_c_function(ctx, tcp_socket_onreceive, 1);
	duk_put_prop_string(ctx, -2, "onreceive");

	//Register the function onerror (msg)
	duk_push_c_function(ctx, tcp_socket_onerror, 2);
	duk_put_prop_string(ctx, -2, "onerror");

	//Register the function close ()
	duk_push_c_function(ctx, tcp_socket_close, 0);
	duk_put_prop_string(ctx, -2, "close");

	return 1;
}

void register_tcp_socket(duk_context *ctx) {
	duk_push_global_object(ctx);
	duk_push_c_function(ctx, tcp_socket, 0);
	duk_put_prop_string(ctx, -2, "TCPSocket");
	duk_pop(ctx);
}
