package de.devzero.krawoom;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;

import org.andengine.entity.sprite.Sprite;
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.input.touch.TouchEvent;
import org.andengine.util.adt.color.Color;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// TODO rausfinden, was die Abgrenzung zur GameActivity darstellt

public class KrawoomWorld {
    public int bobblecount = 0;
    public long flingcount = 0;

    public Vector2 gravity;

    GameActivity handheldDevice;
    private PhysicsWorld box2d;

    public KrawoomWorld(GameActivity gameActivity, PhysicsWorld mPhysicsWorld, Vector2 gravity) {
        this.handheldDevice = gameActivity;
        this.box2d = mPhysicsWorld;
        this.gravity = gravity.cpy();
    }

    public List<Bobble> bobbles = new ArrayList<Bobble>();

    /**
     * spawn a new Bobble
     *  @param pX center x
     * @param pY center y
     */
    // TODO stop manipulating the handheld device from within here!!!
    void spawnBobble(final float pX, final float pY) {
        bobblecount++;


        final Sprite face;
        final Body body;

        final FixtureDef objectFixtureDef = PhysicsFactory.createFixtureDef(1, 0.5f, 0.5f);

        if (bobblecount % 2 == 1) {
            // TODO totally bad style to access the handheld device here
            face = new Sprite(pX, pY, handheldDevice.mBoxFaceTextureRegion, handheldDevice.getVertexBufferObjectManager());
            body = PhysicsFactory.createBoxBody(box2d, face, BodyDef.BodyType.DynamicBody, objectFixtureDef);
        } else {
            // TODO totally bad style to access the handheld device here
            face = new Sprite(pX, pY, handheldDevice.mCircleFaceTextureRegion, handheldDevice.getVertexBufferObjectManager());
            body = PhysicsFactory.createCircleBody(box2d, face, BodyDef.BodyType.DynamicBody, objectFixtureDef);
        }

        Bobble b = new Bobble(this, body, face, (float) Math.random() * 500 + 1500);
        bobbles.add(b);
        body.setUserData(b);
        face.setUserData(b);

        box2d.registerPhysicsConnector(new PhysicsConnector(face, body, true, true));

        face.setColor(Color.GREEN);
        //face.setUserData(body);
        // TODO totally bad style to access the handheld device here
        handheldDevice.mScene.registerTouchArea(face);
        handheldDevice.mScene.attachChild(face);
    }

    public void vibrate(int ms) {
        handheldDevice.vibrate(ms);
    }

    public void playSound(String soundId) {
        handheldDevice.playSound(soundId);
    }

    void explosion(float px, float py) {
        for (Bobble b: bobbles) {
            Vector2 p = new Vector2(b.face.getX(), b.face.getY());
            Vector2 t = new Vector2(px, py);
            Vector2 v = p.sub(t);
            //debugString = String.format("v %.2f, %.2f, t %.2f, %.2f, b %.2f, %.2f",
            //        v.x, v.y, pSceneTouchEvent.getX(), pSceneTouchEvent.getY(), p.x, p.y);

            float len = v.len();
            len *= len;
            if (len > 1e-5) {
                // TODO: fix division-by-zero-like effect for small lengths
                v.nor().mul(10000000.0f / len);
                GameActivity.debugString = String.format("v %.2f, %.2f", v.x, v.y);
                b.body.applyLinearImpulse(v, b.body.getWorldCenter());
            }
        }
    }

    public void informDeath(final Bobble b) {
        bobbles.remove(b);
        // TODO viel zu starke Kopplung mit handheldDevice
        handheldDevice.runOnUpdateThread(new Runnable() {
            @Override
            public void run() {
                box2d.destroyBody(b.body);
                handheldDevice.mScene.detachChild(b.face);
                handheldDevice.mScene.unregisterTouchArea(b.face);
            }
        });
    }
}
