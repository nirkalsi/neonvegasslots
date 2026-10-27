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
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity implements SlotsView.Listener, BillingManager.Listener {
    static final String PREFS = "neonvegasslots", KEY = "state";
    private static final long[] DAILY_BONUS = {200, 300, 400, 500, 700, 900, 1200};

    private SlotsView game;
    private AdsManager ads;
    private BillingManager billing;
    private MissionsManager missions;
    private SharedPreferences sp;
    private long dailyCelebrationUntil;
    private LinearLayout bannerBox;
    private final Handler handler = new Handler();

    /** Debug-only motion demo for emulator video review: never runs in release builds or unit tests. */
    @Override protected void onResume() {
        super.onResume();
        if (music != null) music.start();
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences(PREFS, MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF120826);
        root.addView(buildButtonRow());
        root.addView(buildQuestStrip());
        game = new SlotsView(this);
        game.setListener(this);
        GameDialog.resetQueue();
        GameDialog.busy = () -> System.currentTimeMillis()<dailyCelebrationUntil || (game != null && game.isBusyAnimating());
        sfx = new SoundFx(this);
        music = new Music(this);
        game.setSoundFx(sfx);
        // Short screens: the game view is taller than the viewport so the progression trail panel keeps a readable size; scroll to reach it.
        ScrollView gameScroll = new ScrollView(this) {
            @Override protected void onMeasure(int wSpec, int hSpec) {
                super.onMeasure(wSpec, hSpec);
                float w = getMeasuredWidth();
                int need = (int) ((w * 0.50f + 3 * (w / 5f - 8) * 1.22f + 40 + w * 0.155f) / 0.71f);
                int vh = getMeasuredHeight();
                int want = Math.max(vh, need);
                // ScrollView measures its child UNSPECIFIED and ignores LayoutParams.height; minimum height is what it honours.
                if (game.getMinimumHeight() != want) { game.setMinimumHeight(want); game.requestLayout(); }
            }
            private boolean touched = false; // until the player scrolls, keep the controls in view as the viewport changes (e.g. banner loads)
            @Override public boolean onTouchEvent(android.view.MotionEvent e) { touched = true; return super.onTouchEvent(e); }
            @Override public boolean onInterceptTouchEvent(android.view.MotionEvent e) { if (e.getActionMasked() == android.view.MotionEvent.ACTION_DOWN) touched = true; return super.onInterceptTouchEvent(e); }
            @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
                super.onLayout(changed, l, t, r, b);
                int vh = getHeight(), gh = game.getHeight();
                if (!touched && vh > 0 && gh > vh) { // short screen: open with the reels AND the BET/SPIN row in view
                    float w = getWidth();
                    int ctrlBottom = (int) (gh * 0.195f + 3 * (w / 5f - 8) * 1.22f + 16 + 24 + gh * 0.07f + 12);
                    scrollTo(0, Math.max(0, Math.min(gh - vh, ctrlBottom - vh)));
                }
            }
        };
        gameScroll.setFillViewport(true);
        gameScroll.setVerticalScrollBarEnabled(false);
        gameScroll.addView(game, new android.widget.FrameLayout.LayoutParams(-1, -1));
        root.addView(gameScroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        bannerBox = new LinearLayout(this);
        root.addView(bannerBox, new LinearLayout.LayoutParams(-1, -2));
        android.widget.FrameLayout shell = new android.widget.FrameLayout(this);
        shell.setBackgroundColor(0xFF120826);
        shell.addView(root, new android.widget.FrameLayout.LayoutParams(-1, -1));
        if (Splash.enabled) {
            final Splash splash = new Splash(this);
            shell.addView(splash, new android.widget.FrameLayout.LayoutParams(-1, -1));
            handler.postDelayed(() -> splash.animate().alpha(0f).setDuration(280)
                .withEndAction(() -> shell.removeView(splash)).start(), Splash.SHOW_MS);
        }
        setContentView(shell);
        if (android.os.Build.VERSION.SDK_INT >= 30) { // targetSdk 35+ forces edge-to-edge: keep content clear of system bars
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                android.graphics.Insets bars = insets.getInsets(android.view.WindowInsets.Type.systemBars());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
        }

        String s = sp.getString(KEY, null);
        if (s != null) game.restore(s);
        // The existing500starting coins are the gift. This moment never credits extra currency.
        if(s==null&&!sp.getBoolean("welcome_shown",false)){
            sp.edit().putBoolean("welcome_shown",true).apply();
            handler.postDelayed(()->{if(!isFinishing()&&!isDestroyed()){game.welcomeGiftMoment();float[] target=game.chipCenterOnScreen();ClaimBurst.play(game,500,target[0],target[1]);}},700);
        }

        missions = new MissionsManager(this, null);

        ads = new AdsManager(this);
        ads.setAdEvents(this::onInterstitialShown);
        boolean removed = sp.getBoolean("ads_removed", false) || sp.getBoolean("remove_ads", false);
        if (removed) sp.edit().putBoolean("ads_removed", true).apply();
        ads.setAdsRemoved(removed);
        if (removed) applyAdsRemoved();
        int sessions = sp.getInt("sessions_started", 0) + 1;
        sp.edit().putInt("sessions_started", sessions).apply();
        final boolean firstSession = sessions <= 1 && !Reminders.legacyCohort(this);
        // No banner during the player's first session (design spec A1); from the second session on the bottom banner shows as before.
        ads.initWithConsent(() -> { if (!removed && !firstSession) ads.attachBanner(bannerBox); });

        billing = new BillingManager(this, this);

        if (sp.getString("first_run", null) == null)
            sp.edit().putString("first_run", today()).apply();

        refreshQuestStrip();
        // New installs get to play first: the daily bonus waits for a few spins and a first win (design spec A1). Returning players see it at launch as before.
        if (!Reminders.legacyCohort(this) && sp.getString("daily_last", "").isEmpty()) handler.postDelayed(this::dailyBonusWhenReady, 2500);
        else handler.postDelayed(this::maybeDailyBonus, 900);
        handler.postDelayed(this::maybeAskNotifications, 1800);
    }

    private android.widget.ProgressBar questBar;
    private TextView questText;
    private View buildQuestStrip() {
        LinearLayout strip = new LinearLayout(this);
        strip.setOrientation(LinearLayout.VERTICAL);
        strip.setPadding(16, 6, 16, 6);
        strip.setBackgroundColor(0xFF2A1450);
        questText = new TextView(this);
        questText.setTextColor(0xFFFFD36B); questText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 12f); questText.setTypeface(Typeface.DEFAULT_BOLD);
        questText.setSingleLine(true); questText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        questBar = new android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        questBar.setMax(20);
        questBar.getProgressDrawable().setColorFilter(0xFFFFB300, android.graphics.PorterDuff.Mode.SRC_IN);
        strip.addView(questText);
        strip.addView(questBar, new LinearLayout.LayoutParams(-1, (int) (6 * getResources().getDisplayMetrics().density)));
        strip.setOnClickListener(v -> showMissions());
        return strip;
    }
    private void refreshQuestStrip() {
        if (questText == null || missions == null) return;
        List<MissionsManager.Item> q = missions.weeklyQuest();
        int step = 0; while (step < q.size() - 1 && q.get(step).claimed) step++;
        MissionsManager.Item cur = q.get(step);
        questBar.setMax((int) cur.goal);
        questBar.setProgress((int) Math.min(cur.goal, cur.progress));
        refreshClaimBadge();
        String reward = cur.done() && !cur.claimed ? "tap to claim +" + cur.reward : "reward +" + cur.reward + " coins";
        questText.setText("Quest " + (step + 1) + "/" + q.size() + ": spin " + cur.goal + " times (" + Math.min(cur.goal, cur.progress) + "/" + cur.goal + ") - " + reward);
    }

    private IconTile missionsTile;
    private View removeAdsTile;

    private View buildButtonRow() {
        float dn = getResources().getDisplayMetrics().density;
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding((int) (6 * dn), (int) (2 * dn), (int) (6 * dn), 0);
        int vTop = 0xFF8B5AE0, vBot = 0xFF2E1560, gold = 0xFFE8B84A;
        IconTile coins = new IconTile(this, "FREE COINS", 0xFFFFD166, vTop, vBot, gold, "Watch an ad for free coins", 30);
        coins.setOnClickListener(v -> freeCoins());
        missionsTile = new IconTile(this, "MISSIONS", 0xFFFFD166, vTop, vBot, gold, "Missions", 30);
        missionsTile.setOnClickListener(v -> showMissions());
        IconTile shop = new IconTile(this, "SHOP", 0xFF107A2C, 0xFF5BE36F, 0xFF107A2C, 0xFFFFD36B, "Shop", 34);
        shop.setOnClickListener(v -> showShop());
        IconTile[] tiles = {coins, missionsTile, shop};
        for (int i = 0; i < tiles.length; i++) {
            int size = (int) ((tiles[i] == missionsTile ? 96 : 84) * dn); // every tap area is at least 48dp
            if (i > 0) row.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
            row.addView(tiles[i], new LinearLayout.LayoutParams(size, (int)(72*dn)));
        }
        return row;
    }

    private void refreshClaimBadge() {
        if (missionsTile == null || missions == null || game == null) return;
        missionsTile.setBadge(missions.claimableCount(game.getLevel(), game.getSpinsDone()));
    }

    private GameDialog.Builder dlg() { return new GameDialog.Builder(this, GameDialog.fromTheme(SlotArt.THEMES[game.getThemeIdx()])); }

    // ---------- daily bonus ----------
    private String today() { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()); }
    private String yesterday() {
        Calendar c = Calendar.getInstance(); c.add(Calendar.DAY_OF_YEAR, -1);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.getTime());
    }

    private boolean firstMeaningfulWin=false;
    private void dailyBonusWhenReady() {
        if (isFinishing() || isDestroyed()) return;
        long spins = game.getSpinsDone();
        if (spins >= 4 && (firstMeaningfulWin || game.getLastWin() > 0) && !game.isSpinning()) maybeDailyBonus();
        else handler.postDelayed(this::dailyBonusWhenReady, 1500);
    }

    private void maybeDailyBonus() {
        String last = sp.getString("daily_last", "");
        if (today().equals(last)) return;
        int streak = yesterday().equals(last) ? sp.getInt("daily_streak", 0) + 1 : 1;
        long bonus = DAILY_BONUS[Math.min(streak, DAILY_BONUS.length) - 1];
        LinearLayout dailyBox = new LinearLayout(this);
        dailyBox.setOrientation(LinearLayout.VERTICAL);
        dailyBox.addView(DailyChest.hero(this),
            new LinearLayout.LayoutParams(-1, (int) (72 * getResources().getDisplayMetrics().density)));
        dailyBox.addView(dailyLadder(streak));
        dlg().title("Daily bonus - day " + streak)
            .body("Come back every day to grow your streak.\nCoins are virtual only - no cash value, no prizes.")
            .content(dailyBox)
            .add(GameDialog.Btn.primary("Watch ad for 2X  -  +" + (bonus * 2), () -> ads.showRewarded(() -> grantDaily(streak, bonus * 2))))
            .add(GameDialog.Btn.secondary("Claim +" + bonus, () -> grantDaily(streak, bonus)))
            .auto().show();
    }

    private View dailyLadder(int streak) {
        SlotArt.Theme th=SlotArt.THEMES[game.getThemeIdx()];
        float density=getResources().getDisplayMetrics().density;
        LinearLayout track=new LinearLayout(this);track.setOrientation(LinearLayout.VERTICAL);
        int current=Math.min(streak,DAILY_BONUS.length)-1;
        for(int row=0;row<3;row++) {
            LinearLayout line=new LinearLayout(this);line.setOrientation(LinearLayout.HORIZONTAL);line.setBaselineAligned(false);line.setClipChildren(false);line.setClipToPadding(false);
            int from=row<2?row*3:6,to=row<2?from+3:7;
            for(int i=from;i<to;i++) {
                TextView card=new TextView(this);card.setGravity(Gravity.CENTER);card.setTypeface(Typeface.DEFAULT_BOLD);
                card.setTextSize(i==6?18:i==current?12:14);card.setTextColor(i==current?th.title:th.soft);
                String status=i<current?"  \u2713":i==current?"\nTODAY":"";
                card.setText((i==6?"DAY 7 - TREASURE CHEST":"DAY "+(i+1))+status+"\n+"+String.format(java.util.Locale.US,"%,d",DAILY_BONUS[i])+" coins");
                android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,new int[]{i==current?GameDialog.withAlpha(th.neon,0x55):th.panelBg,th.panelBg});
                bg.setCornerRadius(12*density);bg.setStroke((int)(i==current?3*density:1*density),i==current?th.title:th.panelLine);card.setBackground(bg);
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,(int)((i==6?78:64)*density),1);lp.setMargins((int)(3*density),(int)(3*density),(int)(3*density),(int)(3*density));line.addView(card,lp);
                if(i==current){android.animation.ObjectAnimator bob=android.animation.ObjectAnimator.ofFloat(card,"scaleY",1f,.96f,1f);bob.setDuration(1600);bob.setRepeatCount(android.animation.ValueAnimator.INFINITE);bob.start();card.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(View v){}public void onViewDetachedFromWindow(View v){bob.cancel();}});}
            }
            track.addView(line,new LinearLayout.LayoutParams(-1,-2));
        }
        return track;
    }

    private void grantDaily(int streak, long coins) {
        if(today().equals(sp.getString("daily_last","")))return; // duplicate earned callback/tap cannot pay twice
        sp.edit().putString("daily_last", today()).putInt("daily_streak", streak).apply();
        missions.onDailyClaimed(streak);
        game.addCoins(coins);
        dailyCelebrationUntil=System.currentTimeMillis()+2000;
        float[] target=game.chipCenterOnScreen();DailyChest.celebrate(this,game,coins,target[0],target[1]);
        handler.postDelayed(this::maybeAskNotifications, 700);
    }

    // ---------- rewarded free coins ----------
    private void freeCoins() {
        if(game.pendingLevelReward()>0)return;
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
        addSection(box, "Neon Quest - " + missions.questTimeLeft() + " (ends Monday 00:00)", missions.weeklyQuest());
        addSection(box, "Daily missions", missions.dailyMissions());
        addSection(box, "Achievements", missions.achievements(game.getLevel(), game.getSpinsDone()));
        Button rem = new Button(this);
        rem.setAllCaps(false); rem.setBackgroundColor(0xFF3A1F66); rem.setTextColor(Color.WHITE);
        reminderBtn = rem;
        refreshReminderBtn();
        rem.setOnClickListener(v -> {
            if (Reminders.isOn(this)) { Reminders.setOn(this, false); refreshReminderBtn(); return; }
            if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 91); // turned on only after the player grants it
                return;
            }
            Reminders.setOn(this, true); refreshReminderBtn();
        });
        TextView remNote = new TextView(this);
        remNote.setText("If you turn it on: one reminder at about 10:30 local time, at least 6 hours after you leave the game (it is cancelled if you come back first). It runs on this device, not a server.");
        remNote.setTextSize(11f); remNote.setTextColor(0xFFC9B8E0); remNote.setPadding(0, 6, 0, 12);
        TextView legalNote = new TextView(this);
        legalNote.setText("Free play only. Virtual coins - no real money, no prizes.");
        legalNote.setTextSize(11f); legalNote.setTextColor(0xFFC9B8E0); legalNote.setPadding(0, 0, 0, 12);
        Button snd = new Button(this);
        snd.setAllCaps(false); snd.setBackgroundColor(0xFF3A1F66); snd.setTextColor(Color.WHITE);
        snd.setText("Sound effects: " + (SoundFx.isOn(this) ? "ON" : "OFF"));
        snd.setOnClickListener(v -> { SoundFx.setOn(this, !SoundFx.isOn(this)); snd.setText("Sound effects: " + (SoundFx.isOn(this) ? "ON" : "OFF")); });
        Button mus = new Button(this);
        mus.setAllCaps(false); mus.setBackgroundColor(0xFF3A1F66); mus.setTextColor(Color.WHITE);
        mus.setText("Music: " + (Music.isOn(this) ? "ON" : "OFF"));
        mus.setOnClickListener(v -> { boolean on = !Music.isOn(this); Music.setOn(this, on); if (on) { if (music != null) music.start(); } else if (music != null) music.pause(); mus.setText("Music: " + (on ? "ON" : "OFF")); });
        box.addView(snd); box.addView(mus); box.addView(rem); box.addView(remNote); box.addView(legalNote);
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
            line.setOrientation(LinearLayout.HORIZONTAL);line.setBaselineAligned(false);line.setClipChildren(false);line.setClipToPadding(false);
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
                        float[] tgt = game.chipCenterOnScreen();
                        ClaimBurst.play(c, reward, tgt[0], tgt[1]);
                        refreshClaimBadge();
                        if (missionsTile != null) missionsTile.bounce();
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

    private void onInterstitialShown() {
        int total = sp.getInt("inter_total", 0) + 1;
        sp.edit().putInt("inter_total", total).apply();
    }

    private void applyAdsRemoved() {
        bannerBox.removeAllViews();
        if (removeAdsTile != null) removeAdsTile.setVisibility(View.GONE);
    }

    // ---------- SlotsView.Listener ----------
    @Override public void onOpenShop() { showShop(); }
    @Override public void onOpenPaytable() {
        dlg().title("Paytable & paylines")
            .body("Five paylines. Match 3, 4 or 5 of the same symbol consecutively from the leftmost reel. All winning lines are added. Values below are at BET 10; rewards scale with your bet (rounded down per line). Virtual coins only, no cash value.")
            .content(new PaytableView(this, game.getThemeIdx()))
            .add(GameDialog.Btn.secondary("Close", null)).show();
    }
    @Override public void onOpenMap() { dlg().title("City Tour").content(new MapView(this, game)).pinnedChrome().add(GameDialog.Btn.secondary("Close", null)).show(); }

    @Override public void onTrophyFound(int index) {
        TrophyReveal reveal=new TrophyReveal(this,index);
        reveal.setLayoutParams(new android.widget.LinearLayout.LayoutParams(-1,(int)(170*getResources().getDisplayMetrics().density)));
        dlg().title("Trophy discovered!").body(SlotArt.TROPHY_NAMES[index/3][index%3]+"\n"+SlotArt.THEMES[index/3].name+" collection. Cosmetic only; no coin cost or bonus.")
            .content(reveal).add(GameDialog.Btn.primary("Keep playing",null)).auto().show();
    }

    @Override public void onOutOfCoins() {
        GameDialog.Builder b = dlg().title("Out of coins")
            .body("Watch a short ad for +300 free coins?\nCoins also refill slowly over time.\n\nCoins are virtual only - no cash value, no prizes.")
            .add(GameDialog.Btn.primary("Watch ad  (+300)", () -> ads.showRewarded(() -> game.addCoins(300))));
        b.add(GameDialog.Btn.secondary("Not now", null)).auto().show();
    }

    @Override public void onSpinComplete(int spinsDone, long win, long bet) {
        if(win>0)firstMeaningfulWin=true;
        missions.onSpin(win, bet, game.getLevel(), spinsDone);
        // interstitials never fire during play (AdMob: logical breaks only)
        refreshQuestStrip();
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
        // Reward is now collected once in the queued, save-safe gameplay level moment.
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

    @Override protected void onPause() { super.onPause(); if (music != null) music.pause(); saveState(); }
    private SoundFx sfx;
    SoundFx sfxForTest() { return sfx; }
    private Button reminderBtn;
    /** One-time yes/no notification question for players never asked before. No = never asked again (Settings still works). */
    private void maybeAskNotifications() {
        if (isFinishing() || isDestroyed() || !Reminders.promptEnabled) return;
        if (!today().equals(sp.getString("daily_last", ""))) return; // daily bonus first; never stack dialogs
        if (System.currentTimeMillis()<dailyCelebrationUntil || GameDialog.isOpen() || !GameDialog.pending.isEmpty() || (game != null && game.isBusyAnimating()) || GameDialog.actionsSinceClose < 1) { handler.postDelayed(this::maybeAskNotifications, 1500); return; }
        boolean enabled, rationale = false;
        android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        enabled = nm == null || nm.areNotificationsEnabled();
        if (android.os.Build.VERSION.SDK_INT >= 33) rationale = shouldShowRequestPermissionRationale("android.permission.POST_NOTIFICATIONS");
        // Android 13+ before any prompt reports notifications disabled; only treat as a refusal if a rationale exists or the asked flag is set.
        if (android.os.Build.VERSION.SDK_INT >= 33 && !enabled && !rationale) enabled = true;
        if (!Reminders.shouldAsk(Reminders.legacyCohort(this), Reminders.asked(this), Reminders.isOn(this), enabled, rationale)) return;
        Reminders.markAsked(this); // shown once, whatever the answer
        new android.app.AlertDialog.Builder(this)
            .setTitle("Get game reminders?")
            .setMessage("Allow notifications for your daily bonus and new events? They run on this device only. You can change this any time in Settings.")
            .setPositiveButton("Yes", (d, w) -> {
                if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED)
                    requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 91);
                else { Reminders.setOn(this, true); refreshReminderBtn(); }
            })
            .setNegativeButton("No", (d, w) -> {})
            .setCancelable(true).show();
    }
    private void refreshReminderBtn() {
        if (reminderBtn == null) return;
        boolean on = Reminders.isOn(this), blocked = on && !Reminders.effective(this);
        reminderBtn.setText(blocked ? "Daily reminder: blocked in system settings" : on ? "Daily reminder: ON (tap to turn off)" : "Daily reminder: OFF (tap to turn on)");
    }
    @Override public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        if (code != 91) return;
        boolean granted = res.length > 0 && res[0] == android.content.pm.PackageManager.PERMISSION_GRANTED;
        Reminders.setOn(this, granted); // denied: stays off, nothing scheduled
        refreshReminderBtn();
    }
    private Music music;
    @Override protected void onDestroy() { super.onDestroy(); if (sfx != null) sfx.release(); if (music != null) music.release(); }
    @Override protected void onStop() { super.onStop(); Reminders.schedule(this); }
    @Override protected void onStart() { super.onStart(); Reminders.cancel(this); }
}
