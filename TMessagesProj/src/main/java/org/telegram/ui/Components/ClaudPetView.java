package org.telegram.ui.Components;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.view.View;
import android.view.accessibility.AccessibilityManager;

/** Code-drawn Claude companion with local outfits. Animation follows visibility. */
public final class ClaudPetView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator animator;
    private float phase;
    private long excitedUntil;
    private boolean active = true;
    private boolean motionAllowed = true;
    private int reaction = 1;
    private long lastFrameAt;
    private final android.content.SharedPreferences preferences;
    private String outfit;
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener preferenceListener=(prefs,key) -> {
        if("outfit".equals(key)){outfit=prefs.getString("outfit","none");invalidate();}
    };
    public ClaudPetView(Context context) {
        super(context);
        preferences=context.getApplicationContext().getSharedPreferences("claud_pet",0);
        outfit=preferences.getString("outfit","none");
        paint.setAntiAlias(true);
        setContentDescription("Питомец Клод. Нажми, чтобы открыть питомца");
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }
    public void react() {
        reaction = 1 + java.util.concurrent.ThreadLocalRandom.current().nextInt(4);
        excitedUntil = android.os.SystemClock.uptimeMillis() + 2400;
        invalidate();
    }
    public void setActive(boolean value) { if (active == value) return; active = value; updateAnimation(); }
    private void updateAnimation() {
        if (animator != null) { animator.cancel(); animator = null; }
        AccessibilityManager accessibility = (AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        boolean motion = !(Build.VERSION.SDK_INT >= 26 && !ValueAnimator.areAnimatorsEnabled());
        if (accessibility != null && accessibility.isTouchExplorationEnabled()) motion = false;
        motionAllowed = motion;
        if (!active || !isAttachedToWindow() || !isShown() || !hasWindowFocus() || !motion) return;
        animator = ValueAnimator.ofFloat(0, (float) Math.PI * 2);
        animator.setDuration(3000); animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new android.view.animation.LinearInterpolator());
        lastFrameAt=0;
        animator.addUpdateListener(a -> {
            long now=android.os.SystemClock.uptimeMillis();
            if(now-lastFrameAt<33)return;
            lastFrameAt=now;phase=(float)a.getAnimatedValue();invalidate();
        });
        animator.start();
    }
    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); outfit=preferences.getString("outfit","none");preferences.registerOnSharedPreferenceChangeListener(preferenceListener);updateAnimation(); }
    @Override protected void onDetachedFromWindow() { preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener);if(animator!=null){animator.cancel();animator=null;}super.onDetachedFromWindow(); }
    @Override protected void onWindowVisibilityChanged(int visibility) { super.onWindowVisibilityChanged(visibility); updateAnimation(); }
    @Override protected void onVisibilityChanged(View changed,int visibility){super.onVisibilityChanged(changed,visibility);updateAnimation();}
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);updateAnimation();}
    private void block(Canvas c, int color, float x, float y, float w, float h) {
        paint.setColor(color); c.drawRect(x,y,x+w,y+h,paint);
    }
    private void oval(Canvas c, int color, float l, float t, float r, float b) {
        paint.setColor(color); c.drawOval(l,t,r,b,paint);
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float unit = Math.min(getWidth()/100f, getHeight()/110f);
        boolean happy = android.os.SystemClock.uptimeMillis() < excitedUntil;
        int mood = motionAllowed ? (happy ? reaction : (int)((android.os.SystemClock.uptimeMillis()/9000)%6)) : 0;
        c.save(); c.translate((getWidth()-100*unit)/2,(getHeight()-110*unit)/2); c.scale(unit,unit);
        oval(c,0x22000000,23,98,78,104);
        float hop=mood==1 ? Math.abs((float)Math.sin(phase*2))*9 : 0;
        c.translate(mood==3?(float)Math.sin(phase*2)*4:0,(float)Math.sin(phase)*2-hop);
        if(mood==3)c.rotate((float)Math.sin(phase*2)*9,50,66);
        if(mood==5)c.scale(1,1+(float)Math.sin(phase)*.025f,50,75);
        c.save(); c.translate(0,16); c.scale(4.3f,4.3f); paint.setAntiAlias(false);
        int orange=0xFFDA9775, light=0xFFF1BD94, shade=0xFFAB6146;
        block(c,shade,6,7,12,7); block(c,orange,5,6,12,7); block(c,orange,7,5,8,1); block(c,light,7,6,7,1);
        float leg=mood==5?0:(float)Math.sin(phase*2)*.6f;
        block(c,shade,6,13,2,3+leg); block(c,orange,9,13,2,2-leg); block(c,shade,13,13,2,3-leg); block(c,orange,16,12,2,2+leg);
        block(c,orange,3,8,2,2); block(c,orange,2,6,2,3); block(c,light,1,5,2,2);
        float wave=mood==2?(float)Math.sin(phase*3)*1.7f:0;
        block(c,orange,17,8,2,2); block(c,orange,19,5+wave,2,4); block(c,light,19,4+wave,1,2); block(c,light,21,5+wave,1,2);
        boolean blink=mood==5 || phase>5.75f && phase<6.05f;
        block(c,0xFF211F1D,8,9,1,(blink || mood==4 && phase>2 && phase<4)?.35f:2); block(c,0xFF211F1D,13,9,1,blink?.35f:2);
        if(happy) { block(c,0xFF873E38,10,12,2,1); block(c,0xFFF8BDAC,7,11,1,1); block(c,0xFFF8BDAC,14,11,1,1); }
        c.restore(); paint.setAntiAlias(true);
        if ("scarf".equals(outfit)) {
            paint.setColor(0xFFCF7392); c.drawRoundRect(25,70,76,78,4,4,paint); c.drawRoundRect(65,72,74,94,3,3,paint);
        } else if ("hat".equals(outfit)) {
            paint.setColor(0xFF775C93); c.drawRoundRect(28,42,72,49,3,3,paint); c.drawRoundRect(36,26,64,46,6,6,paint);
            paint.setColor(0xFFE6B1CE); c.drawRect(36,38,64,42,paint);
        } else if ("glasses".equals(outfit)) {
            paint.setColor(0xFF392923); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(3);
            c.drawRoundRect(29,50,46,66,4,4,paint); c.drawRoundRect(54,50,71,66,4,4,paint); c.drawLine(46,56,54,56,paint); paint.setStyle(Paint.Style.FILL);
        }
        if(mood==5) { paint.setColor(0xFFE9C3AE); paint.setTextSize(12); c.drawText("z",79,29,paint); c.drawText("z",87,19,paint); }
        if(happy) { oval(c,0xFFFFCE81,10,26,14,30); oval(c,0xFFFFCE81,86,12,90,16); }
        c.restore();
    }
}
