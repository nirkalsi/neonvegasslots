package nir.com.neonvegasslots;

import android.graphics.*;

/**
 * Original vector artwork + level-unlockable machine themes.
 * Visual-only: symbol indices and payout math stay unchanged.
 * Neon Vegas identity: night city skyline, lit windows, neon signage, marquee frame.
 */
final class SlotArt {

    static final class Theme {
        final String name, tag;
        final int unlockLevel;
        final int skyTop, skyBottom, back, front, lit, dark, neon, title, titleGlow;
        final int cellBg, cellStroke, cellHot, cellHotStroke, panelBg, panelLine, betBg, spinBg, spinOff, soft;
        Theme(String name, String tag, int unlockLevel, int skyTop, int skyBottom, int back, int front,
              int lit, int dark, int neon, int title, int titleGlow, int cellBg, int cellStroke,
              int cellHot, int cellHotStroke, int panelBg, int panelLine, int betBg, int spinBg, int spinOff, int soft) {
            this.name=name; this.tag=tag; this.unlockLevel=unlockLevel;
            this.skyTop=skyTop; this.skyBottom=skyBottom; this.back=back; this.front=front;
            this.lit=lit; this.dark=dark; this.neon=neon; this.title=title; this.titleGlow=titleGlow;
            this.cellBg=cellBg; this.cellStroke=cellStroke; this.cellHot=cellHot; this.cellHotStroke=cellHotStroke;
            this.panelBg=panelBg; this.panelLine=panelLine; this.betBg=betBg; this.spinBg=spinBg; this.spinOff=spinOff; this.soft=soft;
        }
    }

    static final Theme[] THEMES = {
        new Theme("Downtown Lights","DOWNTOWN",1, 0xFF141439,0xFF050D20, 0xFF1B2350,0xFF10163A, 0xFFFFD978,0xFF26315C, 0xFF38E1FF, 0xFFFFD700,0xFFFF9D00, 0xFF241040,0xFF4A2A7A, 0xFF5B2A86,0xFF7CFC00, 0xFF172442,0xFF48536B, 0xFF3A1F66, 0xFFD81B60,0xFF7A4A5E, 0xFFAFC0D8),
        new Theme("Sunset Strip","SUNSET",3,    0xFF3B1446,0xFF12081F, 0xFF4A1E3C,0xFF2A1028, 0xFFFFB35C,0xFF3A1A38, 0xFFFF4D80, 0xFFFFE29A,0xFFFF5C3A, 0xFF33102E,0xFF6E2450, 0xFF7A2D55,0xFFFFE29A, 0xFF2A1330,0xFF7A3A5E, 0xFF4A1E3C, 0xFFE84393,0xFF8A4A66, 0xFFF0C9DA),
        new Theme("Neon Lagoon","LAGOON",6,     0xFF062A3A,0xFF02101C, 0xFF0E3A4C,0xFF082433, 0xFF9FF2FF,0xFF0E3442, 0xFF2AFFD5, 0xFFAFFFEE,0xFF2AFFD5, 0xFF08333A,0xFF1E6E6E, 0xFF0E5252,0xFF7CFC00, 0xFF0A2E38,0xFF2E6E78, 0xFF0E3A4C, 0xFF00B8A9,0xFF3A6E6A, 0xFFB8E6E2),
        new Theme("High Roller","ROYAL",10,     0xFF100C22,0xFF04030C, 0xFF241C3E,0xFF161028, 0xFFFFE9A0,0xFF2C2444, 0xFFFFD700, 0xFFFFF0B8,0xFFFFD700, 0xFF1C1430,0xFF6E5A2E, 0xFF3A2C58,0xFFFFD700, 0xFF18122C,0xFF8A7238, 0xFF2A2044, 0xFFB8860B,0xFF5E5230, 0xFFD8CBAA),
    };

