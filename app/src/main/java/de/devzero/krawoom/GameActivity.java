package de.devzero.krawoom;

import android.content.Context;
import android.graphics.Typeface;
import android.hardware.SensorManager;
import android.os.Vibrator;
import android.widget.Toast;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Manifold;

import org.andengine.audio.sound.Sound;
import org.andengine.audio.sound.SoundFactory;
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
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.text.Text;
import org.andengine.entity.text.TextOptions;
import org.andengine.entity.util.FPSCounter;
import org.andengine.entity.util.FPSLogger;
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.extension.physics.box2d.util.Vector2Pool;
import org.andengine.input.sensor.acceleration.AccelerationData;
import org.andengine.input.sensor.acceleration.IAccelerationListener;
import org.andengine.input.touch.TouchEvent;
import org.andengine.input.touch.controller.MultiTouchController;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.font.FontFactory;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.TextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.util.adt.align.HorizontalAlign;
import org.andengine.util.adt.color.Color;
import org.andengine.util.debug.Debug;

import java.io.IOException;
import java.util.Iterator;

public class GameActivity extends SimpleBaseGameActivity implements IAccelerationListener, IOnSceneTouchListener, IOnAreaTouchListener {
    // THESE ARE ABSOLUTE CONSTANTS! SCALING TO DEVICE SCREEN SIZES HAPPENS ONE LAYER DOWN!
    private static final int XMAX = 1920;
    private static final int YMAX = 1080;
    private static final int INITIAL_BOBBLE_COUNT = 30;

    private TextureRegion mBoxFaceTextureRegion;
    private TextureRegion mCircleFaceTextureRegion;

    private int bobblecount = 0;
    private long flingcount = 0;

    private PhysicsWorld mPhysicsWorld;

    private float mGravityX;
    private float mGravityY;

    private Scene mScene;

    private Font mFont;
    private Sound explosionSound;
    private Vibrator vibrator;
    public static String debugString = "";

