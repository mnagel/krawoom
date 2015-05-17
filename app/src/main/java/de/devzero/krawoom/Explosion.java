package de.devzero.krawoom;

import android.opengl.GLES20;

import org.andengine.entity.IEntity;
import org.andengine.entity.modifier.AlphaModifier;
import org.andengine.entity.modifier.IEntityModifier;
import org.andengine.entity.modifier.ParallelEntityModifier;
import org.andengine.entity.modifier.ScaleModifier;
import org.andengine.entity.sprite.Sprite;
import org.andengine.opengl.texture.region.TextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.util.modifier.IModifier;

public class Explosion extends Sprite {
    public Explosion(float x, float y, TextureRegion textureRegion, VertexBufferObjectManager vertexBufferObjectManager, final GameActivity gameActivity) {
        super(x, y, textureRegion, vertexBufferObjectManager);

        setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        ParallelEntityModifier modifier = new ParallelEntityModifier(new IEntityModifier.IEntityModifierListener() {
            @Override
            public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {
            }

            @Override
            public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
                gameActivity.runOnUpdateThread(new Runnable() {
                    @Override
                    public void run() {
                        detachSelf();
                    }
                });
            }
        },
                new ScaleModifier(0.2f, 0.5f, 2.0f),
                new AlphaModifier(0.2f, 1.0f, 0.0f)
        );

        registerEntityModifier(modifier);
    }
}
