package nir.com.neonvegasslots;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;

public class MainActivity extends Activity implements SlotsView.Listener {
    private static final String PREFS = "neonvegasslots", KEY = "state";
    private SlotsView game;
    private AdsManager ads;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        game = new SlotsView(this);
        game.setListener(this);
        root.addView(game, new LinearLayout.LayoutParams(-1, 0, 1f));
        LinearLayout bannerBox = new LinearLayout(this);
        root.addView(bannerBox, new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);

        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        String s = sp.getString(KEY, null);
        if (s != null) game.restore(s);

        ads = new AdsManager(this);
        ads.initWithConsent(() -> ads.attachBanner(bannerBox));
    }

    @Override public void onOutOfCoins() {
        new AlertDialog.Builder(this)
            .setTitle("Out of coins")
            .setMessage("Watch a short ad for +300 free coins? Coins also refill slowly over time. All coins are virtual and free.")
            .setPositiveButton("Watch ad", (d, w) -> ads.showRewarded(() -> game.addCoins(300)))
            .setNegativeButton("Not now", null)
            .show();
    }

    @Override public void onSpinComplete(int spinsDone) {
        ads.maybeShowInterstitial(spinsDone);
    }

    @Override public void saveState() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY, game.serialize()).apply();
    }

    @Override protected void onPause() { super.onPause(); saveState(); }
}
