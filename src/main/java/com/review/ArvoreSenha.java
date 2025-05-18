package com.review;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class ArvoreSenha {
    private SenhaNode raiz;

    public ArvoreSenha() {
        raiz = new SenhaNode(-1, -1); // Nó raiz inicial
    }

    public void inserirOpcao(int indiceButton, int valorEsquerda, int valorDireita) {
        if (raiz.getIndiceButton() == -1) { // Árvore vazia
            raiz.setIndiceButton(indiceButton);
            raiz.setEsquerda(new SenhaNode(indiceButton, valorEsquerda));
            raiz.setDireita(new SenhaNode(indiceButton, valorDireita));
            return;
        }

        List<SenhaNode> folhas = encontrarFolhas(raiz);

        for (SenhaNode folha : folhas) {
            folha.setEsquerda(new SenhaNode(indiceButton, valorEsquerda));
            folha.setDireita(new SenhaNode(indiceButton, valorDireita));
        }
    }

    private List<SenhaNode> encontrarFolhas(SenhaNode node) {
        List<SenhaNode> folhas = new ArrayList<>();
        encontrarFolhasRecursivo(node, folhas);
        return folhas;
    }

    private void encontrarFolhasRecursivo(SenhaNode node, List<SenhaNode> folhas) {
        if (node == null) return;

        if (node.getEsquerda() == null && node.getDireita() == null) {
            folhas.add(node);
        } else {
            encontrarFolhasRecursivo(node.getEsquerda(), folhas);
            encontrarFolhasRecursivo(node.getDireita(), folhas);
        }
    }

    public void imprimirPorNiveis() {
        if (raiz == null) {
            System.out.println("Árvore vazia!");
            return;
        }

        Queue<SenhaNode> fila = new LinkedList<>();
        fila.add(raiz);
        int nivel = 0;

        while (!fila.isEmpty()) {
            int nosNoNivel = fila.size();
            System.out.print("Nível " + nivel + ": ");

            for (int i = 0; i < nosNoNivel; i++) {
                SenhaNode atual = fila.poll();

                // Imprime o nó atual
                System.out.print("[B:" + atual.getIndiceButton() +
                        ",V:" + atual.getValor() + "] ");

                // Adiciona os filhos na fila
                if (atual.getEsquerda() != null) fila.add(atual.getEsquerda());
                if (atual.getDireita() != null) fila.add(atual.getDireita());
            }

            System.out.println();
            nivel++;
        }
    }

    public List<List<Integer>> coletarCaminhos() {
        List<List<Integer>> caminhos = new ArrayList<>();
        if (raiz == null) return caminhos;

        List<Integer> caminhoAtual = new ArrayList<>();
        dfsColetarCaminhos(raiz, caminhoAtual, caminhos);
        return caminhos;
    }

    private void dfsColetarCaminhos(SenhaNode node, List<Integer> caminhoAtual, List<List<Integer>> caminhos) {
        if (node == null) return;

        // Adiciona o valor do nó ao caminho atual (exceto a raiz com valor -1)
        if (node.getValor() != -1) {
            caminhoAtual.add(node.getValor());
        }

        // Se é uma folha, adiciona o caminho completo à lista
        if (node.getEsquerda() == null && node.getDireita() == null) {
            caminhos.add(new ArrayList<>(caminhoAtual));
        } else {
            // Continua a busca nos filhos
            dfsColetarCaminhos(node.getEsquerda(), caminhoAtual, caminhos);
            dfsColetarCaminhos(node.getDireita(), caminhoAtual, caminhos);
        }

        // Remove o último valor ao retroceder (backtracking)
        if (node.getValor() != -1) {
            caminhoAtual.remove(caminhoAtual.size() - 1);
        }
    }

    public List<String> gerarSequenciasNumericas() {
        List<List<Integer>> caminhos = coletarCaminhos();
        List<String> sequencias = new ArrayList<>();

        for (List<Integer> caminho : caminhos) {
            StringBuilder sb = new StringBuilder();
            for (Integer numero : caminho) {
                sb.append(numero);
            }
            sequencias.add(sb.toString());
        }

        return sequencias;
    }
}

class SenhaNode {
    private int indiceButton;
    private int valor;
    private SenhaNode esquerda;
    private SenhaNode direita;
    private String senhaHash;

    public SenhaNode(int indiceButton, int valor) {
        this.indiceButton = indiceButton;
        this.valor = valor;
        this.esquerda = null;
        this.direita = null;
        this.senhaHash = null;
    }

    // Getters e setters
    public int getIndiceButton() { return indiceButton; }
    public int getValor() { return valor; }
    public SenhaNode getEsquerda() { return esquerda; }
    public SenhaNode getDireita() { return direita; }
    public String getSenhaHash() { return senhaHash; }

    public void setEsquerda(SenhaNode esquerda) { this.esquerda = esquerda; }
    public void setDireita(SenhaNode direita) { this.direita = direita; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public void setIndiceButton(int indiceButton) { this.indiceButton = indiceButton; }
    public void setValor(int valor) { this.valor = valor; }
}