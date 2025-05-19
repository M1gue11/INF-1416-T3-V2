package com.review;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:cofre_digital.db";

    public static void initDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
                Statement stmt = conn.createStatement()) {

            // Tabela Chaveiro (agora criada primeiro)
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS Chaveiro (" +
                            "KID INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "caminho_certificado TEXT NOT NULL," + // Caminho do arquivo (255 chars)
                            "caminho_chave_privada TEXT NOT NULL);"// Caminho do arquivo (255 chars)
            );

            // Table User (agora com referência ao Chaveiro)
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS Usuario (" +
                            "UID INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "nome TEXT NOT NULL," +
                            "email TEXT NOT NULL UNIQUE," +
                            "senha_pessoal_hash TEXT NOT NULL," +
                            "grupo TEXT NOT NULL CHECK (grupo IN ('Administrador', 'Usuario'))," +
                            "KID INTEGER NOT NULL," +
                            "numero_acessos INTEGER NOT NULL," +
                            "ultimo_bloqueio_ts INTEGER, " +
                            "chave_totp_cript TEXT NOT NULL, " +
                            "FOREIGN KEY (KID) REFERENCES Chaveiro(KID));");

            // Tabela Grupo
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS Grupo (" +
                            "GID INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "nome TEXT UNIQUE NOT NULL)");

            stmt.executeUpdate(
                    "INSERT OR IGNORE INTO Grupo (nome) VALUES " +
                            "('Administrador'), " +
                            "('Usuario')");

            // Tabela Mensagem (logs de sistema)
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS Mensagem (" +
                            "MID INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "codigo INTEGER NOT NULL," +
                            "conteudo TEXT NOT NULL)");

            // Tabela Registro
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS Registro (" +
                            "RID INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "UID INTEGER," +
                            "MID INTEGER NOT NULL," +
                            "data_hora TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                            "FOREIGN KEY (UID) REFERENCES Usuario(UID)," +
                            "FOREIGN KEY (MID) REFERENCES Mensagem(MID))");

        } catch (SQLException e) {
            System.err.println("Erro ao criar banco de dados: " + e.getMessage());
        }
    }

    public static Chaveiro getChaveiroByKID(int kid) {
        String sql = "SELECT * FROM Chaveiro WHERE KID = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, kid);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Chaveiro chaveiro = new Chaveiro();
                chaveiro.KID = rs.getInt("KID");
                chaveiro.caminho_certificado = rs.getString("caminho_certificado");
                chaveiro.caminho_chave_privada = rs.getString("caminho_chave_privada");

                return chaveiro;
            } else {
                System.out.println("Chaveiro não encontrado.");
                return null; // Ou retornar um Chaveiro com valores default
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar chaveiro: " + e.getMessage());
            return null; // Ou lançar a exceção novamente
        }
    }

    public static User getUserByEmail(String email) {
        String sql = "SELECT * FROM Usuario WHERE email = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.UID = rs.getInt("UID");
                user.nome = rs.getString("nome");
                user.email = rs.getString("email");
                user.senha_pessoal_hash = rs.getString("senha_pessoal_hash");
                user.grupo = rs.getString("grupo");
                user.KID = rs.getInt("KID");
                user.numero_acessos = rs.getInt("numero_acessos");
                user.ultimo_bloqueio_ts = rs.getInt("ultimo_bloqueio_ts");
                user.chave_totp_cript = rs.getString("chave_totp_cript");

                return user;
            } else {
                System.out.println("Usuário não encontrado.");
                // TODO: log remover
                insereLog(2005, Optional.empty(), Optional.empty());
                return null; // Ou retornar um User com valores default
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar usuário: " + e.getMessage());
            return null; // Ou lançar a exceção novamente
        }
    }

    public static String getPasswordByLogin(String email) {
        String sql = "SELECT senha_pessoal_hash FROM Usuario WHERE email = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            return rs.getString("senha_pessoal_hash");
        } catch (SQLException e) {
            System.err.println("Erro ao buscar senha: " + e.getMessage());
        }
        return null;
    }

    public static int cadastrarUsuario(
            String nome,
            String email,
            String senhaHash,
            String grupo,
            String caminhoCertificado,
            String caminhoChavePrivada,
            String chaveTotpCript) {
        String sqlChaveiro = "INSERT INTO Chaveiro (caminho_certificado, caminho_chave_privada) VALUES (?, ?)";
        String sqlUsuario = "INSERT INTO Usuario (nome, email, senha_pessoal_hash, grupo, KID, numero_acessos, chave_totp_cript) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);

            try {
                int KID;
                try (PreparedStatement pstmtChaveiro = conn.prepareStatement(sqlChaveiro,
                        Statement.RETURN_GENERATED_KEYS)) {

                    pstmtChaveiro.setString(1, caminhoCertificado);
                    pstmtChaveiro.setString(2, caminhoChavePrivada);
                    pstmtChaveiro.executeUpdate();

                    ResultSet rs = pstmtChaveiro.getGeneratedKeys();
                    if (!rs.next()) {
                        System.err.println("Erro ao inserir chaveiro.");
                        conn.rollback();
                        return -1;
                    }
                    KID = rs.getInt(1);
                }

                try (PreparedStatement pstmtUsuario = conn.prepareStatement(sqlUsuario,
                        Statement.RETURN_GENERATED_KEYS)) {
                    int numero_acessos = 0;
                    pstmtUsuario.setString(1, nome);
                    pstmtUsuario.setString(2, email.toLowerCase());
                    pstmtUsuario.setString(3, senhaHash);
                    pstmtUsuario.setString(4, grupo);
                    pstmtUsuario.setInt(5, KID);
                    pstmtUsuario.setInt(6, numero_acessos);
                    pstmtUsuario.setString(7, chaveTotpCript);
                    pstmtUsuario.executeUpdate();

                    ResultSet rs = pstmtUsuario.getGeneratedKeys();
                    if (!rs.next()) {
                        conn.rollback();
                        return -1;
                    }
                    int UID = rs.getInt(1);

                    conn.commit();
                    return UID;
                }
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Erro ao cadastrar usuário: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    public static int getNumberOfUsers() {
        String sql = "SELECT COUNT(*) AS total FROM Usuario";
        try (Connection conn = DriverManager.getConnection(DB_URL);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            System.err.println("Erro ao contar usuários: " + e.getMessage());
        }
        return -1;
    }

    public static int buscarIdGrupo(String nomeGrupo) {
        String sql = "SELECT GID FROM Grupo WHERE nome = ?";
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

    public static void insereLog(int mCodigo, Optional<String> arqName, Optional<User> user) {
        String mensagem = user.isPresent() ? getMessageByMessageCode(mCodigo, arqName, Optional.of(user.get().email))
                : getMessageByMessageCode(mCodigo, arqName, Optional.empty());
        int mid = inserirMensagem(mCodigo, mensagem);

        if (user.isPresent()) {
            int rid = inserirRegistro(Optional.of(user.get().UID), mid);
            if (rid == -1) {
                System.err.println("Erro ao inserir registro.");
            }
        } else {
            int rid = inserirRegistro(Optional.empty(), mid);
            if (rid == -1) {
                System.err.println("Erro ao inserir registro.");
            }
        }
    }

    public static int inserirRegistro(Optional<Integer> uid, int mid) {
        String sql = "INSERT INTO Registro (UID, MID) VALUES (?, ?)";
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

    /**
     * @return return generated MID
     */
    public static int inserirMensagem(int codigo, String conteudo) {
        String sql = "INSERT INTO Mensagem (codigo, conteudo) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, codigo);
            pstmt.setString(2, conteudo);
            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao inserir mensagem: " + e.getMessage());
        }
        return -1;
    }

    public static ResultSet executeSql(String sql) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
                Statement stmt = conn.createStatement()) {

            return stmt.executeQuery(sql);
        } catch (SQLException e) {
            System.err.println("Erro ao executar SQL: " + e.getMessage());
            return null;
        }
    }

    public static User getSuperAdmin() {
        String sql = "SELECT * FROM Usuario ORDER BY UID LIMIT 1";
        try (Connection conn = DriverManager.getConnection(DB_URL);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.UID = rs.getInt("UID");
                user.nome = rs.getString("nome");
                user.email = rs.getString("email");
                user.senha_pessoal_hash = rs.getString("senha_pessoal_hash");
                user.grupo = rs.getString("grupo");
                user.KID = rs.getInt("KID");
                user.numero_acessos = rs.getInt("numero_acessos");
                user.ultimo_bloqueio_ts = rs.getInt("ultimo_bloqueio_ts");
                user.chave_totp_cript = rs.getString("chave_totp_cript");

                return user;
            } else {
                System.out.println("SuperAdmin não encontrado.");
                return null;
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar SuperAdmin: " + e.getMessage());
            return null;
        }
    }

    public static List<String> getMessagesAndTimeByActualTime(long time){
        List<String> logs = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection(DB_URL); // Supondo que você tenha uma classe de conexão

            // Consulta que junta as tabelas Registro e Mensagem
            String sql = "SELECT m.conteudo, r.data_hora " +
                    "FROM Registro r " +
                    "JOIN Mensagem m ON r.MID = m.MID " +
                    "ORDER BY r.data_hora ASC";

            stmt = conn.prepareStatement(sql);

            // Converter o timestamp em milissegundos para um objeto Timestamp do SQL

            rs = stmt.executeQuery();

            while (rs.next()) {
                String conteudo = rs.getString("conteudo");
                Timestamp dataHora = rs.getTimestamp("data_hora");

                // Formatar a saída como desejar
                logs.add(dataHora + " - " + conteudo);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // Fechar recursos
            try { if (rs != null) rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }

        return logs;
    }


    public static Chaveiro getChaveiroSuperAdm() {
        User adm = getSuperAdmin();
        return getChaveiroByKID(adm.KID);
    }

    public static String getMessageByMessageCode(int codigo, Optional<String> arqName, Optional<String> loginName) {
        String login = loginName.orElse("desconhecido");
        String arquivo = arqName.orElse("desconhecido");

        switch (codigo) {
            case 1001:
                return "Sistema iniciado.";
            case 1002:
                return "Sistema encerrado";
            case 1003:
                return "Sessão iniciada para " + login + ".";
            case 1004:
                return "Sessão encerrada para" + login + ".";
            case 1005:
                return "Partida do sistema iniciada para cadastro do administrador";
            case 1006:
                return "Partida do sistema iniciada para operação normal pelos usuários.";
            case 2001:
                return "Autenticação etapa 1 iniciada.";
            case 2002:
                return "Autenticação etapa 1 encerrada.";
            case 2003:
                return "Login name" + login + " identificado com acesso liberado.";
            case 2004:
                return "Login name" + login + " identificado com acesso bloqueado.";
            case 2005:
                return "Login name" + login + " não identificado.";
            case 3001:
                return "Autenticação etapa 2 iniciada para " + login + ".";
            case 3002:
                return "Autenticação etapa 2 encerrada para " + login + ".";
            case 3003:
                return "Senha pessoal verificada positivamente para " + login + ".";
            case 3004:
                return "Primeiro erro da senha pessoal contabilizado para " + login + ".";
            case 3005:
                return "Segundo erro da senha pessoal contabilizado para " + login + ".";
            case 3006:
                return "Terceiro erro da senha pessoal contabilizado para " + login + ".";
            case 3007:
                return "Acesso do usuario " + login + "bloqueado pela autenticação etapa 2.";
            case 4001:
                return "Autenticação etapa 3 iniciada para " + login + ".";
            case 4002:
                return "Autenticação etapa 3 encerrada para " + login + ".";
            case 4003:
                return "Token verificado positivamente para " + login + ".";
            case 4004:
                return "Primeiro erro de token contabilizado para " + login + ".";
            case 4005:
                return "Segundo erro de token contabilizado para " + login + ".";
            case 4006:
                return "Terceiro erro de token contabilizado para " + login + ".";
            case 4007:
                return "Acesso do usuario " + login + "bloqueado pela autenticação etapa 3.";
            case 5001:
                return "Tela principal apresentada para  " + login + ".";
            case 5002:
                return "Opção 1 do menu principal selecionada por " + login + ".";
            case 5003:
                return "Opção 2 do menu principal selecionada por " + login + ".";
            case 5004:
                return "Opção 3 do menu principal selecionada por " + login + ".";
            case 6001:
                return "Tela de cadastro apresentada para " + login + ".";
            case 6002:
                return "Botão cadastrar pressionado por " + login + ".";
            case 6003:
                return "Senha pessoal inválida fornecida por " + login + ".";
            case 6004:
                return "Caminho do certificado digital inválido fornecido por " + login + ".";
            case 6005:
                return "Chave privada verificada negativamente para " + login + "(caminho inválido).";
            case 6006:
                return "Chave privada verificada negativamente para " + login + "(frase secreta inválida).";
            case 6007:
                return "Chave privada verificada negativamente para " + login
                        + "(assinatura digital inválida).";
            case 6008:
                return "Confirmação de dados aceita por " + login + ".";
            case 6009:
                return "Confirmação de dados rejeitada por " + login + ".";
            case 6010:
                return "Botão voltar de cadastro para o menu principal pressionado por " + login + ".";
            case 7001:
                return "Tela de consulta de arquivos secretos apresentada para " + login + ".";
            case 7002:
                return "Botão voltar de consulta para o menu principal pressionado por " + login + ".";
            case 7003:
                return "Botão Listar de consulta pressionado por " + login + ".";
            case 7004:
                return "Caminho de pasta inválido fornecido por " + login + ".";
            case 7005:
                return "Arquivo de índice decriptado com sucesso para " + login + ".";
            case 7006:
                return "Arquivo de índice verificado (integridade e autenticidade) com sucesso para " + login
                        + ".";
            case 7007:
                return "Falha na decriptação do arquivo de índice para " + login + ".";
            case 7008:
                return "Falha na verificação (integridade e autenticidade) do arquivo de índice para " + login
                        + ".";
            case 7009:
                return "Lista de arquivos presentes no índice apresentada para " + login + ".";
            case 7010:
                return "Arquivo " + arquivo + " selecionado por " + login + " para decriptação.";
            case 7011:
                return "Acesso permitido ao arquivo " + arquivo + " para " + login + ".";
            case 7012:
                return "Acesso negado ao arquivo " + arquivo + " para " + login + ".";
            case 7013:
                return "Arquivo " + arquivo + " decriptado com sucesso para " + login + ".";
            case 7014:
                return "Arquivo " + arquivo + " verificado (integridade e autenticidade) com sucesso para "
                        + login + ".";
            case 7015:
                return "Falha na decriptação do arquivo " + arquivo + " para " + login + ".";
            case 7016:
                return "Falha na verificação (integridade e autenticidade) do arquivo " + arquivo + " para "
                        + login + ".";
            case 8001:
                return "Tela de saída apresentada para " + login + ".";
            case 8002:
                return "Botão encerrar sessão pressionado por " + login + ".";
            case 8003:
                return "Botão encerrar sistema pressionado por  " + login + ".";
            case 8004:
                return "Botão voltar de sair para o menu principal pressionado por " + login + ".";

            default:
                System.err.println("codigo de mensagem desconhecido: " + codigo);
        }

        return null;
    }

}