    static final String[][] TROPHY_NAMES = {
        {"Neon Dice","Lucky Chip","Cherry Bell"},
        {"Flamingo Sign","Sunset Key","Star Marquee"},
        {"Lagoon Pearl","Wave Cup","Glow Fish"},
        {"Gold Crown","Diamond Card","Vault Key"},
    };

    private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();
    private final Path shape = new Path();

    private void color(int v){ink.setColor(v);}
    private void oval(Canvas c,float l,float t,float r,float b){box.set(l,t,r,b);c.drawOval(box,ink);}
    private void line(Canvas c,float a,float b,float x,float y,int color,float width){
        ink.setShader(null);ink.setColor(color);ink.setStrokeWidth(width);ink.setStrokeCap(Paint.Cap.ROUND);c.drawLine(a,b,x,y,ink);
    }
    private void polygon(Canvas c,float...xy){shape.reset();shape.moveTo(xy[0],xy[1]);for(int i=2;xy.length>i+1;i+=2)shape.lineTo(xy[i],xy[i+1]);shape.close();c.drawPath(shape,ink);}
    private int hash(int i,int k){return (i*73856093)^(k*19349663);}

    // ---------- backdrop: neon city scene, recolored per theme ----------
    void backdrop(Canvas c, float w, float h, Theme t, int animPhase) {
        ink.setShader(new LinearGradient(0,0,0,h,t.skyTop,t.skyBottom,Shader.TileMode.CLAMP));
        c.drawRect(0,0,w,h,ink); ink.setShader(null);

        float horizon = h*.215f;
        c.save(); c.clipRect(0, h*.15f, w, h); // scene never enters the HUD band
        // stars
        color(t.soft);
        for(int i=0;i<26;i++){float x=w*((hash(i,1)&0xFFFF)/65535f),y=horizon*.14f+horizon*.5f*((hash(i,2)&0xFFFF)/65535f);c.drawCircle(x,y,w*.0016f,ink);}
        // moon / sun disc
        if(t==THEMES[1]){ color(0xFFFFB35C); c.drawCircle(w*.80f,horizon*.72f,w*.062f,ink); color(0xFFFF8A5C);c.drawCircle(w*.80f,horizon*.72f,w*.046f,ink);}
        else { color(0xFFF4F1DE); c.drawCircle(w*.80f,horizon*.74f,w*.028f,ink); color(t.skyTop); c.drawCircle(w*.818f,horizon*.705f,w*.024f,ink); }

        // back skyline (kept below the title band)
        drawSkyline(c,w,horizon,t.back,t.dark,false,3,.40f);
        // neon signs on the back layer
        neonSigns(c,w,horizon,t,animPhase);
        // front skyline
        drawSkyline(c,w,horizon,t.front,t.lit,true,7,.55f);
        // vertical casino sign tower, right side
        signTower(c,w,horizon,t,animPhase);
        // theme extras
        if(t==THEMES[1]) palms(c,w,horizon);
        if(t==THEMES[2]) lagoon(c,w,h,horizon,t);
        if(t==THEMES[3]) spotlights(c,w,horizon,t);
        // street glow strip
        ink.setShader(new LinearGradient(0,horizon,0,horizon+h*.05f,t.neon&0x00FFFFFF|0x33000000,0x00000000,Shader.TileMode.CLAMP));
        c.drawRect(0,horizon,w,horizon+h*.05f,ink); ink.setShader(null);
        c.restore();
    }

