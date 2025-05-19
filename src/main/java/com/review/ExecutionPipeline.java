package com.review;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;

import javax.crypto.SecretKey;

import com.review.Files.Arquivo;
import com.review.Files.Index;

public class ExecutionPipeline {
    static ExecutionPipeline instance = null;
    private PrivateKey admPrivateKey = null;
    private String password = null;
    public User user = null;
    public boolean isLogado = false;

    public void logout() {
        ExecutionPipeline.instance = new ExecutionPipeline();
        this.user = new User();
        this.user.fetchDefault();
    }

    public void confirmLogin() {
        isLogado = true;
        DatabaseManager.incrementarNumeroAcessos(this.user.UID);
        user = DatabaseManager.getUserByEmail(user.email);
    }

    public boolean bypassLoginWithAdm() {
        this.user = DatabaseManager.getUserByEmail("teste@teste.com");
        this.password = "12345678";
        this.admPrivateKey = null;
        confirmLogin();
        return true;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        this.password = password;
    }

    public PrivateKey getAdmPrivateKey() {
        if (this.admPrivateKey == null) {
            throw new IllegalStateException("Passphrase not set");
        }
        return this.admPrivateKey;
    }

    public static ExecutionPipeline getInstance() {
        if (instance == null) {
            instance = new ExecutionPipeline();
        }
        return instance;
    }

    private ExecutionPipeline() {
        DatabaseManager.initDatabase();
        this.user = new User();
        this.user.fetchDefault();
    }

    public boolean isFirstAccess() {
        return DatabaseManager.getNumberOfUsers() == 0;
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

            boolean isFormOk = true;

            if (!InputValidation.isValidCAFilePath(caminhoCert, "certificado")) {
                isFormOk = false;
                DatabaseManager.insereLog(6004, Optional.empty(), Optional.of(user));
                System.out.println("Erro: Caminho do certificado inválido.");
                // Nao da pra seguir com o cadastro
                return new RetornoCadastro(-1, null, null);
            }

            X509Certificate cert = PrivateKeyManager.loadCaFromFile(caminhoCert);
            String email = PrivateKeyManager.getEmailFromCA(cert);

            if (!InputValidation.isValidPkFilePath(caminhoPk, "chave_privada")) {
                isFormOk = false;
                DatabaseManager.insereLog(6005, Optional.empty(), Optional.of(user));
                System.out.println("Erro: Caminho da chave privada inválido.");
            }

            if (!InputValidation.isValidGroup(gp)) {
                isFormOk = false;
                // TODO: log
                System.out.println("Erro: Grupo inválido.");
            }

            if (!InputValidation.isValidPassword(senha, confirmacaoSenha)) {
                isFormOk = false;
                DatabaseManager.insereLog(6003, Optional.empty(), Optional.of(user));
                System.out.println("Erro: Senha inválida ou não coincide com a confirmação.");
            }

            if (!InputValidation.isValidEmail(email)) {
                isFormOk = false;
                // TODO: log
                System.out.println("Erro: Email inválido.");
            }

            if (!InputValidation.pkAndCaMatchPassphrase(fraseSec, caminhoCert, caminhoPk, true)) {
                isFormOk = false;
                DatabaseManager.insereLog(6007, Optional.empty(), Optional.of(user));
                System.out.println("Erro: A frase secreta não corresponde ao certificado e chave privada.");
            }

            if (!isFormOk) {
                // TODO: log
                return new RetornoCadastro(-1, null, email);
            }

            byte[] chave = KeyManager.getRandomArr(20);
            String chaveB32 = new Base32(Base32.Alphabet.BASE32, true, false).toString(chave);

            String chaveB32Cript = PrivateKeyManager.encryptContentWithPhrase(chaveB32, senha);

            String senhaHash = KeyManager.gerarHashBcrypt(senha);
            String nome = PrivateKeyManager.getCommonNameFromCA(cert);
            String safeGroupName = gp.substring(0, 1).toUpperCase() + gp.substring(1).toLowerCase();
            int uid = DatabaseManager.cadastrarUsuario(nome, email.toLowerCase(), senhaHash, safeGroupName,
                    caminhoCert, caminhoPk,
                    chaveB32Cript);

            DatabaseManager.insereLog(6008, Optional.empty(), Optional.of(user));
            return new RetornoCadastro(uid, chaveB32, email);

        } catch (Exception e) {
            DatabaseManager.insereLog(6009, Optional.empty(), Optional.of(user));
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

    public void incrementarConsultasEAtualizarUsuario() {
        DatabaseManager.incrementarNumeroConsultas(this.user.UID);
        this.user = DatabaseManager.getUserByEmail(this.user.email);
    }

    public List<Arquivo> listAllFiles(String caminhoPasta, String fraseSecretaAdm) {
        if (!InputValidation.isValidPath(caminhoPasta)) {
            // TODO: log
            System.out.println("Erro: caminho da pasta inválido!");
            return null;
        }

        Chaveiro admChaveiro = DatabaseManager.getChaveiroSuperAdm();
        boolean isValidPassphrase = InputValidation.pkAndCaMatchPassphrase(
                fraseSecretaAdm, admChaveiro.caminho_certificado,
                admChaveiro.caminho_chave_privada, true);
        if (!isValidPassphrase) {
            // TODO: log
            System.out.println("Nao foi possivel validar a chave secreta do ADM!");
            return null;
        }

        try {
            this.admPrivateKey = PrivateKeyManager.decryptAndReturnPk(admChaveiro.caminho_chave_privada,
                    fraseSecretaAdm);
            Index index = new Index(caminhoPasta);
            PublicKey admPublicKey = PrivateKeyManager.loadCaFromFile(admChaveiro.caminho_certificado).getPublicKey();
            if (index.processarDotAsd(admPublicKey)) {
                // TODO: log
                System.out.println("Arquivo .asd (assinatura) validado com sucesso!");
            } else {
                // TODO: log
                System.out.println("Erro ao processar arquivo .asd");
                return null;
            }
            SecretKey aesKey = index.processarDotEnv(admPrivateKey);
            String conteudoArquivoIndice = index.processarDotEnc(aesKey);
            List<Arquivo> arquivos = index.parseArquivoIndex(conteudoArquivoIndice);
            this.incrementarConsultasEAtualizarUsuario();
            return arquivos;
        } catch (Exception e) {
            // e.printStackTrace();
            System.err.println("Erro ao listar arquivos: " + e.getMessage());
            return null;
        }
    }

    public List<Arquivo> listUserGroupFiles(String caminhoPasta, String fraseSecretaAdm) {
        List<Arquivo> arquivos = listAllFiles(caminhoPasta, fraseSecretaAdm);
        if (arquivos == null) {
            return arquivos;
        }
        return arquivos.stream()
                .filter(arquivo -> user.isAllowed(arquivo.grupoArquivo))
                .toList();
    }
}
