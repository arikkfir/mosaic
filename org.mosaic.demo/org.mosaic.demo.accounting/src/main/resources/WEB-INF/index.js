"use strict";

/**
 * Performs a route to the specific target URL. This function will load the given URL into our page container.
 *
 * @param target the target URL
 */
function routeTo( target ) {
    var $dynamic = $( "#dynamic-container" );
    $dynamic.load( target, null, function( responseText, textStatus, XMLHttpRequest ) {
        // TODO: show error if any
        $dynamic.append( '<div class="footer"><p>&copy; Arik Kfir 2014</p></div>' );
    } );
}

/**
 * Creates a function which, when executed, will always route to the given target URL without any parameters.
 *
 * @param target the target URL
 * @returns a function object
 */
function createRouteFunction( target ) {
    return $.proxy( routeTo, this, target );
}

// setup application routes
Path.map( "" ).to( createRouteFunction( "/home.html" ) );
Path.map( "#" ).to( createRouteFunction( "/home.html" ) );
Path.map( "#/" ).to( createRouteFunction( "/home.html" ) );
Path.map( "#/about" ).to( createRouteFunction( "/about.html" ) );
Path.root( "#" );

// handle unknown routes
Path.rescue( function() {
    alert( "404: Route Not Found" );
} );

// start PathJS
$( document ).ready( function() {
    Path.dispatch( '#' );
    Path.listen();
} );
