package jenkins.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import hudson.FilePath;
import hudson.Functions;
import java.io.File;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DefaultConfidentialStoreTest {

    @Rule
    public TemporaryFolder tmpRule = new TemporaryFolder();

    @Test
    public void roundtrip() throws Exception {
        File tmp = new File(tmpRule.getRoot(), "tmp"); // let ConfidentialStore create a directory

        DefaultConfidentialStore store = new DefaultConfidentialStore(tmp);
        ConfidentialKey key = new ConfidentialKey("test") {};

        // basic roundtrip
        String str = "Hello world!";
        store.store(key, str.getBytes(StandardCharsets.UTF_8));
        assertEquals(str, new String(store.load(key), StandardCharsets.UTF_8));

        // data storage should have some stuff
        assertTrue(new File(tmp, "test").exists());
        assertTrue(new File(tmp, "master.key").exists());

        assertThrows(MalformedInputException.class, () -> Files.readString(tmp.toPath().resolve("test"), StandardCharsets.UTF_8)); // the data shouldn't be a plain text, obviously

        if (!Functions.isWindows()) {
            assertEquals(0700, new FilePath(tmp).mode() & 0777); // should be read only
        }

        // if the master key changes, we should gracefully fail to load the store
        new File(tmp, "master.key").delete();
        DefaultConfidentialStore store2 = new DefaultConfidentialStore(tmp);
        assertTrue(new File(tmp, "master.key").exists()); // we should have a new key now
        assertNull(store2.load(key));
    }

}