    private void drawSkyline(Canvas c,float w,float horizon,int body,int windows,boolean lit,int seed,float maxH){
        for(int i=0;i<10;i++){
            float bw=w*(.07f+((hash(i,seed)>>8&0xFF)/255f)*.05f);
            float x=w*((hash(i,seed+1)&0xFFFF)/65535f)*1.05f-w*.02f;
            float bh=horizon*(maxH*.45f+((hash(i,seed+2)&0xFFFF)/65535f)*maxH*.55f);
            color(body); c.drawRect(x,horizon-bh,x+bw,horizon,ink);
            // rooftop antenna on some towers
            if((hash(i,seed+3)&3)==0) line(c,x+bw*.5f,horizon-bh,x+bw*.5f,horizon-bh-horizon*.05f,body,w*.004f);
            if(lit){
                int cols=Math.max(2,(int)(bw/(w*.022f))), rows=Math.max(2,(int)(bh/(w*.030f)));
                for(int r=0;r<rows;r++)for(int col2=0;col2<cols;col2++){
                    if((hash(i*31+r*7+col2,seed+4)&3)==0) continue;
                    color(windows);
                    float wx=x+bw*.12f+col2*(bw*.76f/cols), wy=horizon-bh+bh*.10f+r*(bh*.82f/rows);
                    c.drawRect(wx,wy,wx+bw*.10f,wy+bh*.045f,ink);
                }
            }
        }
    }

    private void neonSigns(Canvas c,float w,float horizon,Theme t,int phase){
        ink.setShadowLayer(w*.012f,0,0,t.neon);
        // glowing arrow sign
        line(c,w*.14f,horizon*.72f,w*.22f,horizon*.72f,t.neon,w*.006f);
        line(c,w*.22f,horizon*.72f,w*.19f,horizon*.66f,t.neon,w*.006f);
        line(c,w*.22f,horizon*.72f,w*.19f,horizon*.78f,t.neon,w*.006f);
        // circle sign pulsing
        color(t.neon); ink.setStyle(Paint.Style.STROKE); ink.setStrokeWidth(w*.005f);
        c.drawCircle(w*.58f,horizon*.68f,w*.020f+((phase/2)%2)*w*.002f,ink);
        ink.setStyle(Paint.Style.FILL);
        // zigzag
        polygon(c,w*.40f,horizon*.82f,w*.43f,horizon*.75f,w*.46f,horizon*.82f,w*.49f,horizon*.75f);
        ink.clearShadowLayer();
    }

    private void signTower(Canvas c,float w,float horizon,Theme t,int phase){
        float x=w*.90f, tw=w*.065f, top=horizon*.34f;
        color(t.front); c.drawRect(x,top,x+tw,horizon,ink);
        ink.setTextAlign(Paint.Align.CENTER); ink.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        ink.setTextSize(tw*.42f);
        String s="SLOTS";
        for(int i=0;i<s.length();i++){
            ink.setShadowLayer(w*.010f,0,0,t.neon);
            color((phase+i)%2==0?t.neon:t.title);
            c.drawText(""+s.charAt(i),x+tw*.5f,top+tw*.55f+i*tw*.52f,ink);
        }
        ink.clearShadowLayer(); ink.setTypeface(Typeface.DEFAULT);
    }

    private void palms(Canvas c,float w,float horizon){
        color(0xFF1A0B20);
        for(int i=0;i<3;i++){
            float x=w*(.06f+i*.30f), base=horizon, top=horizon*(.52f+i%2*.1f);
            shape.reset(); shape.moveTo(x-w*.008f,base); shape.quadTo(x+w*.01f,(base+top)/2,x+w*.02f,top);
            shape.lineTo(x+w*.030f,top); shape.quadTo(x+w*.022f,(base+top)/2,x+w*.008f,base); shape.close(); c.drawPath(shape,ink);
            for(int f=0;f<5;f++){double a=Math.PI*(.15f+f*.17f);
                line(c,x+w*.025f,top,x+w*.025f+(float)Math.cos(a)*w*.05f,top-(float)Math.sin(a)*w*.028f,0xFF1A0B20,w*.006f);}
        }
    }

