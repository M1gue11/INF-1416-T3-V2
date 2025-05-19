package com.review;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class ExecutionPipeline {
    static ExecutionPipeline instance = null;
    private PrivateKey AdmPassphrase = null;
    private String password = null;
    public User user = null;
    public boolean isLogado;

    public void logout() {
        ExecutionPipeline.instance = new ExecutionPipeline();
        this.user = new User();
        this.user.fetchDefault();
    }

    public String getPassword() {
        if (this.password == null) {
            throw new IllegalStateException("Password not set");
        }
        return this.password;
    }

    public void setPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        if (this.password != null) {
            throw new IllegalArgumentException("Password already set");
        }
        this.password = password;
    }

    public PrivateKey getAdmPassphrase() {
        if (this.AdmPassphrase == null) {
            throw new IllegalStateException("Passphrase not set");
        }
        return this.AdmPassphrase;
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

    public boolean isValidTOTP(String codigoTotp) {
        if (codigoTotp == null || codigoTotp.isEmpty() || codigoTotp.length() != 6) {
            return false;
        }
        return InputValidation.isValidTOTP(codigoTotp, user.chave_totp_cript, this.password);
    }

    public boolean bypassLogin() {
        this.user = DatabaseManager.getUserByEmail("teste@teste.com");
        this.password = "12345678";
        this.AdmPassphrase = null;
        return true;
    }

    public void listFiles(String caminhoPasta, String fraseSecretaAdm) {
        if (!InputValidation.isValidPath(caminhoPasta)) {
            return;
        }
        Chaveiro admChaveiro = DatabaseManager.getChaveiroSuperAdm();
        boolean isValidPassphrase = InputValidation.pkAndCaMatchPassphrase(
                fraseSecretaAdm, admChaveiro.caminho_certificado,
                admChaveiro.caminho_chave_privada, true);
        if (!isValidPassphrase) {
            // TODO: log
            System.out.println("Frase secreta do adm inválida");
            return;
        }
        try {
            this.AdmPassphrase = PrivateKeyManager.decryptAndReturnPk(admChaveiro.caminho_chave_privada,
                    fraseSecretaAdm);
            // TODO: incrementar total de consultas do usuario

        } catch (Exception e) {
            // e.printStackTrace();
            System.err.println("Erro ao listar arquivos: " + e.getMessage());
        }

        InputValidation.isValidPhrase(fraseSecretaAdm);
    }
}
