package nir.com.neonvegasslots;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;

/** Original illustrated journey and trophy shelves. The dialog provides vertical scrolling. */
final class MapView extends View {
    private final SlotsView g;
    private final SlotArt art=new SlotArt();
    private final Paint p=new Paint(3);
    private final float d;
    private float downX,downY;private boolean dragged;
    MapView(Context c,SlotsView g){super(c);this.g=g;d=c.getResources().getDisplayMetrics().density;setContentDescription("Journey map. Tap an unlocked destination to change theme. Trophy collection below.");}
    private float row(){return 154*d;}
    private float head(){return 42*d;}
    float stopX(int i){return getWidth()*(i%2==0?.32f:.68f);}
    float stopY(int i){return head()+row()*i+58*d;}
    @Override protected void onMeasure(int ws,int hs){setMeasuredDimension(MeasureSpec.getSize(ws),(int)(head()+SlotArt.THEMES.length*(row()+112*d)+60*d));}
    private void text(Canvas c,String s,float x,float y,float sp,int color){p.setColor(color);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextSize(sp*getResources().getDisplayMetrics().scaledDensity);c.drawText(s,x,y,p);}
    @Override protected void onDraw(Canvas c){
        float w=getWidth();p.setColor(0xFF101224);c.drawRoundRect(0,0,w,getHeight(),16*d,16*d,p);
        text(c,"Choose your unlocked destination",w/2,25*d,13,0xFFFFDE8A);
        Path road=new Path();road.moveTo(stopX(0),stopY(0));
        for(int i=1;i<SlotArt.THEMES.length;i++){float x=stopX(i-1),y=stopY(i-1),xx=stopX(i),yy=stopY(i);road.cubicTo(x,y+row()*.65f,xx,yy-row()*.65f,xx,yy);}
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(13*d);p.setColor(0xFF4A3A34);c.drawPath(road,p);p.setStrokeWidth(3*d);p.setColor(0xFFCDA966);c.drawPath(road,p);p.setStyle(Paint.Style.FILL);
        for(int i=0;i<SlotArt.THEMES.length;i++){
            SlotArt.Theme t=SlotArt.THEMES[i];boolean open=g.getLevel()>=t.unlockLevel,sel=g.getThemeIdx()==i;float x=stopX(i),y=stopY(i),ww=Math.min(w*.58f,174*d),hh=88*d;
            RectF r=new RectF(x-ww/2,y-hh/2,x+ww/2,y+hh/2);Path clip=new Path();clip.addRoundRect(r,14*d,14*d,Path.Direction.CW);
            c.save();c.clipPath(clip);c.translate(r.left,r.top);landmark(c,ww,hh,t,i);c.restore();
            if(!open){p.setColor(0xAA080B18);c.drawRoundRect(r,14*d,14*d,p);text(c,"LOCKED",x,y+4*d,13,0xFFBDC6D8);}
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth((sel?3:1)*d);p.setColor(sel?t.title:open?t.neon:0xFF596074);c.drawRoundRect(r,14*d,14*d,p);p.setStyle(Paint.Style.FILL);
            if(sel){p.setColor(t.title);c.drawCircle(r.right-8*d,r.top+8*d,10*d,p);text(c,"YOU",r.right-8*d,r.top+11*d,7,0xFF182335);}
            text(c,t.name,x,y+hh/2+20*d,14,open?t.title:0xFF9DA8BE);
            text(c,sel?"CURRENT STOP":open?"Tap to visit":"Unlocks at Lv "+t.unlockLevel,x,y+hh/2+38*d,12,open?t.soft:0xFF9DA8BE);
        }
        float top=head()+SlotArt.THEMES.length*row()+12*d;
        text(c,"TROPHY COLLECTION  "+Integer.bitCount(g.getTrophyMask())+" / "+SlotsView.TROPHY_COUNT,w/2,top,14,0xFFFFDF82);
        for(int i=0;i<SlotArt.THEMES.length;i++){
            float yy=top+30*d+i*112*d;
            text(c,SlotArt.THEMES[i].name,w/2,yy,13,SlotArt.THEMES[i].soft);
            for(int j=0;j<3;j++){float xx=w*(j+.5f)/3;boolean got=(g.getTrophyMask()&(1<<(i*3+j)))!=0;art.trophyCup(c,xx,yy+35*d,44*d,got,SlotArt.THEMES[i]);
                String label=got?SlotArt.TROPHY_NAMES[i][j]:"?";String[] words=label.split(" ",2);
                text(c,words[0],xx,yy+68*d,12,got?SlotArt.THEMES[i].title:0xFF939BAF);
                if(words.length>1)text(c,words[1],xx,yy+84*d,12,SlotArt.THEMES[i].soft);}
        }
    }
    private void landmark(Canvas c,float w,float h,SlotArt.Theme t,int index){
        p.setShader(new LinearGradient(0,0,0,h,t.skyTop,t.skyBottom,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
        p.setColor(t.lit);c.drawCircle(w*.8f,h*.22f,h*.1f,p);
        for(int k=0;k<8;k++){float x=w*k/8f,bh=h*(.3f+.08f*((k+index)%4));p.setColor(k%2==0?t.back:t.front);c.drawRect(x,h-bh,x+w*.1f,h,p);for(int q=0;q<3;q++){p.setColor((k+q)%2==0?t.neon:t.lit);c.drawRect(x+w*.025f,h-bh+h*.07f+q*h*.09f,x+w*.04f,h-bh+h*.1f+q*h*.09f,p);}}
        p.setColor(t.neon);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2*d);c.drawRoundRect(w*.3f,h*.3f,w*.68f,h*.56f,4*d,4*d,p);p.setStyle(Paint.Style.FILL);
        text(c,new String[]{"VEGAS","SUNSET","LAGOON","ROYAL"}[index],w*.49f,h*.46f,11,t.title);
    }
    @Override public boolean onTouchEvent(MotionEvent e){if(e.getActionMasked()==MotionEvent.ACTION_DOWN){downX=e.getX();downY=e.getY();dragged=false;}
        if(e.getActionMasked()==MotionEvent.ACTION_MOVE&&Math.hypot(e.getX()-downX,e.getY()-downY)>android.view.ViewConfiguration.get(getContext()).getScaledTouchSlop())dragged=true;
        if(e.getActionMasked()==MotionEvent.ACTION_UP&&!dragged){for(int i=0;i<SlotArt.THEMES.length;i++)if(Math.abs(e.getX()-stopX(i))<getWidth()*.3f&&Math.abs(e.getY()-stopY(i))<48*d){g.selectTheme(i);invalidate();g.invalidate();performClick();break;}}return true;}
    @Override public boolean performClick(){super.performClick();return true;}
}
