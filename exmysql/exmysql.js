var express = require('express');
var mysql = require('mysql');
var bodyParser = require("body-parser");
var app = express();

var port = process.env.PORT || 8000;

app.use(bodyParser.urlencoded({extended: true}));
app.use(bodyParser.json());
app.use(express.static(__dirname + '/public'));

var routes = require("./routes/routes.js")(app);

app.listen( port, function(){
    console.log("Server PSD utilizando porta %s...", port);
});

