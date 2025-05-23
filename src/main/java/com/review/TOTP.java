package com.review;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class TOTP {
    private byte[] key = null;
    private long timeStepInSeconds = 30;

    // Construtor da classe. Recebe a chave secreta em BASE32 e o intervalo
    // de tempo a ser adotado (default = 30 segundos). Deve decodificar a
    // chave secreta e armazenar em key. Em caso de erro, gera Exception.

    public TOTP(String base32EncodedSecret, long timeStepInSeconds)
            throws Exception {
        if (base32EncodedSecret == null || base32EncodedSecret.isEmpty()) {
            throw new Exception("Secret key cannot be null or empty");
        }
        Base32 b32 = new Base32(Base32.Alphabet.BASE32, true, false);
        this.key = b32.fromString(base32EncodedSecret);

        if (this.key == null || this.key.length == 0) {
            throw new Exception("Failed to decode Base32 secret key or key is empty.");
        }
        if (timeStepInSeconds > 0) {
            this.timeStepInSeconds = timeStepInSeconds;
        }
    }

    // Recebe o HASH HMAC-SHA1 e determina o código TOTP de 6 algarismos
    // decimais, prefixado com zeros quando necessário.
    private String getTOTPCodeFromHash(byte[] hash) {
        if (hash == null || hash.length == 0) {
            return "";
        }

        // Extraindo 4 bytes do hash para cálculo do código
        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset] & 0x7F) << 24) |
                ((hash[offset + 1] & 0xFF) << 16) |
                ((hash[offset + 2] & 0xFF) << 8) |
                (hash[offset + 3] & 0xFF);

        // Obter 6 dígitos do valor binário
        int code = binary % 1000000;

        // Preencher com zeros à esquerda, se necessário
        return String.format("%06d", code);
    }

    // Recebe o contador e a chave secreta para produzir o hash HMAC-SHA1.
    private byte[] HMAC_SHA1(byte[] counter, byte[] keyByteArray) {
        try {
            Mac hmacSha1 = Mac.getInstance("HmacSHA1");
            SecretKeySpec keySpec = new SecretKeySpec(keyByteArray, "HmacSHA1");
            hmacSha1.init(keySpec);
            return hmacSha1.doFinal(counter);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Recebe o intervalo de tempo e executa o algoritmo TOTP para produzir
    // o código TOTP. Usa os métodos auxiliares getTOTPCodeFromHash e HMAC_SHA1.
    private String TOTPCode(long timeInterval) {
        // Convertendo o intervalo de tempo para um contador de 8 bytes (big-endian)
        byte[] counter = new byte[8];
        for (int i = 7; i >= 0; i--) {
            counter[i] = (byte) (timeInterval & 0xFF);
            timeInterval >>= 8;
        }

        // Calcular o hash HMAC-SHA1
        byte[] hash = HMAC_SHA1(counter, this.key);

        // Convertendo o hash para o código TOTP
        return getTOTPCodeFromHash(hash);
    }

    // Metodo que é utilizado para solicitar a geração do código TOTP.
    public String generateCode() {
        // Obter o tempo atual em segundos desde a epoch Unix
        long currentTimeSeconds = System.currentTimeMillis() / 1000;

        // Calcular o intervalo de tempo atual
        long timeInterval = currentTimeSeconds / this.timeStepInSeconds;

        // Gerar o código TOTP para esse intervalo
        return TOTPCode(timeInterval);
    }

    // Metodo que é utilizado para validar um código TOTP (inputTOTP).
    // Deve considerar um atraso ou adiantamento de 30 segundos no
    // relógio da máquina que gerou o código TOTP.
    public boolean validateCode(String inputTOTP) {
        if (inputTOTP == null || inputTOTP.length() != 6) {
            System.out.println("OIIIIII");
            return false;
        }

        // Obter o tempo atual em segundos desde a epoch Unix
        long currentTimeSeconds = System.currentTimeMillis() / 1000;

        // Calcular o intervalo de tempo atual
        long currentInterval = currentTimeSeconds / this.timeStepInSeconds;

        // Verificar o código no intervalo atual e nos intervalos adjacentes (para
        // compensar o desvio do relógio)
        for (int i = -1; i <= 1; i++) {
            String generatedCode = TOTPCode(currentInterval + i);
            if (inputTOTP.equals(generatedCode)) {
                return true;
            }
        }

        return false;
    }

    public static String getGoogleAuthUrl(String email, String chaveB32) {
        String url = String.format("otpauth://totp/Cofre Digital:%s?secret=%s", email, chaveB32);
        return url;
    }
}