    private void lagoon(Canvas c,float w,float h,float horizon,Theme t){
        ink.setShader(new LinearGradient(0,horizon,0,horizon+h*.04f,0x552AFFD5,0x002AFFD5,Shader.TileMode.CLAMP));
        c.drawRect(0,horizon,w,horizon+h*.04f,ink); ink.setShader(null);
        for(int i=0;i<8;i++) line(c,w*((hash(i,9)&0xFFFF)/65535f),horizon+h*.012f*((i%3)+1),w*((hash(i,9)&0xFFFF)/65535f)+w*.06f,horizon+h*.012f*((i%3)+1),0x442AFFD5,w*.003f);
    }

    private void spotlights(Canvas c,float w,float horizon,Theme t){
        for(int i=0;i<3;i++){
            float x=w*(.2f+i*.3f);
            ink.setShader(new LinearGradient(x,horizon,x+w*.06f,0,0x33FFD700,0x00FFD700,Shader.TileMode.CLAMP));
            polygon(c,x,horizon,x+w*.03f,horizon,x+w*.10f,0f,x+w*.02f,0f);
            ink.setShader(null);
        }
    }

    // ---------- machine frame: marquee with chase bulbs ----------
    void frame(Canvas c, float l, float t, float r, float b, Theme th, int phase) {
        float rad=(r-l)*.045f;
        ink.setStyle(Paint.Style.STROKE);
        ink.setStrokeWidth((r-l)*.012f); ink.setShadowLayer((r-l)*.02f,0,0,th.neon); color(th.neon);
        box.set(l,t,r,b); c.drawRoundRect(box,rad,rad,ink);
        ink.clearShadowLayer();
        ink.setStrokeWidth((r-l)*.006f); color(th.title);
        box.set(l+(r-l)*.012f,t+(r-l)*.012f,r-(r-l)*.012f,b-(r-l)*.012f); c.drawRoundRect(box,rad,rad,ink);
        ink.setStyle(Paint.Style.FILL);
        // chase bulbs along top edge
        int n=12; float bw=r-l;
        for(int i=0;i<n;i++){
            float x=l+bw*.06f+i*(bw*.88f/(n-1));
            color((phase+i)%2==0?0xFFFFE9A0:th.neon);
            c.drawCircle(x,t-(r-l)*.006f,(r-l)*.009f,ink);
        }
    }

    // ---------- progression trail: themes that unlock with level ----------
    static float nodeX(float w,int i){return w*(.16f+i*.226f);}

