"use strict";

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
