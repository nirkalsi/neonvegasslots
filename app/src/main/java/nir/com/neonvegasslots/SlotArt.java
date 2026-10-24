package nir.com.neonvegasslots;

import android.graphics.*;

/** Original vector artwork. Visual-only: symbol indices and payout math stay unchanged. */
final class SlotArt {
    private static final boolean DRAGON = false;
    private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();
    private final Path shape = new Path();

    void backdrop(Canvas c, float w, float h) {
        ink.setShader(new LinearGradient(0, 0, w, h,
            DRAGON ? 0xFF300C18 : 0xFF131238, DRAGON ? 0xFF080C17 : 0xFF050D20, Shader.TileMode.CLAMP));
        c.drawRect(0,0,w,h,ink); ink.setShader(null);
        // Layered mountain/city silhouettes, drawn behind the machine rather than a borrowed image.
        ink.setColor(DRAGON ? 0xFF551C29 : 0xFF19224E);
        for (int i=0;i<9;i++) {
            float x=w*i/8f, top=h*(0.10f+(i%3)*0.025f);
            if (DRAGON) {
                shape.reset(); shape.moveTo(x-w*.10f,h*.24f); shape.lineTo(x,top);
                shape.lineTo(x+w*.11f,h*.24f); shape.close(); c.drawPath(shape,ink);
            } else c.drawRect(x,top,x+w*.055f,h*.24f,ink);
        }
        ink.setColor(DRAGON ? 0xFFB67844 : 0xFF6596DA);
        for(int i=0;i<28;i++) c.drawCircle(w*((i*37%101)/101f), h*(.025f+(i*13%89)/600f), w*.0018f,ink);
    }

    void journey(Canvas c, float w, float top, float bottom, int level, long xp, long needed) {
        if(bottom-top < w*.30f) return;
        float height=Math.min(bottom-top,w*.53f), y=top+height*.5f;
        box.set(w*.06f,top,w*.94f,top+height);
        ink.setShader(new LinearGradient(0,top,w,top+height,DRAGON?0xFF3C2029:0xFF172442,0xFF101628,Shader.TileMode.CLAMP));
        c.drawRoundRect(box,w*.025f,w*.025f,ink);ink.setShader(null);
        ink.setTextAlign(Paint.Align.LEFT);ink.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        ink.setTextSize(w*.030f);color(0xFFFFE5A4);
        c.drawText(DRAGON?"THE DRAGON TRAIL":"NEON CITY JOURNEY",w*.10f,top+w*.065f,ink);
        ink.setTextSize(w*.025f);ink.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));color(0xFFAFC0D8);
        c.drawText("Earn XP with each spin. Grow your level.",w*.10f,top+w*.11f,ink);
        int first=Math.max(1,level-1);
        for(int i=0;i<4;i++) {
            float x=w*(.18f+i*.215f), yy=y+(i%2==0?w*.035f:-w*.025f);
            if(i<3)line(c,x,yy,x+w*.215f,y+((i+1)%2==0?w*.035f:-w*.025f),0xFF48536B,w*.008f);
        }
        for(int i=0;i<4;i++) {
            float x=w*(.18f+i*.215f), yy=y+(i%2==0?w*.035f:-w*.025f);
            color(first+i<=level?0xFFDFA957:0xFF39465E);c.drawCircle(x,yy,w*.045f,ink);
            color(first+i<=level?0xFFFFE4A7:0xFF7C8AA6);c.drawCircle(x,yy,w*.033f,ink);
            color(0xFF172039);ink.setTextAlign(Paint.Align.CENTER);ink.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));ink.setTextSize(w*.027f);
            c.drawText(""+(first+i),x,yy+w*.010f,ink);
        }
        ink.setTextAlign(Paint.Align.LEFT);ink.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));ink.setTextSize(w*.027f);color(0xFFDAE6F4);
        c.drawText("Level "+level+"  •  "+xp+" / "+needed+" XP",w*.10f,top+height-w*.055f,ink);
    }

    void symbol(Canvas c, int id, float cx, float cy, float size) {
        c.save(); c.translate(cx,cy); c.scale(size/100f,size/100f);
        ink.setStyle(Paint.Style.FILL); ink.setShader(null);
        ink.setShadowLayer(5,0,3,0x88000000);
        if (DRAGON) dragonSymbol(c,id); else neonSymbol(c,id);
        ink.clearShadowLayer(); ink.setStyle(Paint.Style.FILL); ink.setShader(null); c.restore();
    }
    private void color(int v){ink.setColor(v);}
    private void oval(Canvas c,float l,float t,float r,float b){box.set(l,t,r,b);c.drawOval(box,ink);}
    private void line(Canvas c,float a,float b,float x,float y,int color,float width){
        ink.clearShadowLayer();ink.setColor(color);ink.setStrokeWidth(width);ink.setStrokeCap(Paint.Cap.ROUND);c.drawLine(a,b,x,y,ink);
    }
    private void polygon(Canvas c,float...xy){shape.reset();shape.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)shape.lineTo(xy[i],xy[i+1]);shape.close();c.drawPath(shape,ink);}
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
    private void dragonSymbol(Canvas c,int id){
        switch(id){
        case 0:
            color(0xFF56D2B0);polygon(c,-33,25,-23,-21,-9,-35,18,-26,34,-6,16,5,8,30,-9,39);
            color(0xFF99F0D4);polygon(c,-23,-21,-24,-40,-8,-31,5,-46,13,-27);
            color(0xFF123E40);oval(c,5,-19,13,-10);color(0xFFFFD58C);polygon(c,20,-1,32,-4,23,8);
            line(c,-14,5,-7,20,0xFF219D90,5); break;
        case 1:gold();oval(c,-36,-18,36,26);box.set(-25,-30,25,7);c.drawRoundRect(box,12,12,ink);ink.setShader(null);color(0xFF9C551E);oval(c,-23,-23,23,-6);color(0xFFFFE9A0);oval(c,-17,-19,17,-10);break;
        case 2:line(c,0,-43,0,-29,0xFFFFD17D,4);color(0xFFE44A42);oval(c,-28,-31,28,28);line(c,-12,-27,-12,25,0xFFFF9771,3);line(c,12,-27,12,25,0xFFB62838,3);line(c,-19,-31,19,-31,0xFFFFD17D,6);line(c,-18,28,18,28,0xFFFFD17D,6);line(c,0,29,0,43,0xFFFFD17D,4);break;
        case 3: gold();polygon(c,-36,-24,-17,-4,0,-34,17,-4,36,-24,29,27,-29,27);ink.setShader(null);color(0xFFE84B52);oval(c,-7,5,7,19);break;
        case 4:color(0xFF9F75F4);oval(c,-31,-36,31,26);color(0xFFD5B8FF);oval(c,-20,-27,-4,-8);gold();box.set(-34,26,34,37);c.drawRoundRect(box,7,7,ink);ink.setShader(null);break;
        default:color(0xFFFFE6BE);box.set(-28,-39,28,39);c.drawRoundRect(box,8,8,ink);color(0xFFB82A3B);ink.setTextSize(43);ink.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));ink.setTextAlign(Paint.Align.CENTER);c.drawText("東",0,15,ink);
        }
    }
}
