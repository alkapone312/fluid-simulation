package org.example;

import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.WireBox;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Panel;
import org.example.bean.BeanEditor;

public class Simulation3D extends SimpleApplication {

    private FluidSimulation fluidSimulation;
    private SimulationBean bean = new SimulationBean();

    public static void main(String[] args) {
        Simulation3D app = new Simulation3D();
        AppSettings settings = new AppSettings(true);
        settings.setResolution(1920, 1080);
        settings.setFullscreen(true);
        app.setSettings(settings);
        app.start();
    }

    private void setupCameraAndLight() {
        cam.setLocation(new Vector3f(0, 0, 15));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
        flyCam.setEnabled(true);
        flyCam.setMoveSpeed(5);
        flyCam.setDragToRotate(true);

        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-1, -2, -3).normalizeLocal());
        sun.setColor(ColorRGBA.White);
        rootNode.addLight(sun);
    }

    private void setupBoundaryFrame() {
        float x = bean.getBoundsX() * 0.5f;
        float y = bean.getBoundsY() * 0.5f;
        float z = bean.getBoundsZ() * 0.5f;

        WireBox wireBox = new WireBox(x, y, z);
        wireBox.setLineWidth(2f);

        Geometry frame = new Geometry("BoundaryFrame", wireBox);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.White);

        frame.setMaterial(mat);
        frame.setCullHint(Spatial.CullHint.Never);

        rootNode.attachChild(frame);
    }

    @Override
    public void simpleInitApp() {
        GuiGlobals.initialize(this);
        GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");
        Panel panel = BeanEditor.open(bean);
        panel.setLocalTranslation(settings.getWidth() - panel.getPreferredSize().x, settings.getHeight(), 0);
        guiNode.attachChild(panel);

        setupCameraAndLight();

        // Initialize separated simulation
        fluidSimulation = new FluidSimulation(20 * 20 * 20, assetManager, bean);
        rootNode.attachChild(fluidSimulation.getGeometry());

        setupBoundaryFrame();

        com.jme3.scene.shape.Torus torusMesh = new com.jme3.scene.shape.Torus(32, 32, 0.5f, 2.0f);
        Geometry obstacle = new Geometry("TorusObstacle", torusMesh);

        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Diffuse", ColorRGBA.Red);
        obstacle.setMaterial(mat);
        obstacle.setLocalTranslation(0, -5, 0);
        obstacle.rotate(0.0f, 0.5f, 0);
        rootNode.attachChild(obstacle);
        fluidSimulation.registerCollidable(obstacle);
    }

    @Override
    public void simpleUpdate(float tpf) {
        Spatial obstacle = rootNode.getChild("TorusObstacle");
        if (obstacle != null) {
            obstacle.rotate(tpf * 0.4f, tpf * 0.2f, 0);
        }

        fluidSimulation.update(tpf);
        updateBoundaryFrame();
    }

    @Override
    public void destroy() {
        super.destroy();
        if (fluidSimulation != null) fluidSimulation.cleanup();
    }

    private void updateBoundaryFrame() {
        Spatial frame = rootNode.getChild("BoundaryFrame");
        if (frame instanceof Geometry) {
            WireBox wb = (WireBox) ((Geometry) frame).getMesh();
            wb.updatePositions(bean.getBoundsX() * 0.5f, bean.getBoundsY() * 0.5f, bean.getBoundsZ() * 0.5f);
        }
    }
}
