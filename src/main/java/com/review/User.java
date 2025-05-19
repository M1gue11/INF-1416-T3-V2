package com.review;

public class User {
    public Integer UID;
    public String nome;
    public String email;
    public String senha_pessoal_hash;
    public String grupo;
    public Integer KID;
    public Integer numero_acessos;
    public Integer ultimo_bloqueio_ts;
    public String chave_totp_cript;

    public void fetchDefault() {
        UID = -1;
        nome = "none";
        email = "none";
        senha_pessoal_hash = "none";
        grupo = "none";
        KID = -1;
        numero_acessos = 0;
        ultimo_bloqueio_ts = null;
        chave_totp_cript = "none";
    }

    public boolean isAdmin() {
        return this.grupo.equals("Administrador");
    }

}
