package com.review.Files;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;

import com.review.KeyManager;

public class Index {
    private String caminhoPasta;
    public IndexPaths indexPaths;

    private static IndexPaths getIndexPaths(String caminhoPasta) {
        IndexPaths indexPaths = new IndexPaths();
        indexPaths.envPath = caminhoPasta + File.separator + "index.env";
        indexPaths.encPath = caminhoPasta + File.separator + "index.enc";
        indexPaths.asdPath = caminhoPasta + File.separator + "index.asd";
        if (!new File(indexPaths.envPath).exists()) {
            // TODO: log
            System.out.println("Arquivo não encontrado: " + indexPaths.envPath);
        }
        if (!new File(indexPaths.encPath).exists()) {
            // TODO: log
            System.out.println("Arquivo não encontrado: " + indexPaths.encPath);
        }
        if (!new File(indexPaths.asdPath).exists()) {
            // TODO: log
            System.out.println("Arquivo não encontrado: " + indexPaths.asdPath);
        }
        return indexPaths;
    }

    Index(String caminhoPasta) {
        if (caminhoPasta == null || caminhoPasta.trim().isEmpty()) {
            throw new IllegalArgumentException("Caminho da pasta não pode ser nulo ou vazio");
        }
        File file = new File(caminhoPasta);
        if (!file.exists() || !file.isDirectory()) {
            throw new IllegalArgumentException("Caminho da pasta inválido");
        }

        this.caminhoPasta = caminhoPasta;
        this.indexPaths = Index.getIndexPaths(caminhoPasta);
    }

    public void processarEnvelope(PrivateKey admPk) {
        try {
            byte[] encryptedSeedPrng = Files.readAllBytes(Paths.get(this.indexPaths.envPath));
            byte[] seedPrng = KeyManager.decryptContentWithRSA(encryptedSeedPrng, admPk);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Erro ao processar o envelope: " + e.getMessage());
        }

    }
}
