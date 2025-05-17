package com.review;

import java.io.File;
import java.util.Optional;

public class InputValidation {
    public static boolean isValidPath(String filePath){
        if (filePath == null || filePath.trim().isEmpty()) {
            return false;
        }

        File file = new File(filePath);
        return file.exists() && file.isFile();
    }

    public static boolean isValidPkFilePath(String filePath, String name) {
        boolean isOk = isValidPath(filePath);
        if (!isOk) {
            DatabaseManager.getMessageByMessageCode(6005, Optional.empty(), Optional.of(name));
        }
        return isOk;
    }

    public static boolean isValidCAFilePath(String filePath, String name) {
        boolean isOk = isValidPath(filePath);
        if (!isOk) {
            DatabaseManager.getMessageByMessageCode(6006, Optional.empty(), Optional.of(name));
        }
        return isOk;
    }

    public static boolean isValidPassword(String password, String confirmation) {
        return password != null && confirmation != null && password.length() >= 8 && password.length() <= 10 &&
                password.equals(confirmation);
    }

    public static boolean isUserValidPassword(String password, String confirmation, String name) {
        boolean isOk = isValidPassword(password, confirmation);
        /*conferir com o password do banco*/
        return isOk;
    }

    public static boolean isValidPhrase(String phrase) {
        // Implementar lógica de validação de frase secreta
        return phrase != null && !phrase.trim().isEmpty();
    }

    public static boolean isUserValidPhrase(String phrase) {
        boolean isOk = isValidPhrase(phrase);
        /*conferir com a decode da chave privada mais a assinatura do array de 8192bytes*/
        return isOk;
    }
}
