package com.review;

import java.time.Instant;

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
    public Integer numero_consultas;

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
        numero_consultas = 0;
    }

    public boolean isAdmin() {
        return this.grupo.equals("Administrador");
    }

    public boolean isAllowed(String group) {
        return this.grupo.equals(group) || this.grupo.equals("Administrador");
    }

    public boolean isBloqueado() {
        if (ultimo_bloqueio_ts == null) {
            return false;
        }
        long now = Instant.now().getEpochSecond();
        long diff = now - ultimo_bloqueio_ts;
        return diff < 120;
    }
}
