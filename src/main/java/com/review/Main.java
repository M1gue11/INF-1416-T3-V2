package com.review;

import com.review.view.CofreApp;

import javafx.application.Application;

public class Main {
    public static void main(String[] args) {
        Application.launch(CofreApp.class, args);
        String pkPath = "minha_chave_privada_ca.bin";
        var caPath = "meu_certificado_ca.pem";
        var sec = "1234";
        ExecutionPipeline pipeline = ExecutionPipeline.getInstance();
        pipeline.cadastro(caPath, pkPath, sec, "Administrador", "12345678",
                "12345678");
    }
}