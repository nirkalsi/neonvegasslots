package nir.com.neonvegasslots;
import android.content.Context;import android.graphics.*;import android.view.View;
/** Original cosmetic reveal. Ownership is granted only by the existing game drop. */
final class TrophyReveal extends View {
 private final SlotArt art=new SlotArt();private final Paint p=new Paint(3);private final int index;private long start=System.currentTimeMillis();private float testProgress=-1;
 TrophyReveal(Context c,int index){super(c);this.index=index;setContentDescription(SlotArt.TROPHY_NAMES[index/3][index%3]);}
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();start=System.currentTimeMillis();invalidate();}
 @Override protected void onMeasure(int ws,int hs){setMeasuredDimension(MeasureSpec.getSize(ws),(int)(170*getResources().getDisplayMetrics().density));}
 void setProgress(float t){testProgress=t;invalidate();}
 @Override protected void onDraw(Canvas c){float w=getWidth(),h=getHeight(),cx=w/2,cy=h*.52f;float t=testProgress>=0?testProgress:Math.min(1,(System.currentTimeMillis()-start)/800f);SlotArt.Theme th=SlotArt.THEMES[index/3];
  p.setShader(new RadialGradient(cx,cy,h*.52f,new int[]{GameDialog.withAlpha(th.neon,0x88),0x00000000},null,Shader.TileMode.CLAMP));c.drawCircle(cx,cy,h*.52f,p);p.setShader(null);
  c.save();c.rotate(t*32,cx,cy);p.setColor(GameDialog.withAlpha(th.title,0x44));for(int k=0;k<12;k++){Path ray=new Path();ray.moveTo(cx,cy);double a=k*Math.PI/6;ray.lineTo(cx+(float)Math.cos(a)*h*.52f,cy+(float)Math.sin(a)*h*.52f);ray.lineTo(cx+(float)Math.cos(a+.11)*h*.52f,cy+(float)Math.sin(a+.11)*h*.52f);ray.close();c.drawPath(ray,p);}c.restore();
  float scale=t<.65f?.35f+1.1f*(t/.65f):1.45f-.45f*((t-.65f)/.35f);art.trophyCup(c,cx,cy,h*.53f*scale,t>.18f,th);
  if(testProgress<0&&t<1)postInvalidateDelayed(16);
 }
}
