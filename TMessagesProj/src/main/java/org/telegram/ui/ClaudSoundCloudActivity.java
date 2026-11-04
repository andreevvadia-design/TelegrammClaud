package org.telegram.ui;
import android.content.Context;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.*;
import org.telegram.messenger.*;
import org.telegram.messenger.music.*;
import org.telegram.ui.ActionBar.*;
import org.telegram.ui.Components.ClaudMusicIcon;
import org.telegram.ui.Components.ClaudMusicStyle;
import java.util.*;

/** One ranked SoundCloud search; mixed results retain provider order across pages. */
public final class ClaudSoundCloudActivity extends BaseFragment {
 private EditText query;private TextView status,more;private RecyclerView results;private CatalogAdapter adapter;
 private int generation;private final String initial;private String activeQuery,next;private boolean loading,started;
 private final ArrayList<Object> items=new ArrayList<>();private final ArrayList<SoundCloudPublic.Track> tracks=new ArrayList<>();private final HashSet<String> ids=new HashSet<>(),requestedCursors=new HashSet<>();
 public ClaudSoundCloudActivity(String initial){this.initial=initial;}
 private int dp(float n){return AndroidUtilities.dp(n);}
 private TextView text(Context c,String value,int size){TextView v=new TextView(c);v.setText(value);v.setTextColor(0xFFF2E6DC);v.setTextSize(size);return v;}
 @Override public View createView(Context c){actionBar.setTitle("Поиск · SoundCloud");actionBar.setBackButtonImage(R.drawable.ic_ab_back);actionBar.setBackgroundColor(ClaudMusicStyle.BG);actionBar.setTitleColor(0xFFF2E6DC);actionBar.setItemsColor(0xFFE9C3AE,false);actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick(){public void onItemClick(int id){if(id==-1)finishFragment();}});
  LinearLayout box=new LinearLayout(c);box.setOrientation(1);box.setPadding(dp(16),dp(12),dp(16),0);box.setBackgroundColor(ClaudMusicStyle.BG);LinearLayout input=new LinearLayout(c);input.setGravity(Gravity.CENTER_VERTICAL);GradientDrawable bg=new GradientDrawable();bg.setColor(ClaudMusicStyle.SURFACE);bg.setCornerRadius(dp(12));input.setBackground(bg);box.addView(input);
  query=new EditText(c);query.setText(initial==null?"":initial);query.setHint("Треки, плейлисты, артисты");query.setPadding(dp(14),dp(12),0,dp(12));query.setBackground(null);query.setSingleLine(true);query.setTextSize(16);query.setTextColor(0xFFF2E6DC);query.setHintTextColor(0xFF9F9690);query.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(200)});query.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);input.addView(query,new LinearLayout.LayoutParams(0,dp(52),1));
  ImageView submit=new ImageView(c);submit.setImageDrawable(new ClaudMusicIcon("search",0xFFE9C3AE));submit.setPadding(dp(14),dp(14),dp(14),dp(14));submit.setContentDescription("Найти");input.addView(submit,new LinearLayout.LayoutParams(dp(52),dp(52)));submit.setOnClickListener(v->search());query.setOnEditorActionListener((v,action,event)->{if(action==android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH){search();return true;}return false;});
  status=text(c,"",13);status.setTextColor(0xFFA79C95);status.setPadding(0,dp(12),0,dp(8));box.addView(status);
  results=new RecyclerView(c);results.setLayoutManager(new LinearLayoutManager(c));adapter=new CatalogAdapter();results.setAdapter(adapter);results.addOnScrollListener(new RecyclerView.OnScrollListener(){public void onScrolled(RecyclerView view,int dx,int dy){LinearLayoutManager layout=(LinearLayoutManager)view.getLayoutManager();if(dy>0&&layout.findLastVisibleItemPosition()>=items.size()-6)loadNext();}});box.addView(results,new LinearLayout.LayoutParams(-1,0,1));
  more=text(c,"",14);more.setGravity(Gravity.CENTER);more.setPadding(dp(12),dp(12),dp(12),dp(12));more.setTextColor(0xFFE9C3AE);more.setVisibility(View.GONE);more.setOnClickListener(v->loadNext());box.addView(more);fragmentView=box;if(initial!=null&&!initial.trim().isEmpty())query.post(this::search);return box;
 }
 private void search(){String q=query.getText().toString().trim();if(q.isEmpty()){status.setText("Введите запрос");return;}AndroidUtilities.hideKeyboard(query);++generation;activeQuery=q;next=null;loading=false;started=false;items.clear();tracks.clear();ids.clear();requestedCursors.clear();adapter.notifyDataSetChanged();loadNext();}
 private String key(Object item){if(item instanceof SoundCloudPublic.Track)return "track:"+((SoundCloudPublic.Track)item).id;if(item instanceof SoundCloudPublic.Playlist)return "playlist:"+((SoundCloudPublic.Playlist)item).id;return "artist:"+((SoundCloudPublic.Artist)item).id;}
 private void loadNext(){if(results==null||loading||activeQuery==null||(started&&next==null))return;final int request=generation;final String cursor=next;if(cursor!=null&&requestedCursors.contains(cursor)){next=null;more.setVisibility(View.GONE);return;}loading=true;more.setVisibility(View.VISIBLE);more.setEnabled(false);more.setText("Загрузка…");status.setText(items.isEmpty()?"Поиск…":items.size()+" результатов");final String q=activeQuery;
  new Thread(()->{try{SoundCloudPublic.CatalogPage page=SoundCloudPublic.catalog("/search",q,cursor);AndroidUtilities.runOnUIThread(()->{if(request!=generation||results==null)return;loading=false;started=true;if(cursor!=null)requestedCursors.add(cursor);int first=items.size();for(Object item:page.items)if(ids.add(key(item))){items.add(item);if(item instanceof SoundCloudPublic.Track)tracks.add((SoundCloudPublic.Track)item);}next=page.next;int count=items.size()-first;if(count>0)adapter.notifyItemRangeInserted(first,count);status.setText(items.isEmpty()?"Ничего не найдено":items.size()+" результатов");more.setEnabled(next!=null);more.setVisibility(next==null?View.GONE:View.VISIBLE);more.setText("Ещё");if(next!=null&&(!results.canScrollVertically(1)||count==0))results.post(this::loadNext);});}catch(Exception e){AndroidUtilities.runOnUIThread(()->{if(request!=generation||results==null)return;loading=false;status.setText("Не удалось загрузить. Проверь соединение.");more.setVisibility(View.VISIBLE);more.setText("Повторить");more.setEnabled(true);});}},"claud-sc-search").start();
 }
 private final class Holder extends RecyclerView.ViewHolder{final TextView art,title,detail;Holder(LinearLayout row,TextView a,TextView t,TextView d){super(row);art=a;title=t;detail=d;}}
 private final class CatalogAdapter extends RecyclerView.Adapter<Holder>{
  public Holder onCreateViewHolder(ViewGroup parent,int type){Context c=parent.getContext();LinearLayout row=new LinearLayout(c);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,dp(8),0,dp(8));row.setLayoutParams(new RecyclerView.LayoutParams(-1,dp(76)));TextView art=text(c,"",24);art.setGravity(Gravity.CENTER);art.setClipToOutline(true);row.addView(art,new LinearLayout.LayoutParams(dp(56),dp(56)));LinearLayout labels=new LinearLayout(c);labels.setOrientation(1);labels.setPadding(dp(14),0,0,0);row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));TextView title=text(c,"",16),detail=text(c,"",13);title.setTypeface(android.graphics.Typeface.create("sans-serif-medium",0));title.setMaxLines(1);title.setEllipsize(android.text.TextUtils.TruncateAt.END);detail.setMaxLines(1);detail.setEllipsize(android.text.TextUtils.TruncateAt.END);detail.setTextColor(0xFFA79C95);detail.setPadding(0,dp(4),0,0);labels.addView(title);labels.addView(detail);return new Holder(row,art,title,detail);}
  public void onBindViewHolder(Holder h,int position){Object item=items.get(position);String title,detail,url;boolean artist=item instanceof SoundCloudPublic.Artist;
   h.itemView.setOnLongClickListener(null);
   if(item instanceof SoundCloudPublic.Track){SoundCloudPublic.Track t=(SoundCloudPublic.Track)item;title=t.title;detail="Трек · "+t.artist;url=t.artwork;h.itemView.setOnClickListener(v->{if(results==null)return;SoundCloudPlayback.play(v.getContext(),new ArrayList<>(tracks),t);presentFragment(new ClaudSoundCloudPlayerActivity());});h.itemView.setOnLongClickListener(v->{SoundCloudCollectionActions.track(ClaudSoundCloudActivity.this,t);return true;});}
   else if(item instanceof SoundCloudPublic.Playlist){SoundCloudPublic.Playlist p=(SoundCloudPublic.Playlist)item;title=p.title;detail="Плейлист · "+p.owner;url=p.artwork;h.itemView.setOnClickListener(v->presentFragment(new ClaudSoundCloudBrowseActivity("public",p.id,p.title)));}
   else{SoundCloudPublic.Artist a=(SoundCloudPublic.Artist)item;title=a.name;detail="Артист";url=a.avatar;h.itemView.setOnClickListener(v->presentFragment(new ClaudSoundCloudBrowseActivity("artist",a.id,a.name)));}
   h.title.setText(title);h.detail.setText(detail);h.art.setTag(null);h.art.setText("");h.art.setBackground(new ClaudMusicStyle.Cover());h.art.setOutlineProvider(new ViewOutlineProvider(){public void getOutline(View v,Outline o){o.setRoundRect(0,0,v.getWidth(),v.getHeight(),artist?v.getWidth()/2f:dp(8));}});SoundCloudArtwork.bind(h.art,ClaudMusicStyle.large(url));
  }
  public void onViewRecycled(Holder h){h.art.setTag(null);super.onViewRecycled(h);}public int getItemCount(){return items.size();}
 }
 @Override public void onFragmentDestroy(){++generation;results=null;status=null;more=null;super.onFragmentDestroy();}
}
