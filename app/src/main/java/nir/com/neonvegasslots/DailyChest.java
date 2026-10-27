package nir.com.neonvegasslots;

import android.app.Activity;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.*;

/** Original treasure chest: idle bob in the track, shake/open on claim; no reward logic. */
final class DailyChest extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r=new RectF();
    private final long start=System.currentTimeMillis();
    private float progress=-1;
    DailyChest(Context c){super(c);setContentDescription("Daily treasure chest");}
    void setProgress(float t){progress=t;invalidate();}
    static View hero(Context c){return new DailyChest(c);}
    static void celebrate(Activity a,View game,long coins,float tx,float ty){
        if(a.isFinishing()||a.isDestroyed())return;
        ViewGroup root=(ViewGroup)a.findViewById(android.R.id.content);
        DailyChest chest=new DailyChest(a);chest.setProgress(0);root.addView(chest,new ViewGroup.LayoutParams(-1,-1));
        ValueAnimator animator=ValueAnimator.ofFloat(0,1);animator.setDuration(850);animator.addUpdateListener(v->chest.setProgress((float)v.getAnimatedValue()));
        animator.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator x){root.removeView(chest);if(!a.isDestroyed())ClaimBurst.play(game,coins,tx,ty);}});animator.start();
    }
    @Override protected void onDraw(Canvas c){
        float w=getWidth(),h=getHeight();boolean claiming=progress>=0;
        float size=claiming?Math.min(w*.45f,h*.22f):Math.min(w*.4f,h*.85f),cx=w/2,cy=claiming?h*.47f:h*.6f;
        if(claiming){p.setColor(0x88000000);c.drawRect(0,0,w,h,p);}
        else cy+=(float)Math.sin((System.currentTimeMillis()-start)/300.0)*h*.025f;
        float open=claiming?Math.max(0,Math.min(1,(progress-.35f)/.45f)):0;
        float shake=claiming&&progress<.35f?(float)Math.sin(progress*90)*size*.025f:0;
        cx+=shake;
        p.setShader(new RadialGradient(cx,cy,size*.9f,new int[]{0x99FFE182,0x00FFE182},null,Shader.TileMode.CLAMP));c.drawCircle(cx,cy,size*.9f,p);p.setShader(null);
        for(int k=0;k<7;k++){double angle=-Math.PI+k*Math.PI/6;float reach=size*(.3f+.5f*open),x=cx+(float)Math.cos(angle)*reach,y=cy+(float)Math.sin(angle)*reach;
            p.setColor(0xFFFFD24C);c.drawCircle(x,y,size*.065f,p);p.setColor(0xFFB77917);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(size*.012f);c.drawCircle(x,y,size*.04f,p);p.setStyle(Paint.Style.FILL);}
        r.set(cx-size*.46f,cy-size*.12f,cx+size*.46f,cy+size*.33f);
        p.setShader(new LinearGradient(0,r.top,0,r.bottom,0xFFC16E27,0xFF6E2813,Shader.TileMode.CLAMP));c.drawRoundRect(r,size*.055f,size*.055f,p);p.setShader(null);
        p.setColor(0xFFFFD266);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(size*.035f);c.drawRoundRect(r,size*.055f,size*.055f,p);p.setStyle(Paint.Style.FILL);
        for(int k=-1;k<=1;k+=2){p.setColor(0xFFE6AA3D);c.drawRect(cx+k*size*.29f-size*.025f,cy-size*.1f,cx+k*size*.29f+size*.025f,cy+size*.3f,p);}
        c.save();c.rotate(-open*24,cx-size*.45f,cy-size*.1f);r.set(cx-size*.49f,cy-size*.3f-open*size*.12f,cx+size*.49f,cy-size*.04f-open*size*.12f);p.setShader(new LinearGradient(0,r.top,0,r.bottom,0xFFFFD366,0xFFB96D20,Shader.TileMode.CLAMP));c.drawRoundRect(r,size*.07f,size*.07f,p);p.setShader(null);p.setColor(0xFFFFEE9C);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(size*.025f);c.drawRoundRect(r,size*.07f,size*.07f,p);p.setStyle(Paint.Style.FILL);c.restore();
        p.setColor(0xFFFFE17A);r.set(cx-size*.07f,cy-size*.08f,cx+size*.07f,cy+size*.12f);c.drawRoundRect(r,size*.02f,size*.02f,p);p.setColor(0xFF442715);c.drawCircle(cx,cy+size*.01f,size*.02f,p);
        if(!claiming)postInvalidateDelayed(50);
    }
}
