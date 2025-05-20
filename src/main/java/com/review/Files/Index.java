package com.review.Files;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

import com.review.Chaveiro;
import com.review.DatabaseManager;
import com.review.KeyManager;
import com.review.PrivateKeyManager;

public class Index {
    public IndexPaths indexPaths;

    // Classe interna para agrupar caminhos, como você já tem
    public static class IndexPaths {
        public String envPath;
        public String encPath;
        public String asdPath;
    }

    private static IndexPaths getFilesPaths(String caminhoPasta, String arquivo) {
        IndexPaths paths = new IndexPaths();
        paths.envPath = Paths.get(caminhoPasta, String.format("%s.env", arquivo)).toString();
        paths.encPath = Paths.get(caminhoPasta, String.format("%s.enc", arquivo)).toString();
        paths.asdPath = Paths.get(caminhoPasta, String.format("%s.asd", arquivo)).toString();
        // A verificação de existência é melhor feita antes de tentar ler/escrever
        return paths;
    }

    private static IndexPaths getIndexPaths(String caminhoPasta) {
        return getFilesPaths(caminhoPasta, "index");
    }

    public Index(String caminhoPasta) {
        if (caminhoPasta == null || caminhoPasta.trim().isEmpty()) {
            throw new IllegalArgumentException("Caminho da pasta não pode ser nulo ou vazio");
        }
        File folder = new File(caminhoPasta);
        if (!folder.exists() || !folder.isDirectory()) {
            // Tenta criar o diretório se não existir, para facilitar testes
            if (!folder.mkdirs()) {
                throw new IllegalArgumentException("Caminho da pasta inválido ou não pôde ser criado: " + caminhoPasta);
            }
        }
        this.indexPaths = Index.getIndexPaths(caminhoPasta);
    }

    public Index(String caminhoPasta, String codigoArquivo) {
        if (caminhoPasta == null || caminhoPasta.trim().isEmpty()) {
            throw new IllegalArgumentException("Caminho da pasta não pode ser nulo ou vazio");
        }
        File folder = new File(caminhoPasta);
        if (!folder.exists() || !folder.isDirectory()) {
            // Tenta criar o diretório se não existir, para facilitar testes
            if (!folder.mkdirs()) {
                throw new IllegalArgumentException("Caminho da pasta inválido ou não pôde ser criado: " + caminhoPasta);
            }
        }
        this.indexPaths = Index.getFilesPaths(caminhoPasta, codigoArquivo);
    }

    public SecretKey processarDotEnv(PrivateKey pk) throws Exception {
        byte[] encryptedSeedPrng = Files.readAllBytes(Paths.get(this.indexPaths.envPath));
        byte[] seedPrngBytes = KeyManager.decryptContentWithRSA(encryptedSeedPrng, pk);
        return PrivateKeyManager.deriveAesKeyFromGivenSeed(seedPrngBytes);
    }

    public boolean processarDotAsd(PublicKey pk) throws Exception {
        byte[] conteudoCript = Files.readAllBytes(Paths.get(this.indexPaths.encPath));
        byte[] assinaturaBin = Files.readAllBytes(Paths.get(this.indexPaths.asdPath));
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(pk);
        sig.update(conteudoCript);
        boolean isValid = sig.verify(assinaturaBin);
        if (!isValid) {
            System.err.println("Falha na verificação de integridade/autenticidade do arquivo de índice.");
            return false;
        }
        return isValid;
    }

    public String processarDotEnc(SecretKey aesKey) throws Exception {
        byte[] conteudoCript = Files.readAllBytes(Paths.get(this.indexPaths.encPath));
        byte[] conteudoDescript = PrivateKeyManager.decryptContentWithAES(conteudoCript, aesKey);
        return new String(conteudoDescript, StandardCharsets.UTF_8); // Especificar Charset
    }

