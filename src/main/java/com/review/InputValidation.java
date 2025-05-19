package com.review;

import java.io.File;
import java.util.Optional;

import java.security.cert.X509Certificate;

public class InputValidation {
    public static boolean isValidPath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return false;
        }

        File file = new File(filePath);
        return file.exists() && file.isFile();
    }

    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            System.out.println("Email invalido");
            return false;
        }

        User user = DatabaseManager.getUserByEmail(email);
        if (user != null) {
            System.out.println("Email já cadastrado");
            return false;
        }
        // OK
        return true;
    }

    public static boolean isValidPkFilePath(String filePath, String name) {
        boolean isOk = isValidPath(filePath);
        if (!isOk) {
            DatabaseManager.getMessageByMessageCode(6005, Optional.empty(), Optional.of(name));
            System.out.println("caminho da chave privada invalido");
        }
        return isOk;
    }

    public static boolean isValidCAFilePath(String filePath, String name) {
        boolean isOk = isValidPath(filePath);
        if (!isOk) {
            DatabaseManager.getMessageByMessageCode(6006, Optional.empty(), Optional.of(name));
            System.out.println("caminho do ca invalido");
        }
        return isOk;
    }

    public static boolean isValidPassword(String password, String confirmation) {
        var bool = password != null && confirmation != null && password.length() >= 8 && password.length() <= 10 &&
                password.equals(confirmation);
        if (!bool) {
            System.out.println("Senha inválida");
        }
        return bool;
    }

    public static boolean isValidGroup(String group) {
        String lowerGroup = group.toLowerCase();
        System.out.println("Grupo: " + lowerGroup + lowerGroup != null && !lowerGroup.trim().isEmpty()
                && (lowerGroup == "Administrador".toLowerCase() || lowerGroup == "Usuario".toLowerCase()));
        return lowerGroup != null && !lowerGroup.trim().isEmpty()
                && (lowerGroup == "Administrador".toLowerCase() || lowerGroup == "Usuario".toLowerCase());
    }

    public static boolean isUserValidPassword(String password, String confirmation, String name) {
        boolean isOk = isValidPassword(password, confirmation);
        /* conferir com o password do banco */
        return isOk;
    }

    public static boolean isValidPhrase(String phrase) {
        return phrase != null && !phrase.trim().isEmpty();
    }

    public static boolean pkAndCaMatchPassphrase(String phrase, String caPath, String pkPath, boolean insertLog) {
        boolean isPhraseOk = isValidPhrase(phrase);
        if (!isPhraseOk) {
            // TODO: log
            System.out.println("Frase secreta inválida");
            return false;
        }

        try {
            byte[] pk = PrivateKeyManager.decryptPkFile(pkPath, phrase);
            X509Certificate cert = PrivateKeyManager.loadCaFromFile(caPath);
            boolean isMatchCaAndPk = PrivateKeyManager.validatePrivateKeyWithCertificate(pk, cert, "RSA");
            return isMatchCaAndPk;

        } catch (Exception e) {
            System.out.println("Erro ao descriptografar a chave privada: " + e.getMessage());
            return false;
        }
    }
}
