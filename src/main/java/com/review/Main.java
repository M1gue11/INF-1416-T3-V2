package com.review;

import com.review.view.CofreApp;

import javafx.application.Application;

public class Main {
    public static void main(String[] args) {
        // String phrase = "admin";
        // String caPath = "C:\\Users\\migue\\OneDrive\\Documents\\PUC\\INF 1416 -
        // seguranca\\INF-1416-T3-V2\\Keys_anderson\\admin-x509.crt";
        // String pkPath = "C:\\Users\\migue\\OneDrive\\Documents\\PUC\\INF 1416 -
        // seguranca\\INF-1416-T3-V2\\Keys_anderson\\admin-pkcs8-aes.key";
        // var bool = InputValidation.pkAndCaMatchPassphrase(phrase, caPath, pkPath,
        // true, null);
        // System.out.println(bool);
        DatabaseManager.executeUpdateSql("DELETE FROM Registro");
        // DatabaseManager.executeUpdateSql("DELETE FROM chaveiro WHERE KID=10");
        // KeyManager.generateTestCA();
        Application.launch(CofreApp.class, args);
    }
}