    void trail(Canvas c, float w, float top, float bottom, int level, long xp, long needed, int selected, String justUnlocked, int trophyMask) {
        if(bottom-top < w*.30f) return;
        float height=Math.min(bottom-top,w*.68f);
        float rs=Math.min(1f,height/(w*.68f)); // rhythm scale: shrink card interior proportionally when vertical space is squeezed (insets, small screens)
        Theme cur=THEMES[selected];
        box.set(w*.05f,top,w*.95f,top+height);
        ink.setShader(new LinearGradient(0,top,w,top+height,cur.panelBg,0xFF0C1120,Shader.TileMode.CLAMP));
        c.drawRoundRect(box,w*.025f,w*.025f,ink); ink.setShader(null);
        ink.setStyle(Paint.Style.STROKE); ink.setStrokeWidth(w*.004f); color(cur.panelLine); c.drawRoundRect(box,w*.025f,w*.025f,ink); ink.setStyle(Paint.Style.FILL);

        ink.setTextAlign(Paint.Align.LEFT); ink.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        ink.setTextSize(w*.030f); color(cur.title);
        c.drawText("CITY TOUR - TAP A STOP YOU UNLOCKED",w*.09f,top+w*.058f,ink);

        // connector
        float ny=top+height*.38f;
        line(c,nodeX(w,0),ny,nodeX(w,THEMES.length-1),ny,cur.panelLine,w*.007f*rs);
        for(int i=0;i<THEMES.length;i++){
            Theme t=THEMES[i]; float x=nodeX(w,i);
            boolean open=level>=t.unlockLevel, sel=i==selected;
            // node disc shows the theme's own colors
            color(open?t.neon:0xFF2A3244); c.drawCircle(x,ny,w*.048f*rs,ink);
            color(open?t.skyTop:0xFF3A4458); c.drawCircle(x,ny,w*.036f*rs,ink);
            if(open){ color(t.lit); c.drawRect(x-w*.016f*rs,ny-w*.010f*rs,x+w*.016f*rs,ny+w*.004f*rs,ink); color(t.neon); c.drawRect(x-w*.010f*rs,ny+w*.008f*rs,x+w*.010f*rs,ny+w*.016f*rs,ink); }
            else {
                color(0xFF7C8AA6); // padlock
                box.set(x-w*.014f*rs,ny-w*.004f*rs,x+w*.014f*rs,ny+w*.016f*rs); c.drawRoundRect(box,w*.006f*rs,w*.006f*rs,ink);
                ink.setStyle(Paint.Style.STROKE); ink.setStrokeWidth(w*.005f*rs); c.drawArc(x-w*.011f*rs,ny-w*.020f*rs,x+w*.011f*rs,ny+w*.002f*rs,180,180,false,ink); ink.setStyle(Paint.Style.FILL);
            }
            if(sel){ ink.setStyle(Paint.Style.STROKE); ink.setStrokeWidth(w*.006f*rs); ink.setShadowLayer(w*.012f*rs,0,0,cur.neon); color(0xFFFFFFFF); c.drawCircle(x,ny,w*.055f*rs,ink); ink.clearShadowLayer(); ink.setStyle(Paint.Style.FILL); }
            if((((trophyMask>>(i*3))&7)==7)){ // full set: gold star badge
                color(0xFFFFD700); ink.setShadowLayer(w*.008f*rs,0,0,0xFFFF9D00);
                float bx2=x+w*.040f*rs,by2=ny-w*.040f*rs,r1=w*.016f*rs,r2=w*.007f*rs;
                shape.reset(); for(int k=0;k<10;k++){double a=-Math.PI/2+k*Math.PI/5; float rr=k%2==0?r1:r2; float px=bx2+(float)Math.cos(a)*rr,py=by2+(float)Math.sin(a)*rr; if(k==0)shape.moveTo(px,py);else shape.lineTo(px,py);} shape.close(); c.drawPath(shape,ink);
                ink.clearShadowLayer();
            }
            // label
            ink.setTextAlign(Paint.Align.CENTER); ink.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
            ink.setTextSize(w*.024f*rs); color(open?(sel?0xFFFFFFFF:cur.soft):0xFF6C7890);
            c.drawText(t.tag,x,ny+w*.088f*rs,ink);
            if(!open){ ink.setTextSize(w*.020f*rs); color(0xFF8A96AC); c.drawText("Lv "+t.unlockLevel,x,ny+w*.118f*rs,ink); }
        }

        // status line
        ink.setTextAlign(Paint.Align.LEFT); ink.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
        if(justUnlocked!=null && justUnlocked.startsWith("TROPHY:")){
            color(0xFFFFD700); ink.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
            statusText(c,"TROPHY FOUND: "+justUnlocked.substring(7)+"!",w*.09f,top+height*.66f,w,rs);
        } else if(justUnlocked!=null){
            color(cur.title); ink.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
            statusText(c,"NEW STOP UNLOCKED: "+justUnlocked+"!",w*.09f,top+height*.66f,w,rs);
        } else {
            Theme next=null; for(Theme t:THEMES) if(level<t.unlockLevel){next=t;break;}
            color(cur.soft);
            if(next!=null) statusText(c,"Playing: "+cur.name+"  •  Next stop: "+next.name+" at Lv "+next.unlockLevel+"  ("+xp+" / "+needed+" XP)",w*.09f,top+height*.66f,w,rs);
            else statusText(c,"Playing: "+cur.name+"  •  Every stop unlocked. The city is yours.",w*.09f,top+height*.66f,w,rs);
        }

        // trophy shelf: 12 cups grouped by stop; collected cups take their stop's color
        ink.setTextSize(w*.020f*rs); color(cur.soft); ink.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        int got=Integer.bitCount(trophyMask);
        c.drawText("TROPHY SHELF  "+got+" / 12",w*.09f,top+height*.77f,ink);
        for(int i=0;i<12;i++){
            float x=w*(.11f+i*.0675f), y=top+height*.90f;
            boolean on=(trophyMask&(1<<i))!=0;
            trophyCup(c,x,y,w*.052f*rs,on,THEMES[i/3]);
        }
    }
    private void statusText(Canvas c, String s, float x, float y, float w, float rs) {
        ink.setTextSize(w*.025f*rs);
        float tw = ink.measureText(s);
        if (tw > w*.86f) ink.setTextSize(w*.025f*rs*w*.86f/tw); // shrink-to-fit: never clip or wrap into neighbours
        c.drawText(s, x, y, ink);
    }