    @Override
    public EngineOptions onCreateEngineOptions() {
        Toast.makeText(this, "Touch the screen to add objects. Touch an object to shoot it up into the air.", Toast.LENGTH_LONG).show();

        final Camera camera = new Camera(0, 0, XMAX, YMAX);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        EngineOptions opt = new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, new RatioResolutionPolicy(XMAX, YMAX), camera);
        opt.getAudioOptions().setNeedsSound(true);
        return opt;
    }

    @Override
    public void onCreateResources() {
        BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");

        BitmapTextureAtlas mBitmapTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 256, 256, TextureOptions.BILINEAR);
        this.mBoxFaceTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(mBitmapTextureAtlas, this, "box.png", 0, 0);
        this.mCircleFaceTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(mBitmapTextureAtlas, this, "ball.png", 0, 100);
        mBitmapTextureAtlas.load();

        this.mFont = FontFactory.create(this.getFontManager(), this.getTextureManager(), 256, 256, TextureOptions.BILINEAR, Typeface.create(Typeface.DEFAULT, Typeface.BOLD), 48, Color.WHITE_ARGB_PACKED_INT);
        this.mFont.load();

        SoundFactory.setAssetBasePath("sfx/");
        try {
            explosionSound = SoundFactory.createSoundFromAsset(this.mEngine.getSoundManager(), this, "click.wav");
        } catch (final IOException e) {
            Debug.e(e);
        }
    }

    @Override
    public Scene onCreateScene() {
        mEngine.setTouchController(new MultiTouchController());

        this.mEngine.registerUpdateHandler(new FPSLogger());

        this.mGravityX = 0;
        this.mGravityY = -SensorManager.GRAVITY_EARTH;

        this.mPhysicsWorld = new PhysicsWorld(new Vector2(mGravityX, mGravityY), false);

        this.mScene = new Scene();
        this.mScene.setBackground(new Background(Color.BLACK));
        this.mScene.setOnSceneTouchListener(this);

        final VertexBufferObjectManager vertexBufferObjectManager = this.getVertexBufferObjectManager();
        int w = 10;

        // overlap in corners, we don't care
        final Rectangle top = new Rectangle(XMAX / 2, YMAX - w / 2, XMAX, w, vertexBufferObjectManager);
        final Rectangle bottom = new Rectangle(XMAX / 2, w / 2, XMAX, w, vertexBufferObjectManager);
        final Rectangle left = new Rectangle(w / 2, YMAX / 2, w, YMAX, vertexBufferObjectManager);
        final Rectangle right = new Rectangle(XMAX - w / 2, YMAX / 2, w, YMAX, vertexBufferObjectManager);

        final FixtureDef wallFixtureDef = PhysicsFactory.createFixtureDef(0, 0.5f, 0.5f);
        PhysicsFactory.createBoxBody(this.mPhysicsWorld, top, BodyType.StaticBody, wallFixtureDef);
        PhysicsFactory.createBoxBody(this.mPhysicsWorld, bottom, BodyType.StaticBody, wallFixtureDef);
        PhysicsFactory.createBoxBody(this.mPhysicsWorld, left, BodyType.StaticBody, wallFixtureDef);
        PhysicsFactory.createBoxBody(this.mPhysicsWorld, right, BodyType.StaticBody, wallFixtureDef);

        for (int i = 0; i < INITIAL_BOBBLE_COUNT; i++) {
            float x = (float) (0.2 + 0.6 * Math.random()) * XMAX;
            float y = (float) (0.2 + 0.6 * Math.random()) * YMAX;
            addFace(x, y);
        }

        this.mScene.attachChild(top);
        this.mScene.attachChild(bottom);
        this.mScene.attachChild(left);
        this.mScene.attachChild(right);

        this.mPhysicsWorld.setContactListener(createContactListener());

        this.mScene.registerUpdateHandler(this.mPhysicsWorld);

        this.mScene.setOnAreaTouchListener(this);

        final FPSCounter fpsCounter = new FPSCounter();
        this.mEngine.registerUpdateHandler(fpsCounter);

        int x = 150;
        int y = 150;
        int xl2 = 150;
        int yl2 = 100;
        int xl3 = 960;
        int yl3 = 50;

        // TODO check these buffer sizes...
        final Text elapsedText = new Text(x, y, this.mFont, "Seconds elapsed:", "Seconds elapsed: XXXXX".length(), this.getVertexBufferObjectManager());
        final Text fpsText = new Text(xl2, yl2, this.mFont, "FPS:", "FPS: XXXXX".length(), this.getVertexBufferObjectManager());
        final Text debugText = new Text(xl3, yl3, this.mFont, "DEBUG:", 100, new TextOptions(HorizontalAlign.LEFT), this.getVertexBufferObjectManager());
        this.mScene.attachChild(elapsedText);
        this.mScene.attachChild(fpsText);
        this.mScene.attachChild(debugText);
        this.mScene.registerUpdateHandler(new TimerHandler(1 / 20.0f, true, new ITimerCallback() {
            @Override
            public void onTimePassed(final TimerHandler pTimerHandler) {
                elapsedText.setText(String.format("%d~%d", flingcount, bobblecount));
                fpsText.setText(String.format("%.2f FPS", fpsCounter.getFPS()));
                debugText.setText(debugString);
            }
        }));

        return this.mScene;
    }

    @Override
    public boolean onAreaTouched(final TouchEvent pSceneTouchEvent, final ITouchArea pTouchArea, final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
        if (pSceneTouchEvent.isActionDown()) {
            final Sprite face = (Sprite) pTouchArea;
            this.jumpFace(face);
            return true;
        }

        return false;
    }

    @Override
    public boolean onSceneTouchEvent(final Scene pScene, final TouchEvent pSceneTouchEvent) {
        if (pSceneTouchEvent.isActionDown()) {
            Iterator<Body> i = mPhysicsWorld.getBodies();

            while (i.hasNext()) {
                Body b = i.next();

                if (b.getUserData() instanceof Bobble) {
                    // TODO: v gets overwritten if getPosition is called again !! (side effect)
                    Vector2 p = new Vector2(((Bobble) b.getUserData()).sprite.getX(), ((Bobble) b.getUserData()).sprite.getY());
                    Vector2 v = p.cpy().sub(pSceneTouchEvent.getX(), pSceneTouchEvent.getY());
                    //debugString = String.format("v %.2f, %.2f, t %.2f, %.2f, b %.2f, %.2f",
                    //        v.x, v.y, pSceneTouchEvent.getX(), pSceneTouchEvent.getY(), p.x, p.y);

                    float len = v.len();
                    len *= len;
                    if (len > 1e-5) {
                        v.nor().mul(10000000.0f / len);
                        debugString = String.format("v %.2f, %.2f", v.x, v.y);
                        b.applyLinearImpulse(v, b.getLocalCenter());
                    }
                }
            }

            return true;
        }

        return false;
    }

    @Override
    public void onAccelerationAccuracyChanged(final AccelerationData pAccelerationData) {

    }

    @Override
    public void onAccelerationChanged(final AccelerationData pAccelerationData) {
        /*this.mGravityX = pAccelerationData.getX();
        this.mGravityY = pAccelerationData.getY();

        final Vector2 gravity = Vector2Pool.obtain(this.mGravityX, this.mGravityY);
        this.mPhysicsWorld.setGravity(gravity);
        Vector2Pool.recycle(gravity);*/
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

    /**
     * spawn a new Bobble
     *
     * @param pX center x
     * @param pY center y
     */
    private void addFace(final float pX, final float pY) {
        this.bobblecount++;
        vibrator.vibrate(100);

        final Sprite face;
        final Body body;

        final FixtureDef objectFixtureDef = PhysicsFactory.createFixtureDef(1, 0.5f, 0.5f);

        if (this.bobblecount % 2 == 1) {
            face = new Sprite(pX, pY, this.mBoxFaceTextureRegion, this.getVertexBufferObjectManager());
            body = PhysicsFactory.createBoxBody(this.mPhysicsWorld, face, BodyType.DynamicBody, objectFixtureDef);
        } else {
            face = new Sprite(pX, pY, this.mCircleFaceTextureRegion, this.getVertexBufferObjectManager());
            body = PhysicsFactory.createCircleBody(this.mPhysicsWorld, face, BodyType.DynamicBody, objectFixtureDef);
        }

        body.setUserData(new Bobble(face, (float) Math.random() * 99 + 1));

        this.mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(face, body, true, true));

        face.setColor(Color.GREEN);
        face.setUserData(body);
        this.mScene.registerTouchArea(face);
        this.mScene.attachChild(face);
    }

    private void jumpFace(final Sprite face) {
        final Body faceBody = (Body) face.getUserData();

        final Vector2 velocity = Vector2Pool.obtain(this.mGravityX * -50, this.mGravityY * -50);
        faceBody.setLinearVelocity(velocity);
        Vector2Pool.recycle(velocity);

        vibrator.vibrate(100);
        explosionSound.play();
        flingcount++;
    }

    private ContactListener createContactListener() {
        return new ContactListener() {
            @Override
            public void beginContact(Contact contact) {

                Fixture a = contact.getFixtureA();
                Object o = a.getBody().getUserData();
                if (o instanceof Bobble) {
                    handleBobble((Bobble) o, a.getBody());
                }

                Fixture b = contact.getFixtureB();
                o = b.getBody().getUserData();
                if (o instanceof Bobble) {
                    handleBobble((Bobble) o, b.getBody());
                }
            }

            private void handleBobble(final Bobble b, final Body body) {
                b.health -= 1;
                if (b.health < 0) {
                    b.health = 0;
                    if (b.died) {
                        return;
                    }
                    b.died = true;
                    runOnUpdateThread(new Runnable() {
                        @Override
                        public void run() {
                            mScene.detachChild(b.sprite);
                            mPhysicsWorld.destroyBody(body);
                            mScene.unregisterTouchArea(b.sprite);
                        }
                    });
                }
                b.sprite.setColor(b.getColor());
            }

            @Override
            public void endContact(Contact contact) {
            }

            @Override
            public void preSolve(Contact contact, Manifold oldManifold) {
            }

            @Override
            public void postSolve(Contact contact, ContactImpulse impulse) {
            }
        };
    }
}
