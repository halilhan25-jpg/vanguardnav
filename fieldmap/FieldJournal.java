package app.organicmaps;
import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.*;
import android.net.Uri;
import android.widget.*;
import java.io.*;
import org.json.*;
import android.util.Base64;

/** Private offline fieldbook. Photos live in the same transactional database as their captions. */
public class FieldJournal extends Activity {
 SQLiteDatabase db; LinearLayout page; long entry; double lat,lon;
 EditText title,note; TextView status;
 @Override public void onCreate(Bundle state){super.onCreate(state);
  db=openOrCreateDatabase("fieldbook",0,null);
  db.execSQL("CREATE TABLE IF NOT EXISTS pins(id INTEGER PRIMARY KEY,lat REAL,lon REAL,title TEXT,note TEXT)");
  db.execSQL("CREATE TABLE IF NOT EXISTS photos(id INTEGER PRIMARY KEY,pin INTEGER,image BLOB,caption TEXT)");
  lat=getIntent().getDoubleExtra("lat",Double.NaN);lon=getIntent().getDoubleExtra("lon",Double.NaN);
  entry=state==null?0:state.getLong("entry");
  if(entry==0&&!Double.isNaN(lat))try(Cursor c=db.rawQuery("SELECT id FROM pins WHERE abs(lat-?)<0.00005 AND abs(lon-?)<0.00005 ORDER BY id DESC LIMIT 1",new String[]{""+lat,""+lon})){if(c.moveToFirst())entry=c.getLong(0);}
  if(entry>0)edit();else home();
 }
 @Override public void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putLong("entry",entry);}
 void screen(String heading){ScrollView scroll=new ScrollView(this);page=new LinearLayout(this);page.setOrientation(1);page.setPadding(24,24,24,24);scroll.addView(page);setContentView(scroll);TextView h=new TextView(this);h.setTextSize(22);h.setText(heading);page.addView(h);}
 void button(String label,Runnable action){Button b=new Button(this);b.setText(label);page.addView(b);b.setOnClickListener(v->action.run());}
 EditText input(String hint,String value){EditText e=new EditText(this);e.setHint(hint);e.setText(value);page.addView(e);return e;}
 void home(){entry=0;screen("Seyyah defterim");
  if(!Double.isNaN(lat))button("Bu konuma kayıt ekle",()->{ContentValues v=new ContentValues();v.put("lat",lat);v.put("lon",lon);v.put("title","Saha noktası");v.put("note","");entry=db.insertOrThrow("pins",null,v);app.organicmaps.sdk.bookmarks.data.BookmarkManager.INSTANCE.addNewBookmark(lat,lon);edit();});
  try(Cursor c=db.rawQuery("SELECT id,title FROM pins ORDER BY id DESC",null)){while(c.moveToNext()){long id=c.getLong(0);button(c.getString(1),()->{entry=id;edit();});}}
  button("Fotoğraflarla tam defter yedeği",()->startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").putExtra(Intent.EXTRA_TITLE,"VanguardNav-defter.json"),2));
  button("Defter yedeğini içe aktar",()->startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/json"),3));button("Haritaya dön",this::finish);
 }
 void save(){if(entry==0||title==null)return;ContentValues v=new ContentValues();v.put("title",title.getText().toString());v.put("note",note.getText().toString());db.update("pins",v,"id=?",new String[]{""+entry});}
 void edit(){try(Cursor c=db.rawQuery("SELECT title,note,lat,lon FROM pins WHERE id=?",new String[]{""+entry})){if(!c.moveToFirst()){home();return;}screen("Saha noktası");title=input("Nokta adı",c.getString(0));note=input("Gezi notu",c.getString(1));TextView coord=new TextView(this);coord.setText(c.getDouble(2)+", "+c.getDouble(3));page.addView(coord);}
  button("Kaydet",()->{save();home();});button("Fotoğraf ekle",()->{save();startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true),1);});
  try(Cursor c=db.rawQuery("SELECT id,image,caption FROM photos WHERE pin=?",new String[]{""+entry})){while(c.moveToNext()){long id=c.getLong(0);byte[] jpg=c.getBlob(1);ImageView im=new ImageView(this);BitmapFactory.Options o=new BitmapFactory.Options();o.inSampleSize=2;im.setImageBitmap(BitmapFactory.decodeByteArray(jpg,0,jpg.length,o));im.setAdjustViewBounds(true);page.addView(im);EditText cap=input("Fotoğraf notu",c.getString(2));cap.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){db.execSQL("UPDATE photos SET caption=? WHERE id=?",new Object[]{s.toString(),id});}public void afterTextChanged(android.text.Editable s){}});}}
 }
 @Override protected void onPause(){save();super.onPause();}
 void photo(Uri uri)throws Exception{BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;try(InputStream in=getContentResolver().openInputStream(uri)){BitmapFactory.decodeStream(in,null,o);}o.inSampleSize=1;while(Math.max(o.outWidth,o.outHeight)/o.inSampleSize>1600)o.inSampleSize*=2;o.inJustDecodeBounds=false;
  try(InputStream in=getContentResolver().openInputStream(uri)){Bitmap img=BitmapFactory.decodeStream(in,null,o);if(img==null)throw new IOException("Resim okunamadı");ByteArrayOutputStream b=new ByteArrayOutputStream();img.compress(Bitmap.CompressFormat.JPEG,78,b);img.recycle();db.execSQL("INSERT INTO photos(pin,image,caption) VALUES(?,?,?)",new Object[]{entry,b.toByteArray(),""});}}
 JSONObject backup()throws Exception{JSONArray pins=new JSONArray();try(Cursor c=db.rawQuery("SELECT id,lat,lon,title,note FROM pins",null)){while(c.moveToNext()){JSONArray pics=new JSONArray();try(Cursor f=db.rawQuery("SELECT image,caption FROM photos WHERE pin=?",new String[]{c.getString(0)})){while(f.moveToNext())pics.put(new JSONObject().put("data","data:image/jpeg;base64,"+Base64.encodeToString(f.getBlob(0),Base64.NO_WRAP)).put("caption",f.getString(1)));}pins.put(new JSONObject().put("point",new JSONArray().put(c.getDouble(1)).put(c.getDouble(2))).put("name",c.getString(3)).put("note",c.getString(4)).put("photos",pics));}}return new JSONObject().put("type","vanguardnav.journal").put("pins",pins);}
 void restore(JSONObject obj)throws Exception{String type=obj.optString("type");if(!type.equals("vanguardnav.journal")&&!type.equals("vanguardnav.fieldbook"))throw new IOException("Defter yedeği seç");JSONArray items=obj.getJSONArray("pins");db.beginTransaction();try{for(int j=0;j<items.length();j++){JSONObject p=items.getJSONObject(j);JSONArray a=p.getJSONArray("point");long id;try(Cursor c=db.rawQuery("SELECT id FROM pins WHERE lat=CAST(? AS REAL) AND lon=CAST(? AS REAL) AND title=? AND note=?",new String[]{a.getString(0),a.getString(1),p.getString("name"),p.getString("note")})){if(c.moveToFirst())id=c.getLong(0);else{ContentValues v=new ContentValues();v.put("lat",a.getDouble(0));v.put("lon",a.getDouble(1));v.put("title",p.getString("name"));v.put("note",p.getString("note"));id=db.insertOrThrow("pins",null,v);}}JSONArray pics=p.getJSONArray("photos");for(int k=0;k<pics.length();k++){JSONObject pic=pics.getJSONObject(k);String data=pic.getString("data");byte[] b=Base64.decode(data.substring(data.indexOf(',')+1),Base64.DEFAULT);db.execSQL("INSERT INTO photos(pin,image,caption) SELECT ?,?,? WHERE NOT EXISTS(SELECT 1 FROM photos WHERE pin=? AND image=? AND caption=?)",new Object[]{id,b,pic.getString("caption"),id,b,pic.getString("caption")});}}db.setTransactionSuccessful();}finally{db.endTransaction();}}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null)return;try{if(request==1){if(data.getClipData()!=null){for(int i=0;i<data.getClipData().getItemCount();i++)photo(data.getClipData().getItemAt(i).getUri());}else photo(data.getData());edit();}else if(request==2){try(Writer w=new OutputStreamWriter(getContentResolver().openOutputStream(data.getData()),"UTF-8")){w.write(backup().toString());}}else if(request==3){try(InputStream in=getContentResolver().openInputStream(data.getData());BufferedReader r=new BufferedReader(new InputStreamReader(in,"UTF-8"))){StringBuilder s=new StringBuilder();String line;while((line=r.readLine())!=null)s.append(line);restore(new JSONObject(s.toString()));}home();}}catch(Exception e){Toast.makeText(this,"İşlem tamamlanamadı: "+e.getMessage(),Toast.LENGTH_LONG).show();}}
}
