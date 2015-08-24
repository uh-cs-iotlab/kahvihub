var HelvarnetPlugin = {
	
	needConfiguration: true,
	config: null,
	
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
		var r_ftype = TCPSocket();
		var t;
		var getAnswers = HelvarnetPlugin.getAnswers;
		r_ftype.connect(address, port);
		r_ftype.onreceive = function (msg) {
			var types = getAnswers(msg);
			if (types.length == 1) {t = types[0];};
		};
		r_ftype.send(">V:2,C:104," + feature + "#");
		r_ftype.close();
		return t;
	},
	
	getFeaturesFromGroup: function (groupId, address, port) {
		var r_group = TCPSocket();
		var features = [];
		r_group.connect(address, port);
		var getAnswers = HelvarnetPlugin.getAnswers;
		var getFeatureType = HelvarnetPlugin.getFeatureType;
		r_group.onreceive = function (msg) { 
			var feat_in_group = getAnswers(msg);
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
		};
		var command = ">V:2,C:164,G:" + groupId + "#";
		r_group.send(command);
		r_group.close();
		return features;
	},
	
	initialiseFeatures: function (address, port) {
		var features = {};
		var r_groups = TCPSocket();
		r_groups.connect(address, port);
		var getAnswers = HelvarnetPlugin.getAnswers;
		var getFeaturesFromGroup = HelvarnetPlugin.getFeaturesFromGroup;
		r_groups.onreceive = function (msg) {
			var groups = getAnswers(msg);
			var index; 
			for(index = 0; index < groups.length; ++index) {
				var feats_in_group = getFeaturesFromGroup(groups[index], r_groups.settings.address, r_groups.settings.port);
				for (var i = 0; i < feats_in_group.length; i++) {
					features[feats_in_group[i].name] = feats_in_group[i].type;
				}
			}
		};
		r_groups.send(">V:2,C:165#");
		r_groups.close();
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
	
	getFeatureValue: function(name) { print('BBB'); return null; },
	
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
				var r_post = TCPSocket();
				r_post.connect(this.config.address, this.config.port);
				r_post.onreceive = function (msg) {};
				var command = '>V:2,C:14,L:' + jdata.light.luminosity + ',F:' + jdata.light.fade + ',' + name + '#';
				r_post.send(command);
				r_post.close();
				return result;
			}
			return false;
		} catch (e) { return false; }
	}

};
