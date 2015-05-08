package de.devzero.krawoom;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class GameView extends GLSurfaceView {
    private Renderer renderer;

    public GameView(Context context) {
        super(context);
    }

    public GameView(Context context, PhysicsThread physicsThread) {
        super(context);

        renderer = new GameRenderer(context, physicsThread);

        setRenderer(renderer);
    }
}