    public List<Arquivo> parseArquivoIndex(String conteudoBrutoDescriptografado) {
        if (conteudoBrutoDescriptografado == null || conteudoBrutoDescriptografado.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // Melhor para split de nova linha multiplataforma
        String[] linhas = conteudoBrutoDescriptografado.split("\\R");
        ArrayList<Arquivo> arquivos = new ArrayList<>();
        for (String linha : linhas) {
            if (linha.trim().isEmpty())
                continue;
            String[] partes = linha.split(" ", 4);
            if (partes.length == 4) {
                String nomeCodigoArquivo = partes[0];
                String nomeSecretoArquivo = partes[1];
                String donoArquivo = partes[2];
                String grupoArquivo = partes[3];
                Arquivo a = new Arquivo(nomeCodigoArquivo, nomeSecretoArquivo, donoArquivo, grupoArquivo);
                arquivos.add(a);
            } else {
                System.err.println("Linha do índice mal formatada: " + linha);
                // TODO: Logar erro de formatação da linha
            }
        }
        // TODO: log 7009 Lista de arquivos presentes no índice apresentada para
        // <login_name>.
        return arquivos;
    }

    // --- MÉTODOS PARA CRIAR ARQUIVOS DE ÍNDICE PARA TESTE ---

    /**
     * Cria o conteúdo do arquivo de índice em formato de string.
     * 
     * @param arquivosLista Lista de objetos Arquivo a serem incluídos no índice.
     * @return String formatada para o arquivo de índice.
     */
    public static String criarConteudoStringDoIndice(List<Arquivo> arquivosLista) {
        StringBuilder sb = new StringBuilder();
        for (Arquivo arq : arquivosLista) {
            sb.append(arq.nomeCodigoArquivo).append(" ")
                    .append(arq.nomeSecretoArquivo).append(" ")
                    .append(arq.donoArquivo).append(" ")
                    .append(arq.grupoArquivo).append(System.lineSeparator()); // Usar System.lineSeparator()
        }
        return sb.toString();
    }

    /**
     * Cria e salva os arquivos index.enc, index.env e index.asd.
     *
     * @param caminhoPastaDestino Pasta onde os arquivos de índice serão criados.
     * @param conteudoIndicePlain Texto plano do conteúdo do índice (geralmente
     *                            vindo de criarConteudoStringDoIndice).
     * @param adminPublicKey      Chave pública do administrador (para criptografar
     *                            a semente/chave do .env).
     * @param adminPrivateKey     Chave privada do administrador (para assinar o
     *                            .enc -> .asd).
     * @return O objeto IndexPaths com os caminhos para os arquivos criados, ou null
     *         em caso de falha.
     */
    public static IndexPaths criarArquivosDeIndice(String caminhoPastaDestino,
            String conteudoIndicePlain,
            PublicKey adminPublicKey,
            PrivateKey adminPrivateKey) {
        try {
            File folder = new File(caminhoPastaDestino);
            if (!folder.exists()) {
                if (!folder.mkdirs()) {
                    System.err.println("Não foi possível criar a pasta de destino: " + caminhoPastaDestino);
                    return null;
                }
            }

            IndexPaths paths = getIndexPaths(caminhoPastaDestino); // Reutiliza para obter os nomes de arquivo

            // 1. Gerar uma semente aleatória para a chave AES do índice
            // Ou gerar a chave AES diretamente e criptografá-la.
            // A especificação diz: "protege a semente SHA1PRNG que gera a chave secreta
            // AES"
            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG"); // Conforme especificação
            byte[] seedBytes = new byte[20]; // Tamanho arbitrário para a semente, pode ser 16, 20, 32...
            sr.nextBytes(seedBytes);

            // Derivar a chave AES a partir desta semente (para criptografar o index.enc)
            SecretKey aesKeyParaIndice = PrivateKeyManager.deriveAesKeyFromGivenSeed(seedBytes);

            // 2. Criar index.env: Criptografar a semente (seedBytes) com a chave pública do
            // admin
            byte[] encryptedSeedForEnv = KeyManager.encryptContentWithRSA(seedBytes, adminPublicKey);
            Files.write(Paths.get(paths.envPath), encryptedSeedForEnv);
            System.out.println("index.env criado em: " + paths.envPath);

            // 3. Criar index.enc: Criptografar o conteudoIndicePlain com a aesKeyParaIndice
            byte[] plainIndexBytes = conteudoIndicePlain.getBytes(StandardCharsets.UTF_8);
            byte[] encryptedIndexContent = PrivateKeyManager.encryptContentWithAES(plainIndexBytes, aesKeyParaIndice);
            Files.write(Paths.get(paths.encPath), encryptedIndexContent);
            System.out.println("index.enc criado em: " + paths.encPath);

            // 4. Criar index.asd: Assinar o encryptedIndexContent (ou plainIndexBytes, a
            // especificação diz "assinatura digital do arquivo de índice")
            // É mais comum assinar o conteúdo original (plain) ou o conteúdo criptografado.
            // A especificação diz: "assinatura digital do arquivo de índice é armazenada no
            // arquivo index.asd (representação binária da assinatura digital)"
            // E depois: "verificar a integridade e autenticidade do arquivo de índice"
            // (implicando o .enc)
            // Vamos assumir que se assina o CONTEÚDO CRIPTOGRAFADO (index.enc)
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(adminPrivateKey);
            sig.update(encryptedIndexContent); // Assinando o conteúdo do index.enc

            var hexString = KeyManager.sha256(encryptedIndexContent);
            System.out.println("SHA-256 hash de index.enc: " + hexString.toString());

            byte[] signatureBytes = sig.sign();
            Files.write(Paths.get(paths.asdPath), signatureBytes);
            System.out.println("index.asd criado em: " + paths.asdPath);

            return paths;

        } catch (Exception e) {
            System.err.println("Erro ao criar arquivos de índice: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Método estático de conveniência para criar um conjunto de arquivos de índice
     * de teste.
     */
    public static void gerarIndiceDeTeste(String pastaDeTeste, PublicKey adminPublicKey, PrivateKey adminPrivateKey) {
        ArrayList<Arquivo> listaArquivosTeste = new ArrayList<>();
        listaArquivosTeste.add(new Arquivo("cod1", "segredoA.txt", "miguel@gmail.com", "Usuario"));
        listaArquivosTeste.add(new Arquivo("cod2", "relatorioX.docx", "teste@teste.com", "Administrador"));
        listaArquivosTeste.add(new Arquivo("cod3", "foto_ferias.jpg", "miguel@gmail.com", "Usuario"));
        listaArquivosTeste.add(new Arquivo("cod4", "backup.zip", "miguel@gmail.com", "Usuario"));

        String conteudoPlain = criarConteudoStringDoIndice(listaArquivosTeste);
        System.out.println("--- Conteúdo Plain do Índice de Teste ---");
        System.out.println(conteudoPlain);
        System.out.println("--------------------------------------");

        IndexPaths pathsCriados = criarArquivosDeIndice(pastaDeTeste, conteudoPlain, adminPublicKey, adminPrivateKey);
        if (pathsCriados != null) {
            System.out.println("Arquivos de índice de teste criados com sucesso em: " + pastaDeTeste);
        } else {
            System.err.println("Falha ao criar arquivos de índice de teste.");
        }
    }

    /**
     * Cria os arquivos .enc, .env e .asd para um único arquivo secreto.
     *
     * @param caminhoPastaDestino  Pasta onde os arquivos do segredo serão criados.
     * @param nomeCodigoArquivo    O nome de código do arquivo (ex: "cod1").
     * @param conteudoArquivoPlain O conteúdo em texto plano do arquivo secreto.
     * @param donoPublicKey        Chave pública do dono do arquivo (para
     *                             criptografar a semente/chave do .env).
     * @param donoPrivateKey       Chave privada do dono do arquivo (para assinar o
     *                             .enc -> .asd).
     * @return true se os arquivos foram criados com sucesso, false caso contrário.
     */
    public static boolean criarArquivosDeSegredoIndividual(String caminhoPastaDestino,
            String nomeCodigoArquivo,
            String conteudoArquivoPlain,
            PublicKey donoPublicKey,
            PrivateKey donoPrivateKey) {
        try {
            File folder = new File(caminhoPastaDestino);
            if (!folder.exists() && !folder.mkdirs()) {
                System.err.println("Não foi possível criar a pasta de destino para o segredo: " + caminhoPastaDestino);
                return false;
            }

            Path envPath = Paths.get(caminhoPastaDestino, nomeCodigoArquivo + ".env");
            Path encPath = Paths.get(caminhoPastaDestino, nomeCodigoArquivo + ".enc");
            Path asdPath = Paths.get(caminhoPastaDestino, nomeCodigoArquivo + ".asd");

            // 1. Gerar uma semente aleatória para a chave AES deste arquivo secreto
            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            byte[] seedBytesArquivo = new byte[20]; // Semente para a chave AES do arquivo
            sr.nextBytes(seedBytesArquivo);

            // Derivar a chave AES a partir desta semente
            SecretKey aesKeyParaArquivo = PrivateKeyManager.deriveAesKeyFromGivenSeed(seedBytesArquivo);

            // 2. Criar <nomeCodigo>.env: Criptografar a semente (seedBytesArquivo) com a
            // chave pública do DONO
            byte[] encryptedSeedForFileEnv = KeyManager.encryptContentWithRSA(seedBytesArquivo, donoPublicKey);
            Files.write(envPath, encryptedSeedForFileEnv);
            // System.out.println(nomeCodigoArquivo + ".env criado.");

            // 3. Criar <nomeCodigo>.enc: Criptografar o conteudoArquivoPlain com a
            // aesKeyParaArquivo
            byte[] plainFileBytes = conteudoArquivoPlain.getBytes(StandardCharsets.UTF_8);
            byte[] encryptedFileContent = PrivateKeyManager.encryptContentWithAES(plainFileBytes, aesKeyParaArquivo);
            Files.write(encPath, encryptedFileContent);
            // System.out.println(nomeCodigoArquivo + ".enc criado.");

            // 4. Criar <nomeCodigo>.asd: Assinar o encryptedFileContent com a chave privada
            // do DONO
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(donoPrivateKey);
            sig.update(encryptedFileContent); // Assinando o conteúdo do .enc do arquivo
            byte[] signatureFileBytes = sig.sign();
            Files.write(asdPath, signatureFileBytes);
            // System.out.println(nomeCodigoArquivo + ".asd criado.");

            return true;

        } catch (Exception e) {
            System.err.println(
                    "Erro ao criar arquivos para o segredo individual " + nomeCodigoArquivo + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static void gerarIndiceDeTesteEArquivosSecretos(String pastaDeTeste,
            PublicKey adminPublicKey, PrivateKey adminPrivateKey,
            PublicKey userPublicKey, PrivateKey userPrivateKey) {
        System.out.println("--- Iniciando Geração de Índice de Teste e Arquivos Secretos ---");

        // 1. Definir os arquivos que estarão no índice
        ArrayList<Arquivo> listaArquivosParaIndice = new ArrayList<>();
        listaArquivosParaIndice
                .add(new Arquivo("cod1_usr1", "documento_pessoal.txt", "miguel@gmail.com", "Usuario"));
        listaArquivosParaIndice
                .add(new Arquivo("cod2_adm", "config_sistema.dat", "teste@teste.com", "Administrador"));
        listaArquivosParaIndice.add(new Arquivo("cod3_usr1", "notas_reuniao.md", "miguel@gmail.com", "Usuario"));
        listaArquivosParaIndice
                .add(new Arquivo("cod4_usr1", "foto_importante.txt", "miguel@gmail.com", "Usuario"));

        // 2. Criar o conteúdo string do índice
        String conteudoPlainDoIndice = criarConteudoStringDoIndice(listaArquivosParaIndice);
        System.out.println("\n--- Conteúdo Plain do Índice ---");
        System.out.println(conteudoPlainDoIndice);

        // 3. Criar os arquivos de índice (index.env, index.enc, index.asd)
        IndexPaths pathsIndiceCriados = criarArquivosDeIndice(pastaDeTeste, conteudoPlainDoIndice, adminPublicKey,
                adminPrivateKey);
        if (pathsIndiceCriados != null) {
            System.out.println("\n--- Arquivos de Índice Globais Criados ---");
            System.out.println("index.env, index.enc, index.asd em: " + pastaDeTeste);
        } else {
            System.err.println("Falha ao criar arquivos de índice globais.");
            return; // Não podemos continuar se o índice não foi criado
        }

        // 4. Criar os arquivos secretos individuais (.env, .enc, .asd para cada um)
        System.out.println("\n--- Criando Arquivos Secretos Individuais ---");
        boolean sucessoArquivosSecretos = true;

        // Arquivo de usuario1@exemplo.com
        sucessoArquivosSecretos &= criarArquivosDeSegredoIndividual(pastaDeTeste, "cod1_usr1",
                "Este é o conteúdo secreto do documento pessoal de usuario1.",
                userPublicKey, userPrivateKey);
        sucessoArquivosSecretos &= criarArquivosDeSegredoIndividual(pastaDeTeste, "cod4_usr1",
                "Isso seria uma foto importante, mas está em texto.",
                userPublicKey, userPrivateKey);

        // Arquivo de admin@exemplo.com (neste caso, o admin é o dono)
        sucessoArquivosSecretos &= criarArquivosDeSegredoIndividual(pastaDeTeste, "cod2_adm",
                "Configurações secretas do sistema: python_path=userfiles/python.",
                adminPublicKey, adminPrivateKey); // Admin usando suas próprias chaves como dono

        // Arquivo de usuario2@exemplo.com
        sucessoArquivosSecretos &= criarArquivosDeSegredoIndividual(pastaDeTeste, "cod3_usr1",
                "# Notas da Reunião\n- Discutir item A\n- Revisar item B",
                userPublicKey, userPrivateKey);

        if (sucessoArquivosSecretos) {
            System.out.println(
                    "\nTodos os arquivos secretos individuais (.env, .enc, .asd) foram criados com sucesso na pasta: "
                            + pastaDeTeste);
        } else {
            System.err.println("\nFalha ao criar um ou mais arquivos secretos individuais.");
        }

        System.out.println("\n--- Geração de Teste Concluída ---");
    }

    public static void main(String[] args) throws Exception {
        // Para este teste funcionar, você precisa gerar um par de chaves RSA
        // e carregá-los aqui. Exemplo usando KeyPairGenerator:
        String pastaParaTeste = "pasta_indice_teste"; // Será criada se não existir

        Chaveiro admChav = DatabaseManager.getChaveiroSuperAdm();
        String passphraseAdm = "teste";
        PrivateKey adminPrivateKey = PrivateKeyManager
                .decryptAndReturnPk(admChav.caminho_chave_privada, passphraseAdm);
        PublicKey adminPublicKey = PrivateKeyManager.loadCaFromFile(admChav.caminho_certificado).getPublicKey();

        Chaveiro chaveiroMiguel = DatabaseManager.getChaveiroByKID(2);
        String passphraseMiguel = "miguel";
        PrivateKey miguelPk = PrivateKeyManager
                .decryptAndReturnPk(chaveiroMiguel.caminho_chave_privada, passphraseMiguel);
        PublicKey miguelCA = PrivateKeyManager.loadCaFromFile(chaveiroMiguel.caminho_certificado).getPublicKey();

        // Testar a GERAÇÃO dos arquivos de índice
        Index.gerarIndiceDeTesteEArquivosSecretos(pastaParaTeste, adminPublicKey, adminPrivateKey, miguelCA, miguelPk);

        System.out.println("\n--- Testando a LEITURA dos arquivos de índice criados ---");
        // Testar a LEITURA dos arquivos de índice
        Index leitorIndice = new Index(pastaParaTeste); // Construtor deve funcionar se a pasta foi criada

        // 1. Simular o processamento do .env para obter a chave AES do índice
        SecretKey aesKeyDoIndice = leitorIndice.processarDotEnv(adminPrivateKey);
        if (aesKeyDoIndice == null) {
            System.err.println("Falha ao obter a chave AES do index.env");
            return;
        }
        System.out.println("Chave AES do índice obtida do index.env com sucesso.");

        // 2. Simular a verificação do .asd
        boolean assinaturaValida = leitorIndice.processarDotAsd(adminPublicKey);
        if (!assinaturaValida) {
            System.err.println("Assinatura do index.asd inválida!");
            return;
        }
        System.out.println("Assinatura do index.asd verificada com sucesso.");

        // 3. Simular a descriptografia do .enc
        String conteudoIndiceDescriptografado = leitorIndice.processarDotEnc(aesKeyDoIndice);
        if (conteudoIndiceDescriptografado == null) {
            System.err.println("Falha ao descriptografar o index.enc");
            return;
        }
        System.out.println("Conteúdo do index.enc descriptografado com sucesso:");
        System.out.println(conteudoIndiceDescriptografado);

        // 4. Parsear o conteúdo
        List<Arquivo> arquivosLidos = leitorIndice.parseArquivoIndex(conteudoIndiceDescriptografado);
        System.out.println("Arquivos parseados do índice (" + arquivosLidos.size() + " entradas):");
        for (Arquivo arq : arquivosLidos) {
            System.out.println(" - Cod: " + arq.nomeCodigoArquivo + ", Sec: " + arq.nomeSecretoArquivo +
                    ", Dono: " + arq.donoArquivo + ", Grupo: " + arq.grupoArquivo);
        }
    }
}
