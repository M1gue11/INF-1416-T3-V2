package com.review;

import java.security.cert.X509Certificate;

public class ExecutionPipeline {
    static ExecutionPipeline instance = null;
    private String passphrase = null;

    public void setPassphrase(String passphrase) {
        if (passphrase == null || passphrase.isEmpty()) {
            throw new IllegalArgumentException("Passphrase cannot be null or empty");
        }
        if (this.passphrase != null) {
            throw new IllegalArgumentException("Passphrase already set");
        }
        this.passphrase = passphrase;
    }

    public String getPassphrase() {
        if (this.passphrase == null) {
            throw new IllegalStateException("Passphrase not set");
        }
        return this.passphrase;
    }

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
        try {
            User adm = DatabaseManager.getSuperAdmin();
            Chaveiro admChaveiro = DatabaseManager.getChaveiroByKID(adm.KID);
            return InputValidation.pkAndCaMatchPassphrase(passphrase, admChaveiro.caminho_certificado,
                    admChaveiro.caminho_chave_privada, true);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    public RetornoCadastro cadastro(String caminhoCert, String caminhoPk, String fraseSec,
            String gp, String senha, String confirmacaoSenha) {
        try {
            X509Certificate cert = PrivateKeyManager.loadCaFromFile(caminhoCert);
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
                return new RetornoCadastro(-1, null, email);
            }

            byte[] chave = KeyManager.getRandomArr(20);
            String chaveB32 = new Base32(Base32.Alphabet.BASE32, true, false).toString(chave);

            System.out.println("segredo: " + chaveB32);
            String chaveB32Cript = PrivateKeyManager.encryptContentWithPhrase(chaveB32, senha);

            String senhaHash = KeyManager.gerarHashBcrypt(senha);
            String nome = PrivateKeyManager.getCommonNameFromCA(cert);
            int uid = DatabaseManager.cadastrarUsuario(nome, email, senhaHash, gp, caminhoCert, caminhoPk,
                    chaveB32Cript);
            return new RetornoCadastro(uid, chaveB32, email);

        } catch (Exception e) {
            e.printStackTrace();
            return new RetornoCadastro(-1, null, null);
        }
    }
}
