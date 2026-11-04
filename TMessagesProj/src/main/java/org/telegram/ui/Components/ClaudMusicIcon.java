package org.telegram.ui.Components;
import android.graphics.*;
import android.graphics.drawable.Drawable;
public final class ClaudMusicIcon extends Drawable {
 private final Paint p=new Paint(3);private final String type;private int color;
 public ClaudMusicIcon(String type,int color){this.type=type;this.color=color;}
 public void draw(Canvas c){c.save();c.translate(getBounds().left,getBounds().top);c.scale(getBounds().width()/24f,getBounds().height()/24f);p.setColor(color);p.setStrokeWidth(1.7f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStyle(Paint.Style.STROKE);Path path=new Path();
 if(type.equals("home")){path.moveTo(3,10);path.lineTo(12,3);path.lineTo(21,10);path.lineTo(21,21);path.lineTo(15,21);path.lineTo(15,14);path.lineTo(9,14);path.lineTo(9,21);path.lineTo(3,21);path.close();c.drawPath(path,p);}
 else if(type.equals("search")){c.drawCircle(10.5f,10.5f,7,p);c.drawLine(16,16,22,22,p);}
 else if(type.equals("library")){c.drawLine(4,4,4,21,p);c.drawLine(10,4,10,21,p);c.drawLine(16,4,21,20,p);}
 else if(type.equals("pause")){p.setStyle(Paint.Style.FILL);c.drawRoundRect(6,4,10,20,1,1,p);c.drawRoundRect(14,4,18,20,1,1,p);}
 else if(type.equals("next")||type.equals("previous")||type.equals("play")){if(type.equals("previous")){c.translate(24,0);c.scale(-1,1);}p.setStyle(Paint.Style.FILL);path.moveTo(6,4);path.lineTo(18,12);path.lineTo(6,20);path.close();c.drawPath(path,p);if(!type.equals("play"))c.drawRect(19,4,21,20,p);}
 else if(type.equals("plus")){c.drawLine(12,5,12,19,p);c.drawLine(5,12,19,12,p);}
 else if(type.equals("chevron")){path.moveTo(9,5);path.lineTo(16,12);path.lineTo(9,19);c.drawPath(path,p);}
 else if(type.equals("heart")||type.equals("heartFilled")){if(type.equals("heartFilled"))p.setStyle(Paint.Style.FILL);path.moveTo(12,21);path.cubicTo(-7,9,7,-3,12,7);path.cubicTo(17,-3,31,9,12,21);c.drawPath(path,p);}
 else {c.drawLine(4,6,20,6,p);c.drawLine(4,12,20,12,p);c.drawLine(4,18,15,18,p);}c.restore();}
 public void setAlpha(int alpha){p.setAlpha(alpha);}public void setColorFilter(ColorFilter f){p.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
