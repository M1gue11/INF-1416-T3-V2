package com.review;

import org.bouncycastle.crypto.generators.OpenBSDBCrypt;

import java.security.SecureRandom;

public class PasswordUtils {
    private static final int BCRYPT_COST = 8;

    public static byte[] getRandomSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return salt;
    }

    public static String gerarHashBcrypt(String senha, byte[] salt) {

        String hash = OpenBSDBCrypt.generate(senha.toCharArray(), salt, 8);


        return hash.replace("$2a$", "$2y$");
    }

    public static boolean validarSenha(String senha, String hashArmazenado) {
        return OpenBSDBCrypt.checkPassword(
                hashArmazenado,
                senha.toCharArray()
        );
    }
}