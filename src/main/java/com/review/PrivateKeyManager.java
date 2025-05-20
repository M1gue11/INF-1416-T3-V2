package com.review;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class PrivateKeyManager {

    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/ECB/PKCS5Padding"; // Conforme especificado
    private static final String PRNG_ALGORITHM = "SHA1PRNG"; // Conforme especificado
    private static final int AES_KEY_SIZE_BITS = 256;
    private static final int RANDOM_DATA_SIZE_BYTES = 8192; // Conforme especificado
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    /**
     * Deriva uma chave AES de 256 bits a partir de uma frase secreta usando
     * SHA1PRNG.
     *
     * @param passphrase A frase secreta.
     * @return A SecretKey AES.
     * @throws NoSuchAlgorithmException Se o algoritmo AES ou SHA1PRNG não for
     *                                  encontrado.
     */
    public static SecretKey deriveAesKeyFromGivenSeed(String seed)
            throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance(AES_ALGORITHM);
        SecureRandom secureRandom = SecureRandom.getInstance(PRNG_ALGORITHM);

        secureRandom.setSeed(seed.getBytes(StandardCharsets.UTF_8));

        keyGen.init(AES_KEY_SIZE_BITS, secureRandom);
        return keyGen.generateKey();
    }

    public static SecretKey deriveAesKeyFromGivenSeed(byte[] seed)
            throws NoSuchAlgorithmException {
        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
        secureRandom.setSeed(seed);
        byte[] chaveAESBytes = new byte[32];
        secureRandom.nextBytes(chaveAESBytes);
        return new SecretKeySpec(chaveAESBytes, "AES");
    }

    /**
     * Criptografa o conteúdo de um arquivo de chave privada e salva em um novo
     * arquivo.
     *
     * @param privateKeyFilePath      Caminho para o arquivo da chave privada
     *                                original (binário).
     * @param encryptedOutputFilePath Caminho onde o arquivo criptografado será
     *                                salvo.
     * @param passphrase              A frase secreta para gerar a chave de
     *                                criptografia.
     * @throws Exception Se ocorrer algum erro durante o processo.
     */
    public static void encryptPkFile(String privateKeyFilePath, String encryptedOutputFilePath,
            String passphrase)
            throws Exception {

        SecretKey aesKey = deriveAesKeyFromGivenSeed(passphrase);
        byte[] privateKeyBytes = Files.readAllBytes(Paths.get(privateKeyFilePath));

        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        byte[] encryptedPrivateKeyBytes = cipher.doFinal(privateKeyBytes);

        Files.write(Paths.get(encryptedOutputFilePath), encryptedPrivateKeyBytes);
        System.out.println("Chave privada criptografada e salva em: " + encryptedOutputFilePath);
    }

    public static String encryptContentWithPhrase(String content, String passphrase) throws Exception {

        SecretKey aesKey = deriveAesKeyFromGivenSeed(passphrase);
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        byte[] encryptedContentBytes = cipher.doFinal(contentBytes);

        Base32 b32 = new Base32(Base32.Alphabet.BASE32, true, false);
        String encryptedContentB32 = b32.toString(encryptedContentBytes);
        return encryptedContentB32;
    }

    public static String decryptContentWithPhrase(String encryptedContentB32, String passphrase) throws Exception {

        SecretKey aesKey = deriveAesKeyFromGivenSeed(passphrase);
        Base32 b32 = new Base32(Base32.Alphabet.BASE32, true, false);
        byte[] encryptedContentBytes = b32.fromString(encryptedContentB32);

        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        byte[] decryptedContentBytes = cipher.doFinal(encryptedContentBytes);

        return new String(decryptedContentBytes, StandardCharsets.UTF_8);
    }

    /**
     * Descriptografa um arquivo de chave privada previamente criptografado.
     *
     * @param encryptedPrivateKeyFilePath Caminho para o arquivo da chave privada
     *                                    criptografada.
     * @param decryptedOutputFilePath     Caminho onde o arquivo descriptografado
     *                                    será salvo.
     * @param passphrase                  A frase secreta usada originalmente para
     *                                    criptografar.
     * @return Os bytes da chave privada descriptografada.
     * @throws Exception Se ocorrer algum erro (ex: frase secreta errada resultando
     *                   em BadPaddingException).
     */
    public static byte[] decryptPkFile(String encryptedPrivateKeyFilePath, String decryptedOutputFilePath,
            String passphrase)
            throws Exception {

        SecretKey aesKey = deriveAesKeyFromGivenSeed(passphrase);
        byte[] encryptedPrivateKeyBytes = Files.readAllBytes(Paths.get(encryptedPrivateKeyFilePath));

        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        byte[] decryptedPrivateKeyBytes = cipher.doFinal(encryptedPrivateKeyBytes);

        Files.write(Paths.get(decryptedOutputFilePath), decryptedPrivateKeyBytes);
        System.out.println("Chave privada descriptografada e salva em: " + decryptedOutputFilePath);
        return decryptedPrivateKeyBytes;
    }

    public static byte[] decryptPkFile(String encryptedPrivateKeyFilePath, String passphrase) throws Exception {
        SecretKey aesKey = deriveAesKeyFromGivenSeed(passphrase);
        byte[] encryptedPrivateKeyBytes = Files.readAllBytes(Paths.get(encryptedPrivateKeyFilePath));
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        byte[] decryptedPrivateKeyBytes = cipher.doFinal(encryptedPrivateKeyBytes);

        // Check if we have PEM format
        String pemContent = new String(decryptedPrivateKeyBytes, StandardCharsets.UTF_8);
        if (pemContent.contains("-----BEGIN PRIVATE KEY-----")) {
            // Extract the base64 content (remove header, footer, and newlines)
            String base64Content = pemContent
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", ""); // Remove all whitespace including newlines

            // Decode the base64 content to get the binary DER format
            return Base64.getDecoder().decode(base64Content);
        }

        // If not PEM format, return as is (assuming it's already binary DER)
        return decryptedPrivateKeyBytes;
    }

    public static PrivateKey loadPkFromBytes(byte[] pkBytes, String keyAlgorithm)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(keyAlgorithm);
        return keyFactory.generatePrivate(keySpec);
    }

    public static PrivateKey decryptAndReturnPk(String encryptedPrivateKeyFilePath, String passphrase)
            throws Exception {
        byte[] decryptedPrivateKeyBytes = decryptPkFile(encryptedPrivateKeyFilePath, passphrase);
        return loadPkFromBytes(decryptedPrivateKeyBytes, "RSA");
    }

    public static X509Certificate loadCaFromFile(String certificateFilePath) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        byte[] certBytes = Files.readAllBytes(Paths.get(certificateFilePath));

        String certString = new String(certBytes, StandardCharsets.UTF_8);
        if (certString.contains("-----BEGIN CERTIFICATE-----")) {
            // More robust extraction of the Base64 content
            StringBuilder base64Content = new StringBuilder();
            boolean readingContent = false;

            // Process the PEM file line by line
            String[] lines = certString.split("\\r?\\n");
            for (String line : lines) {
                if (line.contains("-----BEGIN CERTIFICATE-----")) {
                    readingContent = true;
                    continue;
                } else if (line.contains("-----END CERTIFICATE-----")) {
                    break;
                } else if (readingContent) {
                    // Only append actual Base64 content (skip empty lines or metadata)
                    line = line.trim();
                    if (!line.isEmpty() && !line.contains(":")) {
                        base64Content.append(line);
                    }
                }
            }

            // Decode the cleaned Base64 content
            certBytes = Base64.getDecoder().decode(base64Content.toString());
        }

        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certBytes));
    }

    public static boolean validatePrivateKeyWithCertificate(byte[] decryptedPrivateKeyBytes,
            X509Certificate certificate,
            String keyAlgorithm) {
        try {
            PrivateKey privateKey = loadPkFromBytes(decryptedPrivateKeyBytes, keyAlgorithm);
            PublicKey publicKey = certificate.getPublicKey();
            byte[] randomData = KeyManager.getRandomArr(RANDOM_DATA_SIZE_BYTES);

            Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM);
            sig.initSign(privateKey);
            sig.update(randomData);
            byte[] signatureBytes = sig.sign();

            sig.initVerify(publicKey);
            sig.update(randomData);

            return sig.verify(signatureBytes);

        } catch (NoSuchAlgorithmException e) {
            System.err.println("Erro: Algoritmo não encontrado - " + e.getMessage());
        } catch (InvalidKeySpecException e) {
            System.err.println(
                    "Erro: Especificação de chave inválida (provavelmente erro ao carregar a chave privada dos bytes) - "
                            + e.getMessage());
        } catch (SignatureException e) {
            System.err.println("Erro: Problema com a assinatura - " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erro inesperado durante a validação: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public static String getParamFromCASubject(X509Certificate certificate, String key) {
        return certificate.getSubjectX500Principal().toString().split(String.format("%s=", key))[1].split(",")[0]
                .trim();

    }

    public static String getEmailFromCA(X509Certificate certificate) {
        return getParamFromCASubject(certificate, "EMAILADDRESS");
    }

    public static String getCommonNameFromCA(X509Certificate certificate) {
        return getParamFromCASubject(certificate, "CN");
    }

    public static byte[] decryptContentWithAES(byte[] encryptedContent, SecretKey aesKey) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        byte[] decryptedContent = cipher.doFinal(encryptedContent);
        return decryptedContent;
    }

    public static byte[] encryptContentWithAES(byte[] content, SecretKey aesKey) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        byte[] encryptedContent = cipher.doFinal(content);
        return encryptedContent;
    }
}