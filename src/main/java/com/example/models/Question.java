package com.example.models;

public class Question {
    private final int number;
    private final String text;
    private final int maxMarks;
    private String answer = "";

    public Question(int number, String text) {
        this(number, text, 10); // default 10 marks
    }

    public Question(int number, String text, int maxMarks) {
        this.number = number;
        this.text = text;
        this.maxMarks = maxMarks;
    }

    public int getNumber() {
        return number;
    }

    public String getText() {
        return text;
    }

    public int getMaxMarks() {
        return maxMarks;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}