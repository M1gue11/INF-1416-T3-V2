package com.review;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import javax.crypto.SecretKey;

import com.review.Files.Arquivo;
import com.review.Files.ArquivoModel;
import com.review.Files.Index;

public class ExecutionPipeline {
    static ExecutionPipeline instance = null;
    private PrivateKey admPrivateKey = null;
    private static final String DEFAULT_FILE_OUTPUT_FOLDER = "arquivos_output/";
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
        DatabaseManager.insereLog(1003, Optional.empty(), Optional.of(user));
    }

    public boolean bypassLoginWithAdm() {
        this.user = DatabaseManager.getUserByEmail("admin@inf1416.puc-rio.br");
        this.password = "12345678";
        this.admPrivateKey = null;
        confirmLogin();
        return true;
    }

    public boolean bypassLoginWithUser1() {
        this.user = DatabaseManager.getUserByEmail("miguel@gmail.com");
        this.password = "12345678";
        this.admPrivateKey = null;
        confirmLogin();
        return true;
    }

    public void bloquearUsuario() {
        DatabaseManager.bloquearUsuario(this.user.UID, Instant.now().getEpochSecond());
        this.user = DatabaseManager.getUserByEmail(this.user.email);
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
                    admChaveiro.caminho_chave_privada, true, this.user);

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
                System.out.println("Erro: Grupo inválido.");
            }

            if (!InputValidation.isValidPassword(senha, confirmacaoSenha)) {
                isFormOk = false;
                DatabaseManager.insereLog(6003, Optional.empty(), Optional.of(user));
                System.out.println("Erro: Senha inválida ou não coincide com a confirmação.");
            }

            if (!InputValidation.isValidEmail(email)) {
                isFormOk = false;
                System.out.println("Erro: Email inválido.");
            }

            if (!InputValidation.pkAndCaMatchPassphrase(fraseSec, caminhoCert, caminhoPk, true, this.user)) {
                isFormOk = false;
                DatabaseManager.insereLog(6007, Optional.empty(), Optional.of(user));
                System.out.println("Erro: Assinatura digital invalida");
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

    public void selecaoArquivo(ArquivoModel newSelection, String userPhrase, String caminhoPasta) {
        Optional<String> optFile = Optional.of(newSelection.nomeCodigoArquivo.toString());
        Optional<User> optUser = Optional.of(this.user);

        DatabaseManager.insereLog(7010, optFile, optUser);

        boolean isUserOwner = this.user.email.equals(newSelection.donoArquivo.getValue());
        if (!isUserOwner) {
            System.out.println("Arquivo pertence a outro usuario");
            // TODO: mensagem
            DatabaseManager.insereLog(7012, optFile, optUser);
            return;
        }

        System.out.println("Arquivo pertence ao usuario logado");
        try {
            DatabaseManager.insereLog(7011, optFile, optUser);
            Chaveiro chaveiroUser = DatabaseManager.getChaveiroByKID(this.user.KID);
            if (!InputValidation.pkAndCaMatchPassphrase(userPhrase, chaveiroUser.caminho_certificado,
                    chaveiroUser.caminho_chave_privada, true, this.user)) {
                System.out.println("Erro: Assinatura digital invalida");
                return;
            }
            PrivateKey pk = PrivateKeyManager.decryptAndReturnPk(chaveiroUser.caminho_chave_privada,
                    userPhrase);
            PublicKey ca = PrivateKeyManager.loadCaFromFile(chaveiroUser.caminho_certificado)
                    .getPublicKey();
            Index index = new Index(caminhoPasta, newSelection.nomeCodigoArquivo.getValue());
            if (!index.processarDotAsd(ca)) {
                System.out.println(
                        "Erro ao processar arquivo .asd do arquivo selecionado: " + newSelection.nomeCodigoArquivo);
                return;
            }
            DatabaseManager.insereLog(7014, optFile, optUser);
            SecretKey aesKey = index.processarDotEnv(pk);
            String conteudoArquivo = index.processarDotEnc(aesKey);
            DatabaseManager.insereLog(7013, optFile, optUser);
            System.out.println("Conteudo do arquivo: " + conteudoArquivo);

            // escrevendo o conteudo do arquivo no disco
            Path outputPath = Paths.get(DEFAULT_FILE_OUTPUT_FOLDER);
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }
            Path caminhoArquivo = Paths.get(DEFAULT_FILE_OUTPUT_FOLDER, newSelection.nomeSecretoArquivo.getValue());
            Files.writeString(caminhoArquivo, conteudoArquivo);
            System.out.println("Arquivo escrito em: " + caminhoArquivo.toAbsolutePath());
        } catch (Exception e) {
            System.out.println("Erro ao abrir arquivo selecionado: " + e.getMessage());
            return;
        }

    }

    public void limparArquivosDescriptografados() {
        try {
            Path outputPath = Paths.get(DEFAULT_FILE_OUTPUT_FOLDER);
            if (Files.exists(outputPath)) {
                Files.list(outputPath).forEach(file -> {
                    try {
                        Files.delete(file);
                    } catch (Exception e) {
                        System.out.println("Erro ao deletar arquivo: " + file + " - " + e.getMessage());
                    }
                });
            }
        } catch (Exception e) {
            System.out.println("Erro ao limpar arquivos descriptografados: " + e.getMessage());
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
                admChaveiro.caminho_chave_privada, true, this.user);
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
                DatabaseManager.insereLog(7006, Optional.empty(), Optional.of(this.user));
                System.out.println("Arquivo .asd (assinatura) validado com sucesso!");
            } else {
                DatabaseManager.insereLog(7008, Optional.empty(), Optional.of(this.user));
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
