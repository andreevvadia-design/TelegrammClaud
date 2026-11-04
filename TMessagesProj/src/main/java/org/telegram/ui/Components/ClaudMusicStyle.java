package org.telegram.ui.Components;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.music.SoundCloudArtwork;

/** Shared native music components: type scale, surfaces, covers and touch feedback. */
public final class ClaudMusicStyle {
 public static final int BG=0xFF101010,SURFACE=0xFF1C1B1A,TEXT=0xFFF5F1ED,MUTED=0xFF99938D,ACCENT=0xFFF3B995,LINE=0xFF302C29;
 public static int dp(float n){return AndroidUtilities.dp(n);}
 public static TextView text(Context c,String value,int size,boolean bold,int color){TextView v=new TextView(c);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setIncludeFontPadding(false);v.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",Typeface.NORMAL));return v;}
 public static GradientDrawable shape(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
 public static android.graphics.drawable.Drawable touch(int color,int radius){return new RippleDrawable(ColorStateList.valueOf(0x25FFFFFF),shape(color,radius),null);}
 public static void round(View v,int radius){v.setClipToOutline(true);v.setOutlineProvider(new ViewOutlineProvider(){public void getOutline(View view,Outline o){o.setRoundRect(0,0,view.getWidth(),view.getHeight(),dp(radius));}});}
 public static ImageView icon(Context c,String type,String label,int color,Runnable action){ImageView v=new ImageView(c);v.setImageDrawable(new ClaudMusicIcon(type,color));v.setPadding(dp(12),dp(12),dp(12),dp(12));v.setContentDescription(label);v.setBackground(touch(android.graphics.Color.TRANSPARENT,24));if(action!=null)v.setOnClickListener(w->{w.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);action.run();});return v;}
 public static TextView cover(Context c,int radius,String url){TextView v=text(c,"",20,false,ACCENT);v.setGravity(Gravity.CENTER);v.setBackground(new Cover());round(v,radius);SoundCloudArtwork.bind(v,large(url));return v;}
 public static String large(String url){return url==null?null:url.replace("-large.","-t500x500.");}
 public static final class Cover extends Drawable {
  private final Paint p=new Paint(3);
  public void draw(Canvas c){Rect b=getBounds();p.setColor(0xFF262321);c.drawRect(b,p);float cx=b.exactCenterX(),cy=b.exactCenterY(),s=Math.min(b.width(),b.height())*.09f;p.setColor(0xFF756155);for(int i=0;i<4;i++){float h=s*(i==1?3.3f:i==2?2.6f:1.6f);c.drawRoundRect(cx+(i-2)*s*1.1f,cy-h/2,cx+(i-2)*s*1.1f+s*.45f,cy+h/2,s*.2f,s*.2f,p);}}
  public void setAlpha(int a){}public void setColorFilter(ColorFilter f){}public int getOpacity(){return PixelFormat.OPAQUE;}
 }
}
