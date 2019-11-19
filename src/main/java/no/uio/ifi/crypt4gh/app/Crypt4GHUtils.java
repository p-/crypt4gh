package no.uio.ifi.crypt4gh.app;

import no.uio.ifi.crypt4gh.stream.Crypt4GHInputStream;
import no.uio.ifi.crypt4gh.stream.Crypt4GHOutputStream;
import no.uio.ifi.crypt4gh.util.KeyUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.Set;

/**
 * Encryption/decryption utility class, not a public API.
 */
class Crypt4GHUtils {

    private static Crypt4GHUtils ourInstance = new Crypt4GHUtils();

    static Crypt4GHUtils getInstance() {
        return ourInstance;
    }

    private KeyUtils keyUtils = KeyUtils.getInstance();

    private Crypt4GHUtils() {
    }

    void generateX25519KeyPair(String keyName) throws Exception {
        KeyUtils keyUtils = KeyUtils.getInstance();
        KeyPair keyPair = keyUtils.generateKeyPair();
        ConsoleUtils consoleUtils = ConsoleUtils.getInstance();
        File pubFile = new File(keyName + ".pub.pem");
        if (!pubFile.exists() || pubFile.exists() &&
                consoleUtils.promptForConfirmation("Public key file already exists: do you want to overwrite it?")) {
            keyUtils.writePEMFile(pubFile, keyPair.getPublic());
        }
        File secFile = new File(keyName + ".sec.pem");
        if (!secFile.exists() || secFile.exists() &&
                consoleUtils.promptForConfirmation("Private key file already exists: do you want to overwrite it?")) {
            keyUtils.writePEMFile(secFile, keyPair.getPrivate());
        }
        Set<PosixFilePermission> perms = new HashSet<>();
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
        Files.setPosixFilePermissions(secFile.toPath(), perms);
    }

    void encryptFile(String dataFilePath, String privateKeyFilePath, String publicKeyFilePath) throws IOException, GeneralSecurityException {
        File dataInFile = new File(dataFilePath);
        File dataOutFile = new File(dataFilePath + ".enc");
        if (dataOutFile.exists() && !ConsoleUtils.getInstance().promptForConfirmation(dataOutFile.getAbsolutePath() + " already exists. Overwrite?")) {
            return;
        }
        PrivateKey privateKey = keyUtils.readPEMFile(new File(privateKeyFilePath), PrivateKey.class);
        PublicKey publicKey = keyUtils.readPEMFile(new File(publicKeyFilePath), PublicKey.class);
        try (InputStream inputStream = new FileInputStream(dataInFile);
             OutputStream outputStream = new FileOutputStream(dataOutFile);
             Crypt4GHOutputStream crypt4GHOutputStream = new Crypt4GHOutputStream(outputStream, privateKey, publicKey)) {
            System.out.println("Encryption initialized...");
            IOUtils.copyLarge(inputStream, crypt4GHOutputStream);
            System.out.println("Done: " + dataOutFile.getAbsolutePath());
        } catch (GeneralSecurityException e) {
            System.err.println(e.getMessage());
            dataOutFile.delete();
        }
    }

    void decryptFile(String dataFilePath, String privateKeyFilePath) throws IOException, GeneralSecurityException {
        File dataInFile = new File(dataFilePath);
        File dataOutFile = new File(dataFilePath + ".dec");
        if (dataOutFile.exists() && !ConsoleUtils.getInstance().promptForConfirmation(dataOutFile.getAbsolutePath() + " already exists. Overwrite?")) {
            return;
        }
        PrivateKey privateKey = keyUtils.readPEMFile(new File(privateKeyFilePath), PrivateKey.class);
        System.out.println("Decryption initialized...");
        try (FileInputStream inputStream = new FileInputStream(dataInFile);
             OutputStream outputStream = new FileOutputStream(dataOutFile);
             Crypt4GHInputStream crypt4GHInputStream = new Crypt4GHInputStream(inputStream, privateKey)) {
            IOUtils.copyLarge(crypt4GHInputStream, outputStream);
            System.out.println("Done: " + dataOutFile.getAbsolutePath());
        } catch (GeneralSecurityException e) {
            System.err.println(e.getMessage());
            dataOutFile.delete();
        }
    }

}