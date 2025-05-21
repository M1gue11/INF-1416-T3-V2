package com.review;

import com.review.view.CofreApp;

import javafx.application.Application;

public class Main {
    public static void main(String[] args) throws Exception {
        // PrivateKey pk = PrivateKeyManager.decryptAndReturnPk(
        // "C:\\Users\\migue\\OneDrive\\Documents\\PUC\\INF 1416 -
        // seguranca\\INF-1416-T3-V2\\Keys_anderson\\admin-pkcs8-aes.key",
        // "admin");
        // System.out.println("pk " + pk);
        // String phrase = "admin";
        // String caPath = "C:\\Users\\migue\\OneDrive\\Documents\\PUC\\INF 1416 -
        // seguranca\\INF-1416-T3-V2\\Keys_anderson\\admin-x509.crt";
        // String pkPath = "C:\\Users\\migue\\OneDrive\\Documents\\PUC\\INF 1416 -
        // seguranca\\INF-1416-T3-V2\\Keys_anderson\\admin-pkcs8-aes.key";
        // var bool = InputValidation.pkAndCaMatchPassphrase(phrase, caPath, pkPath,
        // true, null);
        // System.out.println(bool);
        // DatabaseManager.executeUpdateSql("DELETE FROM usuario WHERE UID = 2");
        // DatabaseManager.executeUpdateSql("DELETE FROM chaveiro WHERE KID = 2");
        Application.launch(CofreApp.class, args);
    }
}