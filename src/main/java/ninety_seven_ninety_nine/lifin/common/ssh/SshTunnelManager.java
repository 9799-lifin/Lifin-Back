package ninety_seven_ninety_nine.lifin.common.ssh;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import jakarta.annotation.PreDestroy;
import java.util.Properties;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 로컬 환경에서 개발하는 경우, 적용
 */
@Slf4j
@Profile("local")
@Component
@ConfigurationProperties(prefix = "ssh")
@Setter
public class SshTunnelManager {

    private String sshHost;
    private int sshPort;
    private String sshUsername;
    private String sshPrivateKey;
    private String sshPassphrase;

    private int remoteDatabasePort;
    private String remoteDatabaseHost;

    private Session sshSession;

    /**
     * 애플리케이션 컨텍스트가 종료될 때 SSH 세션을 닫습니다.
     */
    @PreDestroy
    public void close() {
        if (sshSession != null && sshSession.isConnected()) {
            sshSession.disconnect();
            log.info("SSH 세션이 종료되었습니다");
        } else {
            log.info("SSH 세션이 연결되지 않았습니다");
        }
    }

    /**
     * 포트 포워딩을 설정하여 SSH 터널을 생성합니다.
     *
     * @return 원격 데이터베이스 포트로 포워딩된 로컬 포트.
     * @throws JSchException SSH 세션 설정 또는 터널링 시 오류가 발생한 경우.
     */
    public Integer setupTunneling() throws JSchException {
        try {
            int forwardedPort = sshSession.setPortForwardingL(0, remoteDatabaseHost, remoteDatabasePort);
            log.info("SSH 터널이 로컬 포트 {}에 설정되었습니다", forwardedPort);
            return forwardedPort;
        } catch (JSchException e) {
            log.error("터널링 실패", e);
            this.close();
            throw e;
        }
    }

    /**
     * SSH 세션을 설정합니다.
     *
     * @throws JSchException SSH 세션 설정 시 오류가 발생한 경우.
     */
    public void setupSession() throws JSchException {
        try {
            JSch jSch = new JSch();
            jSch.addIdentity(sshPrivateKey, sshPassphrase);
            sshSession = jSch.getSession(sshUsername, sshHost, sshPort);

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            sshSession.setConfig(config);

            sshSession.connect();
            log.info("SSH 세션이 연결되었습니다");
        } catch (JSchException e) {
            log.error("SSH 세션 설정 실패", e);
            this.close();
            throw e;
        }
    }
}
