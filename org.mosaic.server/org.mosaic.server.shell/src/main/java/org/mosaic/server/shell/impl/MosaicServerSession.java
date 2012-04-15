package org.mosaic.server.shell.impl;

import org.apache.mina.core.session.IoSession;
import org.apache.sshd.common.FactoryManager;
import org.apache.sshd.server.session.ServerSession;

/**
 * @author arik
 */
public class MosaicServerSession extends ServerSession {

    public MosaicServerSession( FactoryManager server, IoSession ioSession ) throws Exception {
        super( server, ioSession );
    }

    public boolean isClosing() {
        return this.closing;
    }
}
