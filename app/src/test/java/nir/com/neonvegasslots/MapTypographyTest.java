package nir.com.neonvegasslots;
import android.graphics.*;import android.view.*;import java.io.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;
@RunWith(RobolectricTestRunner.class)@Config(sdk=34,qualifiers="w320dp-h570dp-hdpi")@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class MapTypographyTest{
 @Test public void readablePositionPillAtLargeFontNarrowWidth()throws Exception{android.content.Context c=RuntimeEnvironment.getApplication();android.util.DisplayMetrics dm=c.getResources().getDisplayMetrics();float old=dm.scaledDensity;dm.scaledDensity=dm.density*1.35f;
 try{SlotsView g=new SlotsView(c);MapView v=new MapView(c,g);v.measure(View.MeasureSpec.makeMeasureSpec(420,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.AT_MOST));v.layout(0,0,420,v.getMeasuredHeight());Bitmap b=Bitmap.createBitmap(420,700,Bitmap.Config.ARGB_8888);v.draw(new Canvas(b));try(FileOutputStream o=new FileOutputStream("/tmp/renders/neonvegasslots-map-narrow-font135.png")){b.compress(Bitmap.CompressFormat.PNG,100,o);}}finally{dm.scaledDensity=old;}}
}
