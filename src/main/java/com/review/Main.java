package com.review;

import com.review.view.CofreApp;

import javafx.application.Application;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Application.launch(CofreApp.class, args);
        String pkPath = "minha_chave_privada_ca.bin";
        var caPath = "meu_certificado_ca.pem";
        var sec = "1234";
        ExecutionPipeline pipeline = ExecutionPipeline.getInstance();
        pipeline.cadastro(caPath, pkPath, sec, "Administrador", "12345678",
                "12345678");

//        KeyManager.generateDefaultCA();

//        ArvoreSenha arvore = new ArvoreSenha();
//
//        arvore.inserirOpcao(1, 10, 20);
//        arvore.inserirOpcao(2, 30, 40);
//        arvore.inserirOpcao(3, 50, 60);
//
//        // Opção 1: Números concatenados
//        List<String> sequencias = arvore.gerarSequenciasNumericas();
//        System.out.println("Sequências numéricas:");
//        sequencias.forEach(System.out::println);


    }
}