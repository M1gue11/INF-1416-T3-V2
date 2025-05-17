package com.review;

public class ExecutionPipeline {
    static ExecutionPipeline instance = null;

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

    public void login() {
    }

    public void cadastro() {

    }
}
