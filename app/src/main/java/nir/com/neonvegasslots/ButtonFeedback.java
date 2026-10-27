package nir.com.neonvegasslots;
import android.content.*;import android.view.*;import android.view.animation.OvershootInterpolator;
/** Presentation-only touch feedback. Never consumes input or replaces click actions. */
final class ButtonFeedback {
 private ButtonFeedback(){}
 static void install(View v){
  v.setOnTouchListener((view,e)->{
   if(!view.isEnabled())return false;
   int a=e.getActionMasked();
   if(a==android.view.MotionEvent.ACTION_DOWN){view.animate().cancel();view.setScaleX(.95f);view.setScaleY(.95f);view.setAlpha(.76f);}
   else if(a==android.view.MotionEvent.ACTION_UP||a==android.view.MotionEvent.ACTION_CANCEL){
    boolean click=a==android.view.MotionEvent.ACTION_UP&&view.isPressed()&&e.getX()>=0&&e.getY()>=0&&e.getX()<view.getWidth()&&e.getY()<view.getHeight();
    view.setAlpha(1);view.animate().scaleX(1).scaleY(1).setDuration(210).setInterpolator(new OvershootInterpolator(1.5f)).start();
    if(click){view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);Context c=view.getContext();while(c instanceof ContextWrapper&&!(c instanceof MainActivity)){Context next=((ContextWrapper)c).getBaseContext();if(next==c)break;c=next;}if(c instanceof MainActivity){String text=view instanceof android.widget.TextView?((android.widget.TextView)view).getText().toString().toLowerCase(java.util.Locale.US):"";((MainActivity)c).playUi(text.contains("claim")||text.contains("collect"));}}
   }return false;
  });
 }
}
