package nir.com.neonvegasslots;
import android.content.Context;import android.graphics.*;import android.view.*;import android.widget.*;
/** Bundled OFL fonts. Display is a rounded heavy face; body stays clean and readable. */
final class GameFonts {
 private static Typeface display,body;
 static Typeface display(Context c){if(display==null)display=load(c,"fredoka.ttf","'wght' 650, 'wdth' 100");return display;}
 static Typeface body(Context c){if(body==null)body=load(c,"nunito.ttf","'wght' 500");return body;}
 private static Typeface load(Context c,String name,String axes){if(android.os.Build.VERSION.SDK_INT>=26)return new Typeface.Builder(c.getAssets(),"fonts/"+name).setFontVariationSettings(axes).build();return Typeface.createFromAsset(c.getAssets(),"fonts/"+name.replace(".ttf","-static.ttf"));}
 static final class Heading extends TextView{Heading(Context c){super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}protected void onDraw(Canvas c){Paint p=getPaint();int col=getCurrentTextColor();p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(getTextSize()*.055f);p.setShader(new LinearGradient(0,0,0,1,0xFF1A0E25,0xFF1A0E25,Shader.TileMode.CLAMP));setShadowLayer(2,0,2,0xAA000000);super.onDraw(c);p.setStyle(Paint.Style.FILL);setShadowLayer(0,0,0,0);p.setShader(new LinearGradient(0,0,0,getHeight(),0xFFFFF8D9,col,Shader.TileMode.CLAMP));super.onDraw(c);p.setShader(null);}}
 static void apply(View v){if(v instanceof TextView){TextView t=(TextView)v;boolean heavy=t instanceof Button || (t.getTypeface()!=null&&t.getTypeface().isBold());t.setTypeface(heavy?display(v.getContext()):body(v.getContext()));}if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)apply(g.getChildAt(i));}}
 static void hero(Canvas c,String text,float x,float y,Paint p,int light,int dark){int color=p.getColor();Typeface old=p.getTypeface();p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(2,p.getTextSize()*.07f));p.setColor(0xFF392619);p.setShadowLayer(p.getTextSize()*.09f,0,p.getTextSize()*.055f,0x99000000);c.drawText(text,x,y,p);p.clearShadowLayer();p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(0,y-p.getTextSize(),0,y,light,dark,Shader.TileMode.CLAMP));c.drawText(text,x,y,p);p.setShader(null);p.setColor(color);p.setTypeface(old);}
}
