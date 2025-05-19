package com.review;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

import javax.crypto.Cipher;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.crypto.generators.OpenBSDBCrypt;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class KeyManager {
    private static final int BCRYPT_COST = 8;
    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public static byte[] getRandomSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return salt;
    }

    public static byte[] getRandomArr(int size) {
        SecureRandom random = new SecureRandom();
        byte[] arr = new byte[size];
        random.nextBytes(arr);
        return arr;
    }

    public static String gerarHashBcrypt(String senha, byte[] salt) {
        return OpenBSDBCrypt.generate(senha.toCharArray(), salt, BCRYPT_COST);
    }

    public static String gerarHashBcrypt(String senha) {
        byte[] salt = getRandomSalt();
        return OpenBSDBCrypt.generate(senha.toCharArray(), salt, BCRYPT_COST);
    }

    public static boolean validarSenha(String senha, String hashArmazenado) {
        if (hashArmazenado == null) {
            System.err.println("Invalid or null hash provided for password validation.");
            return false; // Indicate that the hash is invalid
        }
        try {
            return OpenBSDBCrypt.checkPassword(hashArmazenado, senha.toCharArray());
        } catch (IllegalArgumentException e) {
            System.err.println("Error: The provided hash is not a valid BCrypt string. " + e.getMessage());
            return false; // Validation failure
        }
    }

    /**
     * Gera um par de chaves (pública e privada).
     * 
     * @param keySize Tamanho da chave (ex: 2048, 4096)
     * @return KeyPair gerado
     * @throws NoSuchAlgorithmException Se o algoritmo RSA não for encontrado
     * @throws NoSuchProviderException  Se o provider BouncyCastle não for
     *                                  encontrado
     */
    public static KeyPair generateKeyPair(int keySize) throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
        keyGen.initialize(keySize, new SecureRandom());
        return keyGen.generateKeyPair();
    }

    /**
     * Gera um certificado X.509 autoassinado.
     * 
     * @param keyPair      O par de chaves a ser usado para assinar o certificado. A
     *                     chave pública é incorporada no certificado.
     * @param subjectDN    O Distinguished Name (DN) do sujeito do certificado (ex:
     *                     "CN=Test CA, O=MyOrg, C=BR").
     * @param validityDays Número de dias pelos quais o certificado será válido.
     * @return O certificado X.509 gerado.
     * @throws OperatorCreationException Se houver um erro ao criar o assinador de
     *                                   conteúdo.
     * @throws CertificateException      Se houver um erro ao construir ou converter
     *                                   o certificado.
     */
    public X509Certificate generateSelfSignedCertificate(KeyPair keyPair, String subjectDN, int validityDays)
            throws OperatorCreationException, CertificateException {

        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        long nowMillis = System.currentTimeMillis();
        Date startDate = new Date(nowMillis);

        X500Name issuerAndSubject = new X500Name(subjectDN);

        BigInteger serialNumber = new BigInteger(Long.toString(nowMillis));

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        calendar.add(Calendar.DAY_OF_YEAR, validityDays);
        Date endDate = calendar.getTime();

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuerAndSubject,
                serialNumber,
                startDate,
                endDate,
                issuerAndSubject,
                publicKey);

        // Assinador de conteúdo usando SHA256 com RSA
        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSA")
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build(privateKey);

        // Constrói e converte o certificado
        X509Certificate certificate = new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(certBuilder.build(contentSigner));

        return certificate;
    }

    /**
     * Salva um certificado X.509 em um arquivo no formato PEM.
     * 
     * @param certificate O certificado a ser salvo.
     * @param filePath    O caminho do arquivo onde o certificado será salvo.
     * @throws IOException Se ocorrer um erro de I/O ao escrever o arquivo.
     */
    public void saveCertificateToFile(X509Certificate certificate, String filePath) throws IOException {
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(new FileWriter(filePath))) {
            pemWriter.writeObject(certificate);
        }
        System.out.println("Certificado salvo em: " + filePath);
    }

    /**
     * Salva uma chave privada em um arquivo no formato PEM (PKCS#8).
     * 
     * @param privateKey A chave privada a ser salva.
     * @param filePath   O caminho do arquivo onde a chave privada será salva.
     * @throws IOException Se ocorrer um erro de I/O ao escrever o arquivo.
     */
    public void savePrivateKeyToFile(PrivateKey privateKey, String filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            byte[] encoded = privateKey.getEncoded();
            fos.write(encoded);
            fos.flush();
        }
        System.out.println("Chave privada salva em formato binário em: " + filePath);
    }

    /**
     * Método principal para gerar e salvar o certificado e a chave privada.
     * 
     * @param commonName      O Common Name (CN) para o DN do certificado.
     * @param emailAddress    O Endereço de Email (E) para o DN do certificado.
     * @param orgUnit         A Organizational Unit (OU) para o DN do certificado.
     * @param organization    A Organization (O) para o DN do certificado.
     * @param locality        A Locality (L) para o DN do certificado.
     * @param stateOrProvince O State or Province (ST) para o DN do certificado.
     * @param country         O Country (C) para o DN do certificado.
     * @param validityDays    Número de dias de validade do certificado.
     * @param keySize         Tamanho da chave RSA (ex: 2048).
     * @param certFilePath    Caminho para salvar o arquivo do certificado (ex:
     *                        "certificate.pem").
     * @param keyFilePath     Caminho para salvar o arquivo da chave privada (ex:
     *                        "private_key.pem").
     */
    public void generateAndSaveAssets(String commonName, String emailAddress, String orgUnit, String organization,
            String locality, String stateOrProvince, String country,
            int validityDays, int keySize,
            String certFilePath, String keyFilePath, String passPhrase) {
        try {
            // 1. Gerar par de chaves
            KeyPair keyPair = generateKeyPair(keySize);

            String subjectDN = String.format("CN=%s, E=%s, OU=%s, O=%s, L=%s, ST=%s, C=%s",
                    commonName, emailAddress, orgUnit, organization, locality, stateOrProvince, country);

            // 3. Gerar certificado autoassinado
            X509Certificate certificate = generateSelfSignedCertificate(keyPair, subjectDN, validityDays);

            // 4. Salvar certificado em arquivo
            saveCertificateToFile(certificate, certFilePath);

            // 5. Salvar chave privada em arquivo
            savePrivateKeyToFile(keyPair.getPrivate(), keyFilePath);

            // 6. Criptografar chave privada com a frase secreta
            PrivateKeyManager.encryptPkFile(keyFilePath, keyFilePath,
                    passPhrase);

            System.out.println("Certificado e chave privada gerados com sucesso.");

        } catch (NoSuchAlgorithmException e) {
            System.err.println("Erro: Algoritmo de criptografia não encontrado. " + e.getMessage());
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            System.err.println(
                    "Erro: Provedor BouncyCastle não encontrado. Certifique-se de que a biblioteca está no classpath. "
                            + e.getMessage());
            e.printStackTrace();
        } catch (OperatorCreationException e) {
            System.err.println("Erro ao criar o assinador do certificado: " + e.getMessage());
            e.printStackTrace();
        } catch (CertificateException e) {
            System.err.println("Erro ao gerar o certificado: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Erro de I/O ao salvar arquivos: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Ocorreu um erro inesperado: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static byte[] decryptContentWithRSA(byte[] content, PrivateKey pk) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, pk);
        byte[] decryptedContent = cipher.doFinal(content);
        return decryptedContent;
    }

    public static byte[] encryptContentWithRSA(byte[] content, PublicKey pk) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, pk);
        byte[] encryptedContent = cipher.doFinal(content);
        return encryptedContent;
    }

    public static void generateDefaultCA() {

        // Parâmetros para o certificado
        String commonName = "CA de Teste";
        String emailAddress = "teste@teste.com";
        String orgUnit = "MIANPL";
        String organization = "MIANPL LTDA.";
        String locality = "Rio de Janeiro";
        String stateOrProvince = "Rio de Janeiro";
        String country = "BR"; // Código do país com 2 letras (ISO 3166-1 alpha-2)
        String passPhrase = "teste"; // Frase secreta para criptografar a chave privada

        int validityDays = 365; // Validade de 1 ano
        int keySize = 2048; // Tamanho da chave RSA

        // Nomes dos arquivos de saída
        String certFilePath = "keys/meu_certificado_ca.pem";
        String keyFilePath = "keys/minha_chave_privada_ca.bin";

        KeyManager km = new KeyManager();
        km.generateAndSaveAssets(
                commonName, emailAddress, orgUnit, organization, locality, stateOrProvince, country,
                validityDays, keySize,
                certFilePath, keyFilePath, passPhrase);
    }
}