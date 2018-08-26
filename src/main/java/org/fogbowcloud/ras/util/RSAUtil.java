package org.fogbowcloud.ras.util;

import org.bouncycastle.util.encoders.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Random;

public class RSAUtil {

    private static String getKey(String filename) throws IOException {
        // Read key from file
        String strKeyPEM = "";
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        while ((line = br.readLine()) != null) {
            strKeyPEM += line;
        }
        br.close();
        return strKeyPEM;
    }

    public static RSAPrivateKey getPrivateKey(String filename) throws IOException, GeneralSecurityException {
        String privateKeyPEM = getKey(filename);
        return getPrivateKeyFromString(privateKeyPEM);
    }

    public static RSAPrivateKey getPrivateKeyFromString(String key) throws GeneralSecurityException {
        String privateKeyPEM = key;

        // Remove the first and last lines
        privateKeyPEM = privateKeyPEM.replace("-----BEGIN PRIVATE KEY-----", "");
        privateKeyPEM = privateKeyPEM.replace("-----END PRIVATE KEY-----", "");

        // Base64 decode data
        byte[] encoded = org.bouncycastle.util.encoders.Base64.decode(privateKeyPEM);

        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPrivateKey privKey = (RSAPrivateKey) kf.generatePrivate(new PKCS8EncodedKeySpec(encoded));
        return privKey;
    }

    public static RSAPublicKey getPublicKey(String filename) throws IOException, GeneralSecurityException {
        String publicKeyPEM = getKey(filename);
        return getPublicKeyFromString(publicKeyPEM);
    }

    public static RSAPublicKey getPublicKeyFromString(String key) throws GeneralSecurityException {
        String publicKeyPEM = key;

        // Remove the first and last lines
        publicKeyPEM = publicKeyPEM.replace("-----BEGIN PUBLIC KEY-----", "");
        publicKeyPEM = publicKeyPEM.replace("-----END PUBLIC KEY-----", "");

        // Base64 decode data
        byte[] encoded = org.bouncycastle.util.encoders.Base64.decode(publicKeyPEM);

        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPublicKey pubKey = (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(encoded));
        return pubKey;
    }

    public static String sign(PrivateKey privateKey, String message)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, UnsupportedEncodingException {
        Signature sign = Signature.getInstance("SHA1withRSA");
        sign.initSign(privateKey);
        sign.update(message.getBytes("UTF-8"));
        return new String(Base64.encode(sign.sign()), "UTF-8");
    }

    public static boolean verify(PublicKey publicKey, String message, String signature)
            throws SignatureException, NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException {
        Signature sign = Signature.getInstance("SHA1withRSA");
        sign.initVerify(publicKey);
        sign.update(message.getBytes("UTF-8"));
        return sign.verify(Base64.decode(signature.getBytes("UTF-8")));
    }

    public static String encrypt(String rawText, PublicKey publicKey)
            throws IOException, GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return new String(
                org.bouncycastle.util.encoders.Base64.encode(
                        cipher.doFinal(rawText.getBytes("UTF-8"))));
    }

    public static String encryptAES(byte[] keyData, String data) throws Exception {
        byte[] ivData = new byte[16];
        IvParameterSpec iv = new IvParameterSpec(ivData);
        SecretKeySpec keySpec = new SecretKeySpec(keyData, "AES");
        Cipher aes = Cipher.getInstance("AES/CBC/PKCS5Padding");
        aes.init(Cipher.ENCRYPT_MODE, keySpec, iv);
        byte[] encryptBytes = aes.doFinal(data.getBytes("UTF-8"));
        return new String(org.bouncycastle.util.encoders.Base64.encode(encryptBytes));
    }

    public static String decryptAES(byte[] keyData, String data) throws Exception {
        byte[] ivData = new byte[16];
        IvParameterSpec iv = new IvParameterSpec(ivData);
        SecretKeySpec keySpec = new SecretKeySpec(keyData, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, iv);
        byte[] decryptedBytes =
                cipher.doFinal(org.bouncycastle.util.encoders.Base64.decode(data.getBytes("UTF-8")));
        return new String(decryptedBytes, "UTF8");
    }

    public static String generateAESKey() {
        char[] chars = "abcdefghijklmnopqrstuvwxyz".toCharArray();
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 16; i++) {
            char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }
        return sb.toString();
    }

    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);
        KeyPair keyPair = keyGen.genKeyPair();
        return keyPair;
    }

    public static String savePublicKey(PublicKey publ) throws GeneralSecurityException {
        KeyFactory fact = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec spec = fact.getKeySpec(publ, X509EncodedKeySpec.class);
        return new String(org.bouncycastle.util.encoders.Base64.encode(spec.getEncoded()));
    }

    public static String savePrivateKey(PrivateKey priv) throws GeneralSecurityException {
        KeyFactory fact = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec spec = fact.getKeySpec(priv, PKCS8EncodedKeySpec.class);
        byte[] packed = spec.getEncoded();
        String key64 = new String(org.bouncycastle.util.encoders.Base64.encode(packed));

        Arrays.fill(packed, (byte) 0);
        return key64;
    }

    public static String decrypt(String cipherText, PrivateKey privateKey)
            throws IOException, GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return new String(
                cipher.doFinal(org.bouncycastle.util.encoders.Base64.decode(cipherText)), "UTF-8");
    }
}
