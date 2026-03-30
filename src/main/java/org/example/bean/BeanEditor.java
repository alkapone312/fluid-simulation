package org.example.bean;

import com.simsilica.lemur.Panel;

import javax.swing.*;
import java.util.function.Consumer;

public final class BeanEditor {

    private BeanEditor() {}

    public static Panel open(Object bean, String name) {
        return new BeanEditorLemur<>(bean, (b) -> {}).build(name);
    }

    public static <T> Panel openWithCallback(T bean, Consumer<T> consumer, String name) {
        return new BeanEditorLemur<>(bean, consumer).build(name);
    }
}
