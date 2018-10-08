var exec = require('cordova/exec');
var channel = require('cordova/channel');

function GeomobilePlugin() {
	this.eventsCallback = {};
	
	this.fireEvent = function(eventName, data){
		if(this.eventsCallback[eventName]){
			this.eventsCallback[eventName](data);
			
			exec(function(){console.log(eventName+" receivedMessage success");}, function(){console.log(eventName+" receivedMessage error");}, "GeomobilePlugin","receivedMessage",[eventName]);
		} else{
			console.log("GeomobilePlugin received event with no listener: ",eventName);
		}
	};
}


//Clients
GeomobilePlugin.prototype.open = function(data, callback, onSuccess, onError) {
	this.eventsCallback["com.gisbase.geomobile.updates"] = callback;

    exec(onSuccess, onError, "GeomobilePlugin","openGeoMobile",[data]);
};

GeomobilePlugin.prototype.close = function(onSuccess, onError) {
    exec(onSuccess, onError, "GeomobilePlugin","closeGeoMobile",[]);
};

GeomobilePlugin.prototype.registerListener = function(callback, onSuccess, onError) {
	this.eventsCallback["com.gisbase.geomobile.updates"] = callback;

    exec(onSuccess, onError, "GeomobilePlugin","registerListenerGeoMobile",[]);
};

GeomobilePlugin.prototype.unregisterListener = function() {
	this.eventsCallback["com.gisbase.geomobile.updates"] = null;
};

GeomobilePlugin.prototype.openWithoutData = function(callback, onSuccess, onError) {
	this.eventsCallback["com.gisbase.geomobile.updates"] = callback;

    exec(onSuccess, onError, "GeomobilePlugin","openWithoutDataGeoMobile",[]);
};

module.exports = new GeomobilePlugin();