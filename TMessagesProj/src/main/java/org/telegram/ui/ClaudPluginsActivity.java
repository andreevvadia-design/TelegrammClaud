package org.telegram.ui;

import android.app.Activity;
import android.content.*;
import android.view.View;
import android.view.Gravity;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.Components.ClaudPetView;
import android.widget.*;
import org.json.JSONObject;
import org.telegram.messenger.plugins.PythonPlugins;
import org.json.JSONArray;
import org.telegram.ui.ActionBar.*;
import java.io.File;
import java.io.FileOutputStream;

public class ClaudPluginsActivity extends BaseFragment {
    private static final int IMPORT = 7401;
    private String requestedPlugin;
    public ClaudPluginsActivity() { }
    public ClaudPluginsActivity(String plugin) { requestedPlugin = plugin; }

    private LinearLayout rows;
    private int refreshGeneration;
    private LinearLayout pluginRows;
    private ClaudPetView pet;
    private TextView speech;
    private String phrase = "Привет! Я claud. Давай добавим что-нибудь полезное.";
    private int phraseIndex;
    private final String[] phrases = {"Зажигаем новые идеи!", "Маленький плагин — большие возможности.", "Я слежу за порядком. Почти всегда.", "Люблю тёплые темы и хороший код.", "Тык! Я тоже рад тебя видеть.", "Давай настроим всё под тебя."};
    private int dp(float value) { return AndroidUtilities.dp(value); }
    private android.graphics.drawable.Drawable surface(int color, float radius) {
        GradientDrawable drawable = new GradientDrawable(); drawable.setColor(color); drawable.setCornerRadius(dp(radius)); return drawable;
    }
    private TextView label(Context c, String text, int size, int color) {
        TextView v = new TextView(c); v.setText(text); v.setTextSize(size); v.setTextColor(color); return v;
    }
    private TextView action(Context c, String text, boolean primary) {
        TextView v = label(c, text, 15, primary ? 0xFF211F1D : 0xFFE9C3AE);
        v.setTypeface(AndroidUtilities.bold()); v.setGravity(Gravity.CENTER); v.setMinHeight(dp(52));
        v.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), surface(primary ? 0xFFDA9775 : 0xFF302B27,16),null));
        v.setFocusable(true); v.setClickable(true); return v;
    }
    private void gap(LinearLayout parent, int height) { parent.addView(new View(parent.getContext()), new LinearLayout.LayoutParams(1,dp(height))); }
    private void say(String value) {
        if (value == null || value.isEmpty()) value = "Что-то пошло не так. Попробуем ещё раз?";
        phrase = value.length() > 120 ? value.substring(0,117)+"…" : value;
        if (speech != null) speech.setText(phrase);
        if (pet != null) pet.react();
    }
    @Override public void onResume() { super.onResume(); if (pet != null) pet.setActive(true); }
    @Override public void onPause() { if (pet != null) pet.setActive(false); super.onPause(); }
    @Override public void onFragmentDestroy() {
        refreshGeneration++; if (pet != null) pet.setActive(false); pet = null; speech = null; rows = null; pluginRows = null; super.onFragmentDestroy();
    }
    @Override public View createView(Context context) {
        actionBar.setTitle("Мастерская плагинов");
        actionBar.setBackgroundColor(0xFF211F1D);
        actionBar.setTitleColor(0xFFF2E6DC);
        actionBar.setItemsColor(0xFFE9C3AE, false);
        actionBar.setBackButtonImage(org.telegram.messenger.R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });
        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xFF211F1D);
        rows = new LinearLayout(context); rows.setOrientation(LinearLayout.VERTICAL);
        rows.setPadding(dp(20), dp(20), dp(20), dp(32)); scroll.addView(rows);
        fragmentView = scroll; refresh(); return fragmentView;
    }
    private void message(String text) {
        say(text);
        if (getParentActivity() != null) Toast.makeText(getParentActivity(), text, Toast.LENGTH_LONG).show();
    }
    private void refresh() {
        if (rows == null) return;
        final int generation = ++refreshGeneration;
        Context c = rows.getContext(); rows.removeAllViews();
        rows.setBackgroundColor(0xFF211F1D);
        TextView heading = label(c, "Сделай claud своим", 26, 0xFFF2E6DC); heading.setTypeface(AndroidUtilities.bold()); rows.addView(heading);
        gap(rows,6); rows.addView(label(c,"Твои плагины. Твои возможности.",14,0xFFB7A99E)); gap(rows,16);
        LinearLayout companion = new LinearLayout(c); companion.setGravity(Gravity.CENTER_VERTICAL);
        companion.setPadding(dp(12),dp(12),dp(16),dp(12)); companion.setBackground(surface(0xFF302923,20));
        pet = new ClaudPetView(c); companion.addView(pet,new LinearLayout.LayoutParams(dp(108),dp(106)));
        speech = label(c,phrase,15,0xFFF1DDD0); speech.setPadding(dp(10),0,0,0);
        companion.addView(speech,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1)); rows.addView(companion);
        View.OnClickListener greet = v -> say(phrases[(phraseIndex++) % phrases.length]); pet.setOnClickListener(greet); speech.setOnClickListener(greet);
        gap(rows,9);
        TextView petHome = action(c,"Клод · имя, одежда и игра",false); rows.addView(petHome);
        petHome.setOnClickListener(v -> org.telegram.ui.Components.ClaudPetOverlay.showHome(getParentActivity()));
        gap(rows,18);
        TextView install = action(c,"＋  Добавить плагин",true); rows.addView(install,new LinearLayout.LayoutParams(-1,dp(54)));
        gap(rows,9); rows.addView(label(c,"Файлы .plugin и .py · до 4 МБ",12,0xFFB7A99E));
        gap(rows,24);
        TextView library = label(c,"Мои плагины",18,0xFFF2E6DC); library.setTypeface(AndroidUtilities.bold()); rows.addView(library); gap(rows,12);
        pluginRows = new LinearLayout(c); pluginRows.setOrientation(LinearLayout.VERTICAL); rows.addView(pluginRows);
        install.setOnClickListener(v -> {
            say("Выбирай плагин — я подготовлю для него место.");
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*"); intent.addCategory(Intent.CATEGORY_OPENABLE);
            try { startActivityForResult(intent, IMPORT); }
            catch (Exception e) { message("Не удалось открыть выбор файлов"); }
        });
        gap(rows,24);
        rows.addView(label(c,"Плагины выполняются с правами приложения. Включай только код, которому доверяешь.",12,0xFFB7A99E)); gap(rows,12);
        TextView reset = action(c,"Выключить все после перезапуска",false); rows.addView(reset,new LinearLayout.LayoutParams(-1,dp(52)));
        reset.setOnClickListener(v -> {
            try {
                File dir = new File(c.getFilesDir(), "python_plugins");
                if (!dir.isDirectory() && !dir.mkdirs()) throw new java.io.IOException("Cannot create plugin directory");
                try (FileOutputStream output = new FileOutputStream(new File(dir, "disable_all.flag"))) { output.write(1); }
                message("После полного перезапуска приложения все плагины будут выключены");
            } catch (Exception e) { message(e.getMessage()); }
        });
        PythonPlugins.call(c, "listing", (result, error) -> {
            if (error != null) { message(error); return; }
            if (rows == null || generation != refreshGeneration || getParentActivity() == null) return;
            if (requestedPlugin != null) {
                String target = requestedPlugin; requestedPlugin = null;
                try {
                    JSONArray list = new JSONArray(result); boolean found = false;
                    for (int i=0;i<list.length();i++) {
                        JSONObject item=list.getJSONObject(i);
                        if(target.equals(item.optString("__id__"))) {
                            found=true;
                            if(item.optBoolean("enabled")) openSettings(target);
                            else message("Сначала включи Custom Profile в списке плагинов.");
                            break;
                        }
                    }
                    if(!found) message("Добавь custom_profile.plugin, затем включи его и открой настройки.");
                } catch(Exception e) { message(e.getMessage()); }
            }

            try {
                JSONArray list = new JSONArray(result);
                if (list.length() == 0) {
                    TextView empty = label(c,"Здесь пока тихо\nДобавь первый плагин кнопкой выше",15,0xFFB7A99E);
                    empty.setGravity(Gravity.CENTER); empty.setPadding(dp(20),dp(30),dp(20),dp(30)); empty.setBackground(surface(0xFF292521,18)); pluginRows.addView(empty);
                }
                for (int i = 0; i < list.length(); i++) {
                    JSONObject plugin = list.getJSONObject(i);
                    String pid = plugin.getString("__id__");
                    boolean enabled = plugin.optBoolean("enabled");
                    LinearLayout item = new LinearLayout(c); item.setOrientation(LinearLayout.VERTICAL);
                    item.setPadding(dp(18),dp(16),dp(18),dp(16));
                    item.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22DA9775),surface(0xFF302B27,18),null));
                    item.setFocusable(true); item.setContentDescription(plugin.optString("__name__")+(enabled ? ", включён" : ", выключен"));
                    TextView name = label(c,plugin.optString("__name__"),17,0xFFF2E6DC); name.setTypeface(AndroidUtilities.bold()); item.addView(name);
                    gap(item,5); item.addView(label(c,"v"+plugin.optString("__version__")+"  ·  "+(enabled ? "● Включён" : "○ Выключен"),12,enabled ? 0xFFDA9775 : 0xFFB7A99E));
                    String description = plugin.optString("__description__");
                    if (!description.isEmpty()) { gap(item,8); TextView desc = label(c,description,13,0xFFB7A99E); desc.setMaxLines(3); item.addView(desc); }
                    gap(item,10); item.addView(label(c,"Управление и настройки  ›",12,0xFFE9C3AE));
                    pluginRows.addView(item); gap(pluginRows,10);
                    item.setOnClickListener(v -> {
                        AlertDialog.Builder dialog = new AlertDialog.Builder(getParentActivity());
                        dialog.setTitle(plugin.optString("__name__"));
                        dialog.setItems(new CharSequence[]{enabled ? "Выключить" : "Включить", "Информация", "Удалить", "Настройки", "Действия плагина"}, (d, which) -> {
                            if (which == 4) {
                                PythonPlugins.uiCall(c,"menu_actions",(actionJson,actionError) -> {
                                    if(actionError!=null) { message(actionError); return; }
                                    try {
                                        JSONArray actions=new JSONArray(actionJson);
                                        if(actions.length()==0) { say("У этого плагина нет отдельных действий."); return; }
                                        CharSequence[] titles=new CharSequence[actions.length()];
                                        for(int a=0;a<actions.length();a++) titles[a]=actions.getJSONObject(a).getString("text");
                                        AlertDialog.Builder menu=new AlertDialog.Builder(getParentActivity()); menu.setTitle(plugin.optString("__name__"));
                                        menu.setItems(titles,(ad,choice) -> {
                                            try { PythonPlugins.uiCall(c,"menu_click",(answer,invokeError) -> { if(invokeError!=null) message(invokeError); },pid,actions.getJSONObject(choice).getString("id")); }
                                            catch(Exception e) { message(e.getMessage()); }
                                        }); showDialog(menu.create());
                                    } catch(Exception e) { message(e.getMessage()); }
                                },pid); return;
                            }
                            if (which == 3) { openSettings(pid); return; }
                            if (which == 1) {
                                AlertDialog.Builder details = new AlertDialog.Builder(getParentActivity());
                                details.setTitle(plugin.optString("__name__"));
                                details.setMessage(plugin.optString("__description__") + "\nID: " + pid + "\n" + plugin.optString("error"));
                                details.setPositiveButton("OK", null); showDialog(details.create()); return;
                            }
                            PythonPlugins.call(c, which == 2 ? "remove" : enabled ? "disable" : "enable", (answer, failure) -> {
                                if (failure != null) message(failure);
                                else say(which == 2 ? "Освободил место для новых идей." : enabled ? "Плагин отдыхает. Я пока на посту." : "Есть! Плагин включён, можно пробовать.");
                                if (getParentActivity() != null) refresh();
                            }, pid);
                        });
                        showDialog(dialog.create());
                    });
                }
            } catch (Exception e) { message(e.getMessage()); }
        });
    }

    private void openSettings(String pid) {
        if (getParentActivity() == null) return;
        PythonPlugins.uiCall(getParentActivity(), "settings_schema", (schema, loadError) -> {
            if (loadError != null) { message(loadError); return; }
            try { renderSettings(pid, new JSONArray(schema)); }
            catch (Exception e) { message(e.getMessage()); }
        }, pid);
    }
    private void clickSetting(String pid, int index, View target) {
        PythonPlugins.uiCall(getParentActivity(), "setting_click", (schema, clickError) -> {
            if (clickError != null) { message(clickError); return; }
            if (!schema.isEmpty()) try { renderSettings(pid,new JSONArray(schema)); }
            catch (Exception e) { message(e.getMessage()); }
        }, pid,index,target);
    }
    private void renderSettings(String pid, JSONArray schema) throws Exception {
        if (getParentActivity() == null) return;
        Context c = getParentActivity();
        LinearLayout form = new LinearLayout(c); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(20),dp(12),dp(20),dp(16));
        java.util.Map<Integer,View> fields = new java.util.LinkedHashMap<>();
        for (int i=0;i<schema.length();i++) {
            JSONObject row = schema.getJSONObject(i);
            String type=row.getString("type"); int index=row.getInt("index");
            if ("custom".equals(type)) {
                View custom = PythonPlugins.customView(pid,index,c);
                if (custom.getParent() instanceof android.view.ViewGroup) ((android.view.ViewGroup)custom.getParent()).removeView(custom);
                form.addView(custom); continue;
            }
            if ("header".equals(type) || "text".equals(type)) {
                TextView text = label(c,row.optString("text"),15,0xFFE9C3AE);
                text.setPadding(0,dp(12),0,dp(12)); form.addView(text);
                if ("text".equals(type)) {
                    if (!row.optString("subtext").isEmpty()) form.addView(label(c,row.optString("subtext"),12,0xFFB7A99E));
                    text.setOnClickListener(v -> clickSetting(pid,index,v));
                }
            } else if ("switch".equals(type)) {
                android.widget.Switch toggle = new android.widget.Switch(c); toggle.setMinHeight(dp(48));
                toggle.setText(row.optString("text")); toggle.setTextColor(0xFFF2E6DC); toggle.setChecked(row.optBoolean("value"));
                form.addView(toggle); fields.put(index,toggle);
            } else if ("selector".equals(type)) {
                form.addView(label(c,row.optString("text"),14,0xFFF2E6DC));
                Spinner selector = new Spinner(c); JSONArray options=row.getJSONArray("items");
                java.util.ArrayList<String> titles=new java.util.ArrayList<>();
                for(int j=0;j<options.length();j++) titles.add(options.getString(j));
                ArrayAdapter<String> adapter=new ArrayAdapter<>(c,android.R.layout.simple_spinner_dropdown_item,titles);
                selector.setAdapter(adapter); selector.setSelection(Math.max(0,Math.min(options.length()-1,row.optInt("value"))));
                form.addView(selector); fields.put(index,selector);
            } else {
                EditText editor=new EditText(c); editor.setHint(row.optString("text")); editor.setText(row.optString("value"));
                editor.setTextColor(0xFFF2E6DC); editor.setHintTextColor(0xFFB7A99E); form.addView(editor); fields.put(index,editor);
            }
        }
        ScrollView container=new ScrollView(c); container.addView(form);
        AlertDialog.Builder dialog=new AlertDialog.Builder(getParentActivity()); dialog.setTitle("Настройки плагина"); dialog.setView(container);
        dialog.setPositiveButton("Сохранить", (d,w) -> {
            for(java.util.Map.Entry<Integer,View> field:fields.entrySet()) {
                View control=field.getValue();
                Object value=control instanceof android.widget.Switch ? ((android.widget.Switch)control).isChecked()
                    : control instanceof Spinner ? ((Spinner)control).getSelectedItemPosition() : ((EditText)control).getText().toString();
                PythonPlugins.uiCall(c,"change_setting",(answer,saveError) -> { if(saveError!=null) message(saveError); },pid,field.getKey(),value);
            }
        });
        dialog.setNegativeButton("Отмена",null); showDialog(dialog.create());
    }

    @Override public void onActivityResultFragment(int request, int result, Intent data) {
        super.onActivityResultFragment(request, result, data);
        if (request != IMPORT || result != Activity.RESULT_OK || data == null || data.getData() == null) return;
        PythonPlugins.importFile(getParentActivity(), data.getData(), (answer, failure) -> {
            if (failure != null) message("Ошибка импорта: " + failure);
            else message("Плагин на месте! Включи его, когда будешь готов.");
            if (getParentActivity() != null) refresh();
        });
    }
}
