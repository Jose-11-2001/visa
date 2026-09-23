package com.example.components;

import javafx.scene.control.Button;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * A large, high-contrast button suitable for accessibility.
 */
public class BigButton extends Button {

    public BigButton(String text) {
        super(text);
        setPrefWidth(360);
        setPrefHeight(70);
        setFont(Font.font("Arial", FontWeight.BOLD, 22));
        setStyle(
            "-fx-background-color: #1A1A1A;" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 12;" +
            "-fx-cursor: hand;"
        );
        setOnMouseEntered(e -> setStyle(
            "-fx-background-color: #333333;" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 12;" +
            "-fx-cursor: hand;"
        ));
        setOnMouseExited(e -> setStyle(
            "-fx-background-color: #1A1A1A;" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 12;" +
            "-fx-cursor: hand;"
        ));
    }

    /** Variant with a yellow accent (for secondary actions). */
    public static BigButton accent(String text) {
        BigButton b = new BigButton(text);
        String normal =
            "-fx-background-color: #FFD500;" +
            "-fx-text-fill: #1A1A1A;" +
            "-fx-background-radius: 12;" +
            "-fx-cursor: hand;";
        String hover =
            "-fx-background-color: #FFE44D;" +
            "-fx-text-fill: #1A1A1A;" +
            "-fx-background-radius: 12;" +
            "-fx-cursor: hand;";
        b.setStyle(normal);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e -> b.setStyle(normal));
        return b;
    }
}