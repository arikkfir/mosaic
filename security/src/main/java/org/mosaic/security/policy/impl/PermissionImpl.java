package org.mosaic.security.policy.impl;

import com.google.common.base.Splitter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;
import org.mosaic.security.policy.Permission;
import org.mosaic.security.policy.PermissionParseException;
import org.mosaic.security.policy.PermissionPoliciesManager;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

/**
 * @author arik
 */
public class PermissionImpl implements Permission
{
    private static final Splitter COLON_SPLITTER = Splitter.on( ":" ).trimResults();

    private static final Splitter COMMA_SPLITTER = Splitter.on( "," ).trimResults();

    @Nonnull
    private final PermissionPoliciesManager permissionPoliciesManager;

    @Nonnull
    private final List<List<String>> tokens;

    public PermissionImpl( @Nonnull PermissionPoliciesManager permissionPoliciesManager, @Nonnull String string )
    {
        this.permissionPoliciesManager = permissionPoliciesManager;

        // parse into a token list
        List<List<String>> tokens = new LinkedList<>();
        for( String token : COLON_SPLITTER.split( string ) )
        {
            // reject empty tokens
            if( token.isEmpty() )
            {
                throw new PermissionParseException( "Permission '" + string + "' is illegal - contains an empty token" );
            }

            // parse sub-tokens
            List<String> subTokens = new LinkedList<>();
            for( String subToken : COMMA_SPLITTER.split( token ) )
            {
                // reject empty sub-tokens
                if( subToken.isEmpty() )
                {
                    throw new PermissionParseException( "Permission '" + string + "' is illegal - contains an empty sub-token" );
                }
                subTokens.add( subToken );
            }

            // if this token contains the "*" sub token - no need for other sub tokens (it implies everything anyway)
            if( subTokens.contains( "*" ) )
            {
                // add "*" but stop here - no point moving on since we allow everything down the road
                tokens.add( asList( "*" ) );
                break;
            }
            else
            {
                tokens.add( subTokens );
            }
        }

        // save as an unmodifiable list
        this.tokens = unmodifiableList( tokens );
    }

    @Override
    public boolean implies( @Nonnull String childPermissionString )
    {
        return implies( this.permissionPoliciesManager.parsePermission( childPermissionString ) );
    }

    @Override
    public boolean implies( @Nonnull Permission childPermission )
    {
        // iterate in parallel our tokens and the child's tokens
        Iterator<List<String>> myIterator = this.tokens.iterator();
        Iterator<List<String>> childIterator = childPermission.getTokens().iterator();
        while( true )
        {
            // if we don't have more tokens (we are shorter), child is implied; otherwise, get the next token
            if( !myIterator.hasNext() )
            {
                return true;
            }
            List<String> mySubTokens = myIterator.next();

            // if child has no more tokens (child is shorter), child is NOT implied; otherwise, get next child token
            if( !childIterator.hasNext() )
            {
                return false;
            }
            List<String> childSubTokens = childIterator.next();

            // if our sub tokens contain "*", we're good, move on to the next token
            if( mySubTokens.contains( "*" ) )
            {
                continue;
            }

            // both we and child have another token - compare
            for( String childSubToken : childSubTokens )
            {
                if( !mySubTokens.contains( childSubToken ) )
                {
                    // we don't allow this sub token :(
                    return false;
                }
            }
        }
    }

    @Override
    public boolean impliedBy( @Nonnull String parentPermissionString )
    {
        return impliedBy( this.permissionPoliciesManager.parsePermission( parentPermissionString ) );
    }

    @Override
    public boolean impliedBy( @Nonnull Permission parentPermission )
    {
        return parentPermission.implies( this );
    }

    @Nonnull
    @Override
    public List<List<String>> getTokens()
    {
        return this.tokens;
    }
}
