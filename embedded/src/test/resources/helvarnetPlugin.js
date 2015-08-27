var HelvarnetPlugin = {
	
	needConfiguration: true,
	config: null,
	socket: null,
	
	checkConfiguration: function(data) { 
		var config = JSON.parse(data);
		if (typeof(config.address) !== 'string') { return false; } 
		if (typeof(config.port) !== 'number') { return false; }
		return true; 
	},
	
	getAnswers: function (answer) {
		if(answer.slice(1) === "!") { return []; }
		var indexEqual = answer.lastIndexOf("=");
		var sub = answer.slice(indexEqual + 1, -1);
		return sub.split(",");
	},
	
	getFeatureType: function (feature, address, port) {
		print('getFeatureType: ' + feature + ', address: ' + address + ', port: ' + port);
		if (!this.socket) {
			this.socket = TCPSocket();
			this.socket.connect(address, port);
		}
		else if (this.socket.status === this.socket.DISCONNECTED) {
			this.socket.connect(address, port);
		}
		var t;
		var getAnswers = HelvarnetPlugin.getAnswers;
		this.socket.onreceive = function (msg) {
			var types = getAnswers(msg);
			if (types.length == 1) {t = types[0];};
		};
		this.socket.send(">V:2,C:104," + feature + "#");
		return t;
	},
	
	getFeaturesFromGroup: function (groupId, address, port) {
		print('getFeaturesFromGroup: ' + groupId + ', port: ' + port + ', address: ' + address);
		if (!this.socket) {
			this.socket = TCPSocket();
			this.socket.connect(address, port);
		}
		else if (this.socket.status === this.socket.DISCONNECTED) {
			this.socket.connect(address, port);
		}
		else {
			print('The socket looks fine');
		}
		var features = [];
		var getAnswers = HelvarnetPlugin.getAnswers;
		var getFeatureType = HelvarnetPlugin.getFeatureType;
		var feat_in_group;
		this.socket.onreceive = function (msg) { 
			feat_in_group = getAnswers(msg);
		};
		var command = ">V:2,C:164,G:" + groupId + "#";
		print(command);
		this.socket.send(command);
		if (feat_in_group) {
			var index;
			for(index = 0; index < feat_in_group.length; ++index) {
				var t = getFeatureType(feat_in_group[index], address, port);
				if (t == "1537") {
					var ft = {
						light: {
							luminosity: "number",
							fade: "number"
						}
					};
					var n = feat_in_group[index];
					features[features.length] = {name: n, type: ft};
				}
			}
		}
		return features;
	},
	
	initialiseFeatures: function (address, port) {
		print('initialiseFeatures');
		var features = {};
		if (!this.socket) {
			this.socket = TCPSocket();
			this.socket.connect(address, port);
		}
		else if (this.socket.status === this.socket.DISCONNECTED) {
			this.socket.connect(address, port);
		}
		var getAnswers = HelvarnetPlugin.getAnswers;
		var getFeaturesFromGroup = HelvarnetPlugin.getFeaturesFromGroup;
		var groups;
		this.socket.onreceive = function (msg) {
			groups = getAnswers(msg);
		};
		this.socket.send(">V:2,C:165#");
		if (groups) {
			var index; 
			for(index = 0; index < groups.length; ++index) {
				var feats_in_group = getFeaturesFromGroup(groups[index], address, port);
				for (var i = 0; i < feats_in_group.length; i++) {
					features[feats_in_group[i].name] = feats_in_group[i].type;
				}
			}
		}
		return features;
	},
	
	checkDataType: function (dat, typ) {
		var res = true;
		for (var key in typ) {
			if (typeof(dat[key]) == 'undefined') { return false; }
			else if (typeof(dat[key]) == 'object') { 
				if (typeof(typ[key]) !== 'object') { return false; } 
				else { return res && this.checkDataType(dat[key], typ[key]); }
			}
			else { res = res && typeof(dat[key]) == typ[key]; }
		}
		return res;
	},
	
	configure: function(config) { 
		if (this.checkConfiguration(config)) {
			this.config = JSON.parse(config);
			if (typeof(this.config.features) == 'undefined') {
				this.config.features = this.initialiseFeatures(this.config.address, this.config.port);
			}
			makeConfigurationPersistant(JSON.stringify(this.config));
		}
	},
	
	isFeatureSupported: function(name) { return this.config.features[name] !== 'undefined'; },
	isFeatureAvailable: function(name) { return this.config.features[name] !== 'undefined'; },
	isFeatureReadable: function(name) { return false; },
	isFeatureWritable: function(name) { return this.config.features[name] !== 'undefined'; },
	
	getNumberOfFeatures: function() { 
		if (this.config.features) { 
			return Object.keys(this.config.features).length;
		} else { return 0; }
	},
	
	getFeatureValue: function(name) { return null; },
	
	getFeatureDescription: function(index) {
		if (this.config.features) { 
			var key = Object.keys(this.config.features)[index];
			var fd = {name: key, type: this.config.features[key]};
			return JSON.stringify(fd); 
		} else { return null; }
	},
	
	postFeatureValue: function (name, data) {
		if (!this.config || !this.config.features) { return false; }
		var fd = this.config.features[name];
		try {
			var jdata = JSON.parse(data);
			if (this.checkDataType(jdata, fd)) {
				var result = true;
				if (!this.socket) {
					this.socket = TCPSocket();
					this.socket.connect(address, port);
				}
				else if (this.socket.status === this.socket.DISCONNECTED) {
					this.socket.connect(address, port);
				}
				this.socket.onreceive = function (msg) {};
				var command = '>V:2,C:14,L:' + jdata.light.luminosity + ',F:' + jdata.light.fade + ',' + name + '#';
				this.socket.send(command);
				this.socket.close();
				return result;
			}
			return false;
		} catch (e) { return false; }
	}

};