    // ---------- trophy cup glyph + machine ornament ----------
    void trophyCup(Canvas c,float cx,float cy,float s,boolean on,Theme th){
        c.save(); c.translate(cx,cy); c.scale(s/100f,s/100f);
        if(on){ ink.setColor(th.title); ink.setShadowLayer(10,0,0,th.neon); } else { ink.setColor(0xFF39414F); }
        polygon(c,-30,-38,30,-38,18,8,-18,8);                    // bowl
        ink.setStyle(Paint.Style.STROKE); ink.setStrokeWidth(9);
        c.drawArc(-46,-34,-12,6,90,270,false,ink);               // left handle
        c.drawArc(12,-34,46,6,90,-270,false,ink);                // right handle
        ink.setStyle(Paint.Style.FILL);
        c.drawRect(-8,8,8,26,ink);                               // stem
        box.set(-26,26,26,38); c.drawRoundRect(box,6,6,ink);     // base
        if(on) ink.clearShadowLayer();
        c.restore();
    }

    /** Set-completion ornament above the machine frame for the active theme. */
    void frameOrnament(Canvas c,float cx,float cy,float s,Theme th){
        trophyCup(c,cx,cy,s,true,th);
        color(th.neon); ink.setTextAlign(Paint.Align.CENTER);
    }

