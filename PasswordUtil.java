import sun.misc.BASE64Decoder;

/**
* Utility class to encrypt and decrypt passwords using Java
cryptography.
*/

//package com.codefutures.security;
import sun.misc.BASE64Encoder;

import java.io.IOException;

import java.security.*;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;


public class PasswordUtil
{
    private static final String CIPHER_TYPE = "DES/ECB/PKCS5Padding";

    public static String encrypt(String password, Key key)
    {
        try
        {
            Cipher cipher = Cipher.getInstance(CIPHER_TYPE);
            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] outputBytes = cipher.doFinal(password.getBytes());

            BASE64Encoder encoder = new BASE64Encoder();
            String base64 = encoder.encode(outputBytes);

            return base64;
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to encrypt password", e);
        }
    }

    public static String decrypt(String password, Key key)
    {
        try
        {
            BASE64Decoder decoder = new BASE64Decoder();
            byte[] encrypted = decoder.decodeBuffer(password);

            Cipher cipher = Cipher.getInstance(CIPHER_TYPE);
            cipher.init(Cipher.DECRYPT_MODE, key);

            byte[] outputBytes = cipher.doFinal(encrypted);
            String ret = new String(outputBytes);

            return ret;
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to decrypt password", e);
        }
    }

    /**
    * Create a key for use in the cipher code
    */
    public static Key generateRandomKey() throws NoSuchAlgorithmException
    {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("DES");
        keyGenerator.init(new SecureRandom());

        SecretKey secretKey = keyGenerator.generateKey();

        return secretKey;
    }

    /**
    * Encode a secret key as a string that can be stored for later
    use.
    *
    * @param key
    * @return
    */
    public static String encodeKey(Key key)
    {
        BASE64Encoder encoder = new BASE64Encoder();

        return encoder.encode(key.getEncoded());
    }

    /**
    * Reconstruct a secret key from a string representation.
    *
    * @param encodedKey
    * @return
    * @throws IOException
    */
    public static Key decodeKey(String encodedKey) throws IOException
    {
        BASE64Decoder decoder = new BASE64Decoder();
        byte[] raw = decoder.decodeBuffer(encodedKey);
        SecretKey key = new SecretKeySpec(raw, "DES");

        return key;
    }
}

