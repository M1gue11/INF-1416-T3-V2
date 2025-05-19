package com.review;

import com.review.view.CofreApp;

import javafx.application.Application;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // try {
        // var foo = PrivateKeyManager.decryptContentWithPhrase(
        // "6HJBJPGZEMIDX7LPJBJ6FQCDV57KAKLVBLHN5UHY2PRRN7PED6DQI4PIJXIT26675GPYIHPUKYGHK===",
        // "12345678");
        // System.out.println("BAGOMANTE" + foo);
        // return;
        // } catch (Exception e) {
        // e.printStackTrace();
        // }

        Application.launch(CofreApp.class, args);
    }
}