package org.mosaic.server.shell.impl;

import org.apache.sshd.common.Factory;
import org.apache.sshd.server.Command;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
public class MosaicSshShellFactory implements Factory<Command> {

    private ApplicationContext applicationContext;

    @Autowired
    public void setApplicationContext( ApplicationContext applicationContext ) {
        this.applicationContext = applicationContext;
    }

    @Override
    public Command create() {
        return this.applicationContext.getBean( Shell.class );
    }

}
