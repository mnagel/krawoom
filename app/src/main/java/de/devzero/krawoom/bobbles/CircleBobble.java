package de.devzero.krawoom.bobbles;

import com.badlogic.gdx.physics.box2d.Body;

import org.andengine.entity.sprite.Sprite;

import de.devzero.krawoom.KrawoomWorld;

public class CircleBobble extends Bobble {

    public CircleBobble(KrawoomWorld world, Body body, Sprite face, float maxHealth) {
        super(world, body, face, maxHealth);
    }
}
