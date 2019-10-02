var parameters = {
    "apikey" : "",
    "url" : "https://www.*.jpg",
};

var watson = require('watson-developer-cloud');
var fs = require('fs');
var http = require('http');

var visual_recognition = new watson.VisualRecognitionV3({
    api_key: parameters.api_key,
    version_date: '2016-05-20'
});

visual_recognition.detectFaces(parameters, (err, response) =>{
    if (err) {
        console.log('error: ', err);
        if (typeof callback != 'undefined' && typeof callback == "function")
            return callback(err);
    }
    else{
        console.log(JSON.stringify(response, null, 2));
        if (typeof callback != 'undefined' && typeof callback == "function")
            return callback(response);
    }
});

