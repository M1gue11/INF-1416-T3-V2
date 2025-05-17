package com.review;

import java.io.File;
import java.util.Optional;

public class InputValidation {

    public static boolean isValidPkFilePath(String filePath, String name) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return false;
        }

        File file = new File(filePath);
        boolean isOk = file.exists() && file.isFile();
        if (!isOk) {
            DatabaseManager.getMessageByMessageCode(6005, Optional.empty(), Optional.of(name));
        }
        return isOk;
    }

    public static boolean isValidCAFilePath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return false;
        }

        File file = new File(filePath);
        return file.exists() && file.isFile();
    }

    public static boolean isValidPassword(String password, String confirmation) {
        return password != null && confirmation != null && password.length() >= 8 && password.length() <= 10 &&
                password.equals(confirmation);
    }

    public static boolean isValidPhrase(String phrase) {
        // Implementar lógica de validação de frase secreta
        return phrase != null && !phrase.trim().isEmpty();
    }
}
