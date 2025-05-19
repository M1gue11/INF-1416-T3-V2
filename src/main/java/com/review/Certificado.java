package com.review;

public class Certificado {
    public String commonName = "CA de Teste";
    public String emailAddress = "teste@teste.com";
    public String orgUnit = "MIANPL";
    public String organization = "MIANPL LTDA.";
    public String locality = "Rio de Janeiro";
    public String stateOrProvince = "Rio de Janeiro";
    public String country = "BR"; // Código do país com 2 letras (ISO 3166-1 alpha-2)
    public String passPhrase = "teste"; // Frase secreta para criptografar a chave privada
    public int validityDays = 365; // Validade de 1 ano

    public Certificado(String commonName, String emailAddress, String orgUnit,
            String organization, String locality, String stateOrProvince, String country,
            String passPhrase, int validityDays) {
        this.commonName = commonName;
        this.emailAddress = emailAddress;
        this.orgUnit = orgUnit;
        this.organization = organization;
        this.locality = locality;
        this.stateOrProvince = stateOrProvince;
        this.country = country;
        this.passPhrase = passPhrase;
        this.validityDays = validityDays;

    }

    public static Certificado getDefault() {
        return new Certificado("CA de Teste", "teste@teste.com", "MIANPL",
                "MIANPL LTDA.", "Rio de Janeiro", "Rio de Janeiro", "BR", "teste", 365);
    }
}
