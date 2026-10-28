package nir.com.neonvegasslots;
import android.content.Context;import android.view.View;import android.graphics.*;
/** Table uses the game's real payout arrays and original symbol assets, never a duplicate reward table. */
final class PaytableView extends View {
 private final SlotArt art=new SlotArt(); private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final SlotArt.Theme theme;
 PaytableView(Context c,int t){super(c);art.loadBitmaps(c);theme=SlotArt.THEMES[t];}
 private float fs(){return getResources().getDisplayMetrics().scaledDensity;}
 private float rowHeight(float w){return Math.max(w*.13f,32*fs());}
 private float headerHeight(float w){return Math.max(w*.10f,26*fs());}
 @Override protected void onMeasure(int w,int h){int width=MeasureSpec.getSize(w);setMeasuredDimension(width,(int)(headerHeight(width)+6*rowHeight(width)+82*fs()));}
 @Override protected void onDraw(Canvas c){float w=getWidth(),rh=rowHeight(w),head=headerHeight(w);
  p.setTypeface(GameFonts.display(getContext()));p.setTextAlign(Paint.Align.CENTER);p.setTextSize(Math.max(w*.043f,12*fs()));p.setColor(theme.title);
  c.drawText("SYMBOL",w*.16f,head*.65f,p);c.drawText("3",w*.46f,head*.65f,p);c.drawText("4",w*.66f,head*.65f,p);c.drawText("5",w*.86f,head*.65f,p);
  for(int s=0;s<6;s++){float y=head+s*rh;art.symbol(c,s,w*.16f,y+rh*.35f,rh*.65f);p.setColor(theme.soft);p.setTextSize(Math.max(w*.042f,18*fs()));
   for(int n=3;n<=5;n++)c.drawText(String.format(java.util.Locale.US,"%,d",SlotsView.symbolPayout(s,n)),w*(.46f+(n-3)*.20f),y+rh*.45f,p);
  }
  p.setTextSize(Math.max(w*.035f,12*fs()));p.setColor(theme.soft);c.drawText("At BET 10 - values per winning line",w*.50f,head+6*rh+18*fs(),p);
  int[][] lines={{1,1,1,1,1},{0,0,0,0,0},{2,2,2,2,2},{0,1,2,1,0},{2,1,0,1,2}};
  for(int l=0;l<5;l++){float left=w*(.03f+l*.195f),top=head+6*rh+32*fs(),cw=w*.033f,ch=w*.045f;
   p.setColor(theme.panelLine);p.setStrokeWidth(w*.002f);for(int row=0;row<3;row++)for(int col=0;col<5;col++)c.drawCircle(left+col*cw,top+row*ch,w*.005f,p);
   p.setColor(theme.title);p.setStrokeWidth(w*.005f);for(int col=1;col<5;col++)c.drawLine(left+(col-1)*cw,top+lines[l][col-1]*ch,left+col*cw,top+lines[l][col]*ch,p);
  }

 }
}
