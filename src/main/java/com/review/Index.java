package com.review;

import java.io.File;

public class Index {

    Index(String caminhoPasta) {
        if (caminhoPasta == null || caminhoPasta.trim().isEmpty()) {
            throw new IllegalArgumentException("Caminho da pasta não pode ser nulo ou vazio");
        }
        File file = new File(caminhoPasta);
        if (!file.exists() || !file.isDirectory()) {
            throw new IllegalArgumentException("Caminho da pasta inválido");
        }

    }
}
