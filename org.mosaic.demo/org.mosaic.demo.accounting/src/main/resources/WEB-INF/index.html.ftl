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

    <!-- ======================================================  -->
    <!-- NAVIGATION BAR                                          -->
    <!-- ======================================================  -->
    <div class="navbar navbar-default navbar-fixed-top" role="navigation">
        <div class="container">

            <!-- BRAND -->
            <div class="navbar-header">
                <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
                    <span class="sr-only">Toggle navigation</span>
                    <span class="icon-bar"></span>
                    <span class="icon-bar"></span>
                    <span class="icon-bar"></span>
                </button>
                <a class="navbar-brand" href="#">Mosaic Accounting</a>
            </div>

            <!-- LEFT MENU -->
            <div class="navbar-collapse collapse">
                <ul class="nav navbar-nav">
                    <li><a href="#">Overview</a></li>
                    <li><a href="#/expenses">Expenses</a></li>
                    <li><a href="#/income">Income</a></li>
                    <li class="dropdown">
                        <a href="#" class="dropdown-toggle" data-toggle="dropdown">Reports <b class="caret"></b></a>
                        <ul class="dropdown-menu">
                            <li><a href="#/report1">Report 1</a></li>
                            <li><a href="#/report2">Report 2</a></li>
                            <li class="divider"></li>
                            <li><a href="#/report3">Report 3</a></li>
                        </ul>
                    </li>
                </ul>
                <ul class="nav navbar-nav navbar-right">
                    <li><a href="#/about">About</a></li>
                    <li><a href="https://github.com/arikkfir/mosaic">Mosaic</a></li>
                </ul>
            </div>
        </div>
    </div>

    <div id="dynamic-container" class="container"></div>

    <script src="lib/jquery-2.0.3.min.js"></script>
    <script src="lib/bootstrap/js/bootstrap.min.js"></script>
    <script src="lib/path.min.js"></script>
    <script src="/index.js"></script>
</body>
</html>
