package tech.eisen.server.security;

import org.jetbrains.annotations.NotNull;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.CharArrayWriter;
import java.nio.CharBuffer;
import java.security.*;
import java.security.spec.InvalidKeySpecException;

public final class SecureHashes {
    
    private final static int ITERATIONS = 1024;
    
    private SecureHashes() {}
    
    public static char[] hashPasswordPBDKDF2(char[] password, int iterations) {
        byte[] salt;
        try {
            salt = genSaltSHA1();
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
        
        byte[] hash = hashPBDKDF2(password, salt, iterations, 64 * 8);
        //noinspection StringBufferReplaceableByString
        CharArrayWriter writer = new CharArrayWriter();
        writer.append(Integer.toString(iterations));
        writer.append(':');
        writer.append(CharBuffer.wrap(toHex(salt)));
        writer.append(':');
        writer.append(CharBuffer.wrap(toHex(hash)));
        return writer.toCharArray();
    }
    
    public static char[] hashPasswordPBDKDF2(char[] password) {
        return hashPasswordPBDKDF2(password, ITERATIONS);
    }
    
    public static boolean validatePasswordPBDKDF2(char[] password, char[] hash) {
        String[] parts = new String(hash).split(":", 3);
        int iterations = Integer.parseInt(parts[0]);
        byte[] saltBytes = fromHex(parts[1].toCharArray());
        byte[] hashBytes = fromHex(parts[2].toCharArray());
        
        byte[] testHash = hashPBDKDF2(password, saltBytes, iterations, hashBytes.length * 8);
    
        if (hashBytes.length != testHash.length)
            return false;
        
        for (int i = 0; i < hashBytes.length && i < testHash.length; i++)
            if (hashBytes[i] != testHash[i])
                return false;
        return true;
    }
    
    private static byte[] hashPBDKDF2(@NotNull char[] chars, @NotNull byte[] salt, int iterations, int bits) {
        PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, bits);
        
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return skf.generateSecret(spec).getEncoded();
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }
    
    public static byte[] genSaltSHA1() throws NoSuchAlgorithmException {
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        byte[] salt = new byte[16];
        sr.nextBytes(salt);
        return salt;
    }
    
    private static char[] toHex(byte[] array) {
        char[] result = new char[array.length * 2];
        for (int i = 0, j = 0; i < array.length; i++, j++) {
            result[j] = (char) (((array[i] >>> 4) & 0xF) + '0');
            if (result[j] > '9')
                result[j] += 39;
            ++j;
            result[j] = (char) ((array[i] & 0xF) + '0');
            if (result[j] > '9')
                result[j] += 39;
        }
        return result;
    }
    
    private static byte[] fromHex(char[] hex) {
        byte[] bytes = new byte[hex.length / 2];
        for (int i = 0, j = 0; i < bytes.length; i++) {
            int upper = hex[j++];
            int lower = hex[j++];
            
            upper = (((upper > '9'? upper - 39 : upper) - '0') << 4);
            lower = (((lower > '9'? lower - 39 : lower) - '0'));
            
            bytes[i] = (byte) (upper | lower);
        }
        return bytes;
    }
    
}
