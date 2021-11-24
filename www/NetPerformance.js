var exec = require('cordova/exec');

exports.startNetPerformData = function (phone,imei,brand,model, success, error) {
    args = {
        "KEY_PHONE" : phone,
        "KEY_IMEI" : imei,
        "KEY_BRAND" : brand,
        "KEY_MODEL" : model
    };
    exec(success, error, 'NetPerformance', 'startNetPerformData', [args]);
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

