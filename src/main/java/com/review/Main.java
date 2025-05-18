package com.review;

import com.review.view.CofreApp;

import javafx.application.Application;

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
        String pkPath = "minha_chave_privada_ca.bin";
        var caPath = "meu_certificado_ca.pem";
        var sec = "1234";
        ExecutionPipeline pipeline = ExecutionPipeline.getInstance();
        pipeline.cadastro(caPath, pkPath, sec, "Administrador", "12345678",
                "12345678");
    }
}