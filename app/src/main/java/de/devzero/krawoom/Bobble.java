package de.devzero.krawoom;

import org.andengine.entity.sprite.Sprite;
import org.andengine.util.adt.color.Color;

public class Bobble {
    final Sprite sprite;
    final float maxHealth;
    float health;

    public Bobble(Sprite sprite, float maxHealth) {
        this.sprite = sprite;
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
}
