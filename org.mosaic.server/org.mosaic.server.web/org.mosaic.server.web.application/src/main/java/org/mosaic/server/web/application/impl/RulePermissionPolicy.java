package org.mosaic.server.web.application.impl;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.mosaic.security.PermissionPolicy;
import org.mosaic.security.RoleCredential;
import org.mosaic.security.User;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * @author arik
 */
public class RulePermissionPolicy implements PermissionPolicy {

    private static class CredentialTypeExpressionRule implements PermissionPolicy {

        private final String credentialType;

        private final Expression rule;

        private CredentialTypeExpressionRule( String credentialType, String rule ) {
            this.credentialType = credentialType;
            this.rule = new SpelExpressionParser().parseExpression( rule );
        }

        @Override
        public boolean permits( String operation, User.Credential... credentials ) {
            for( User.Credential credential : credentials ) {
                if( credential.getType().equalsIgnoreCase( this.credentialType ) ) {
                    EvaluationContext ctx = new StandardEvaluationContext( credential );
                    ctx.setVariable( "operation", operation );
                    ctx.setVariable( "op", operation );
                    if( this.rule.getValue( ctx, Boolean.class ) ) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private static class RolePermissionsRule implements PermissionPolicy {

        private final String roleName;

        private final Set<Pattern> permissions = new LinkedHashSet<>( 10 );

        private RolePermissionsRule( String roleName, Set<String> permissionPatterns ) {
            this.roleName = roleName;
            for( String pattern : permissionPatterns ) {
                this.permissions.add( Pattern.compile( pattern.trim() ) );
            }
        }

        @Override
        public boolean permits( String operation, User.Credential... credentials ) {
            for( User.Credential credential : credentials ) {
                if( credential instanceof RoleCredential && credential.getName().equalsIgnoreCase( this.roleName ) ) {
                    for( Pattern pattern : this.permissions ) {
                        if( pattern.matcher( operation ).matches() ) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    private final List<PermissionPolicy> rules = new LinkedList<>();

    public RulePermissionPolicy() {
        // allow admins to do anything
        addCredentialTypeExpressionRule( "admin", "true" );
    }

    public void addCredentialTypeExpressionRule( String type, String rule ) {
        this.rules.add( new CredentialTypeExpressionRule( type, rule ) );
    }

    public void addRolePermissionsRule( String roleName, Set<String> permissionPatterns ) {
        this.rules.add( new RolePermissionsRule( roleName, permissionPatterns ) );
    }

    @Override
    public boolean permits( String operation, User.Credential... credentials ) {
        for( PermissionPolicy rule : this.rules ) {
            if( rule.permits( operation, credentials ) ) {
                return true;
            }
        }
        return false;
    }
}
