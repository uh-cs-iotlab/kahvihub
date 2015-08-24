var LastCallLight = {

	needConfiguration: true,
	config: false,
	
	checkConfiguration: function(data) { 
		var config = JSON.parse(data);
		if (typeof(config.server) !== 'string') { return false; } 
		if (typeof(config.oneshot) !== 'boolean') { return false; }
		if (!config.oneshot && typeof(config.interval) !== 'number') { return false; }
		if (typeof(config.rooms) !== 'object') { return false; }
		if (typeof(config.rooms.length) !== 'number') { return false; }
		if (config.rooms.length === 0) { return false; }
		var index;
		for (index = 0; index < config.rooms.length; ++index) {
			var as = config.rooms[index];
			if (typeof(as.calendar) !== 'string') {return false; }
			if (typeof(as.lights) !== 'object') {return false; }
			if (typeof(as.lights.length) !== 'number') {return false; }
			if (as.lights.length === 0) { return false; }
			var index2;
			for (index2 = 0; index2 < as.lights.length; ++index2) {
				var light = as.lights[index2];
				if (typeof(light) !== 'string') {return false; }
			}
		}
		return true; 
	},
	
	configure: function(config) { 
		if (this.checkConfiguration(config)) {
			this.config = JSON.parse(config);
		}
	},
	
	rfc3339ToData: function(time) {
		return new Date(time.replace("T", " "));
	},
	
	dimTheLights: function (server, lights, lum) {
		var index;
		for (index = 0; index < lights.length; ++index) {
			var xhr = XMLHttpRequest();
			var url = server + '/feeds/' + lights[index];
			xhr.open('POST', url, true);
			xhr.onreadystatechange = function (event) {};
			var light = {light: { luminosity: lum, fade: 200}};
			xhr.send(JSON.stringify(light));
		}
	},
	
	needToDim: function(periods) {
		var index;
		for (index = 0; index < periods.length; ++index) {
			var now = new Date();
			var start = this.rfc3339ToData(periods[index].period.start.date.time);
			var end = this.rfc3339ToData(periods[index].period.end.date.time);
			if (start < now && end > now) {
				return true;
			}	
		}
		return false;
	},
	
	checkRooms: function () {
		if (!this.config) return;
		var rooms = this.config.rooms;
		var index = 0;
		for (index = 0; index < rooms.length; ++index) {
			var calendar = rooms[index].calendar;
			var xhr = XMLHttpRequest();
			var res;
			var url = this.config.server + '/feeds/' + calendar;
			xhr.open('GET', url, true);
			xhr.onreadystatechange = function (event) { if (xhr.status == 200) { res = JSON.parse(xhr.responseText); }};
			xhr.send(null);
			if (res) {
				if (this.needToDim(res)) {
					setTimeout(this.dimTheLights, 0, this.config.server, rooms[index].lights, 10);
					setTimeout(this.dimTheLights, 200, this.config.server, rooms[index].lights, 100);
				}
			}
		}
	},
	
	run: function () {
		if (this.config.oneshot) {
			this.checkRooms();
		}
		else {
			setInterval(this.checkRooms, this.config.interval);
		}
	}
	
	
};