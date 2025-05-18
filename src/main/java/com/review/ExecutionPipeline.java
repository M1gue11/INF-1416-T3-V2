package com.review;

import java.security.cert.X509Certificate;

public class ExecutionPipeline {
    static ExecutionPipeline instance = null;

    public static ExecutionPipeline getInstance() {
        if (instance == null) {
            instance = new ExecutionPipeline();
        }
        return instance;
    }

    private ExecutionPipeline() {
        DatabaseManager.initDatabase();
    }

    public boolean isFirstAccess() {
        System.out.println(DatabaseManager.getNumberOfUsers());
        return DatabaseManager.getNumberOfUsers() == 0;
    }

    public void login(String email, String passphrase, String senha) {
        // var cert = PrivateKeyManager.loadCertificateFromFile(email);
    }

    public boolean admPassphraseValidation(String passphrase) {
        return false;
    }

    public int cadastro(String caminhoCert, String caminhoPk, String fraseSec,
            String gp, String senha, String confirmacaoSenha) {
        try {
            X509Certificate cert = PrivateKeyManager.loadCertificateFromFile(caminhoCert);
            String email = PrivateKeyManager.getEmailFromCA(cert);
            boolean isFormOk = InputValidation.isValidCAFilePath(caminhoCert, "certificado")
                    && InputValidation.isValidPkFilePath(caminhoPk, "chave_privada")
                    // && InputValidation.isValidGroup(gp);
                    && InputValidation.isValidPassword(senha, confirmacaoSenha)
                    && InputValidation.isValidEmail(email)
                    && InputValidation.pkAndCaMatchPassphrase(fraseSec, caminhoCert, caminhoPk,
                            true);

            if (!isFormOk) {
                // TODO: log
                System.out.println("Formulário inválido");
                return -1;
            }

            String senhaHash = KeyManager.gerarHashBcrypt(senha);
            String nome = PrivateKeyManager.getCommonNameFromCA(cert);
            int uid = DatabaseManager.cadastrarUsuario(nome, email, senhaHash, gp, caminhoCert, caminhoPk);
            return uid;

        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}
