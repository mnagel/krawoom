package de.devzero.krawoom;

import android.content.Context;
import android.graphics.Typeface;
import android.hardware.SensorManager;
import android.os.Vibrator;
import android.widget.Toast;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;

import org.andengine.engine.camera.Camera;
import org.andengine.engine.handler.timer.ITimerCallback;
import org.andengine.engine.handler.timer.TimerHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.IOnAreaTouchListener;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.ITouchArea;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.entity.text.Text;
import org.andengine.entity.util.FPSCounter;
import org.andengine.entity.util.FPSLogger;
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.extension.physics.box2d.util.Vector2Pool;
import org.andengine.input.sensor.acceleration.AccelerationData;
import org.andengine.input.sensor.acceleration.IAccelerationListener;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.font.FontFactory;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.util.adt.color.Color;

public class GameActivity extends SimpleBaseGameActivity implements IAccelerationListener, IOnSceneTouchListener, IOnAreaTouchListener {
    // THESE ARE ABSOLUTE CONSTANTS! SCALING TO DEVICE SCREEN SIZES HAPPENS ONE LAYER DOWN!
    private static final int XMAX = 1920;
    private static final int YMAX = 1080;

    private BitmapTextureAtlas mBitmapTextureAtlas;

    private TiledTextureRegion mBoxFaceTextureRegion;
    private TiledTextureRegion mCircleFaceTextureRegion;

    private int bobblecount = 0;
    private long flingcount = 0;

    private PhysicsWorld mPhysicsWorld;

    private float mGravityX;
    private float mGravityY;

    private Scene mScene;

    private Font mFont;
    private Vibrator vibrator;

