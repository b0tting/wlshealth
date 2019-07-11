<!doctype html>
<html lang="en">
<head>
    <!-- Required meta tags -->
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">

    <!-- Bootstrap CSS -->
    <link rel="stylesheet" href="static/css/bootstrap.min.css">

    <title>LB config</title>
</head>
<body>
<div class="container">
    <div class="jumbotron">
        <div class="container">
            <h3 class="display-3">ZZHealth load balancer config</h3>
            <p>This page shows you the servers in the current WebLogic domain and which have the load balancer URL enabled (<i>/zzwlshealth/health/lb</i>). The goal
            of this page is to enable or disable this check allowing you to tell the load balancer to remove servers from the load balancer pool.</p>
            <p><a class="btn btn-primary btn-lg" href="https://github.com/b0tting/wlshealth" role="button">Learn more &raquo;</a></p>
        </div>
    </div>
    <div class="row" id="serverlist"></div>
</div>

<!-- Optional JavaScript -->
<!-- jQuery first, then Popper.js, then Bootstrap JS -->
<script src="static/js/jquery-3.4.1.min.js"></script>
<script src="static/js/popper.min.js"></script>
<script src="static/js/bootstrap.min.js"></script>
<script>
    var BASEURL = "health/";
    var ENABLEURL = BASEURL + "enable";
    var DISABLEURL = BASEURL + "disable";
    var INFOURL = BASEURL + "info";

    var STATE_ENABLED = "0";
    var STATE_DISABLED = "1";
    var STATE_NO_CHECK = "2";
    var STATE_NO_SERVER = "3";
    var STATE_UNKNOWN = "4";
    var STATE_TIMEOUT = "5";

    var serversJson;

    // Javacript dicts zijn geen python dicts maar objecten. Ik mag de variabele niet als key gebruiken dus. Dan maar zo.
    var helpText = {
        "0": "Server is running and accepting load balancer requests",
        "1": "Server is running but not accepting load balancer requests",
        "2": "This server is not running the load balancer health check",
        "3": "Server is not running",
        "4": "An unknown error occurred checking this server",
        "5": "A timeout occurred checking this server"
    }

    function drawServerCard(server) {
        card = "<div class='col-sm-3'>";
        card += "<div class='card' id='card_"+ server.name+"'>";
        card += "    <div class='card-header'>"+ server.name + "</div>";
        card += "    <div class='card-body'>";
        card += "       <h5 class='card-title'>"+ server.host + ":" + server.port +"</h5>";
        card += "       <p class='card-text' data-role='helptext'></p>";
        card += "       <button class='btn btn-primary' id='" + server.name + "'>Toggle server</button>";
        card += "    </div>";
        card += "</div>";
        card += "</div>";
        return card;
    }

    function heartBeat() {
        $.getJSON( INFOURL , function( data ) {
            serversJson = data;
            serverList = $("#serverlist");
            if(serverList.is(':empty')) {
                $.each(serversJson, function(key,val){
                    serverList.append(drawServerCard(val));
                    $("#" + key).click(function() {
                        serverButton = $(this);
                        server = serversJson[serverButton.attr('id')];
                        url = server.state == STATE_ENABLED ? server.disableURL : server.enableURL;
                        $.get( url ).done(heartBeat());
                    })
                });
            }

            $.each(serversJson, function(name, server){
                serverCard = $("#card_" + name);
                serverButton = $("#" + name);
                serverCard.find("[data-role='helptext']").text(helpText[server.state]);
                                switch(server.state) {
                    case STATE_ENABLED:
                        serverCard.removeClass("bg-light bg-danger bg-warning text-black").addClass("bg-success text-white");
                        serverButton.attr("disabled", false).text("Disable");;
                        break;
                    case STATE_DISABLED:
                        serverCard.removeClass("bg-success bg-light bg-danger text-black").addClass("bg-warning text-white");
                        serverButton.attr("disabled", false).text("Enable");
                        break;
                    case STATE_NO_SERVER:
                    case STATE_UNKNOWN:
                    case STATE_TIMEOUT:
                        serverCard.removeClass("bg-success bg-light bg-warning text-white").addClass("bg-danger text-black");
                        serverButton.attr("disabled", true).text("Disabled");;
                        break;
                    case STATE_NO_CHECK:
                        serverCard.removeClass("bg-success bg-danger bg-warning text-white").addClass("bg-light text-black");
                        serverButton.attr("disabled", true).text("Disabled");
                        break;
                }
            })
        });
    }

    $(function() {
        document.title = window.location.hostname + " " + document.title;
        heartBeat();
        setInterval(heartBeat, 1500);
    });
</script>
</body>
</html>