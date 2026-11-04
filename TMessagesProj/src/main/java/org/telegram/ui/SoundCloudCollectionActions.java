package org.telegram.ui;
import android.widget.*;
import java.util.*;
import org.telegram.ui.ActionBar.*;
import org.telegram.messenger.music.*;
public final class SoundCloudCollectionActions {
 public static void create(BaseFragment f,Runnable done){if(f.getParentActivity()==null)return;EditText name=new EditText(f.getParentActivity());name.setHint("Название плейлиста");name.setSingleLine(true);name.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(100)});new AlertDialog.Builder(f.getParentActivity()).setTitle("Новый плейлист").setView(name).setPositiveButton("Создать",(d,w)->{String n=name.getText().toString().trim();if(n.isEmpty())return;SoundCloudLibrary.create(f.getCurrentAccount(),n);if(done!=null)done.run();}).setNegativeButton("Отмена",null).show();}
 public static void add(BaseFragment f,SoundCloudPublic.Track track){if(track==null||f.getParentActivity()==null)return;LinkedHashMap<String,String> all=SoundCloudLibrary.playlists(f.getCurrentAccount());ArrayList<String> ids=new ArrayList<>(all.keySet()),names=new ArrayList<>(all.values());names.add("＋ Новый плейлист");new AlertDialog.Builder(f.getParentActivity()).setTitle("Добавить в плейлист").setItems(names.toArray(new String[0]),(d,w)->{if(w==ids.size()){create(f,()->add(f,track));return;}SoundCloudLibrary.add(f.getCurrentAccount(),ids.get(w),track);Toast.makeText(f.getParentActivity(),"Добавлено в «"+names.get(w)+"»",Toast.LENGTH_SHORT).show();}).setNegativeButton("Отмена",null).show();}
 public static void track(BaseFragment f,SoundCloudPublic.Track track){if(f.getParentActivity()==null)return;new AlertDialog.Builder(f.getParentActivity()).setTitle(track.title).setItems(new String[]{"Добавить в плейлист","Профиль автора в SoundCloud"},(d,w)->{if(w==0)add(f,track);else{SoundCloudPublic.Artist a=SoundCloudPublic.artistFor(track);if(a!=null)f.presentFragment(new ClaudSoundCloudBrowseActivity("artist",a.id,a.name));}}).show();}
}
