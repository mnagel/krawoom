package de.devzero.krawoom;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;

public class GameActivity extends Activity {
    private PhysicsThread physicsThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        physicsThread = new PhysicsThread(this);
        physicsThread.start();

        setContentView(new GameView(this, physicsThread));
    }

    @Override
    protected void onPause() {
        super.onPause();

        physicsThread.pauseGame();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        physicsThread.stopGame();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }
}