var express = require('express');
var mysql = require('mysql');
var bodyParser = require("body-parser");
var app = express();

var path = require('path');
var formidable = require('formidable');
var fs = require('fs');



//app.use(bodyParser.urlencoded({extended: true}));
app.use(bodyParser.urlencoded());
app.use(bodyParser.json());

var appRouter = function(app) {

	app.get("/json", function(req, res) {
		
		var responseJson = {teste: "Testando json... Utilizando a extensão JSONView este resultado ficará organizado no Chrome."};
		res.json(responseJson);
		//return res.send(" **** json via get... ****");
	});
    

	app.post("/json", function(req, res) {
        
		console.log( req.body);
        
        var texto = JSON.stringify(req.body);
        
        var ini = texto.indexOf("-");
        var fim = texto.indexOf(",") - 2;
        var lat = texto.substring(ini, fim);
        
        texto = texto.substring(ini + 15, texto.length);

        ini = texto.indexOf("-");
        fim = texto.indexOf(",") - 2;
        var lon = texto.substring(ini, fim);
        
        texto = texto.substring(ini + 15, texto.length);

        var temTexto = texto.indexOf(":\"\"}");
        var vTexto = "";
        
        if( temTexto == -1 ){
            ini = texto.indexOf(":") + 3;
            fim = texto.indexOf("\"}") -1;
            vTexto = texto.substring(ini, fim);
        }
        
        
        var database = 'sqldb';
        var table = 'ponto';

        var client = mysql.createConnection({
            user: 'root',
            password: 'root',
            host: 'localhost',
            port: 3306
        });
        
        
        client.query('USE ' + database);

        client.query('INSERT INTO ' + table + ' ' +
                     'SET latitude = ?, longitude = ?, texto =?', 
                     [lat, lon, vTexto ]);
        
          return res.json(req.body);

	});
    
    app.post("/foto", function(req, res){
        
                
          var form = new formidable.IncomingForm();
          form.multiples = true;
          form.uploadDir = path.join(__dirname, '/uploads');
          form.on('file', function(field, file) {
            fs.rename(file.path, path.join(form.uploadDir, file.name));
          });

          form.on('error', function(err) {
            console.log('An error has occured: \n' + err);
          });

          form.on('end', function() {
            res.end('success');
          });

          form.parse(req);

        
    });
	
}

module.exports = appRouter;
