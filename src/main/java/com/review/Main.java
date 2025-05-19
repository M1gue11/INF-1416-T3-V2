package com.review;

import com.review.view.CofreApp;

import javafx.application.Application;

public class Main {
    public static void main(String[] args) {
        DatabaseManager.executeUpdateSql("DELETE FROM Registro");
        // DatabaseManager.executeUpdateSql("DELETE FROM chaveiro WHERE KID=10");
        // KeyManager.generateTestCA();
        Application.launch(CofreApp.class, args);
    }
}