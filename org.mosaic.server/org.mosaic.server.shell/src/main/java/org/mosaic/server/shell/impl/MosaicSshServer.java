package org.mosaic.server.shell.impl;

import java.io.IOException;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.apache.sshd.SshServer;
import org.springframework.stereotype.Component;

/**
 * @author arik
 */
@Component
public class MosaicSshServer {

    private SshServer sshServer;

    @PostConstruct
    public void start() throws IOException {
        SshServer sshServer = SshServer.setUpDefaultServer();
        this.sshServer = sshServer;
        this.sshServer.start();
    }

    @PreDestroy
    public void stop() throws InterruptedException {
        this.sshServer.stop();
    }

}
