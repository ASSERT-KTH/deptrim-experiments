package hudson.cli;


import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.spec.InvalidKeySpecException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
keys were generated with ssh-keygen from OpenSSH_7.9p1, LibreSSL 2.7.3
*/
@Execution(ExecutionMode.CONCURRENT)
public class PrivateKeyProviderTest {

    /**
    key command: ssh-keygen -f dsa -t dsa -b 1024 -m PEM
    */
    @Test
    public void loadKeyDSA() throws IOException, GeneralSecurityException {
        File file = new File(this.getClass().getResource("dsa").getFile());
        assertKeyPairNotNull(file, null);
    }

    /**
     * Asserts the keyPair private and public are not null.
     * @param file the file to load the key from
     * @param password the password
     */
    private void assertKeyPairNotNull(File file, String password) throws IOException, GeneralSecurityException {
        KeyPair keyPair = PrivateKeyProvider.loadKey(file, password);
        assertNotNull(keyPair);
        assertNotNull(keyPair.getPrivate());
        assertNotNull(keyPair.getPublic());
    }

    /**
    key command: ssh-keygen -f dsa-password -t dsa -b 1024 -m PEM -p password
    */
    @Test
    public void loadKeyDSAPassword() throws IOException, GeneralSecurityException {
        File file = new File(this.getClass().getResource("dsa-password").getFile());
        String password = "password";
        assertKeyPairNotNull(file, password);
    }

    /**
    key command: ssh-keygen -f rsa -t rsa -b 1024 -m PEM
    */
    @Test
    public void loadKeyRSA() throws IOException, GeneralSecurityException {
        File file = new File(this.getClass().getResource("rsa").getFile());
        assertKeyPairNotNull(file, null);
    }

    /**
    key command: ssh-keygen -f rsa-password -t rsa -b 1024 -m PEM -p password
    */
    @Test
    public void loadKeyRSAPassword() throws IOException, GeneralSecurityException {
        File file = new File(this.getClass().getResource("rsa-password").getFile());
        String password = "password";
        assertKeyPairNotNull(file, password);
    }

    /**
    key command: ssh-keygen -f openssh -t rsa -b 1024
    */
    @Test
    public void loadKeyOpenSSH() throws IOException, GeneralSecurityException {
        File file = new File(this.getClass().getResource("openssh").getFile());
        assertKeyPairNotNull(file, null);
    }

    /**
     key command: ssh-keygen -f openssh-unsupported -t rsa -b 1024 -m PKCS8 -p password
     */
    @Test
    public void loadKeyOpenSSHPKCS8() throws IOException, GeneralSecurityException {
        File file = new File(this.getClass().getResource("openssh-pkcs8").getFile());
        String password = "password";
        assertKeyPairNotNull(file, password);
    }

    /**
     key command: ssh-keygen -f openssh-unsupported -t rsa -b 1024 -m RFC4716 -p password
     */
    @Test
    public void loadKeyOpenSSHRFC4716() throws IOException, GeneralSecurityException {
        File file = new File(this.getClass().getResource("openssh-rfc4716").getFile());
        String password = "password";
        assertKeyPairNotNull(file, password);
    }

    /**
     key command: ssh-keygen -f openssh-unsupported -t rsa -b 1024 -m RFC4716 -p password
     Copy pasted the same key twice
     */
    @Test
    public void loadKeyOpenSSHMultipleKeys() throws IOException, GeneralSecurityException {
        File file = new File(this.getClass().getResource("openssh-multiple-keys").getFile());
        String password = "password";
        assertThrows(InvalidKeySpecException.class, () -> PrivateKeyProvider.loadKey(file, password));
    }

    /**
     * Uses a blank file
     */
    @Test
    public void loadBlankKey() throws IOException, GeneralSecurityException {
        File file = new File(this.getClass().getResource("blank").getFile());
        String password = "password";
        assertThrows(InvalidKeyException.class, () -> PrivateKeyProvider.loadKey(file, password));
    }

    /**
    key command: ssh-keygen -f openssh -t rsa -b 1024
    in this key we remove some lines to break the key.
    */
    @Test
    public void loadKeyBroken() throws IOException, GeneralSecurityException {
        File file = new File(this.getClass().getResource("openssh-broken").getFile());
        String password = "password";
        assertThrows(IllegalArgumentException.class, () -> PrivateKeyProvider.loadKey(file, password));
    }
}
