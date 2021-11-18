var exec = require('cordova/exec');

exports.startNetPerformData = function (arg0, success, error) {
    exec(success, error, 'NetPerformance', 'startNetPerformData', [arg0]);
};

exports.stopNetPerformData = function (success, error) {
    exec(success, error, 'NetPerformance', 'stopNetPerformData', []);
};

exports.requestRequiredPermission = function (success, error) {
    exec(success, error, 'NetPerformance', 'requestRequiredPermission',[]);
};

exports.enableGPSDialog = function (success, error) {
    exec(success, error, 'NetPerformance', 'enableGPSDialog',[]);
};

