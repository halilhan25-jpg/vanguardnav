package tr.vanguardnav.field;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.location.*;
import android.os.*;
import android.util.Base64;
import android.view.Surface;
import androidx.annotation.NonNull;
import androidx.car.app.*;
import androidx.car.app.model.*;
import androidx.car.app.navigation.model.*;
import androidx.lifecycle.*;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Silent POI exploration. The projected map uses the same saved areas as the phone. */
final class FieldMapScreen extends Screen implements SurfaceCallback, LocationListener, DefaultLifecycleObserver {
 private final Handler main=new Handler(Looper.getMainLooper());
 private final ExecutorService io=Executors.newSingleThreadExecutor();
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
 private final Rect visible=new Rect();
 private Surface surface;
 private double latitude=36.8969,longitude=30.7133,scale=256*Math.pow(2,15);
 private boolean follow=true,alive=true;
 private Location fix;
 private JSONArray roads=new JSONArray(),pins=new JSONArray();
 private List<PictureArea> images=new ArrayList<>();
 private final LocationManager gps;
 private final SharedPreferences prefs;
 private final SharedPreferences.OnSharedPreferenceChangeListener change=(p,k)->{if("version".equals(k))load();};
 private static final class PictureArea {
  Bitmap image;JSONArray bounds;
  PictureArea(Bitmap b,JSONArray r){image=b;bounds=r;}
 }
 FieldMapScreen(CarContext context){
  super(context);gps=(LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
  prefs=context.getSharedPreferences("car",0);getLifecycle().addObserver(this);
 }
 @Override public void onStart(@NonNull LifecycleOwner owner){
  getCarContext().getCarService(AppManager.class).setSurfaceCallback(this);
  prefs.registerOnSharedPreferenceChangeListener(change);load();
  try{Location last=gps.getLastKnownLocation(LocationManager.GPS_PROVIDER);if(last!=null)onLocationChanged(last);
   gps.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,1,this,Looper.getMainLooper());}catch(SecurityException|IllegalArgumentException ignored){}
 }
 @Override public void onStop(@NonNull LifecycleOwner owner){
  prefs.unregisterOnSharedPreferenceChangeListener(change);gps.removeUpdates(this);
  getCarContext().getCarService(AppManager.class).setSurfaceCallback(null);surface=null;
 }
 @Override public void onDestroy(@NonNull LifecycleOwner owner){alive=false;io.shutdownNow();}
 private void load(){io.execute(()->{
  try{
   JSONObject model=FieldTools.readMap(getCarContext());
   JSONArray areas=model.optJSONArray("regions");List<PictureArea> decoded=new ArrayList<>();
   if(areas!=null)for(int i=0;i<areas.length();i++){
    JSONObject ar=areas.getJSONObject(i);String url=ar.getString("data");byte[] bytes=Base64.decode(url.substring(url.indexOf(',')+1),Base64.DEFAULT);
    BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=2;
    Bitmap b=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);if(b!=null)decoded.add(new PictureArea(b,ar.getJSONArray("bounds")));
   }
   main.post(()->{if(!alive)return;images=decoded;roads=model.optJSONArray("roads");pins=model.optJSONArray("pins");draw();});
  }catch(Exception ignored){}
 });}
 private static double mx(double lon){return (lon+180)/360;}
 private static double my(double lat){double s=Math.sin(Math.toRadians(Math.max(-85.05,Math.min(85.05,lat))));return .5-Math.log((1+s)/(1-s))/(4*Math.PI);}
 private float px(double lon){double delta=mx(lon)-mx(longitude);return (float)((delta-Math.rint(delta))*scale);}
 private float py(double lat){return (float)((my(lat)-my(latitude))*scale);}
 @Override public void onSurfaceAvailable(@NonNull SurfaceContainer s){surface=s.getSurface();draw();}
 @Override public void onSurfaceDestroyed(@NonNull SurfaceContainer s){surface=null;}
 @Override public void onVisibleAreaChanged(@NonNull Rect r){visible.set(r);draw();}
 @Override public void onScroll(float dx,float dy){follow=false;longitude+=dx/scale*360;latitude=Math.toDegrees(Math.atan(Math.sinh(Math.PI*(1-2*(my(latitude)+dy/scale)))));draw();}
 @Override public void onScale(float x,float y,float factor){scale=Math.max(256*64,Math.min(256*262144,scale*factor));draw();}
 @Override public void onLocationChanged(Location loc){
  fix=loc;if(follow){latitude=loc.getLatitude();longitude=loc.getLongitude();}draw();
 }
 @Override public void onProviderDisabled(String name){fix=null;draw();}
 @Override public void onProviderEnabled(String name){}
 @Override public void onStatusChanged(String name,int status,Bundle b){}
 private void draw(){
  if(surface==null||!surface.isValid())return;
  Canvas c=null;
  try{c=surface.lockCanvas(null);c.drawColor(0xff152222);
   c.translate(visible.isEmpty()?c.getWidth()/2f:visible.exactCenterX(),visible.isEmpty()?c.getHeight()/2f:visible.exactCenterY());
   paint.setStyle(Paint.Style.FILL);paint.setColor(0xffffffff);
   for(PictureArea a:images){JSONArray sw=a.bounds.getJSONArray(0),ne=a.bounds.getJSONArray(1);
    c.drawBitmap(a.image,null,new RectF(px(sw.getDouble(1)),py(ne.getDouble(0)),px(ne.getDouble(1)),py(sw.getDouble(0))),paint);}
   if(roads!=null)for(int i=0;i<roads.length();i++){
    JSONObject r=roads.getJSONObject(i);JSONArray coords=r.getJSONArray("points"),breaks=r.optJSONArray("breaks");int bi=0;
    Path path=new Path();for(int j=0;j<coords.length();j++){JSONArray p=coords.getJSONArray(j);float x=px(p.getDouble(1)),y=py(p.getDouble(0));
     boolean gap=breaks!=null&&bi<breaks.length()&&breaks.getInt(bi)==j;if(gap)bi++;
     if(j==0||gap)path.moveTo(x,y);else path.lineTo(x,y);}
    boolean manual="manual".equals(r.optString("source"));paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(5);
    paint.setPathEffect(manual?new DashPathEffect(new float[]{12,7},0):null);
    paint.setColor(Color.parseColor(r.optString("color",manual?"#39c9ed":"#45d891")));c.drawPath(path,paint);
   }
   paint.setPathEffect(null);paint.setStyle(Paint.Style.FILL);paint.setTextSize(21);
   if(pins!=null)for(int i=0;i<pins.length();i++){JSONObject p=pins.getJSONObject(i);float x=px(p.getDouble("lng")),y=py(p.getDouble("lat"));paint.setColor(0xffffb332);c.drawCircle(x,y,7,paint);
    String name=p.getString("name");paint.setColor(0xff152222);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(4);c.drawText(name,x+12,y,paint);paint.setColor(Color.WHITE);paint.setStyle(Paint.Style.FILL);c.drawText(name,x+12,y,paint);}
   if(fix!=null){boolean fresh=System.currentTimeMillis()-fix.getTime()<60000;paint.setColor(Color.WHITE);float x=px(fix.getLongitude()),y=py(fix.getLatitude());c.drawCircle(x,y,12,paint);paint.setColor(fresh?0xff2685ff:0xff757575);c.drawCircle(x,y,8,paint);}
   if(!images.isEmpty()){paint.setTextSize(14);paint.setColor(Color.WHITE);c.drawText("EOX / Copernicus · Mapzen / AWS",-(visible.isEmpty()?c.getWidth():visible.width())/2f+8,(visible.isEmpty()?c.getHeight():visible.height())/2f-10,paint);}
  }catch(Exception ignored){}finally{if(c!=null&&surface!=null)try{surface.unlockCanvasAndPost(c);}catch(Exception ignored){}}
 }
 @NonNull @Override public Template onGetTemplate(){
  Action center=new Action.Builder().setTitle("Konumum").setOnClickListener(()->{follow=true;if(fix!=null)onLocationChanged(fix);}).build();
  Pane pane=new Pane.Builder().addRow(new Row.Builder().setTitle("Serbest navigasyon").addText("Kayıtlı yollar ve saha noktaları").build()).build();
  return new MapWithContentTemplate.Builder().setContentTemplate(new PaneTemplate.Builder(pane).build())
   .setActionStrip(new ActionStrip.Builder().addAction(Action.BACK).addAction(center).build())
   .setMapController(new MapController.Builder().setMapActionStrip(new ActionStrip.Builder().addAction(Action.PAN).build()).build()).build();
 }
}
