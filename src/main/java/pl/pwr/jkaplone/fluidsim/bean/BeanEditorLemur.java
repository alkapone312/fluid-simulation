package pl.pwr.jkaplone.fluidsim.bean;

import com.jme3.math.ColorRGBA;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.core.VersionedReference;
import com.simsilica.lemur.text.DocumentModel;

import java.beans.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class BeanEditorLemur<T> {

    private final T rootBean;
    private final Consumer<T> consumer;

    private final List<Runnable> watchers = new ArrayList<>();

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public BeanEditorLemur(T bean, Consumer<T> consumer) {
        this.rootBean = bean;
        this.consumer = consumer;

        executor.scheduleAtFixedRate(() -> {
            for (Runnable r : watchers) {
                r.run();
            }
        }, 0, 200, TimeUnit.MILLISECONDS);
    }

    public Panel build(String name) {
        Container root = new Container(new SpringGridLayout(Axis.Y, Axis.X));
        addBeanProperties(root, rootBean);
        var rollup = new RollupPanel(name, "glass");
        rollup.setContents(root);
        rollup.setOpen(false);
        rollup.setBackground(new QuadBackgroundComponent(new ColorRGBA(0, 0, 0, 0.5f)));

        return rollup;
    }

    private void addBeanProperties(Container parent, Object bean) {
        try {
            BeanInfo info = Introspector.getBeanInfo(bean.getClass(), Object.class);

            for (PropertyDescriptor pd : info.getPropertyDescriptors()) {

                Method getter = pd.getReadMethod();
                Method setter = pd.getWriteMethod();
                if (getter == null) continue;

                Object value = getter.invoke(bean);

                Nested nested = getter.getAnnotation(Nested.class);
                if (nested != null && value != null) {
                    addNested(parent, pd, value, nested);
                    continue;
                }

                Control control = getter.getAnnotation(Control.class);
                if (control != null && setter != null) {
                    addControl(parent, pd, getter, setter, control, bean);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void addNested(
        Container parent,
        PropertyDescriptor pd,
        Object nestedBean,
        Nested nested
    ) {
        String title = nested.label().isEmpty() ? pd.getName() : nested.label();
        Container nestedContainer = parent.addChild(new Container(new SpringGridLayout(Axis.Y, Axis.X)));
        addBeanProperties(nestedContainer, nestedBean);
    }

    private void addControl(
        Container parent,
        PropertyDescriptor pd,
        Method getter,
        Method setter,
        Control control,
        Object targetBean
    ) throws Exception {

        String label = control.label().isEmpty() ? pd.getName() : control.label();
        parent.addChild(new Label(label));

        Panel editor;
        if (control.type() == ControlType.RANGE) {
            editor = createSlider(getter, setter, control, targetBean);
        } else {
            editor = createText(getter, setter, targetBean);
        }

        parent.addChild(editor);
    }

    private Panel createSlider(
        Method getter,
        Method setter,
        Control control,
        Object bean
    ) throws Exception {

        double min = control.min();
        double max = control.max();
        double value = ((Number) getter.invoke(bean)).doubleValue();

        Slider slider = new Slider(new DefaultRangedValueModel(min, max, value));
        Label valueLabel = new Label(String.format("%.2f", value));

        VersionedReference<Double> ref = slider.getModel().createReference();

        watchers.add(() -> {
            if (ref.update()) {
                double newVal = ref.get();
                try {
                    Object converted = convertNumber((float) newVal, getter.getReturnType());
                    setter.invoke(bean, converted);
                    valueLabel.setText(String.format("%.2f", newVal));
                    consumer.accept(rootBean);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

        Container c = new Container(new SpringGridLayout());
        c.addChild(slider);
        c.addChild(valueLabel);

        return c;
    }

    private Panel createText(
        Method getter,
        Method setter,
        Object bean
    ) throws Exception {

        TextField tf = new TextField(String.valueOf(getter.invoke(bean)));

        VersionedReference<DocumentModel> ref = tf.getDocumentModel().createReference();

        watchers.add(() -> {
            if (ref.update()) {
                try {
                    setter.invoke(bean, ref.get());
                    consumer.accept(rootBean);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

        return tf;
    }

    private Object convertNumber(float v, Class<?> type) {
        if (type == float.class || type == Float.class) return v;
        if (type == double.class || type == Double.class) return (double) v;
        if (type == int.class || type == Integer.class) return Math.round(v);
        throw new IllegalArgumentException("Unsupported numeric type: " + type);
    }
}