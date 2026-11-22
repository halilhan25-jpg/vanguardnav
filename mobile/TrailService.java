package tr.vanguardnav.field;
import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.*;
import android.os.*;
import com.getcapacitor.JSObject;
import org.json.*;
import java.util.UUID;
public class TrailService extends Service implements LocationListener {
 static volatile boolean active;
 private LocationManager manager;
 private Location previous;
 private boolean gap=true;
 static SQLiteDatabase db(Context c){SQLiteDatabase d=c.openOrCreateDatabase("trail.sqlite",0,null);d.execSQL("CREATE TABLE IF NOT EXISTS samples(t INTEGER PRIMARY KEY, lat REAL, lon REAL, gap INTEGER)");return d;}
 static synchronized void prepare(Context c){
  if(!c.getSharedPreferences("track",0).contains("id"))c.getSharedPreferences("track",0).edit().putString("id",UUID.randomUUID().toString()).commit();
 }
 static synchronized JSObject snapshot(Context c)throws Exception{
  JSObject o=new JSObject();JSONArray pts=new JSONArray(),br=new JSONArray();
  try(SQLiteDatabase d=db(c);Cursor cur=d.rawQuery("SELECT lat,lon,gap FROM samples ORDER BY t",null)){
   while(cur.moveToNext()){if(pts.length()>0&&cur.getInt(2)!=0)br.put(pts.length());pts.put(new JSONArray().put(cur.getDouble(0)).put(cur.getDouble(1)));}
  }
  o.put("points",pts);o.put("breaks",br);o.put("id",c.getSharedPreferences("track",0).getString("id",""));o.put("active",active);return o;
 }
 static synchronized boolean clear(Context c,String id){
  if(!id.equals(c.getSharedPreferences("track",0).getString("id","")))return false;
  try(SQLiteDatabase d=db(c)){d.delete("samples",null,null);}
  c.getSharedPreferences("track",0).edit().clear().commit();return true;
 }
 @Override public int onStartCommand(Intent i,int flags,int startId){
  NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
  Notification.Builder nb;
  if(Build.VERSION.SDK_INT>=26){nm.createNotificationChannel(new NotificationChannel("trail","Rota kaydı",NotificationManager.IMPORTANCE_LOW));nb=new Notification.Builder(this,"trail");}else nb=new Notification.Builder(this);
  PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  startForeground(41,nb.setSmallIcon(android.R.drawable.ic_menu_mylocation).setContentTitle("VanguardNav · GPS kaydı").setContentText("Kaydı durdurmak için uygulamayı aç.").setContentIntent(open).setOngoing(true).build());
  if(!active){try{manager=(LocationManager)getSystemService(LOCATION_SERVICE);manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,0,this);active=true;}catch(RuntimeException e){stopSelf();}}
  return START_NOT_STICKY;
 }
 @Override public void onLocationChanged(Location p){
  if(!active)return;if(!p.hasAccuracy()||p.getAccuracy()>25){gap=true;return;}
  if(previous!=null){if(p.getTime()<=previous.getTime())return;if(p.getTime()-previous.getTime()>20000)gap=true;if(!gap&&previous.distanceTo(p)<4)return;}
  synchronized(TrailService.class){try(SQLiteDatabase d=db(this)){d.execSQL("INSERT OR IGNORE INTO samples VALUES(?,?,?,?)",new Object[]{p.getTime(),p.getLatitude(),p.getLongitude(),gap?1:0});}}
  previous=p;gap=false;
 }
 @Override public void onProviderDisabled(String p){gap=true;}
 @Override public void onProviderEnabled(String p){gap=true;}
 @Override public void onStatusChanged(String p,int status,Bundle b){}
 @Override public void onDestroy(){active=false;if(manager!=null)manager.removeUpdates(this);stopForeground(true);super.onDestroy();}
 @Override public IBinder onBind(Intent i){return null;}
}
