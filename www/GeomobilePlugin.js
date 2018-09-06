var exec = require('cordova/exec');
var channel = require('cordova/channel');

function GeomobilePlugin() {
	this.eventCallback = null;
	
	this.fireEvent = function(eventName, data){
		if(this.eventCallback){
			this.eventCallback(data);
		}
	};
}


//Clients
GeomobilePlugin.prototype.open = function(data, callback, onSuccess, onError) {
	this.eventCallback = callback;

    exec(onSuccess, onError, "GeomobilePlugin","openGeoMobile",[data]);
};

GeomobilePlugin.prototype.close = function(onSuccess, onError) {
    exec(onSuccess, onError, "GeomobilePlugin","closeGeoMobile",[]);
};

module.exports = new GeomobilePlugin();