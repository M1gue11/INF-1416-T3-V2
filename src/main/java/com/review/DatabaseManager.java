package com.review;

import java.sql.*;
import java.util.Arrays;
import java.util.Optional;

import static com.review.PasswordUtils.gerarHashBcrypt;
import static com.review.PasswordUtils.getRandomSalt;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:cofre_digital.db";

    public static void initDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            // Table User
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS Usuarios (" +
                            "UID INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "nome TEXT NOT NULL," +
                            "email TEXT UNIQUE NOT NULL," +
                            "senha_pessoal_hash TEXT NOT NULL," +
                            "salt TEXT NOT NULL," +
                            "grupo TEXT NOT NULL CHECK (grupo IN ('Administrador', 'Usuario'))," +  // Grupo como enum
                            "bloqueado BOOLEAN DEFAULT FALSE)"
            );

            // Tabela Chaveiro
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS Chaveiro (" +
                            "KID INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "UID INTEGER NOT NULL," +
                            "caminho_certificado TEXT NOT NULL," +  // Caminho do arquivo (255 chars)
                            "caminho_chave_privada TEXT NOT NULL," +  // Caminho do arquivo (255 chars)
                            "frase_secreta_hash TEXT NOT NULL," +  // Hash da frase secreta (255 chars)
                            "frase_secreta_salt TEXT NOT NULL," +
                            "FOREIGN KEY (UID) REFERENCES Usuarios(UID))"
            );

            // Tabela Grupos
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS Grupos (" +
                            "GID INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "nome TEXT UNIQUE NOT NULL)"
            );

            stmt.executeUpdate(
                    "INSERT OR IGNORE INTO Grupos (nome) VALUES " +
                            "('Administrador'), " +
                            "('Usuario')"
            );

            // Tabela Mensagens (logs de sistema)
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS Mensagens (" +
                            "MID INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "codigo INTEGER NOT NULL," +
                            "conteudo TEXT NOT NULL)"
            );

            // Tabela Registros
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS Registros (" +
                            "RID INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "UID INTEGER," +
                            "MID INTEGER NOT NULL," +
                            "data_hora TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                            "FOREIGN KEY (UID) REFERENCES Usuarios(UID)," +
                            "FOREIGN KEY (MID) REFERENCES Mensagens(MID))"
            );

        } catch (SQLException e) {
            System.err.println("Erro ao criar banco de dados: " + e.getMessage());
        }
    }

    public static int cadastrarUsuario(
            String nome,
            String email,
            String senhaPessoal,
            String grupo,
            String caminhoCertificado,
            String caminhoChavePrivada,
            String fraseSecreta
    ) {
        String sqlUsuario = "INSERT INTO Usuarios (nome, email, senha_pessoal_hash, salt, grupo) VALUES (?, ?, ?, ?, ?)";
        String sqlChaveiro = "INSERT INTO Chaveiro (UID, caminho_certificado, caminho_chave_privada, frase_secreta_hash, frase_secreta_salt) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);

            try (PreparedStatement pstmtUsuario = conn.prepareStatement(sqlUsuario, Statement.RETURN_GENERATED_KEYS)) {
                byte[] salt = getRandomSalt();
                String senhaHash = gerarHashBcrypt(senhaPessoal, salt);

                pstmtUsuario.setString(1, nome);
                pstmtUsuario.setString(2, email);
                pstmtUsuario.setString(3, senhaHash);
                pstmtUsuario.setString(4, Arrays.toString(salt));
                pstmtUsuario.setString(5, grupo);
                pstmtUsuario.executeUpdate();

                ResultSet rs = pstmtUsuario.getGeneratedKeys();
                if (!rs.next()) {
                    conn.rollback();
                    return -1;
                }
                int UID = rs.getInt(1);

                // chaveiro referente ao usuário
                try (PreparedStatement pstmtChaveiro = conn.prepareStatement(sqlChaveiro)) {
                    byte[] fraseSalt = getRandomSalt();
                    String fraseHash = gerarHashBcrypt(fraseSecreta, fraseSalt);

                    pstmtChaveiro.setInt(1, UID);
                    pstmtChaveiro.setString(2, caminhoCertificado);
                    pstmtChaveiro.setString(3, caminhoChavePrivada);
                    pstmtChaveiro.setString(4, fraseHash);
                    pstmtChaveiro.setString(5, Arrays.toString(fraseSalt));
                    pstmtChaveiro.executeUpdate();
                }

                conn.commit();
                return UID;
            }
        } catch (SQLException e) {
            System.err.println("Erro ao cadastrar usuário: " + e.getMessage());
            return -1;
        }
    }

    public static int buscarIdGrupo(String nomeGrupo) {
        String sql = "SELECT GID FROM Grupos WHERE nome = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nomeGrupo);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("GID");
            }

        } catch (SQLException e) {
            System.err.println("Erro ao buscar ID do grupo: " + e.getMessage());
        }
        return -1;
    }

    public static int inserirRegistro(Optional<Integer> uid, int mid) {
        String sql = "INSERT INTO Registros (UID, MID) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (uid.isPresent()) {
                pstmt.setInt(1, uid.get());
            } else {
                pstmt.setNull(1, Types.INTEGER); // UID NULL
            }
            pstmt.setInt(2, mid);
            pstmt.executeUpdate();

            // Retorna o RID gerado
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao inserir registro: " + e.getMessage());
        }
        return -1;
    }



    private static String getMessageByMessageCode(int codigo, Optional<String> arqName, Optional<String> loginName) {
        switch (codigo) {
            case 1001:
                return "Sistema iniciado.";
            case 1002:
                return "Sistema encerrado";
            case 1003:
                return "Sessão iniciada para " + loginName.get() + ".";
            case 1004:
                return "Sessão encerrada para" + loginName.get() + ".";
            case 1005:
                return "Partida do sistema iniciada para cadastro do administrador";
            case 1006:
                return "Partida do sistema iniciada para operação normal pelos usuários.";
            case 2001:
                return "Autenticação etapa 1 iniciada.";
            case 2002:
                return "Autenticação etapa 1 encerrada.";
            case 2003:
                return "Login name" + loginName.get() + " identificado com acesso liberado.";
            case 2004:
                return "Login name" + loginName.get() + " identificado com acesso bloqueado.";
            case 2005:
                return "Login name" + loginName.get() + " não identificado.";
            case 3001:
                return "Autenticação etapa 2 iniciada para " + loginName.get() + ".";
            case 3002:
                return "Autenticação etapa 2 encerrada para " + loginName.get() + ".";
            case 3003:
                return "Senha pessoal verificada positivamente para " + loginName.get() + ".";
            case 3004:
                return "Primeiro erro da senha pessoal contabilizado para " + loginName.get() + ".";
            case 3005:
                return "Segundo erro da senha pessoal contabilizado para " + loginName.get() + ".";
            case 3006:
                return "Terceiro erro da senha pessoal contabilizado para " + loginName.get() + ".";
            case 3007:
                return "Acesso do usuario " + loginName.get() + "bloqueado pela autenticação etapa 2.";
            case 4001:
                return "Autenticação etapa 3 iniciada para " + loginName.get() + ".";
            case 4002:
                return "Autenticação etapa 3 encerrada para " + loginName.get() + ".";
            case 4003:
                return "Token verificado positivamente para " + loginName.get() + ".";
            case 4004:
                return "Primeiro erro de token contabilizado para " + loginName.get() + ".";
            case 4005:
                return "Segundo erro de token contabilizado para " + loginName.get() + ".";
            case 4006:
                return "Terceiro erro de token contabilizado para " + loginName.get() + ".";
            case 4007:
                return "Acesso do usuario " + loginName.get() + "bloqueado pela autenticação etapa 3.";
            case 5001:
                return "Tela principal apresentada para  " + loginName.get() + ".";
            case 5002:
                return "Opção 1 do menu principal selecionada por " + loginName.get() + ".";
            case 5003:
                return "Opção 2 do menu principal selecionada por " + loginName.get() + ".";
            case 5004:
                return "Opção 3 do menu principal selecionada por " + loginName.get() + ".";
            case 6001:
                return "Tela de cadastro apresentada para " + loginName.get() + ".";
            case 6002:
                return "Botão cadastrar pressionado por " + loginName.get() + ".";
            case 6003:
                return "Senha pessoal inválida fornecida por " + loginName.get() + ".";
            case 6004:
                return "Caminho do certificado digital inválido fornecido por " + loginName.get() + ".";
            case 6005:
                return "Chave privada verificada negativamente para " + loginName.get() + "(caminho inválido).";
            case 6006:
                return "Chave privada verificada negativamente para " + loginName.get() + "(frase secreta inválida).";
            case 6007:
                return "Chave privada verificada negativamente para " + loginName.get() + "(assinatura digital inválida).";
            case 6008:
                return "Confirmação de dados aceita por " + loginName.get() + ".";
            case 6009:
                return "Confirmação de dados rejeitada por " + loginName.get() + ".";
            case 6010:
                return "Botão voltar de cadastro para o menu principal pressionado por " + loginName.get() + ".";
            case 7001:
                return "Tela de consulta de arquivos secretos apresentada para " + loginName.get() + ".";
            case 7002:
                return "Botão voltar de consulta para o menu principal pressionado por " + loginName.get() + ".";
            case 7003:
                return "Botão Listar de consulta pressionado por " + loginName.get() + ".";
            case 7004:
                return "Caminho de pasta inválido fornecido por " + loginName.get() + ".";
            case 7005:
                return "Arquivo de índice decriptado com sucesso para " + loginName.get() + ".";
            case 7006:
                return "Arquivo de índice verificado (integridade e autenticidade) com sucesso para " + loginName.get() + ".";
            case 7007:
                return "Falha na decriptação do arquivo de índice para " + loginName.get() + ".";
            case 7008:
                return "Falha na verificação (integridade e autenticidade) do arquivo de índice para " + loginName.get() + ".";
            case 7009:
                return "Lista de arquivos presentes no índice apresentada para " + loginName.get() + ".";
            case 7010:
                return "Arquivo " + arqName.get() +" selecionado por " +loginName.get() + " para decriptação.";
            case 7011:
                return "Acesso permitido ao arquivo " + arqName.get() +" para " + loginName.get() + ".";
            case 7012:
                return "Acesso negado ao arquivo " + arqName.get() +" para " + loginName.get() + ".";
            case 7013:
                return "Arquivo " + arqName.get() +" decriptado com sucesso para " +loginName.get() + ".";
            case 7014:
                return "Arquivo " + arqName.get() +" verificado (integridade e autenticidade) com sucesso para " +loginName.get() + ".";
            case 7015:
                return "Falha na decriptação do arquivo " + arqName.get() +" para " +loginName.get() + ".";
            case 7016:
                return "Falha na verificação (integridade e autenticidade) do arquivo " + arqName.get() +" para " +loginName.get() + ".";
            case 8001:
                return "Tela de saída apresentada para " +loginName.get() + ".";
            case 8002:
                return "Botão encerrar sessão pressionado por " +loginName.get() + ".";
            case 8003:
                return "Botão encerrar sistema pressionado por  "  +loginName.get() + ".";
            case 8004:
                return "Botão voltar de sair para o menu principal pressionado por " +loginName.get() + ".";

            default:
                System.err.println("codigo de mensagem desconhecido: " + codigo);
        }

        return null;
    }

}

