package nir.com.neonvegasslots;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity implements SlotsView.Listener, BillingManager.Listener {
    private static final String PREFS = "neonvegasslots", KEY = "state";
    private static final long[] DAILY_BONUS = {200, 300, 400, 500, 700, 900, 1200};

    private SlotsView game;
    private AdsManager ads;
    private BillingManager billing;
    private MissionsManager missions;
    private SharedPreferences sp;
    private LinearLayout bannerBox;
    private Button removeAdsBtn;
    private final Handler handler = new Handler();

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences(PREFS, MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF120826);
        root.addView(buildButtonRow());
        game = new SlotsView(this);
        game.setListener(this);
        root.addView(game, new LinearLayout.LayoutParams(-1, 0, 1f));
        bannerBox = new LinearLayout(this);
        root.addView(bannerBox, new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);
        if (android.os.Build.VERSION.SDK_INT >= 30) { // targetSdk 35+ forces edge-to-edge: keep content clear of system bars
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                android.graphics.Insets bars = insets.getInsets(android.view.WindowInsets.Type.systemBars());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
        }

        String s = sp.getString(KEY, null);
        if (s != null) game.restore(s);

        missions = new MissionsManager(this, null);

        ads = new AdsManager(this);
        ads.setAdEvents(this::onInterstitialShown);
        boolean removed = sp.getBoolean("ads_removed", false);
        ads.setAdsRemoved(removed);
        if (removed) applyAdsRemoved();
        ads.initWithConsent(() -> { if (!removed) ads.attachBanner(bannerBox); });

        billing = new BillingManager(this, this);

        if (sp.getString("first_run", null) == null)
            sp.edit().putString("first_run", today()).apply();

        handler.postDelayed(this::maybeDailyBonus, 900);
        handler.postDelayed(this::maybeDay2Offer, 2000);
    }

    private View buildButtonRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(4, 6, 4, 0);
        addRowButton(row, "FREE COINS", v -> freeCoins());
        addRowButton(row, "MISSIONS", v -> showMissions());
        addRowButton(row, "SHOP", v -> showShop());
        addRowButton(row, "MORE GAMES", v ->
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.DEV_PAGE_URL))));
        removeAdsBtn = addRowButton(row, "NO ADS", v -> buyRemoveAds());
        fitRowButtons(row);
        return row;
    }

    private Button addRowButton(LinearLayout row, String label, View.OnClickListener l) {
        Button btn = new Button(this);
        btn.setText(label);
        btn.setTextSize(11f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundColor(0xFF3A1F66);
        btn.setMinHeight(0); btn.setMinimumHeight(0);
        btn.setPadding(6, 0, 6, 0);
        btn.setSingleLine(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, (int) (48 * getResources().getDisplayMetrics().density), 1f);
        lp.setMargins(4, 0, 4, 0);
        row.addView(btn, lp);
        btn.setOnClickListener(l);
        return btn;
    }

    private void fitRowButtons(LinearLayout row) {
        int count = row.getChildCount();
        if (count == 0) return;
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        // each button's usable text width: equal weight share minus margins (4+4) and padding (6+6)
        float avail = (dm.widthPixels - row.getPaddingLeft() - row.getPaddingRight()) / (float) count - 8 - 12;
        android.graphics.Paint m = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        m.setTypeface(Typeface.DEFAULT_BOLD);
        float maxPx = android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 11f, dm);
        float minPx = android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 7f, dm);
        m.setTextSize(maxPx);
        float need = 0;
        for (int i = 0; i < count; i++)
            need = Math.max(need, m.measureText(((Button) row.getChildAt(i)).getText().toString()));
        float size = need > avail ? Math.max(minPx, maxPx * avail / need) : maxPx;
        for (int i = 0; i < count; i++) ((Button) row.getChildAt(i)).setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, size);
    }

    private GameDialog.Builder dlg() { return new GameDialog.Builder(this, GameDialog.fromTheme(SlotArt.THEMES[game.getThemeIdx()])); }

    // ---------- daily bonus ----------
    private String today() { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()); }
    private String yesterday() {
        Calendar c = Calendar.getInstance(); c.add(Calendar.DAY_OF_YEAR, -1);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.getTime());
    }

    private void maybeDailyBonus() {
        String last = sp.getString("daily_last", "");
        if (today().equals(last)) return;
        int streak = yesterday().equals(last) ? sp.getInt("daily_streak", 0) + 1 : 1;
        long bonus = DAILY_BONUS[Math.min(streak, DAILY_BONUS.length) - 1];
        LinearLayout dailyBox = new LinearLayout(this);
        dailyBox.setOrientation(LinearLayout.VERTICAL);
        dailyBox.addView(GameDialog.rewardHero(this, GameDialog.fromTheme(SlotArt.THEMES[game.getThemeIdx()])),
            new LinearLayout.LayoutParams(-1, (int) (120 * getResources().getDisplayMetrics().density)));
        dailyBox.addView(dailyLadder(streak));
        dlg().title("Daily bonus - day " + streak)
            .body("Come back every day to grow your streak.\nCoins are virtual only - no cash value, no prizes.")
            .content(dailyBox)
            .add(GameDialog.Btn.primary("Claim +" + bonus, () -> grantDaily(streak, bonus)))
            .add(GameDialog.Btn.secondary("Watch ad for 2X", () -> ads.showRewarded(() -> grantDaily(streak, bonus * 2))))
            .show();
    }

    private View dailyLadder(int streak) {
        SlotArt.Theme th = SlotArt.THEMES[game.getThemeIdx()];
        float density = getResources().getDisplayMetrics().density;
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(0, (int) (6 * density), 0, (int) (2 * density));
        for (int i = 0; i < DAILY_BONUS.length; i++) {
            TextView chip = new TextView(this);
            chip.setText("DAY " + (i + 1) + "\n+" + DAILY_BONUS[i]);
            chip.setTextSize(10.5f);
            chip.setTypeface(Typeface.DEFAULT_BOLD);
            chip.setGravity(Gravity.CENTER);
            chip.setSingleLine(false);
            android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
            g.setCornerRadius(10 * density);
            int todayIdx = Math.min(streak, DAILY_BONUS.length) - 1;
            if (i == todayIdx) {             // today (streak beyond day 7 keeps day 7 highlighted, matching the capped reward)
                g.setColor(GameDialog.withAlpha(th.neon, 0x30));
                g.setStroke((int) (2 * density), th.neon);
                chip.setTextColor(th.title);
            } else if (i < streak - 1) {     // already claimed
                g.setColor(GameDialog.withAlpha(th.panelLine, 0x22));
                chip.setTextColor(GameDialog.withAlpha(th.soft, 0x88));
            } else {                          // future days
                g.setColor(GameDialog.withAlpha(th.panelLine, 0x40));
                g.setStroke((int) (1 * density), GameDialog.withAlpha(th.neon, 0x33));
                chip.setTextColor(th.soft);
            }
            chip.setBackground(g);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, (int) (54 * density), 1f);
            lp.setMargins((int) (2 * density), 0, (int) (2 * density), 0);
            row.addView(chip, lp);
        }
        return row;
    }

    private void grantDaily(int streak, long coins) {
        sp.edit().putString("daily_last", today()).putInt("daily_streak", streak).apply();
        missions.onDailyClaimed(streak);
        game.addCoins(coins);
        Toast.makeText(this, "+" + coins + " daily coins!", Toast.LENGTH_SHORT).show();
    }

    // ---------- rewarded free coins ----------
    private void freeCoins() {
        long last = sp.getLong("last_free_ad", 0);
        long wait = 60000 - (System.currentTimeMillis() - last);
        if (wait > 0) {
            Toast.makeText(this, "Next free-coin ad in " + (wait / 1000 + 1) + "s", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!ads.hasRewarded()) {
            Toast.makeText(this, "Ad not ready yet - try again in a moment", Toast.LENGTH_SHORT).show();
            return;
        }
        sp.edit().putLong("last_free_ad", System.currentTimeMillis()).apply();
        ads.showRewarded(() -> {
            game.addCoins(300);
            Toast.makeText(this, "+300 coins!", Toast.LENGTH_SHORT).show();
        });
    }

    // ---------- missions & achievements ----------
    private void showMissions() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);
        addSection(box, "Daily missions", missions.dailyMissions());
        addSection(box, "Achievements", missions.achievements(game.getLevel(), game.getSpinsDone()));
        dlg().title("Missions").content(box)
            .add(GameDialog.Btn.secondary("Close", null)).show();
    }

    private void addSection(LinearLayout box, String title, List<MissionsManager.Item> items) {
        SlotArt.Theme th = SlotArt.THEMES[game.getThemeIdx()];
        TextView h = new TextView(this);
        h.setText(title);
        h.setTypeface(Typeface.DEFAULT_BOLD);
        h.setTextSize(16f);
        h.setTextColor(th.title);
        h.setPadding(0, 12, 0, 6);
        box.addView(h);
        for (MissionsManager.Item i : items) {
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.setGravity(Gravity.CENTER_VERTICAL);
            TextView t = new TextView(this);
            t.setTextSize(14f);
            t.setTextColor(SlotArt.THEMES[game.getThemeIdx()].soft);
            t.setSingleLine(false);
            String status = i.claimed ? " (claimed)" : " - " + i.progress + "/" + i.goal;
            t.setText(i.title + status + "  [+" + i.reward + "]");
            LinearLayout txt = new LinearLayout(this);
            txt.setOrientation(LinearLayout.VERTICAL);
            txt.addView(t);
            if (!i.claimed) {
                GameDialog.Palette pal = GameDialog.fromTheme(SlotArt.THEMES[game.getThemeIdx()]);
                View bar = GameDialog.progressBar(this, pal, i.progress, i.goal);
                LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, (int) (10 * getResources().getDisplayMetrics().density));
                blp.topMargin = (int) (3 * getResources().getDisplayMetrics().density);
                blp.bottomMargin = (int) (4 * getResources().getDisplayMetrics().density);
                txt.addView(bar, blp);
            }
            line.addView(txt, new LinearLayout.LayoutParams(0, -2, 1f));
            if (!i.claimed && i.done()) {
                Button c = new Button(this);
                c.setText("Claim");
                c.setBackgroundTintList(android.content.res.ColorStateList.valueOf(GameDialog.fromTheme(SlotArt.THEMES[game.getThemeIdx()]).accent));
                c.setTextColor(GameDialog.fromTheme(SlotArt.THEMES[game.getThemeIdx()]).accentText);
                c.setTypeface(Typeface.DEFAULT_BOLD);
                c.setOnClickListener(v -> {
                    long reward = missions.claim(i);
                    if (reward > 0) {
                        game.addCoins(reward);
                        Toast.makeText(this, "+" + reward + " coins!", Toast.LENGTH_SHORT).show();
                    }
                    c.setVisibility(View.GONE); // claimed missions read as text state, not a dead button
                    t.setText(i.title + " (claimed)  [+" + i.reward + "]");
                });
                line.addView(c);
            }
            box.addView(line);
        }
    }

    // ---------- shop & remove ads ----------
    private void showShop() {
        if (!billing.hasProducts()) {
            billing.refreshProducts();
            Dialog d = dlg().title("Coin shop")
                .body("Loading the shop...\n\nIf packs don't show up, check your connection and tap TRY AGAIN.")
                .add(GameDialog.Btn.primary("TRY AGAIN", this::showShop))
                .add(GameDialog.Btn.secondary("Close", null))
                .show();
            shopLoadingDlg = d;
            d.setOnDismissListener(x -> { if (shopLoadingDlg == d) shopLoadingDlg = null; });
            return;
        }
        SlotArt.Theme th = SlotArt.THEMES[game.getThemeIdx()];
        float density = getResources().getDisplayMetrics().density;
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        // neutral labels only: no popularity/value claims without real usage or per-coin price facts
        for (int i = 0; i < BillingManager.PACK_IDS.length; i++) {
            String id = BillingManager.PACK_IDS[i];
            String price = billing.priceOf(id);
            if (price == null) continue;
            Button b = new Button(this);
            b.setText(String.format("%,d", BillingManager.PACK_COINS[i]) + " coins - " + price);
            b.setSingleLine(false); // wrap instead of clipping on narrow screens
            b.setAllCaps(false);
            b.setTextSize(15f); b.setTypeface(Typeface.DEFAULT_BOLD);
            b.setTextColor(th.title);
            android.graphics.drawable.GradientDrawable pb = new android.graphics.drawable.GradientDrawable();
            pb.setColor(GameDialog.withAlpha(th.panelLine, 0x44)); pb.setCornerRadius(12 * density);
            pb.setStroke((int) (1 * density), GameDialog.withAlpha(th.neon, 0x55));
            b.setBackground(pb);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = (int) (8 * density);
            box.addView(b, lp);
            b.setOnClickListener(v -> billing.buy(this, id));
        }
        TextView note = new TextView(this);
        note.setText("Coins are virtual only - no cash value, no prizes, 18+.");
        note.setTextSize(12f);
        note.setTextColor(th.soft);
        note.setPadding(0, (int) (12 * density), 0, 0);
        box.addView(note);
        dlg().title("Coin shop")
            .body("Running low? Top up your coins:")
            .content(box)
            .add(GameDialog.Btn.secondary("Close", null)).show();
    }

    private void buyRemoveAds() {
        if (billing.getDetails(BillingManager.REMOVE_ADS) != null) {
            billing.buy(this, BillingManager.REMOVE_ADS);
        } else {
            Toast.makeText(this, "Remove ads is not available right now", Toast.LENGTH_SHORT).show();
        }
    }

    private void offerRemoveAds(String why) {
        if (sp.getBoolean("ads_removed", false)) return;
        String price = billing.priceOf(BillingManager.REMOVE_ADS);
        if (price == null) return; // never offer a purchase without the live localized price
        String text = "Enjoying Neon Vegas Slots? Remove banner and between-round ads forever with one small one-time purchase"
            + " (" + price + "). Free rewarded ads for coins stay available either way.";
        dlg().title("Remove ads")
            .body(text)
            .add(GameDialog.Btn.primary("Remove ads - " + price, this::buyRemoveAds))
            .add(GameDialog.Btn.secondary("No thanks", null))
            .show();
    }

    private void onInterstitialShown() {
        int total = sp.getInt("inter_total", 0) + 1;
        sp.edit().putInt("inter_total", total).apply();
        if (total == 3 && !sp.getBoolean("offer3_done", false)) {
            sp.edit().putBoolean("offer3_done", true).apply();
            handler.postDelayed(() -> offerRemoveAds("after3"), 1500);
        }
    }

    private void maybeDay2Offer() {
        if (sp.getBoolean("ads_removed", false) || sp.getBoolean("offer_day2_done", false)) return;
        String first = sp.getString("first_run", today());
        if (!today().equals(first)) {
            sp.edit().putBoolean("offer_day2_done", true).apply();
            offerRemoveAds("day2");
        }
    }

    private void applyAdsRemoved() {
        bannerBox.removeAllViews();
        if (removeAdsBtn != null) removeAdsBtn.setVisibility(View.GONE);
    }

    // ---------- SlotsView.Listener ----------
    @Override public void onOutOfCoins() {
        GameDialog.Builder b = dlg().title("Out of coins")
            .body("Watch a short ad for +300 free coins?\nCoins also refill slowly over time.\n\nCoins are virtual only - no cash value, no prizes.")
            .add(GameDialog.Btn.primary("Watch ad  (+300)", () -> ads.showRewarded(() -> game.addCoins(300))));
        if (!sp.getBoolean("ads_removed", false) && billing.priceOf(BillingManager.REMOVE_ADS) != null)
            b.add(GameDialog.Btn.secondary("Remove banner & interstitial ads", () -> offerRemoveAds("outofcoins")));
        b.add(GameDialog.Btn.secondary("Not now", null)).show();
    }

    @Override public void onSpinComplete(int spinsDone, long win, long bet) {
        missions.onSpin(win, bet, game.getLevel(), spinsDone);
        ads.maybeShowInterstitial(spinsDone);
    }

    @Override public void onBigWin(long win, long bet) {
        if (sp.getBoolean("review_prompted", false)) return;
        sp.edit().putBoolean("review_prompted", true).apply();
        handler.postDelayed(() -> {
            ReviewManager manager = ReviewManagerFactory.create(this);
            manager.requestReviewFlow().addOnCompleteListener(task -> {
                if (task.isSuccessful())
                    manager.launchReviewFlow(this, task.getResult());
            });
        }, 1800);
    }

    @Override public void onLevelUp(int level, long reward) {
        Toast.makeText(this, "Level " + level + "! +" + reward + " coins", Toast.LENGTH_SHORT).show();
    }

    @Override public void saveState() {
        sp.edit().putString(KEY, game.serialize()).apply();
    }

    // ---------- BillingManager.Listener ----------
    @Override public void onAdsRemovedOwned() {
        if (sp.getBoolean("ads_removed", false)) return;
        sp.edit().putBoolean("ads_removed", true).apply();
        runOnUiThread(() -> {
            ads.setAdsRemoved(true);
            applyAdsRemoved();
            Toast.makeText(this, "Ads removed - thank you!", Toast.LENGTH_LONG).show();
        });
    }

    @Override public void onCoinsPurchased(long coins) {
        runOnUiThread(() -> {
            game.addCoins(coins);
            Toast.makeText(this, String.format("+%,d coins - thank you!", coins), Toast.LENGTH_LONG).show();
        });
    }

    private Dialog shopLoadingDlg = null;
    @Override public void onProductsReady() {
        // only swap a still-visible loading dialog for the real shop; never reopen a dismissed one
        if (shopLoadingDlg != null && shopLoadingDlg.isShowing() && billing.hasProducts()) {
            shopLoadingDlg.dismiss();
            showShop();
        }
    }

    @Override protected void onPause() { super.onPause(); saveState(); }
}
