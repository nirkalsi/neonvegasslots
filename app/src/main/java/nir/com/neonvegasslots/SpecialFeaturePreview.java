package nir.com.neonvegasslots;
import android.content.Context;import android.graphics.*;import android.view.*;import java.util.Random;
/** Isolated disabled-feature integration preview. No Activity route or live-save access. */
final class SpecialFeaturePreview extends View {
 private final Bitmap[] special=new Bitmap[2];private final SpecialFeature engine;private final SlotArt art=new SlotArt();private final Paint p=new Paint(3);private SpecialFeature.State state;private long start;private boolean quick,settled,anticipated;private int anticipationFrom=5;private int stoppedFloor=0;private final Random random=new Random();
 SpecialFeaturePreview(Context c){super(c);engine=new SpecialFeature(c);art.loadBitmaps(c);for(int i=0;i<2;i++){int id=c.getResources().getIdentifier("special_"+(i+6),"drawable",c.getPackageName());if(id!=0)special[i]=BitmapFactory.decodeResource(c.getResources(),id);}state=engine.snapshot();if(state.pending){start=android.os.SystemClock.uptimeMillis();settled=false;}else settled=true;}
 void spin(long stake){if(!settled)return;state=engine.begin(stake,random);start=android.os.SystemClock.uptimeMillis();quick=false;anticipated=false;anticipationFrom=5;stoppedFloor=0;settled=false;invalidate();}
 SpecialFeature.State snapshot(){return engine.snapshot();}
 void finishForTest(){if(!settled){state=engine.settle(state.sequence);settled=true;}}
 @Override protected void onDraw(Canvas c){long age=android.os.SystemClock.uptimeMillis()-start;float w=getWidth(),cell=w/5f,top=w*.22f;SlotArt.Theme theme=SlotArt.THEMES[0];c.drawColor(theme.skyTop);int stopped=0;for(int col=0;col<5;col++)if(age>=(quick?150+col*70:600+col*150+(col>=anticipationFrom?500:0)))stopped++;
  stopped=Math.max(stopped,stoppedFloor);stoppedFloor=stopped;boolean anticipation=SpecialFeature.anticipation(state.grid,stopped,quick);if(anticipation&&!anticipated){anticipated=true;anticipationFrom=stopped;}for(int col=0;col<5;col++){float x=(col+.5f)*cell;for(int row=0;row<3;row++){float y=top+(row+.5f)*cell;int id=state.grid[row][col];if(!settled&&col>=stopped){y+=(age%160)/160f*cell;if(y>top+3*cell)y-=3*cell;}
   if(id>=6){if(special[id-6]!=null){float size=cell*.92f*(1+.025f*(float)Math.sin(age/180f));c.drawBitmap(special[id-6],null,new RectF(x-size/2,y-size/2,x+size/2,y+size/2),p);}else SpecialFeature.symbol(c,id,x,y,cell*.82f,age,settled,false);}else art.symbolAnimated(c,id,x,y,cell*.82f,age,settled&&state.award>state.stake);}
   if(anticipation&&col>=stopped){p.setColor(theme.title);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);c.drawRect(col*cell,top,(col+1)*cell,top+3*cell,p);p.setStyle(Paint.Style.FILL);}}
  p.setColor(theme.title);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(w*.04f);c.drawText("DISABLED FEATURE PREVIEW",w/2,w*.07f,p);c.drawText("Coins "+state.balance+"  Stake "+state.stake+"  Free "+state.remaining,w/2,w*.15f,p);c.drawText(settled?"Tap for next spin":"Tap to quick stop",w/2,top+3.5f*cell,p);
  if(!settled&&stopped==5){state=engine.settle(state.sequence);settled=true;invalidate();}else if(!settled)postInvalidateDelayed(16);
 }
 @Override public boolean onTouchEvent(MotionEvent e){if(e.getActionMasked()==MotionEvent.ACTION_UP){if(settled)spin(state.stake);else{quick=true;anticipationFrom=5;start=android.os.SystemClock.uptimeMillis();invalidate();}performClick();}return true;}
 @Override public boolean performClick(){super.performClick();return true;}
}
