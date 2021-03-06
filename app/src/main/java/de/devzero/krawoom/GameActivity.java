package de.devzero.krawoom;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Vibrator;

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
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
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

import de.devzero.krawoom.bobbles.Bobble;

public class GameActivity extends SimpleBaseGameActivity implements IAccelerationListener, IOnSceneTouchListener, IOnAreaTouchListener {
    // THESE ARE ABSOLUTE CONSTANTS! SCALING TO DEVICE SCREEN SIZES HAPPENS ONE LAYER DOWN!
    private static final int XMAX = 1920;
    private static final int YMAX = 1080;
    private static final int INITIAL_BOBBLE_COUNT = 50;
    public static String debugString = "";
    public TextureRegion mBoxFaceTextureRegion;
    public TextureRegion mCircleFaceTextureRegion;
    public TextureRegion mExplosionFaceTextureRegion;
    public Scene mScene;
    private KrawoomWorld krawoomWorld;
    private PhysicsWorld mPhysicsWorld;
    private Font mFont;
    private Sound explosionSound;
    private Vibrator vibrator;

    @Override
    public EngineOptions onCreateEngineOptions() {
        final Camera camera = new Camera(0, 0, XMAX, YMAX);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        EngineOptions opt = new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, new RatioResolutionPolicy(XMAX, YMAX), camera);
        opt.getAudioOptions().setNeedsSound(true);
        return opt;
    }

    @Override
    public void onCreateResources() {
        BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");

        BitmapTextureAtlas mBitmapTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 512, 1024, TextureOptions.BILINEAR);
        this.mBoxFaceTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(mBitmapTextureAtlas, this, "box.png", 0, 0);
        this.mCircleFaceTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(mBitmapTextureAtlas, this, "ball.png", 100, 0);
        this.mExplosionFaceTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(mBitmapTextureAtlas, this, "explosion.png", 0, 100);

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

        float mGravityX = 0;
        float mGravityY = 0; // -SensorManager.GRAVITY_EARTH;
        Vector2 grav = new Vector2(mGravityX, mGravityY);

        this.mPhysicsWorld = new PhysicsWorld(grav, false);
        krawoomWorld = new KrawoomWorld(this, mPhysicsWorld, grav);

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
            krawoomWorld.spawnBobble(x, y);
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
        final Text elapsedText = new Text(x, y, this.mFont, "Seconds elapsed:", 100, this.getVertexBufferObjectManager());
        final Text fpsText = new Text(xl2, yl2, this.mFont, "FPS:", 100, this.getVertexBufferObjectManager());
        final Text debugText = new Text(xl3, yl3, this.mFont, "DEBUG:", 100, new TextOptions(HorizontalAlign.LEFT), this.getVertexBufferObjectManager());
        this.mScene.attachChild(elapsedText);
        this.mScene.attachChild(fpsText);
        this.mScene.attachChild(debugText);
        this.mScene.registerUpdateHandler(new TimerHandler(1 / 20.0f, true, new ITimerCallback() {
            @Override
            public void onTimePassed(final TimerHandler pTimerHandler) {
                // elapsedText.setText(String.format("%d~%d", krawoomWorld.flingcount, krawoomWorld.bobblecount));
                // TODO listener statt polling bei jedem tick...
                float good = krawoomWorld.getGoodHP() / 100;
                float bad = krawoomWorld.getBadHP() / 100;
                // TODO hp scaling...
                elapsedText.setText(String.format("%.2f", good - bad)); // (%.2f-%.2f)", good-bad, good, bad));
                fpsText.setText(String.format("%.2f FPS", fpsCounter.getFPS()));
                debugText.setText(debugString);
            }
        }));

        return this.mScene;
    }

    public void vibrate(int ms) {
        vibrator.vibrate(ms);
    }

    // TODO better handle sound Ids
    public void playSound(String soundId) {
        switch (soundId) {
            case "explosion":
                explosionSound.play();
                return;
            default:
                return;
        }
    }

    @Override
    public boolean onAreaTouched(final TouchEvent pSceneTouchEvent, final ITouchArea pTouchArea, final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
        if (pSceneTouchEvent.isActionDown()) {
            final Sprite face = (Sprite) pTouchArea;
            Object o = face.getUserData();
            //this.debugString = o.getClass().toString();
            if (o instanceof Bobble) {
                ((Bobble) o).jump();
                mScene.attachChild(new Explosion(pSceneTouchEvent.getX(), pSceneTouchEvent.getY(), this.mExplosionFaceTextureRegion, this.getVertexBufferObjectManager(), this));
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean onSceneTouchEvent(final Scene pScene, final TouchEvent pSceneTouchEvent) {
        if (pSceneTouchEvent.isActionDown()) {
            krawoomWorld.explosion(pSceneTouchEvent.getX(), pSceneTouchEvent.getY());
            mScene.attachChild(new Explosion(pSceneTouchEvent.getX(), pSceneTouchEvent.getY(), this.mExplosionFaceTextureRegion, this.getVertexBufferObjectManager(), this));

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

    private ContactListener createContactListener() {
        return new ContactListener() {
            @Override
            public void beginContact(Contact contact) {
            }

            private void handleBobble(final Bobble b, final Body body, float impulse) {
                b.takeDamage(impulse);
            }

            @Override
            public void endContact(Contact contact) {
            }

            @Override
            public void preSolve(Contact contact, Manifold oldManifold) {
            }

            @Override
            public void postSolve(Contact contact, ContactImpulse impulse) {
                float[] impulses = impulse.getNormalImpulses();
                float amount = (float) Math.sqrt(Math.pow(impulses[0], 2) + Math.pow(impulses[1], 2)) - 500;
                if (amount < 0) {
                    // This is abysmal impulse, so ignore it. (Vector length is squared, hence bigger than normal.)
                    return;
                }

                Fixture a = contact.getFixtureA();
                Object o = a.getBody().getUserData();
                if (o instanceof Bobble) {
                    handleBobble((Bobble) o, a.getBody(), amount);
                }

                Fixture b = contact.getFixtureB();
                o = b.getBody().getUserData();
                if (o instanceof Bobble) {
                    handleBobble((Bobble) o, b.getBody(), amount);
                }
            }
        };
    }
}
