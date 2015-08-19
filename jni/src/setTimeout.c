#include "duktape.h"

extern void print_context(const char *prefix, duk_context *ctx);

void register_settimeout(duk_context *ctx) {

	const char *setTimeoutEvalScript =
			"function setTimeout(func, delay) {var cb_func;var bind_args;var timer_id;if (typeof delay !== 'number') {throw new TypeError('delay is not a number');}if (typeof func === 'string') {cb_func = eval.bind(this, func);}else if (typeof func !== 'function') {throw new TypeError('callback is not a function/string');}else if (arguments.length > 2) bind_args = Array.prototype.slice.call(arguments, 2);bind_args.unshift(this);cb_func = func.bind.apply(func, bind_args);} else {cb_func = func;} timer_id = EventLoop.createTimer(cb_func, delay, true);return timer_id;}";
	duk_eval_string(ctx, setTimeoutEvalScript);
	duk_pop(ctx);

	const char *clearTimeoutEvalScript =
			"function clearTimeout(timer_id) {if (typeof timer_id !== 'number') {throw new TypeError('timer ID is not a number');}var success = EventLoop.deleteTimer(timer_id);}";
	duk_eval_string(ctx, clearTimeoutEvalScript);
	duk_pop(ctx);

	const char *setIntervalEvalScript =
			"function setTimeout(func, delay) {var cb_func;var bind_args;var timer_id;if (typeof delay !== 'number') {throw new TypeError('delay is not a number');}if (typeof func === 'string') {cb_func = eval.bind(this, func);}else if (typeof func !== 'function') {throw new TypeError('callback is not a function/string');}else if (arguments.length > 2) {bind_args = Array.prototype.slice.call(arguments, 2);bind_args.unshift(this);cb_func = func.bind.apply(func, bind_args);} else {cb_func = func;} timer_id = EventLoop.createTimer(cb_func, delay, false);return timer_id;}";
	duk_eval_string(ctx, setIntervalEvalScript);
	duk_pop(ctx);

	const char *clearIntervalEvalScript =
			"function clearInterval(timer_id) {if (typeof timer_id !== 'number') {throw new TypeError('timer ID is not a number');}EventLoop.deleteTimer(timer_id);}";
	duk_eval_string(ctx, clearIntervalEvalScript);
	duk_pop(ctx);

	const char *requestEventLoopExit =
			"function requestEventLoopExit() {EventLoop.requestExit();}";
	duk_eval_string(ctx, requestEventLoopExit);
	duk_pop(ctx);

	const char *socketHandling =
			"EventLoop.socketListening = {};EventLoop.socketReading = {};EventLoop.socketConnecting = {};";
	duk_eval_string(ctx, socketHandling);
	duk_pop(ctx);

	const char *fdPollHandler =
			"EventLoop.fdPollHandler = function(fd, revents) {var data;var cb;var rc;var acc_res;if (revents & Poll.POLLIN) {cb = this.socketReading[fd];if (cb) {data = Socket.read(fd);if (data.length === 0) {this.close(fd);return;}cb(fd, data);} else {cb = this.socketListening[fd];if (cb) {acc_res = Socket.accept(fd);cb(acc_res.fd, acc_res.addr, acc_res.port);}}}if (revents & Poll.POLLOUT) {cb = this.socketConnecting[fd];if (cb) {delete this.socketConnecting[fd];cb(fd);}}if ((revents & ~(Poll.POLLIN | Poll.POLLOUT)) !== 0) {this.close(fd);}}";
	duk_eval_string(ctx, fdPollHandler);
	duk_pop(ctx);

	const char *rest1 =
			"EventLoop.server = function(address, port, cb_accepted) {var fd = Socket.createServerSocket(address, port);this.socketListening[fd] = cb_accepted;this.listenFd(fd, Poll.POLLIN);}";
	duk_eval_string(ctx, rest1);
	duk_pop(ctx);

	const char *rest2 =
			"EventLoop.connect = function(address, port, cb_connected) {var fd = Socket.connect(address, port);this.socketConnecting[fd] = cb_connected;this.listenFd(fd, Poll.POLLOUT);}";
	duk_eval_string(ctx, rest2);
	duk_pop(ctx);

	const char *rest3 =
			"EventLoop.close = function(fd) {EventLoop.listenFd(fd, 0);delete this.socketListening[fd];delete this.socketReading[fd];delete this.socketConnecting[fd];Socket.close(fd);}";
	duk_eval_string(ctx, rest3);
	duk_pop(ctx);

	const char *rest4 =
			"EventLoop.setReader = function(fd, cb_read) {this.socketReading[fd] = cb_read;this.listenFd(fd, Poll.POLLIN);}\n";
	duk_eval_string(ctx, rest4);
	duk_pop(ctx);

	const char *rest5 =
			"EventLoop.write = function(fd, data) {var rc = Socket.write(fd, Duktape.Buffer(data));}";
	duk_eval_string(ctx, rest5);
	duk_pop(ctx);
}
