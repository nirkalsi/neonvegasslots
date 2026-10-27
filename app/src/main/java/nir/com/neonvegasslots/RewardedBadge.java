package nir.com.neonvegasslots;
import android.graphics.*;import android.graphics.drawable.Drawable;
/** Original video+2X badge; decorative only. Reward still comes exclusively from earned callback. */
final class RewardedBadge extends Drawable{
 private final Paint p=new Paint(3);private final RectF r=new RectF();
 public void draw(Canvas c){c.save();c.translate(getBounds().left,getBounds().top);c.scale(getBounds().width()/56f,getBounds().height()/40f);p.setColor(0xFF14233D);r.set(0,7,36,33);c.drawRoundRect(r,6,6,p);p.setColor(0xFFFFFFFF);Path play=new Path();play.moveTo(13,13);play.lineTo(13,27);play.lineTo(25,20);play.close();c.drawPath(play,p);p.setColor(0xFFFFD166);c.drawCircle(42,12,13,p);p.setColor(0xFF352012);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(12);c.drawText("2X",42,16,p);c.restore();}
 public void setAlpha(int a){p.setAlpha(a);}public void setColorFilter(ColorFilter f){p.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
