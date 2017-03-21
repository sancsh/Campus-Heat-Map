/*
 * Server.js
 * 
 * The main portion of this project. Contains all the defined routes for express,
 * rules for the websockets, and rules for the MQTT broker.
 * 
 * Refer to the portions surrounded by --- for points of interest
 */
var express   = require('express'),
	app       = express();
var pug       = require('pug');
var sockets   = require('socket.io');
var path      = require('path');
var bodyParser = require('body-parser');


var conf      = require(path.join(__dirname, 'config'));
var internals = require(path.join(__dirname, 'internals'));
var threshold = 5;
var localStorage = require('local-storage');
			
// var server;
// var io;
// -- Setup the application
setupSocket();
setupExpress();


function setupSocket(){
// setup socket
var server = require('http').createServer(app);
var io = sockets(server);

// Setup the internals
internals.start(mqtt => {
		io.on('connection', socket => {
			socket_handler(socket, mqtt);
	});
});

server.listen(conf.PORT, conf.HOST, () => { 
	console.log("Listening on: " + conf.HOST + ":" + conf.PORT);
});

}




// -- Socket Handler
// Here is where you should handle socket/mqtt events
// The mqtt object should allow you to interface with the MQTT broker through 
// events. Refer to the documentation for more info 
// -> https://github.com/mcollina/mosca/wiki/Mosca-basic-usage
// ----------------------------------------------------------------------------
function socket_handler(socket, mqtt) {
	// Called when a client connects
	mqtt.on('clientConnected', client => {
	console.log("inside client connected");
		socket.emit('debug', {
			type: 'CLIENT', msg: 'New client connected: ' + client.id
		});
	});

	// Called when a client disconnects
	mqtt.on('clientDisconnected', client => {
		console.log("inside client disconnected");
		socket.emit('debug', {
			type: 'CLIENT', msg: 'Client "' + client.id + '" has disconnected'
		});
	});

	

	
	// Called when a client publishes data
	mqtt.on('published', (data, client) => {
			
			//var x = topic;
		if (!client) return;
		//console.log("data is" +data.topic);

		console.log("inside client published"+data+"  --client:"+ ""+client);
			var local_topic = ""+data.topic;
			console.log("incoming topic is: "+local_topic);

		console.log("inside locl storage:"+localStorage.get(local_topic));			

		
		if(localStorage.get(local_topic) == null)
		{
			console.log("local storage is null");
					localStorage.set(local_topic, 2);
		}
		
	else
	{
	
		console.log("count is " + localStorage.get(local_topic));
		
		localStorage.set(local_topic , localStorage.get(local_topic)+1);
			
			if(localStorage.get(local_topic) >threshold){
				
				//Message for above the threshold value
				var newTopic = "ifAboveThreshold_"+local_topic;
				console.log("new topic:"+newTopic);
				var message = {
				topic : newTopic,
				payload : 'red',
				qos :0,
				retain: false
			};			

				console.log("message to be publshed "+message);

				mqtt.publish(message, function(){
				console.log("local storage published");
			});
			}
	}	

		console.log("current count for buttom:"+localStorage.get(local_topic));
						
		 socket.emit('debug', {
 			type: 'PUBLISH', 
 			msg: 'Client "' + client.id + '" published "' + JSON.stringify(data) + '"',
 			key: local_topic,
 			payload: localStorage.get(local_topic)
 		});
	console.log("done!");

});

	// Called when a client subscribes
	mqtt.on('subscribed', (topic, client) => {
		console.log("inside client subscribed");
		console.log("subscribed to topic"+topic+"");
		if (!client) return;

		socket.emit('debug', {
			type: 'SUBSCRIBE',
			msg: 'Client "' + client.id + '" subscribed to "' + topic + '"'
		});
	});

	// Called when a client unsubscribes
	mqtt.on('unsubscribed', (topic, client) => {
		console.log("inside client unsub");
		if (!client) return;

		socket.emit('debug', {
			type: 'SUBSCRIBE',
			msg: 'Client "' + client.id + '" unsubscribed from "' + topic + '"'
		});
	});
}
// ----------------------------------------------------------------------------


// Helper functions
function setupExpress() {
	app.set('view engine', 'pug'); // Set express to use pug for rendering HTML

	// Setup the 'public' folder to be statically accessable
	var publicDir = path.join(__dirname, 'public');
	app.use(express.static(publicDir));

	// Setup the paths (Insert any other needed paths here)
	app.use(bodyParser.json()); // support json encoded bodies
	app.use(bodyParser.urlencoded({ extended: true }));
	//var listener = io.listen(server);
	// ------------------------------------------------------------------------
	
	// Home page
	app.get('/', (req, res) => {
		res.render('index', {title: 'MQTT Tracker'});
	});
	
	app.post('/', (req, res) => {
		console.log("Receving android request");
		var key = Object.keys(req.body)[0];	
		var bo = JSON.parse(key);

		//create a map between beacon area index from android and mac address of mbed
		//example , id area = 1, localStroage.set(mac1,1);
		var areaIndex = bo.beaconIndex;


		localStorage.set('area',bo.beaconIndex); 
			console.log("post local obj: "+localStorage.get('area'));
	});
	
	app.get('/poll',function(req,res){
		//console.log("local obj: "+localStorage.get('area'));
		if(localStorage.get('area')!=null)
			{
				//console.log("not null!");
				res.send(localStorage.get('area'));
			}
		else{
			console.log("Empty local storage");
			res.send("-1");
		}
	});

	
	// Basic 404 Page
	app.use((req, res, next) => {
		var err = {
			stack: {},
			status: 404,
			message: "Error 404: Page Not Found '" + req.path + "'"
		};

		// Pass the error to the error handler below
		next(err);
	});

	// Error handler
	app.use((err, req, res, next) => {
		console.log("Error found: ", err);
		res.status(err.status || 500);

		res.render('error', {title: 'Error', error: err.message});
	});
	// ------------------------------------------------------------------------

	// Handle killing the server
	process.on('SIGINT', () => {
		internals.stop();
		process.kill(process.pid);
	});
}
