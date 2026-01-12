package org.example.bean;

import javax.swing.*;
import java.awt.*;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.function.Consumer;

public class BeanEditorFrame<T> extends JFrame {

    private final T bean;

    private final Consumer<T> consumer;

    public BeanEditorFrame(T bean) {
        this(bean, (o) -> {});
    }

    public BeanEditorFrame(T bean, Consumer<T> consumer) {
        super(bean.getClass().getSimpleName() + " Editor");
        this.bean = bean;
        this.consumer = consumer;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());

        buildUI();

        pack();
        setLocationRelativeTo(null);
    }

    private void buildUI() {
        try {
            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new GridBagLayout());

            addBeanProperties(mainPanel, bean, 0);

            JScrollPane scroll = new JScrollPane(mainPanel);
            scroll.setPreferredSize(new Dimension(500, 600));

            // Use GridBagConstraints instead of BorderLayout.CENTER
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            gbc.fill = GridBagConstraints.BOTH;

            getContentPane().add(scroll, gbc);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int addBeanProperties(JPanel panel, Object currentBean, int startRow) throws Exception {
        BeanInfo info = Introspector.getBeanInfo(currentBean.getClass(), Object.class);
        PropertyDescriptor[] props = info.getPropertyDescriptors();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridy = startRow;

        for (PropertyDescriptor pd : props) {
            Method getter = pd.getReadMethod();
            Method setter = pd.getWriteMethod();
            if (getter == null) continue;

            Object value = getter.invoke(currentBean);

            Nested nestedAnno = getter.getAnnotation(Nested.class);
            if (nestedAnno != null && value != null) {
                String title = nestedAnno.label().isEmpty() ? pd.getName() : nestedAnno.label();

                JPanel nestedPanel = new JPanel(new GridBagLayout());
                nestedPanel.setBorder(BorderFactory.createTitledBorder(title));

                JButton toggle = new JButton("-");
                toggle.addActionListener(e -> {
                    nestedPanel.setVisible(!nestedPanel.isVisible());
                    toggle.setText(nestedPanel.isVisible() ? "-" : "+");
                    nestedPanel.revalidate();
                });

                gbc.gridx = 0;
                gbc.gridy++;
                gbc.gridwidth = 2;
                panel.add(nestedPanel, gbc);

                addBeanProperties(nestedPanel, value, 0);
            } else {
                Control control = getter.getAnnotation(Control.class);
                if (control != null && setter != null) {
                    JLabel label = new JLabel(control.label().isEmpty() ? pd.getName() : control.label());
                    JComponent editor = createEditor(control, getter, setter, currentBean);

                    gbc.gridx = 0;
                    gbc.gridy++;
                    gbc.gridwidth = 1;
                    panel.add(label, gbc);

                    gbc.gridx = 1;
                    panel.add(editor, gbc);
                }
            }
        }

        return gbc.gridy + 1;
    }

    private JComponent createEditor(
        Control control,
        Method getter,
        Method setter,
        Object bean
    ) throws Exception {

        Class<?> type = getter.getReturnType();
        Object value = getter.invoke(bean);

        switch (control.type()) {

            case RANGE:
                if (control.type() == ControlType.RANGE &&
                    !(Number.class.isAssignableFrom(type) || type.isPrimitive())) {
                    throw new IllegalStateException(
                        "@Control(RANGE) used on non-numeric type: " + type
                    );
                }

                return createSlider(value, control, setter, type, bean);

            case TEXT:
                JTextField tf = new JTextField(
                    value != null ? value.toString() : "",
                    12
                );
                tf.addActionListener(e ->
                    invokeSetter(setter, tf.getText(), bean)
                );
                return tf;

            default:
                return new JLabel("Unsupported control");
        }
    }

    private JComponent createSlider(
        Object value,
        Control control,
        Method setter,
        Class<?> targetType,
        Object bean
    ) {
        double min = control.min();
        double max = control.max();
        double step = control.step();

        int scale = (int) Math.round(1.0 / step);

        int intMin = (int) (min * scale);
        int intMax = (int) (max * scale);
        int intValue = (int) (((Number) value).doubleValue() * scale);

        JSlider slider = new JSlider(intMin, intMax, intValue);

        int range = intMax - intMin;
        slider.setMajorTickSpacing(range / 2);
        slider.setMinorTickSpacing(range / 4);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);

        Hashtable<Integer, JLabel> labels = new Hashtable<>();
        labels.put(intMin, new JLabel(fmt(min)));
        labels.put(intMin + range / 2, new JLabel(fmt((min + max) / 2)));
        labels.put(intMax, new JLabel(fmt(max)));
        slider.setLabelTable(labels);

        // Label to show the exact current value
        JLabel valueLabel = new JLabel(fmt(((Number) value).doubleValue()));
        valueLabel.setPreferredSize(new Dimension(50, 20));

        slider.addChangeListener(e -> {
            Object converted = convertSliderValue(slider.getValue(), scale, targetType);
            invokeSetter(setter, converted, bean);

            // Update the value label
            if (converted instanceof Number) {
                valueLabel.setText(fmt(((Number) converted).doubleValue()));
            }
        });

        // Wrap slider and value label in a panel
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(slider, BorderLayout.CENTER);
        panel.add(valueLabel, BorderLayout.EAST);

        panel.setPreferredSize(new Dimension(270, 45));
        return panel;
    }

    private Object convertSliderValue(
        int sliderValue,
        int scale,
        Class<?> targetType
    ) {
        if (targetType == int.class || targetType == Integer.class) {
            return sliderValue / scale;
        }

        if (targetType == double.class || targetType == Double.class) {
            return sliderValue / (double) scale;
        }

        if (targetType == float.class || targetType == Float.class) {
            return sliderValue / (float) scale;
        }

        throw new IllegalArgumentException(
            "Unsupported RANGE type: " + targetType.getName()
        );
    }

    private static String fmt(double v) {
        return String.format("%.2f", v);
    }

    private void invokeSetter(Method setter, Object value, Object currentBean) {
        try {
            setter.invoke(currentBean, value);
            this.consumer.accept(bean);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
