package cardanowallet;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class EncryptDecryptMnemonic {

    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 16;
    private static final int KEY_LENGTH = 256;
    private static final int ITERATION_COUNT = 100000;
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY_FACTORY_ALGORITHM = "PBKDF2WithHmacSHA256";

    private static SecretKeySpec deriveKey(String passphrase, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_FACTORY_ALGORITHM);
        PBEKeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }

    private static Cipher initCipher(int mode, SecretKeySpec key, IvParameterSpec ivSpec) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(mode, key, ivSpec);
        return cipher;
    }

    public static String encrypt(String mnemonic, String passphrase) throws Exception {
        SecureRandom random = new SecureRandom();

        // Generate a random salt
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);

        // Derive a key from the passphrase and salt
        SecretKeySpec key = deriveKey(passphrase, salt);

        // Encrypt the mnemonic
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        Cipher cipher = initCipher(Cipher.ENCRYPT_MODE, key, ivSpec);
        byte[] encrypted = cipher.doFinal(mnemonic.getBytes());

        // Return the salt, iv, and encrypted mnemonic
        byte[] encryptedData = new byte[salt.length + iv.length + encrypted.length];
        // concatenate salt, iv, and encrypted mnemonic (simply use + instead of byte array copy)
        System.arraycopy(salt, 0, encryptedData, 0, salt.length);
        System.arraycopy(iv, 0, encryptedData, salt.length, iv.length);
        System.arraycopy(encrypted, 0, encryptedData, salt.length + iv.length, encrypted.length);

        // Convert to base64 for persistence or transmission over network.
        return Base64.getEncoder().encodeToString(encryptedData);
    }

    public static String decrypt(String encryptedData, String passphrase) throws Exception {
        byte[] decodedData = Base64.getDecoder().decode(encryptedData);

        // Extract the salt, iv, and encrypted mnemonic
        byte[] salt = new byte[SALT_LENGTH];
        byte[] iv = new byte[IV_LENGTH];
        byte[] encryptedMnemonic = new byte[decodedData.length - SALT_LENGTH - IV_LENGTH];

        System.arraycopy(decodedData, 0, salt, 0, SALT_LENGTH);
        System.arraycopy(decodedData, SALT_LENGTH, iv, 0, IV_LENGTH);
        System.arraycopy(decodedData, SALT_LENGTH + IV_LENGTH, encryptedMnemonic, 0, encryptedMnemonic.length);

        // Derive the key from the passphrase and salt
        SecretKeySpec key = deriveKey(passphrase, salt);

        // Decrypt the mnemonic
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        Cipher cipher = initCipher(Cipher.DECRYPT_MODE, key, ivSpec);
        byte[] decrypted = cipher.doFinal(encryptedMnemonic);

        return new String(decrypted);
    }

}