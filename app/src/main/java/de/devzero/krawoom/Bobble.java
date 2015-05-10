package de.devzero.krawoom;

import com.badlogic.gdx.physics.box2d.Body;

import org.andengine.entity.sprite.Sprite;
import org.andengine.util.adt.color.Color;

public class Bobble {
    final Sprite face;
    final Body body;
    final KrawoomWorld world;
    final float maxHealth;
    float health;
    public boolean died;

    public Bobble(KrawoomWorld world, Body body, Sprite face, float maxHealth) {
        // TODO we do not want to know our face!!!
        this.face = face;
        this.world = world;
        this.body = body;
        this.maxHealth = maxHealth;
        this.health = this.maxHealth;

    }

    public float getRelativeHealth() {
        return health / maxHealth;
    }

    public Color getColor() {
        // TODO: HSV und buffer color
        return new Color(1 - getRelativeHealth(), getRelativeHealth(), 0, 0.8f);
    }

    public void jump() {
        float x = face.getX();
        float y = face.getY();
        world.explosion(x, y);
        if (1==1) return;

        // TODO remove dependency from world here
        // TODO dont set velocity, apply impulse!
        body.setLinearVelocity(world.gravity.cpy().mul(-7f));

        // TODO implement directed pressschlag instead of upwards jump

        world.vibrate(10);
        // TODO better manage sound ids
        world.playSound("explosion");
        // todo remove inverse dependency
        world.flingcount++;
    }

    public void takeDamage(float dmg) {
        health -= dmg;
        if (health < 0) {
            die();
        }
        face.setColor(getColor());
    }

    public void die() {
        // always set to 0, takeDamage might have subtracted even if died==true
        health = 0;
        if (died) {
            return;
        }
        died = true;

        world.informDeath(this);
        world.vibrate(100);
    }
}
