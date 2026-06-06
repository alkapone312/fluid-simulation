package pl.pwr.jkaplone.fluidsim;

import com.jme3.app.SimpleApplication;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.WireBox;
import com.jme3.scene.shape.Quad;
import com.jme3.system.AppSettings;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import com.jme3.util.SkyFactory;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Panel;
import pl.pwr.jkaplone.fluidsim.bean.BeanEditor;
import pl.pwr.jkaplone.fluidsim.render.PlainParticleProcessor;
import pl.pwr.jkaplone.fluidsim.render.ssfr.*;
import pl.pwr.jkaplone.fluidsim.render.volume.PerspectiveVolumeBean;
import pl.pwr.jkaplone.fluidsim.render.volume.PerspectiveVolumeProcessor;
import pl.pwr.jkaplone.fluidsim.simulation.FluidSimulation;
import pl.pwr.jkaplone.fluidsim.simulation.SimulationBean;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Simulation3D extends SimpleApplication {

    private FluidSimulation fluidSimulation;
    public SimulationBean bean = new SimulationBean();

    private int currentRenderMode = -1;

    public static void main(String[] args) {
        Simulation3D app = new Simulation3D();
        AppSettings settings = new AppSettings(true);
        settings.setResolution(1920, 1080);
        settings.setRenderer(AppSettings.LWJGL_OPENGL43);
        settings.setFullscreen(true);
        settings.setVSync(false);
        app.setSettings(settings);
        app.start();
    }

    private Texture2D generateFloorTexture(int size) {
        Image image = new Image(
            Image.Format.RGBA8, size, size,
            BufferUtils.createByteBuffer(size * size * 4),
            null, ColorSpace.sRGB);

        ByteBuffer data = image.getData(0);
        int subTileCount = 128; // Number of small tiles per side
        int pixelsPerSubTile = size / subTileCount;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                ColorRGBA baseColor;
                if (x < size / 2) {
                    baseColor = (y < size / 2) ? ColorRGBA.Red : ColorRGBA.Blue;
                } else {
                    baseColor = (y < size / 2) ? ColorRGBA.Green : ColorRGBA.Yellow;
                }

                int tx = x / pixelsPerSubTile;
                int ty = y / pixelsPerSubTile;
                float brightness = ((tx + ty) % 2 == 0) ? 0.3f : 0.6f;

                data.put((byte) (baseColor.r * brightness * 255));
                data.put((byte) (baseColor.g * brightness * 255));
                data.put((byte) (baseColor.b * brightness * 255));
                data.put((byte) 255);
            }
        }
        data.rewind();
        return new Texture2D(image);
    }

    private void setupCameraAndLight() {
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(0.3f));
        rootNode.addLight(al);

        Spatial sky = SkyFactory.createSky(assetManager,
            "textures/PanoramaSky.png",
            SkyFactory.EnvMapType.EquirectMap);
        sky.setLocalRotation(new com.jme3.math.Quaternion().fromAngleAxis(-30 * FastMath.DEG_TO_RAD, Vector3f.UNIT_Y));
        rootNode.attachChild(sky);

        float floorSize = 100f;
        Quad quad = new Quad(floorSize, floorSize);
        Geometry floor = new Geometry("Floor", quad);

        floor.rotate(-FastMath.HALF_PI, 0, 0);
        floor.setLocalTranslation(-floorSize/2, -bean.getBoundsY()*0.5f - 0.1f, floorSize/2);

        Material floorMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        floorMat.setTexture("ColorMap", generateFloorTexture(1024));
        floor.setMaterial(floorMat);

        rootNode.attachChild(floor);

        cam.setLocation(new Vector3f(0, 0, 20));
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

    private List<Panel> panels = new ArrayList<>();
    public <T> void setupBeanEditor(T bean, Consumer<T> consumer) {
        if (GuiGlobals.getInstance() == null) {
            GuiGlobals.initialize(this);
            GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");
        }

        Panel panel = null;
        if (consumer == null) {
            panel = BeanEditor.open(bean, bean.getClass().getSimpleName());
        }

        if (consumer != null) {
            panel = BeanEditor.openWithCallback(bean, consumer, bean.getClass().getSimpleName());
        }

        if (panel != null) {
            guiNode.attachChild(panel);
            panels.add(panel);
        }
    }

    public <T> void setupBeanEditor(T bean) {
        setupBeanEditor(bean, (b) -> {});
    }

    public void updateGui() {
        float lastPanelY = 0;
        for (var panel : panels) {
            panel.setLocalTranslation(
                settings.getWidth() - panel.getPreferredSize().x,
                settings.getHeight() - lastPanelY,
                0
            );
            lastPanelY += panel.getPreferredSize().y;
        }
    }

    private PerspectiveVolumeProcessor volumeProcessor;
    private SsfrProcessor ssfrProcessor;
    private PlainParticleProcessor particleProcessor;

    public SsfrBean ssfrBean = new SsfrBean();
    public GaussianSmoothingBean gaussianSmoothingBean = new GaussianSmoothingBean();
    public CurvatureFlowSmoothingBean curvatureFlowSmoothingBean = new CurvatureFlowSmoothingBean();
    public PerspectiveVolumeBean volumeBean = new PerspectiveVolumeBean();
    private GaussianSmoothing gaussianSmoothing;
    private CurvatureFlowSmoothing curvatureFlowSmoothing;

    private int lastRenderMode = -1;
    private int lastGaussianSmoothing = -1;
    private int lastCurvatureFlowSmoothing = -1;
    private int lastGridWidth = -1;
    private int lastGridHeight = -1;
    @Override
    public void simpleInitApp() {
        curvatureFlowSmoothing = new CurvatureFlowSmoothing(assetManager, curvatureFlowSmoothingBean);
        gaussianSmoothing = new GaussianSmoothing(assetManager, gaussianSmoothingBean);
        fluidSimulation = new FluidSimulation(24*24*24, assetManager, bean);
        ssfrProcessor = new SsfrProcessor(
            assetManager,
            fluidSimulation.getGeometry(),
            ssfrBean,
            ssfrBean.getGaussianSmoothing() == 1 ? gaussianSmoothing : curvatureFlowSmoothing
        );
        volumeProcessor = new PerspectiveVolumeProcessor(
            assetManager,
            fluidSimulation.getGeometry(),
            volumeBean
        );
        particleProcessor = new PlainParticleProcessor(
            fluidSimulation.getGeometry(),
            new Material(assetManager, "materials/particles/Particles.j3md")
        );

        this.viewPort.addProcessor(particleProcessor);
        this.viewPort.addProcessor(ssfrProcessor);
        this.viewPort.addProcessor(volumeProcessor);

        setupBeanEditor(bean);
        switchRenderMode(bean.getRenderMode());
        setupBeanEditor(ssfrBean);
        setupBeanEditor(gaussianSmoothingBean);
        setupBeanEditor(curvatureFlowSmoothingBean);
        setupBeanEditor(volumeBean);
        setupCameraAndLight();

        switchRenderMode(bean.getRenderMode());
        lastRenderMode = bean.getRenderMode();
        lastGaussianSmoothing = ssfrBean.getGaussianSmoothing();
        lastCurvatureFlowSmoothing = ssfrBean.getCurvatureFlowSmoothing();
        lastGridWidth = volumeBean.getPerspectiveGridWidth();
        lastGridHeight = volumeBean.getPerspectiveGridHeight();

        setupBoundaryFrame();
    }

    private void pollBeanChanges() {
        if (lastRenderMode != bean.getRenderMode()) {
            switchRenderMode(bean.getRenderMode());
            lastRenderMode = bean.getRenderMode();
        }

        if (lastGaussianSmoothing != ssfrBean.getGaussianSmoothing() ||
            lastCurvatureFlowSmoothing != ssfrBean.getCurvatureFlowSmoothing()) {

            if (ssfrBean.getGaussianSmoothing() == 1) {
                ssfrProcessor.setSsfrSmoothing(gaussianSmoothing);
            } else if (ssfrBean.getCurvatureFlowSmoothing() == 1) {
                ssfrProcessor.setSsfrSmoothing(curvatureFlowSmoothing);
            }

            lastGaussianSmoothing = ssfrBean.getGaussianSmoothing();
            lastCurvatureFlowSmoothing = ssfrBean.getCurvatureFlowSmoothing();
        }

        if (lastGridWidth != volumeBean.getPerspectiveGridWidth() ||
            lastGridHeight != volumeBean.getPerspectiveGridHeight()) {

            if (volumeProcessor != null && volumeProcessor.getViewport() != null) {
                volumeProcessor.setupGridTexture();
                lastGridWidth = volumeBean.getPerspectiveGridWidth();
                lastGridHeight = volumeBean.getPerspectiveGridHeight();
            }
        }
    }

    private void switchRenderMode(int mode) {
        if (particleProcessor != null) viewPort.removeProcessor(particleProcessor);
        if (ssfrProcessor != null) viewPort.removeProcessor(ssfrProcessor);
        if (volumeProcessor != null) viewPort.removeProcessor(volumeProcessor);

        switch (mode) {
            case 0:
                System.out.println("Switching to: Plain Particles");
                if (particleProcessor != null) viewPort.addProcessor(particleProcessor);
                break;
            case 1:
                System.out.println("Switching to: Screen Space Fluid Rendering (SSFR)");
                if (ssfrProcessor != null) viewPort.addProcessor(ssfrProcessor);
                break;
            case 2:
                System.out.println("Switching to: Perspective Volume Rendering");
                if (volumeProcessor != null) viewPort.addProcessor(volumeProcessor);
                break;
            default:
                System.err.println("Nieznany tryb renderowania!");
                break;
        }

        currentRenderMode = mode;
    }

    @Override
    public void simpleUpdate(float tpf) {
        updateGui();
        pollBeanChanges();
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
