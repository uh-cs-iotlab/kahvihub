var GCalPlugin = { 
	
	needConfiguration: true,
	
	checkConfiguration: function(data) { 
		var config = JSON.parse(data);
		if (typeof(config.server) !== 'string') { return false; } 
		if (typeof(config.calId) !== 'string') { return false; } 
		if (typeof(config.calKey) !== 'string') { return false; }
		return true; 
	},
	
	configure: function(config) { 
		if (this.checkConfiguration(config)) {this.config = JSON.parse(config);}
	},
	
	isFeatureSupported: function(name) { return (this.config && name == "events"); },
	isFeatureAvailable: function(name) { return (this.config && name == "events"); },
	isFeatureReadable: function(name) { return (this.config && name == "events"); },
	isFeatureWritable: function(name) { return false; },
	getNumberOfFeatures: function() { return 1; },
	postFeatureValue: function(name, data) { return false; },
	getFeatureDescription: function(index) { 
		if (index == 0) { 
			return JSON.stringify({name: "events", type: "TimePeriod"});
		} else {return null;}
	},
	
	getFeatureValue: function (name) {
		if (!this.config) { return null; }
		if (name !== "events") { return null; }
		var url = this.config.server + "/calendar/v3/calendars/" + this.config.calId + "/events?key=" + this.config.calKey;
		var xhr = XMLHttpRequest();
		var res;
		xhr.open('GET', url, true);
		xhr.onreadystatechange = function (event) { if (xhr.status == 200) { res = xhr.responseText; }};
		xhr.send(null);
		var processResults = function (results) {
			var jres = JSON.parse(results);
			var array = [];
			if (typeof(jres.items) !== 'undefined') {
				var index;
				for	(index = 0; index < jres.items.length; index++) {
					var item = jres.items[index];
					var s = {date: {time: item.start.dateTime, format: "RFC 3339"}};
					var e = {date: {time: item.end.dateTime, format: "RFC 3339"}};
					array[array.length] = {period: {start: s, end: e}};
				}
			}
			return array;
		};
		return JSON.stringify(processResults(res));
	}
	
};
