<!DOCTYPE html>
<html lang="en">
<head>
    <title>Mosaic Accounting</title>
    <meta name="description" content="Mosaic accounting application">
    <meta name="author" content="Arik Kfir">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="icon">
    <link href="lib/bootstrap/css/bootstrap.min.css" rel="stylesheet">
    <link href="accounting.css" rel="stylesheet">
</head>
<body>

    <nav class="navbar navbar-default" role="navigation">

        <div class="navbar-header">
            <button type="button" class="navbar-toggle" data-toggle="collapse" data-target="#top-navbar-collapse-1">
                <span class="sr-only">Navigation</span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
            </button>
            <a class="navbar-brand" href="#">Mosaic Accounting</a>
        </div>

        <div class="collapse navbar-collapse" id="top-navbar-collapse-1">

        <#if subject.authenticated>
            <ul class="nav navbar-nav navbar-right">
                <li class="dropdown">
                    <a href="#" class="dropdown-toggle" data-toggle="dropdown"><span class="glyphicon glyphicon-cog"></span></a>
                    <ul class="dropdown-menu">
                        <li><a href="#" onclick="testMarshalling()">Something else here</a></li>
                        <li class="divider"></li>
                        <li><a href="/admin">Administration</a></li>
                        <li class="divider"></li>
                        <li><a href="#" onclick="logout()">Log-out</a></li>
                    </ul>
                </li>
            </ul>

            <form class="navbar-form navbar-right" role="search" action="/search">
                <div class="form-group">
                    <input type="text" class="form-control" name="q" placeholder="Search">
                </div>
            </form>
        <#else>
            <form class="navbar-form navbar-right" role="form" action="/login" method="post">
            <div class="form-group">
                    <input type="text" class="form-control" name="username" placeholder="Username">
                </div>
                <div class="form-group">
                    <input type="password" class="form-control" name="password" placeholder="Password">
                </div>
                <button type="submit" class="btn btn-primary">Login</button>
            </form>
        </#if>

        </div>
    </nav>

    <div class="jumbotron">
        <div class="container">
            <h1>Mosaic Accounting</h1>
            <p>Mosaic Accounting is an application for personal accounting made easy!</p>
            <p><a class="btn btn-primary btn-lg" role="button" href="#/about">Learn more &raquo;</a></p>
        </div>
    </div>

    <div class="container">
        <div class="row">
            <div class="col-md-4">
                <h2>Heading</h2>
                <p>Donec id elit non mi porta gravida at eget metus. Fusce dapibus, tellus ac cursus commodo, tortor mauris condimentum nibh, ut fermentum massa justo sit amet risus. Etiam porta sem malesuada magna mollis euismod. Donec sed odio dui. </p>
                <p><a class="btn btn-default" href="#" role="button">View details &raquo;</a></p>
            </div>
            <div class="col-md-4">
                <h2>Heading</h2>
                <p>Donec id elit non mi porta gravida at eget metus. Fusce dapibus, tellus ac cursus commodo, tortor mauris condimentum nibh, ut fermentum massa justo sit amet risus. Etiam porta sem malesuada magna mollis euismod. Donec sed odio dui. </p>
                <p><a class="btn btn-default" href="#" role="button">View details &raquo;</a></p>
            </div>
            <div class="col-md-4">
                <h2>Heading</h2>
                <p>Donec sed odio dui. Cras justo odio, dapibus ac facilisis in, egestas eget quam. Vestibulum id ligula porta felis euismod semper. Fusce dapibus, tellus ac cursus commodo, tortor mauris condimentum nibh, ut fermentum massa justo sit amet risus.</p>
                <p><a class="btn btn-default" href="#" role="button">View details &raquo;</a></p>
            </div>
        </div>

        <hr>

        <footer>
            <p>&copy; Arik Kfir 2013</p>
        </footer>
    </div>

    <script src="lib/jquery-2.0.3.min.js"></script>
    <script src="lib/bootstrap/js/bootstrap.min.js"></script>
    <script src="lib/path.min.js"></script>
    <script src="/index.js"></script>
</body>
</html>
