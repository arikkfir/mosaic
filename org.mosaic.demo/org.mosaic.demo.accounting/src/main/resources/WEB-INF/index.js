"use strict";

/**
 * Routes the UI to the About Us page.
 */
function home() {
    alert( "Home!" );
}
function about() {
    alert( "Learn more!?" );
}

// setup application routes
Path.map( "#" ).to( home );
Path.map( "#/about" ).to( about );
Path.root( "#" );

// handle unknown routes
Path.rescue( function() {
    alert( "404: Route Not Found" );
} );

// start PathJS
$( document ).ready( function() {
    Path.listen();
} );

/**
 * Performs logout
 */
function logout() {
    // TODO arik: implement
}

function testMarshalling() {
    $.getJSON( "/hello" )
        .done( function( data ) {
            console.log( "success" );
        } )
        .fail( function() {
            console.log( "error" );
        } )
        .always( function() {
            console.log( "complete" );
        } );
}