    // ---------- symbols (unchanged art, kept from approved first increment) ----------
    private android.graphics.Bitmap[] bmp;
    private static final int[] SYM_FILE = {1,5,2,0,3,4};
    private final android.graphics.Paint bp = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG | android.graphics.Paint.FILTER_BITMAP_FLAG);
    private final android.graphics.RectF bd = new android.graphics.RectF();
    /** Original generated symbol art (res/drawable-nodpi). Falls back to the vector art when a bitmap is missing. */
    void loadBitmaps(android.content.Context ctx) {
        if (bmp != null) return;
        android.graphics.Bitmap[] b = new android.graphics.Bitmap[SYM_FILE.length];
        for (int i = 0; i < b.length; i++) {
            int rid = ctx.getResources().getIdentifier("nvs_" + SYM_FILE[i], "drawable", ctx.getPackageName());
            if (rid != 0) b[i] = android.graphics.BitmapFactory.decodeResource(ctx.getResources(), rid);
        }
        bmp = b;
        int bid = ctx.getResources().getIdentifier("nv_bg", "drawable", ctx.getPackageName());
        if (bid != 0) bgBmp = android.graphics.BitmapFactory.decodeResource(ctx.getResources(), bid);
    }
    private android.graphics.Bitmap bgBmp;
    private final android.graphics.Rect bgSrc = new android.graphics.Rect();
    private final android.graphics.RectF bgDst = new android.graphics.RectF();
    /** Illustrated scene behind the whole game (cover-fit), tinted toward the current land. Returns false when the art is missing. */
    boolean sceneBackdrop(Canvas c, float w, float h, int tint) {
        if (bgBmp == null) return false;
        float s = Math.max(w / bgBmp.getWidth(), h / bgBmp.getHeight());
        float sw = w / s, sh = h / s;
        bgSrc.set((int) ((bgBmp.getWidth() - sw) / 2), (int) ((bgBmp.getHeight() - sh) / 2), (int) ((bgBmp.getWidth() + sw) / 2), (int) ((bgBmp.getHeight() + sh) / 2));
        bgDst.set(0, 0, w, h);
        c.drawBitmap(bgBmp, bgSrc, bgDst, bp);
        android.graphics.Paint t = new android.graphics.Paint();
        t.setColor((tint & 0x00FFFFFF) | 0x33000000); c.drawRect(0, 0, w, h, t);
        return true;
    }
    void symbol(Canvas c, int id, float cx, float cy, float size) {
        if (bmp != null && id >= 0 && id < bmp.length && bmp[id] != null) {
            float h = size * 1.12f;
            bd.set(cx - h / 2, cy - h / 2, cx + h / 2, cy + h / 2);
            c.drawBitmap(bmp[id], null, bd, bp);
            return;
        }
        c.save(); c.translate(cx,cy); c.scale(size/100f,size/100f);
        ink.setStyle(Paint.Style.FILL); ink.setShader(null);
        ink.setShadowLayer(5,0,3,0x88000000);
        neonSymbol(c,id);
        ink.clearShadowLayer(); ink.setStyle(Paint.Style.FILL); ink.setShader(null); c.restore();
    }
    private void gold(){ink.setShader(new LinearGradient(-30,-40,25,40,0xFFFFF0AA,0xFFC87820,Shader.TileMode.CLAMP));}
    private void gem(Canvas c){
        color(0xFF55DFFF);polygon(c,-35,-17,-19,-36,20,-36,36,-17,0,39);
        color(0xFF1A79CF);polygon(c,-35,-17,0,39,-12,-17);
        color(0xFFB6FCFF);polygon(c,-19,-36,-12,-17,12,-17,20,-36);
        color(0xFFEBFFFF);polygon(c,-35,-17,-19,-36,-12,-17);
        line(c,-28,-15,27,-15,0xFFDAFFFF,2);
    }
    private void star(Canvas c){gold();shape.reset();for(int i=0;i<10;i++){
        double a=-Math.PI/2+i*Math.PI/5;float r=i%2==0?38:17;
        float x=(float)Math.cos(a)*r,y=(float)Math.sin(a)*r;if(i==0)shape.moveTo(x,y);else shape.lineTo(x,y);
        }shape.close();c.drawPath(shape,ink);ink.setShader(null);}
    private void neonSymbol(Canvas c,int id){
        switch(id){
        case 0: color(0xFFFF4D80);polygon(c,-31,-35,33,-35,33,-20,-4,38,-26,38,8,-17,-31,-17);line(c,-26,-29,25,-29,0xFFFFCEDB,4);break;
        case 1: gem(c);break;
        case 2: gold();oval(c,-27,-33,27,24);box.set(-34,12,34,28);c.drawRoundRect(box,8,8,ink);ink.setShader(null);color(0xFFFFE4A0);oval(c,-10,24,10,38);line(c,0,-40,0,-30,0xFFFFE4A0,7);break;
        case 3: line(c,-16,1,4,-30,0xFF84DA73,6);line(c,20,4,4,-30,0xFF84DA73,6);color(0xFFEE3666);oval(c,-37,-5,0,35);color(0xFFB71549);oval(c,1,0,39,39);color(0xFFFFB2C2);oval(c,-28,0,-19,10);oval(c,9,5,18,15);break;
        case 4: color(0xFFF8DD46);oval(c,-36,-25,36,27);color(0xFFFFFFA1);oval(c,-25,-17,25,17);color(0xFFF4D32E);oval(c,-20,-13,20,13);color(0xFF7DCE7C);polygon(c,12,-26,32,-37,29,-19);break;
        default: star(c);
        }
    }
}
