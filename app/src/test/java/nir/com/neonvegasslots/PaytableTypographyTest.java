package nir.com.neonvegasslots;
import android.graphics.*;import android.view.*;import java.io.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;
@RunWith(RobolectricTestRunner.class)@Config(sdk=34,qualifiers="w320dp-h570dp-hdpi")@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class PaytableTypographyTest{
 @Test public void valuesStayReadableAtNarrowLargeFont()throws Exception{android.content.Context c=RuntimeEnvironment.getApplication();android.util.DisplayMetrics dm=c.getResources().getDisplayMetrics();float old=dm.scaledDensity;dm.scaledDensity=dm.density*1.35f;
 try{PaytableView v=new PaytableView(c,0);v.measure(View.MeasureSpec.makeMeasureSpec(420,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));v.layout(0,0,420,v.getMeasuredHeight());Assert.assertTrue(v.getMeasuredHeight()>500);Bitmap b=Bitmap.createBitmap(420,v.getMeasuredHeight(),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(b);canvas.drawColor(0xff160d25);v.draw(canvas);try(FileOutputStream o=new FileOutputStream("/tmp/renders/neonvegasslots-paytable-font135.png")){b.compress(Bitmap.CompressFormat.PNG,100,o);}}finally{dm.scaledDensity=old;}}
}