    @Override
    public EngineOptions onCreateEngineOptions() {
        Toast.makeText(this, "Touch the screen to add objects. Touch an object to shoot it up into the air.", Toast.LENGTH_LONG).show();

        final Camera camera = new Camera(0, 0, XMAX, YMAX);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        return new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, new RatioResolutionPolicy(XMAX, YMAX), camera);
    }

    @Override
    public void onCreateResources() {
        // BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");

        this.mBitmapTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 256, 256, TextureOptions.BILINEAR);
        this.mBoxFaceTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.mBitmapTextureAtlas, this, "box.png", 0, 0, 2, 1); // 64x32
        this.mCircleFaceTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.mBitmapTextureAtlas, this, "ball.png", 0, 100, 2, 1); // 64x32
        this.mBitmapTextureAtlas.load();

        this.mFont = FontFactory.create(this.getFontManager(), this.getTextureManager(), 256, 256, TextureOptions.BILINEAR, Typeface.create(Typeface.DEFAULT, Typeface.BOLD), 48, Color.WHITE_ARGB_PACKED_INT);
        this.mFont.load();
    }

    @Override
    public Scene onCreateScene() {
        this.mEngine.registerUpdateHandler(new FPSLogger());

        this.mPhysicsWorld = new PhysicsWorld(new Vector2(0, SensorManager.GRAVITY_EARTH), false);

        this.mScene = new Scene();
        this.mScene.setBackground(new Background(Color.BLACK));
        this.mScene.setOnSceneTouchListener(this);

        final VertexBufferObjectManager vertexBufferObjectManager = this.getVertexBufferObjectManager();
        int w = 10;

        // overlap in corners, we dont care
        final Rectangle top = new Rectangle(XMAX/2, YMAX-w/2, XMAX, w, vertexBufferObjectManager);
        final Rectangle bottom = new Rectangle(XMAX/2, w/2, XMAX, w, vertexBufferObjectManager);
        final Rectangle left = new Rectangle(w/2, YMAX/2, w, YMAX, vertexBufferObjectManager);
        final Rectangle right = new Rectangle(XMAX-w/2, YMAX/2, w, YMAX, vertexBufferObjectManager);

        final FixtureDef wallFixtureDef = PhysicsFactory.createFixtureDef(0, 0.5f, 0.5f);
        PhysicsFactory.createBoxBody(this.mPhysicsWorld, top, BodyType.StaticBody, wallFixtureDef);
        PhysicsFactory.createBoxBody(this.mPhysicsWorld, bottom, BodyType.StaticBody, wallFixtureDef);
        PhysicsFactory.createBoxBody(this.mPhysicsWorld, left, BodyType.StaticBody, wallFixtureDef);
        PhysicsFactory.createBoxBody(this.mPhysicsWorld, right, BodyType.StaticBody, wallFixtureDef);

        this.mScene.attachChild(top);
        this.mScene.attachChild(bottom);
        this.mScene.attachChild(left);
        this.mScene.attachChild(right);

        this.mScene.registerUpdateHandler(this.mPhysicsWorld);

        this.mScene.setOnAreaTouchListener(this);

        final FPSCounter fpsCounter = new FPSCounter();
        this.mEngine.registerUpdateHandler(fpsCounter);

        int x = 150;
        int y = 150;
        int xl2 = 150;
        int yl2 = 100;

        // TODO check these buffer sizes...
        final Text elapsedText = new Text(x, y, this.mFont, "Seconds elapsed:", "Seconds elapsed: XXXXX".length(), this.getVertexBufferObjectManager());
        final Text fpsText = new Text(xl2, yl2, this.mFont, "FPS:", "FPS: XXXXX".length(), this.getVertexBufferObjectManager());
        this.mScene.attachChild(elapsedText);
        this.mScene.attachChild(fpsText);
        this.mScene.registerUpdateHandler(new TimerHandler(1 / 20.0f, true, new ITimerCallback() {
            @Override
            public void onTimePassed(final TimerHandler pTimerHandler) {
                elapsedText.setText(String.format("%d~%d", flingcount, bobblecount));
                fpsText.setText(String.format("%.2f FPS", fpsCounter.getFPS()));
            }
        }));

        return this.mScene;
    }

    @Override
    public boolean onAreaTouched(final TouchEvent pSceneTouchEvent, final ITouchArea pTouchArea, final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
        if (pSceneTouchEvent.isActionDown()) {
            final AnimatedSprite face = (AnimatedSprite) pTouchArea;
            this.jumpFace(face);
            return true;
        }

        return false;
    }

    @Override
    public boolean onSceneTouchEvent(final Scene pScene, final TouchEvent pSceneTouchEvent) {
        if (this.mPhysicsWorld != null) {
            if (pSceneTouchEvent.isActionDown()) {
                this.addFace(pSceneTouchEvent.getX(), pSceneTouchEvent.getY());
                    vibrator.vibrate(100);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onAccelerationAccuracyChanged(final AccelerationData pAccelerationData) {

    }

    @Override
    public void onAccelerationChanged(final AccelerationData pAccelerationData) {
        this.mGravityX = pAccelerationData.getX();
        this.mGravityY = pAccelerationData.getY();

        final Vector2 gravity = Vector2Pool.obtain(this.mGravityX, this.mGravityY);
        this.mPhysicsWorld.setGravity(gravity);
        Vector2Pool.recycle(gravity);
    }

    @Override
    public void onResumeGame() {
        super.onResumeGame();

        this.enableAccelerationSensor(this);
    }

    @Override
    public void onPauseGame() {
        super.onPauseGame();

        this.disableAccelerationSensor();
    }

    private void addFace(final float pX, final float pY) {
        this.bobblecount++;

        final AnimatedSprite face;
        final Body body;

        final FixtureDef objectFixtureDef = PhysicsFactory.createFixtureDef(1, 0.5f, 0.5f);

        if (this.bobblecount % 2 == 1) {
            face = new AnimatedSprite(pX, pY, this.mBoxFaceTextureRegion, this.getVertexBufferObjectManager());
            body = PhysicsFactory.createBoxBody(this.mPhysicsWorld, face, BodyType.DynamicBody, objectFixtureDef);
        } else {
            face = new AnimatedSprite(pX, pY, this.mCircleFaceTextureRegion, this.getVertexBufferObjectManager());
            body = PhysicsFactory.createCircleBody(this.mPhysicsWorld, face, BodyType.DynamicBody, objectFixtureDef);
        }

        this.mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(face, body, true, true));

        face.animate(new long[]{200, 200}, 0, 1, true);
        face.setUserData(body);
        this.mScene.registerTouchArea(face);
        this.mScene.attachChild(face);
    }

    private void jumpFace(final AnimatedSprite face) {
        final Body faceBody = (Body) face.getUserData();

        final Vector2 velocity = Vector2Pool.obtain(this.mGravityX * -50, this.mGravityY * -50);
        faceBody.setLinearVelocity(velocity);
        Vector2Pool.recycle(velocity);

        vibrator.vibrate(100);
        flingcount++;
    }
}