package org.telegram.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.ClaudPetOverlay;

public final class ClaudModeSettingsActivity extends BaseFragment {
    private void row(LinearLayout box,String title,String detail,Runnable action) {
        Context c=box.getContext(); LinearLayout card=new LinearLayout(c); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(18),dp(18),dp(18),dp(18));
        GradientDrawable bg=new GradientDrawable(); bg.setColor(0xFF302923); bg.setCornerRadius(dp(20)); card.setBackground(bg);
        TextView name=new TextView(c); name.setText(title+"  ›"); name.setTextSize(19); name.setTextColor(0xFFF2E6DC); name.setTypeface(AndroidUtilities.bold()); card.addView(name);
        TextView hint=new TextView(c); hint.setText(detail); hint.setTextSize(14); hint.setTextColor(0xFFC5B2A4); hint.setPadding(0,dp(7),0,0); card.addView(hint);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.bottomMargin=dp(14); box.addView(card,lp); card.setOnClickListener(v -> action.run()); card.setFocusable(true);
    }
    private int dp(float v) { return AndroidUtilities.dp(v); }
    @Override public View createView(Context c) {
        actionBar.setTitle("Настройки claud mode"); actionBar.setBackgroundColor(0xFF211F1D); actionBar.setTitleColor(0xFFF2E6DC); actionBar.setItemsColor(0xFFE9C3AE,false); actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick(){ @Override public void onItemClick(int id) { if(id==-1) finishFragment(); } });
        ScrollView scroll=new ScrollView(c); scroll.setFillViewport(true); scroll.setBackgroundColor(0xFF211F1D);
        LinearLayout box=new LinearLayout(c); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(20),dp(24),dp(20),dp(30)); scroll.addView(box);
        TextView heading=new TextView(c); heading.setText("Твой claud mode"); heading.setTextSize(27); heading.setTextColor(0xFFF2E6DC); heading.setTypeface(AndroidUtilities.bold()); heading.setPadding(0,0,0,dp(24)); box.addView(heading);
        row(box,"Плагины","Установка, управление и настройки расширений",() -> presentFragment(new ClaudPluginsActivity()));
        row(box,"Уведомления","Разрешение, проверка и восстановление доставки",() -> presentFragment(new ClaudNotificationsActivity()));
        row(box,"Питомец Клод","Имя, одежда, игра и положение на экране",() -> ClaudPetOverlay.showHome(getParentActivity()));
        row(box,"Оформление профиля","Custom Profile — оформление и эффекты",() -> presentFragment(new ClaudProfileActivity()));
        row(box,"Анимация сообщений","Message Motion · появление и отправка",() -> presentFragment(new ClaudProfileActivity("message_motion","Анимация сообщений")));
        row(box,"Анимация текста","Text Animation · эффекты текста",() -> presentFragment(new ClaudProfileActivity("text_animation_private_let","Анимация текста")));
        row(box,"Нижние вкладки","iExtera · оформление навигации",() -> presentFragment(new ClaudProfileActivity("liquid_bottom_tabs","Нижние вкладки")));
        row(box,"Выбор сообщений","Меню и выделение в стиле iOS",() -> presentFragment(new ClaudProfileActivity("ios_like_selection","Выбор сообщений")));
        row(box,"Подарки","Deleted Gift Sender · инструменты подарков",() -> presentFragment(new ClaudProfileActivity("deleted_gift_sender","Подарки")));
        fragmentView=scroll; return scroll;
    }
}
